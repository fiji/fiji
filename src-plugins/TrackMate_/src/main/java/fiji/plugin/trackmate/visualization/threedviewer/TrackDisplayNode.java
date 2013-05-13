package fiji.plugin.trackmate.visualization.threedviewer;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import ij3d.ContentNode;
import ij3d.TimelapseListener;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Appearance;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.jgrapht.graph.DefaultWeightedEdge;

public class TrackDisplayNode extends ContentNode implements TimelapseListener {

	/** The model, needed to retrieve connectivity. */
	private final TrackMateModel model;

	/** Hold the color and transparency of all spots for a given track. */
	private HashMap<Integer,Color> colors = new HashMap<Integer, Color>();

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
	private HashMap<Integer, HashMap<Integer, ArrayList<Integer> > > frameIndices;
	/** 
	 * Dictionary referencing the line vertices corresponding to each edge, for each track. 
	 */
	private Map<Integer, HashMap<DefaultWeightedEdge, Integer>> edgeIndices;

	/** 
	 * Primitives: one {@link LineArray} per track. 
	 */
	private Map<Integer, LineArray> lines;
	/**
	 * Switch used for display. Is the only child of this {@link ContentNode}.
	 */
	private Switch trackSwitch;
	/**
	 * Boolean set that controls the visibility of each tracks.
	 */
	private BitSet switchMask;
	/**
	 * Maps track IDs to their index in the switch mask. 
	 */
	private HashMap<Integer, Integer> switchMaskIndex;


	/*
	 * CONSTRUCTOR
	 */

