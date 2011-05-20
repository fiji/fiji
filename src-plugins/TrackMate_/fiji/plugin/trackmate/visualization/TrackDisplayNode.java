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

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.TransparencyAttributes;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class TrackDisplayNode extends ContentNode implements TimelapseListener {

	/** The graph containing the connectivity. */
	protected SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph;
	/** The spots indexed by frame. */
	protected SpotCollection spots;
	/** The list of tracks. */
	protected List<Set<Spot>> tracks;
	/** Hold the color and transparency of all spots for a given track. */
	protected Map<Set<Spot>, Color3f> colors;
	/** Switch used for display. Is the only child of this {@link ContentNode}.	 */
	protected Switch trackSwitch;
	/** Boolean set that controls the visibility of each mesh.	 */
	protected BitSet switchMask;
	/** Hold a reference of the meshes corresponding to each edge. */
	protected HashMap<DefaultWeightedEdge, Shape3D> edgeMeshes = new HashMap<DefaultWeightedEdge, Shape3D>();
	/** Hold a reference of the meshes indexed by frame. */
	protected HashMap<Integer, List<Shape3D>> frameMeshes = new HashMap<Integer, List<Shape3D>>();

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
			Map<Set<Spot>, Color3f> colors) {
		this.graph = graph;
		this.spots = spots;
		this.tracks = tracks;
		this.colors = colors;
		this.trackSwitch = new Switch(Switch.CHILD_MASK);
		trackSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_WRITE);
		trackSwitch.setCapability(Switch.ALLOW_CHILDREN_EXTEND);
		for(int frame : spots.keySet()) 
			frameMeshes.put(frame, new ArrayList<Shape3D>());
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
			for(Shape3D mesh : edgeMeshes.values())
				mesh.getAppearance().getTransparencyAttributes().setTransparency(0f);
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
				for(Shape3D mesh : frameMeshes.get(frame))
					mesh.getAppearance().getTransparencyAttributes().setTransparency(tp);
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
				for(Shape3D mesh : frameMeshes.get(frame))
					mesh.getAppearance().getTransparencyAttributes().setTransparency(tp);
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
				for(Shape3D mesh : frameMeshes.get(frame))
					mesh.getAppearance().getTransparencyAttributes().setTransparency(tp);
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
		edgeMeshes.get(edge).getAppearance().getColoringAttributes().setColor(color);
	}
	
	/**
	 * Return the color of the specified edge mesh.
	 */
	public Color3f getColor(final DefaultWeightedEdge edge) {
		Color3f color = new Color3f();
		edgeMeshes.get(edge).getAppearance().getColoringAttributes().getColor(color);
		return color;
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
		Shape3D mesh;
		Spot target, source;
		Set<Spot> parentTrack;
		LineAttributes atts = new LineAttributes(4f, LineAttributes.PATTERN_SOLID, true);

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
			
			// Create line and set common attributes
			mesh = makeMesh(source, target, colors.get(parentTrack));
			mesh.getAppearance().setLineAttributes(atts);
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
	
	
	private Shape3D makeMesh(final Spot source, final Spot target, final Color3f color) {
		double x0 = source.getFeature(Feature.POSITION_X);
		double x1 = target.getFeature(Feature.POSITION_X);
		double y0 = source.getFeature(Feature.POSITION_Y);
		double y1 = target.getFeature(Feature.POSITION_Y);
		double z0 = source.getFeature(Feature.POSITION_Z);
		double z1 = target.getFeature(Feature.POSITION_Z);
		
		LineArray line = new LineArray(2, LineArray.COORDINATES);
		Point3d p1 = new Point3d(new double[] {x0, y0, z0});
		Point3d p2 = new Point3d(new double[] {x1, y1, z1});
		line.setCoordinate(0, p1);
		line.setCoordinate(1, p2);
		
		Appearance appearance = new Appearance();
		ColoringAttributes coloringAttributes = new ColoringAttributes(color, ColoringAttributes.SHADE_FLAT);
		appearance.setColoringAttributes(coloringAttributes);
		TransparencyAttributes transpaAtt = new TransparencyAttributes(TransparencyAttributes.BLENDED, 0f);
		transpaAtt.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		appearance.setTransparencyAttributes(transpaAtt);
		Shape3D sh = new Shape3D(line, appearance);
		return sh;
	}
	
	
	/*
	 * CONTENTNODE METHODS
	 */
	
	
	@Override
	public void channelsUpdated(boolean[] channels) {}

	@Override
	public void colorUpdated(Color3f color) {
		for(Shape3D mesh : edgeMeshes.values())
			mesh.getAppearance().getColoringAttributes().setColor(color);
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
		return 0;
	}

	@Override
	public void shadeUpdated(boolean shaded) {
		int shadeModel;
		if (shaded) 
			shadeModel = ColoringAttributes.SHADE_GOURAUD;
		else 
			shadeModel = ColoringAttributes.SHADE_FLAT;
		for (Shape3D mesh : edgeMeshes.values())
			 mesh.getAppearance().getColoringAttributes().setShadeModel(shadeModel);
	}

	@Override
	public void thresholdUpdated(int threshold) {}

	@Override
	public void transparencyUpdated(float transparency) {}

	@Override
	public void lutUpdated(int[] r, int[] g, int[] b, int[] a) {}

	@Override
	public void swapDisplayedData(String path, String name) {}

	@Override
	public void restoreDisplayedData(String path, String name) {}

	@Override
	public void clearDisplayedData() {}

}
