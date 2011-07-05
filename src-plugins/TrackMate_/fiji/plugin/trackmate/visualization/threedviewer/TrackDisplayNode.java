package fiji.plugin.trackmate.visualization.threedviewer;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij3d.ContentNode;
import ij3d.TimelapseListener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackDisplayNode extends ContentNode implements TimelapseListener {

	/** The model, needed to retrieve connectivity. */
	private TrackMateModel model;
	/** Hold the color and transparency of all spots for a given track. */
	protected List<Color> colors;

	private int displayDepth = TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;
	private int displayMode = TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;

	private int currentTimePoint = 0;

	/** Dictionary referencing the line vertices indexed by frame. */
	protected HashMap<Integer, ArrayList<Integer>> frameIndex = new HashMap<Integer, ArrayList<Integer>>();
	/** Dictionary referencing the line vertices corresponding to each edge. */
	protected HashMap<DefaultWeightedEdge, Integer> edgeIndex = new HashMap<DefaultWeightedEdge, Integer>();
	/** Primitive containing all lines representing track edges. */
	private LineArray line;



	/*
	 * CONSTRUCTOR
	 */

	public TrackDisplayNode(TrackMateModel model, List<Color> colors) {
		this.model = model;
		this.colors = colors;

		for(int frame : model.getFilteredSpots().keySet())
			frameIndex.put(frame, new ArrayList<Integer>());

		makeMeshes();
	}

	/*
	 * PUBLIC METHODS
	 */

	public void setTrackDisplayMode(int mode) {
		this.displayMode = mode;
		if (displayMode == TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE) {
			Color4f color = new Color4f();
			for (int i = 0; i < line.getVertexCount(); i++) {
				line.getColor(i, color);
				color.w = 1f;
				line.setColor(i, color);
			}
		}
	}

	public void setTrackDisplayDepth(int displayDepth) {
		this.displayDepth = displayDepth;
	}

	void refresh() {
		// Holder for passing values 
		Color4f color = new Color4f();
		switch(displayMode) {

		case TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE: {
			break;
		}

		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL: {
			float tp;
			int frameDist;
			for(int frame : frameIndex.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer index : frameIndex.get(frame)) {
					line.getColor(index, color);
					color.w = tp;
					line.setColor(index, color);
					line.setColor(index+1, color);
				}
			}
			break;
		}
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {
			float tp;
			int frameDist;
			for(int frame : frameIndex.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer index : frameIndex.get(frame)) {
					line.getColor(index, color);
					color.w = tp;
					line.setColor(index, color);
					line.setColor(index+1, color);
				}
			}
			break;
		}
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD: {
			float tp;
			int frameDist;
			for(int frame : frameIndex.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer index : frameIndex.get(frame)) {
					line.getColor(index, color);
					color.w = tp;
					line.setColor(index, color);
					line.setColor(index+1, color);
				}
			}
			break;
		}
		}

	}


	/**
	 * Set the color of the whole specified track.
	 */
	public void setColor(final Set<Spot> track, final Color color) {
		Set<DefaultWeightedEdge> edges;
		for(Spot spot : track) {
			edges = model.edgesOf(spot);
			for(DefaultWeightedEdge edge : edges)
				setColor(edge, color);
		}
	}

	/**
	 * Set the color of the given edge mesh.
	 */
	public void setColor(final DefaultWeightedEdge edge, final Color color) {		
		Color4f color4 = new Color4f(); 
		int index = edgeIndex.get(edge);
		line.getColor(index, color4);
		float[] val = color.getRGBColorComponents(null);
		color4.x = val[0];
		color4.y = val[1];
		color4.z = val[2];
		line.setColor(index, color4);
		line.setColor(index+1, color4);
	}

	/**
	 * Return the color of the specified edge mesh.
	 */
	public Color getColor(final DefaultWeightedEdge edge) {
		Color4f color = new Color4f();
		int index = edgeIndex.get(edge);
		line.getColor(index, color);
		return color.get();
	}


	/*
	 * TIMELAPSE LISTENER
	 */

	@Override
	public void timepointChanged(int timepoint) {
		this.currentTimePoint = timepoint;
		refresh();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void makeMeshes() {

		// All edges
		List<Set<DefaultWeightedEdge>> trackEdges = model.getTrackEdges();

		// Holder for coordinates (array ref will not be used, just its elements)
		float[] coordinates = new float[3];

		// One big object to display all edges
		line = new LineArray(2 * model.edgeSet().size(), LineArray.COORDINATES | LineArray.COLOR_4);
		line.setCapability(LineArray.ALLOW_COLOR_WRITE);

		int index = 0;
		for (int i = 0; i < trackEdges.size(); i++) {
			// Color
			final Color trackColor = colors.get(i);
			final Color4f color = new Color4f(trackColor);
			color.w = 1f; // opaque edge for now

			// Iterate over track edge
			for (DefaultWeightedEdge edge : trackEdges.get(i)) {
				// Find source and target
				Spot target = model.getEdgeTarget(edge);
				Spot source = model.getEdgeSource(edge);

				// Add coords and colors of each vertex
				line.setCoordinate(index, source.getPosition(coordinates));
				line.setColor(index, color);
				index++;
				line.setCoordinate(index, target.getPosition(coordinates));
				line.setColor(index, color);
				index++;

				// Keep refs
				edgeIndex.put(edge, index-2);
				frameIndex.get(model.getFilteredSpots().getFrame(source)).add(index-2);
			}
		}

		Appearance appearance = new Appearance();
		LineAttributes lineAtts = new LineAttributes(4f, LineAttributes.PATTERN_SOLID, true);
		appearance.setLineAttributes(lineAtts);
		Shape3D shape = new Shape3D(line, appearance);
		removeAllChildren();
		addChild(shape);

	}


	/*
	 * CONTENTNODE METHODS
	 */

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void channelsUpdated(boolean[] channels) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void colorUpdated(Color3f color) {}

	@Override
	public void eyePtChanged(View view) {}

	@Override
	public void getCenter(Tuple3d center) {
		double x = 0, y = 0, z = 0;
		for (Spot spot : model.getFilteredSpots()) {
			x += spot.getFeature(SpotFeature.POSITION_X);
			y += spot.getFeature(SpotFeature.POSITION_Y);
			z += spot.getFeature(SpotFeature.POSITION_Z);
		}
		int nspot = model.getFilteredSpots().getNSpots();
		x /= nspot;
		y /= nspot;
		z /= nspot;
		center.x = x;
		center.y = y;
		center.z = z;
	}

	@Override
	public void getMax(Tuple3d max) {
		double xmax = Double.NEGATIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		double zmax = Double.NEGATIVE_INFINITY;
		float radius;
		for (Spot spot : model.getFilteredSpots()) {
			radius = spot.getFeature(SpotFeature.RADIUS);
			if (xmax < spot.getFeature(SpotFeature.POSITION_X) + radius)
				xmax = spot.getFeature(SpotFeature.POSITION_X) + radius;
			if (ymax < spot.getFeature(SpotFeature.POSITION_Y) + radius)
				ymax = spot.getFeature(SpotFeature.POSITION_Y) + radius;
			if (zmax < spot.getFeature(SpotFeature.POSITION_Z) + radius)
				zmax = spot.getFeature(SpotFeature.POSITION_Z) + radius;
		}
		max.x = xmax;
		max.y = ymax;
		max.z = zmax;

	}

	@Override
	public void getMin(Tuple3d min) {
		double xmin = Double.POSITIVE_INFINITY;
		double ymin = Double.POSITIVE_INFINITY;
		double zmin = Double.POSITIVE_INFINITY;
		float radius;
		for (Spot spot : model.getFilteredSpots()) {
			radius = spot.getFeature(SpotFeature.RADIUS);
			if (xmin > spot.getFeature(SpotFeature.POSITION_X) - radius)
				xmin = spot.getFeature(SpotFeature.POSITION_X) - radius;
			if (ymin > spot.getFeature(SpotFeature.POSITION_Y) - radius)
				ymin = spot.getFeature(SpotFeature.POSITION_Y) - radius;
			if (zmin > spot.getFeature(SpotFeature.POSITION_Z) - radius)
				zmin = spot.getFeature(SpotFeature.POSITION_Z) - radius;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}

	@Override
	public float getVolume() {
		Point3d min = new Point3d();
		Point3d max = new Point3d();
		getMin(min);
		getMax(max);
		max.sub(min);
		return (float) (max.x * max.y * max.z);
	}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void shadeUpdated(boolean shaded) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void thresholdUpdated(int threshold) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void transparencyUpdated(float transparency) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void swapDisplayedData(String path, String name) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void restoreDisplayedData(String path, String name) {}

	/** Ignored for {@link TrackDisplayNode} */
	@Override
	public void clearDisplayedData() {}

}
