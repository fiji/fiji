package fiji.plugin.trackmate.visualization;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;
import ij3d.ContentNode;
import ij3d.TimelapseListener;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.j3d.Switch;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3d;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import customnode.CustomTriangleMesh;
import customnode.MeshMaker;

public class TrackDisplayNode extends ContentNode implements TimelapseListener {

	private static final int DEFAULT_PARALLEL_NUMBER = 12;
	/** The track tube radius ratio (ratio of source radius). */
	protected final static double RADIUS_RATIO = 0.1;

	/** The graph containing the connectivity. */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	/** The spots indexed by frame. */
	protected SpotCollection spots;
	/** The list of tracks. */
	protected List<Set<Spot>> tracks;
	/** Hold the color and transparency of all spots for a given track. */
	protected Map<Set<Spot>, Color4f> colors;
	/** Switch used for display. Is the only child of this {@link ContentNode}.	 */
	protected Switch trackSwitch;
	/** Boolean set that controls the visibility of each mesh.	 */
	protected BitSet switchMask;
	/** Hold a reference of the meshes corresponding to each edge. */
	protected HashMap<DefaultWeightedEdge, CustomTriangleMesh> edgeMeshes = new HashMap<DefaultWeightedEdge, CustomTriangleMesh>();
	/** Hold a reference of the meshes indexed by frame. */
	protected HashMap<Integer, List<CustomTriangleMesh>> frameMeshes = new HashMap<Integer, List<CustomTriangleMesh>>();

	private TrackDisplayMode displayMode = TrackDisplayMode.ALL_WHOLE_TRACKS;

	private int displayDepth;

	private int currentTimePoint;
	
	
	 
	/*
	 * CONSTRUCTOR
	 */
	
	public TrackDisplayNode(
			SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, 
			SpotCollection spots, 
			List<Set<Spot>> tracks, 
			Map<Set<Spot>, Color4f> colors) {
		this.graph = graph;
		this.spots = spots;
		this.tracks = tracks;
		this.colors = colors;
		this.trackSwitch = new Switch(Switch.CHILD_MASK);
		trackSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		for(int frame : spots.keySet()) 
			frameMeshes.put(frame, new ArrayList<CustomTriangleMesh>());
		makeMeshes();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		this.displayMode = mode;
		this.displayDepth = displayDepth;
		refresh();
	}
	
	private void refresh() {
		
		switch(displayMode) {
		case ALL_WHOLE_TRACKS: {
			for(CustomTriangleMesh mesh : edgeMeshes.values())
				mesh.setTransparency(0);
			break;
		}
		case LOCAL_WHOLE_TRACKS: {
			float tp;
			int frameDist;
			for(int frame : frameMeshes.keySet()) {
				frameDist = Math.abs(frame - currentTimePoint); 
				if (frameDist > displayDepth)
					tp = 1;
				else 
					tp = (float) frameDist / displayDepth;
				for(CustomTriangleMesh mesh : frameMeshes.get(frame))
					mesh.setTransparency(tp);
			}
			break;
		}
		case LOCAL_FORWARD_TRACKS: {
			float tp;
			int frameDist;
			for(int frame : frameMeshes.keySet()) {
				frameDist = frame - currentTimePoint; 
				if (frameDist < 0 || frameDist > displayDepth)
					tp = 1;
				else 
					tp = (float) frameDist / displayDepth;
				for(CustomTriangleMesh mesh : frameMeshes.get(frame))
					mesh.setTransparency(tp);
			}
			break;
		}
		case LOCAL_BACKWARD_TRACKS: {
			float tp;
			int frameDist;
			for(int frame : frameMeshes.keySet()) {
				frameDist = currentTimePoint - frame; 
				if (frameDist <= 0 || frameDist > displayDepth)
					tp = 1;
				else 
					tp = (float) frameDist / displayDepth;
				for(CustomTriangleMesh mesh : frameMeshes.get(frame))
					mesh.setTransparency(tp);
			}
			break;
		}
		}
		
	}

	
	/**
	 * Set the color of the whole specified track.
	 */
	public void setColor(final Set<Spot> track, final Color3f color) {
		Set<DefaultWeightedEdge> edges;
		for(Spot spot : track) {
			edges = graph.edgesOf(spot);
			for(DefaultWeightedEdge edge : edges)
				setColor(edge, color);
		}
	}
	
	/**
	 * Set the color of the given edge mesh.
	 */
	public void setColor(final DefaultWeightedEdge edge, final Color3f color) {
		edgeMeshes.get(edge).setColor(color);
	}
	