	public TrackDisplayNode(TrackMateModel model) {
		this.model = model;

		this.trackSwitch = new Switch(Switch.CHILD_MASK);
		trackSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		this.switchMask = new BitSet();

		makeMeshes();
		setTrackVisible(model.getTrackModel().getFilteredTrackIDs());
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the visibility of the tracks which indices are given to true, and of 
	 * all other tracks to false. 
	 */
	public void setTrackVisible(Collection<Integer> trackIDs) {
		switchMask.set(0, model.getTrackModel().getNTracks(), false);
		for (Integer trackID : trackIDs) {
			int trackIndex = switchMaskIndex.get(trackID).intValue();
			switchMask.set(trackIndex, true);
		}
		trackSwitch.setChildMask(switchMask);
	}

	public void setTrackDisplayMode(int mode) {
		this.displayMode = mode;
		if (displayMode == TrackMateModelView.TRACK_DISPLAY_MODE_WHOLE) {
			Color4f color = new Color4f();
			for (Integer trackID : lines.keySet()) {
				LineArray line = lines.get(trackID);
					
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
			for (int frame : frameIndices.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
			for (int frame : frameIndices.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
			for (int frame : frameIndices.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
			for (int frame : frameIndices.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
			for (int frame : frameIndices.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f - (float) frameDist / displayDepth;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
			for (int frame : frameIndices.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 0f;
				else 
					tp = 1f;

				for (Integer trackID : lines.keySet()) {
					final LineArray line = lines.get(trackID);
					if (null == line) {
						continue;
					}	
					for (Integer index : frameIndices.get(frame).get(trackID)) {
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
	 * Sets the color of the given edge mesh.
	 */
	public void setColor(final DefaultWeightedEdge edge, final Color color) {
		// First, find to what track it belongs to
		int trackID = model.getTrackModel().getTrackIDOf(edge);

		// Set color of corresponding line primitive
		Color4f color4 = new Color4f(); 
		int index = edgeIndices.get(trackID).get(edge);
		final LineArray line = lines.get(trackID);
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
	 * Returns the color of the specified edge mesh.
	 */
	public Color getColor(final DefaultWeightedEdge edge) {
		// First, find to what track it belongs to
		int trackID = model.getTrackModel().getTrackIDOf(edge);
		// Retrieve color from index
		Color4f color = new Color4f();
		int index = edgeIndices.get(trackID).get(edge);
		LineArray line = lines.get(trackID);
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
		Map<Integer,Set<DefaultWeightedEdge>> trackEdges = model.getTrackModel().getTrackEdges();
		final int ntracks = trackEdges.size();

		// Instantiate refs fields
		final int nframes = model.getFilteredSpots().keySet().size();
		frameIndices = new HashMap<Integer, HashMap<Integer, ArrayList<Integer>>>(nframes, 1); // optimum
		for (int frameIndex : model.getFilteredSpots().keySet()) {
			frameIndices.put(frameIndex, new HashMap<Integer, ArrayList<Integer>>(ntracks));
			for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
				frameIndices.get(frameIndex).put(trackID, new ArrayList<Integer>());
			}
		}
		edgeIndices = new HashMap<Integer, HashMap<DefaultWeightedEdge,Integer>>(ntracks);
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			final int nedges = trackEdges.get(trackID).size();
			edgeIndices.put(trackID, new HashMap<DefaultWeightedEdge, Integer>(nedges, 1));
		}
		lines = new HashMap<Integer, LineArray>(ntracks);

		// Holder for coordinates (array ref will not be used, just its elements)
		double[] coordinates = new double[3];

		// Common line appearance
		Appearance appearance = new Appearance();
		LineAttributes lineAtts = new LineAttributes(4f, LineAttributes.PATTERN_SOLID, true);
		appearance.setLineAttributes(lineAtts);
		TransparencyAttributes transAtts = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.2f);
		appearance.setTransparencyAttributes(transAtts);
		RenderingAttributes renderingAtts = new RenderingAttributes();
		renderingAtts.setAlphaTestFunction(RenderingAttributes.GREATER_OR_EQUAL);
		renderingAtts.setAlphaTestValue(0.3f);
		appearance.setRenderingAttributes(renderingAtts);

		// Iterate over each track
		trackSwitch.removeAllChildren();
		switchMaskIndex = new HashMap<Integer, Integer>(ntracks);
		int trackIndex = 0;
		for (Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
			switchMaskIndex.put(trackID, trackIndex++);
			
			Set<DefaultWeightedEdge> track = trackEdges.get(trackID);

			if (track.size() == 0) {
				lines.put(trackID, null);
				trackSwitch.addChild(new Shape3D(null, appearance));
				continue;
			}
			
			// One line object to display all edges of one track
			LineArray line = new LineArray(2 * track.size(), LineArray.COORDINATES | LineArray.COLOR_4);
			line.setCapability(LineArray.ALLOW_COLOR_WRITE);

			// Color
			Color trackColor = colors.get(trackID);
			if (null == trackColor) {
				trackColor = TrackMateModelView.DEFAULT_COLOR;
			}
			final Color4f color = new Color4f(trackColor);
			color.w = 1f; // opaque edge for now

			// Iterate over track edge
			int edgeIndex = 0;
			for (DefaultWeightedEdge edge : track) {
				// Find source and target
				Spot target = model.getTrackModel().getEdgeTarget(edge);
				Spot source = model.getTrackModel().getEdgeSource(edge);

				// Add coords and colors of each vertex
				coordinates = new double[3];
				TMUtils.localize(source, coordinates);
				line.setCoordinate(edgeIndex, coordinates);
				line.setColor(edgeIndex, color);
				edgeIndex++;
				coordinates = new double[3];
				TMUtils.localize(target, coordinates);
				line.setCoordinate(edgeIndex, coordinates);
				line.setColor(edgeIndex, color);
				edgeIndex++;

				// Keep refs
				edgeIndices.get(trackID).put(edge, edgeIndex-2);
				int frame = model.getFilteredSpots().getFrame(source);
				frameIndices.get(frame).get(trackID).add(edgeIndex-2);

			} // Finished building this track's line

			// Add primitive to the switch and to the ref list
			lines.put(trackID, line);
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
			x += spot.getFeature(Spot.POSITION_X);
			y += spot.getFeature(Spot.POSITION_Y);
			z += spot.getFeature(Spot.POSITION_Z);
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
		double radius;
		for (Spot spot : model.getFilteredSpots()) {
			radius = spot.getFeature(Spot.RADIUS);
			if (xmax < spot.getFeature(Spot.POSITION_X) + radius)
				xmax = spot.getFeature(Spot.POSITION_X) + radius;
			if (ymax < spot.getFeature(Spot.POSITION_Y) + radius)
				ymax = spot.getFeature(Spot.POSITION_Y) + radius;
			if (zmax < spot.getFeature(Spot.POSITION_Z) + radius)
				zmax = spot.getFeature(Spot.POSITION_Z) + radius;
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
		double radius;
		for (Spot spot : model.getFilteredSpots()) {
			radius = spot.getFeature(Spot.RADIUS);
			if (xmin > spot.getFeature(Spot.POSITION_X) - radius)
				xmin = spot.getFeature(Spot.POSITION_X) - radius;
			if (ymin > spot.getFeature(Spot.POSITION_Y) - radius)
				ymin = spot.getFeature(Spot.POSITION_Y) - radius;
			if (zmin > spot.getFeature(Spot.POSITION_Z) - radius)
				zmin = spot.getFeature(Spot.POSITION_Z) - radius;
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
