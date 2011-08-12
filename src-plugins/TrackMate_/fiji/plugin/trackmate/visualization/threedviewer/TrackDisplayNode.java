package fiji.plugin.trackmate.visualization.threedviewer;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij3d.ContentNode;
import ij3d.TimelapseListener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackDisplayNode extends ContentNode implements TimelapseListener {

	/** The model, needed to retrieve connectivity. */
	protected TrackMateModel model;

	protected Map<String, Object> displaySettings;

	/** Hold the color and transparency of all spots for a given track. */
	protected HashMap<Integer,Color> colors;

	private int displayDepth = TrackMateModelView.DEFAULT_TRACK_DISPLAY_DEPTH;
	private int displayMode = TrackMateModelView.DEFAULT_TRACK_DISPLAY_MODE;

	private int currentTimePoint = 0;

	/** 
	 * Reference for each frame, then for each track, the line primitive indices of edges 
	 * present in that track and in that frame. <p>
	 * For instance
	 * <pre>
	 *  frameIndices.get(2).get(3) = { 5, 10 }
	 * </pre>
	 * indicates that in the frame number 2, the track number 3 has 2 edges, that 
	 * are represented in the {@link LineArray} primitive by points with indices 5 and 10.
	 */
	protected HashMap<Integer, ArrayList< ArrayList<Integer> > > frameIndices;
	/** 
	 * Dictionary referencing the line vertices corresponding to each edge, for each track. 
	 */
	protected ArrayList<HashMap<DefaultWeightedEdge, Integer>> edgeIndices;

	/** 
	 * Primitives: one {@link LineArray} per track. 
	 */
	protected ArrayList<LineArray> lines;
	/**
	 * Switch used for display. Is the only child of this {@link ContentNode}.
	 */
	protected Switch trackSwitch;
	/**
	 * Boolean set that controls the visibility of each tracks.
	 */
	protected BitSet switchMask;


	/*
	 * CONSTRUCTOR
	 */

	public TrackDisplayNode(TrackMateModel model, final Map<String, Object> displaySettings) {
		this.model = model;
		this.displaySettings = displaySettings;

		this.trackSwitch = new Switch(Switch.CHILD_MASK);
		trackSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		this.switchMask = new BitSet();

		computeTrackColors();
		makeMeshes();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the visibility of the tracks which indices are given to true, and of 
	 * all other tracks to false. 
	 */
	public void setTrackVisible(Iterable<Integer> trackIndices) {
		switchMask.set(0, model.getNTracks(), false);
		for (int trackIndex : trackIndices) {
			switchMask.set(trackIndex, true);
		}
		trackSwitch.setChildMask(switchMask);
		// Set color to tracks
		for (int i : colors.keySet()) {
			setColor(model.getTrackSpots(i), colors.get(i));
		}
	}

	public void computeTrackColors() {
		int ntracks = model.getNFilteredTracks();
		final InterpolatePaintScale colorMap = (InterpolatePaintScale) displaySettings.get(TrackMateModelView.KEY_COLORMAP);
		colors = new HashMap<Integer, Color>(ntracks);
		if (ntracks == 0) {
			// Not filtered yet
			ntracks = model.getNTracks();
			for(int i = 0; i < ntracks; i++) {
				colors.put(i, colorMap.getPaint((float) i / (ntracks-1)));
			}
		} else {
			Color defaultColor = (Color) displaySettings.get(TrackMateModelView.KEY_COLOR);
			for(int i = 0; i < model.getNTracks(); i++) {
				colors.put(i, defaultColor);
			}
			int index = 0;
			for(int i : model.getVisibleTrackIndices()) {
				colors.put(i, colorMap.getPaint((float) index / (ntracks-1)));
				index ++;
			}	
		}

	}

	public void setTrackDisplayMode(int mode) {
		this.displayMode = mode;
		if (displayMode == TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE) {
			Color4f color = new Color4f();
			for (int trackIndex = 0; trackIndex < lines.size(); trackIndex++) {
				LineArray line = lines.get(trackIndex);
				if (null == line) {
					continue;
				}
					
				for (int i = 0; i < line.getVertexCount(); i++) {
					line.getColor(i, color);
					color.w = 1f;
					line.setColor(i, color);
				}
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
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
				}
			}
			break;
		}
		
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_QUICK: {
			float tp;
			int frameDist;
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
				}
			}
			break;
		}
		
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD: {
			float tp;
			int frameDist;
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
				}
			}
			break;
		}
		
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_BACKWARD_QUICK: {
			float tp;
			int frameDist;
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
				}
			}
			break;
		}
		
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD: {
			float tp;
			int frameDist;
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
				}
			}
			break;
		}
		
		case TrackMateModelView.TRACK_DISPLAY_MODE_LOCAL_FORWARD_QUICK: {
			float tp;
			int frameDist;
			final int ntracks = model.getNTracks();
			for (int frame : frameIndices.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
					final LineArray line = lines.get(trackIndex);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackIndex)) {
						line.getColor(index, color);
						color.w = tp;
						line.setColor(index, color);
						line.setColor(index+1, color);
					}
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
		// First, find to what track it belongs to
		final int ntracks = model.getNTracks();
		int trackIndex = -1;
		for (int i = 0; i < ntracks; i++) {
			if (model.getTrackEdges(i).contains(edge)) {
				trackIndex = i;
				break;
			}
		} 
		if (trackIndex < 0)
			return;
		// Set color of corresponding line primitive
		Color4f color4 = new Color4f(); 
		int index = edgeIndices.get(trackIndex).get(edge);
		final LineArray line = lines.get(trackIndex);
		if (null == line) {
			return;
		}	
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
		// First, find to what track it belongs to
		final int ntracks = model.getNTracks();
		int trackIndex = -1;
		for (int i = 0; i < ntracks; i++) {
			if (model.getTrackEdges(i).contains(edge)) {
				trackIndex = i;
				break;
			}
		} 
		if (trackIndex < 0)
			return null;
		// Retrieve color from index
		Color4f color = new Color4f();
		int index = edgeIndices.get(trackIndex).get(edge);
		LineArray line = lines.get(trackIndex);
		if (null == line) {
			return null;
		}	
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

		// All edges of ALL tracks
		List<Set<DefaultWeightedEdge>> trackEdges = model.getTrackEdges();
		final int ntracks = trackEdges.size();

		// Instantiate refs fields
		final int nframes = model.getFilteredSpots().keySet().size();
		frameIndices = new HashMap<Integer, ArrayList< ArrayList<Integer>>>(nframes, 1); // optimum
		for (int frameIndex : model.getFilteredSpots().keySet()) {
			frameIndices.put(frameIndex, new ArrayList<ArrayList<Integer>>(ntracks));
			for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
				frameIndices.get(frameIndex).add(new ArrayList<Integer>());
			}
		}
		edgeIndices = new ArrayList<HashMap<DefaultWeightedEdge,Integer>>(ntracks);
		for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
			final int nedges = trackEdges.get(trackIndex).size();
			edgeIndices.add(new HashMap<DefaultWeightedEdge, Integer>(nedges, 1));
		}
		lines = new ArrayList<LineArray>(ntracks);

		// Holder for coordinates (array ref will not be used, just its elements)
		float[] coordinates = new float[3];

		// Common line appearance
		Appearance appearance = new Appearance();
		LineAttributes lineAtts = new LineAttributes(4f, LineAttributes.PATTERN_SOLID, true);
		appearance.setLineAttributes(lineAtts);

		// Iterate over each track
		trackSwitch.removeAllChildren();
		for (int trackIndex = 0; trackIndex < ntracks; trackIndex++) {
			Set<DefaultWeightedEdge> track = trackEdges.get(trackIndex);

			if (track.size() == 0) {
				lines.add(null);
				trackSwitch.addChild(new Shape3D(null, appearance));
				continue;
			}
			
			// One line object to display all edges of one track
			LineArray line = new LineArray(2 * track.size(), LineArray.COORDINATES | LineArray.COLOR_4);
			line.setCapability(LineArray.ALLOW_COLOR_WRITE);

			// Color
			final Color trackColor = colors.get(trackIndex);
			final Color4f color = new Color4f(trackColor);
			color.w = 1f; // opaque edge for now

			// Iterate over track edge
			int edgeIndex = 0;
			for (DefaultWeightedEdge edge : track) {
				// Find source and target
				Spot target = model.getEdgeTarget(edge);
				Spot source = model.getEdgeSource(edge);

				// Add coords and colors of each vertex
				line.setCoordinate(edgeIndex, source.getPosition(coordinates));
				line.setColor(edgeIndex, color);
				edgeIndex++;
				line.setCoordinate(edgeIndex, target.getPosition(coordinates));
				line.setColor(edgeIndex, color);
				edgeIndex++;

				// Keep refs
				edgeIndices.get(trackIndex).put(edge, edgeIndex-2);
				int frame = model.getFilteredSpots().getFrame(source);
				frameIndices.get(frame).get(trackIndex).add(edgeIndex-2);

			} // Finished building this track's line

			// Add primitive to the switch and to the ref list
			lines.add(line);
			trackSwitch.addChild(new Shape3D(line, appearance));

		} // Finish iterating over tracks

		// Add main switch to this content 
		switchMask = new BitSet(ntracks);
		switchMask.set(0, ntracks, true); // all visible
		trackSwitch.setChildMask(switchMask);
		removeAllChildren();
		addChild(trackSwitch);
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