	/**
	 * Return the color of the specified edge mesh.
	 */
	public Color3f getColor(final DefaultWeightedEdge edge) {
		return edgeMeshes.get(edge).getColor();
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
		
		CustomTriangleMesh mesh;
		Spot target, source;
		Set<Spot> parentTrack;
		
		Set<DefaultWeightedEdge> allEdges = graph.edgeSet();
		int index = 0;
		for(DefaultWeightedEdge edge : allEdges) {
			// Find source and target
			target = graph.getEdgeTarget(edge);
			source = graph.getEdgeSource(edge);
			// Find track it belongs to
			parentTrack = null;
			for (Set<Spot> track : tracks) 
				if (track.contains(source)) {
					parentTrack = track;
					break;
				}
			mesh = makeMesh(source, target, colors.get(parentTrack));
			// Store the individual mesh indexed by edge
			edgeMeshes.put(edge, mesh);
			// Store the mesh by frame index
			frameMeshes.get(spots.getFrame(source)).add(mesh);			
			// Add the tube to the content
			trackSwitch.addChild(mesh);
			index++;
			}
		switchMask = new BitSet(index);
		switchMask.set(0, index, true); // all visible
		trackSwitch.setChildMask(switchMask);
		removeAllChildren();
		addChild(trackSwitch);
	}
	
	
	private CustomTriangleMesh makeMesh(final Spot source, final Spot target, final Color4f color) {
		final float radius = source.getFeature(Feature.RADIUS);
		double[] x = new double[] { source.getFeature(Feature.POSITION_X), target.getFeature(Feature.POSITION_X) };
		double[] y = new double[] { source.getFeature(Feature.POSITION_Y), target.getFeature(Feature.POSITION_Y) };
		double[] z = new double[] { source.getFeature(Feature.POSITION_Z), target.getFeature(Feature.POSITION_Z) };
		double[] r = new double[] { radius * RADIUS_RATIO, radius * RADIUS_RATIO };
		// Avoid trouble if the source and target are at the same location
		if (x[0] == x[1] && y[0] == y[1] && z[0] == z[1])
			z[1] += radius/100;
		List<Point3f> points = MeshMaker.createTube(x, y, z, r, DEFAULT_PARALLEL_NUMBER, false);
		CustomTriangleMesh node = new CustomTriangleMesh(points, new Color3f(color.x, color.y, color.z), color.w);
		return node;
	}
	
	
	/*
	 * CONTENTNODE METHODS
	 */
	
	
	@Override
	public void channelsUpdated(boolean[] channels) {}

	@Override
	public void colorUpdated(Color3f color) {
		for(CustomTriangleMesh mesh : edgeMeshes.values())
			mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(View view) {}

	@Override
	public void getCenter(Tuple3d center) {
		double x = 0, y = 0, z = 0;
		for (Spot spot : graph.vertexSet()) {
			x += spot.getFeature(Feature.POSITION_X);
			y += spot.getFeature(Feature.POSITION_Y);
			z += spot.getFeature(Feature.POSITION_Z);
		}
		int nspot = graph.vertexSet().size();
		x /= nspot;
		y /= nspot;
		z /= nspot;
	}

	@Override
	public void getMax(Tuple3d max) {
		double xmax = Double.NEGATIVE_INFINITY;
		double ymax = Double.NEGATIVE_INFINITY;
		double zmax = Double.NEGATIVE_INFINITY;
		float radius;
		for (Spot spot : graph.vertexSet()) {
			radius = spot.getFeature(Feature.RADIUS);
			if (xmax < spot.getFeature(Feature.POSITION_X) + radius)
				xmax = spot.getFeature(Feature.POSITION_X) + radius;
			if (ymax < spot.getFeature(Feature.POSITION_Y) + radius)
				ymax = spot.getFeature(Feature.POSITION_Y) + radius;
			if (zmax < spot.getFeature(Feature.POSITION_Z) + radius)
				zmax = spot.getFeature(Feature.POSITION_Z) + radius;
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
		for (Spot spot : graph.vertexSet()) {
			radius = spot.getFeature(Feature.RADIUS);
			if (xmin > spot.getFeature(Feature.POSITION_X) - radius)
				xmin = spot.getFeature(Feature.POSITION_X) - radius;
			if (ymin > spot.getFeature(Feature.POSITION_Y) - radius)
				ymin = spot.getFeature(Feature.POSITION_Y) - radius;
			if (zmin > spot.getFeature(Feature.POSITION_Z) - radius)
				zmin = spot.getFeature(Feature.POSITION_Z) - radius;
		}
		min.x = xmin;
		min.y = ymin;
		min.z = zmin;
	}

	@Override
	public float getVolume() {
		float volume = 0;
		for (CustomTriangleMesh mesh : edgeMeshes.values()) 
			volume += mesh.getVolume();
		return volume;
	}

	@Override
	public void shadeUpdated(boolean shaded) {
		for (CustomTriangleMesh mesh : edgeMeshes.values())
			 mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(int threshold) {}

	@Override
	public void transparencyUpdated(float transparency) {
		for(CustomTriangleMesh mesh : edgeMeshes.values()) 
			mesh.setTransparency(transparency);
	}

	@Override
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {}

	@Override
	public void swapDisplayedData(String path, String name) {}

	@Override
	public void restoreDisplayedData(String path, String name) {}

	@Override
	public void clearDisplayedData() {}

}
