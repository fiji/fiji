package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public abstract class SpotDisplayer {

	
	public static final TrackDisplayMode DEFAULT_TRACK_DISPLAY_MODE = TrackDisplayMode.ALL_WHOLE_TRACKS;
	public static final int DEFAULT_TRACK_DISPLAY_DEPTH 			= 10;
	
	/** The default display radius. */
	protected static final float DEFAULT_DISPLAY_RADIUS = 5;
	/** The default color. */
	protected static final Color DEFAULT_COLOR = new Color(1f, 0, 1f);
	/** The display radius. */
	protected float radius = DEFAULT_DISPLAY_RADIUS;
	
	/** The colorMap. */
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	/** The default color to paint the spots and tracks. */ 
	protected Color color = DEFAULT_COLOR;
	
	/** The track collections emanating from tracking. They are represented as a graph of Spot,
	 * which must be the same objects as for {@link #spots}. */
	protected SimpleGraph<Spot, DefaultEdge> trackGraph;
	/** The individual tracks contained in the {@link #trackGraph}. */ 
	protected List<Set<Spot>> tracks;
	/** The track colors. */
	protected Map<Set<Spot>, Color> trackColors;
	/** The Spot lists emanating from segmentation, indexed by the frame index as Integer. */
	protected TreeMap<Integer, List<Spot>> spots = new TreeMap<Integer, List<Spot>>();
	/** The subset of Spots retained from {@link #spots} for displaying. */
	protected TreeMap<Integer, List<Spot>> spotsToShow = new TreeMap<Integer, List<Spot>>();

	/** The display track mode. */
	protected TrackDisplayMode trackDisplayMode = DEFAULT_TRACK_DISPLAY_MODE;
	/** The display depth: how many track segments will be shown on the track display. */
	protected int trackDisplayDepth = DEFAULT_TRACK_DISPLAY_DEPTH;

	
	/*
	 * ENUMS
	 */
	
	public enum TrackDisplayMode {
		DO_NOT_DISPLAY,
		ALL_WHOLE_TRACKS,
		LOCAL_WHOLE_TRACKS,
		LOCAL_BACKWARD_TRACKS,
		LOCAL_FORWARD_TRACKS;
		
		@Override
		public String toString() {
			switch(this) {
			case DO_NOT_DISPLAY:
				return "Do not display";
			case ALL_WHOLE_TRACKS:
				return "Show all entire tracks";
			case LOCAL_WHOLE_TRACKS:
				return "Show current tracks";
			case LOCAL_BACKWARD_TRACKS:
				return "Show current tracks, only backward";
			case LOCAL_FORWARD_TRACKS:
				return "Show current tracks, only forward";
			}
			return "Not implemented";
		}
		
	}

	
	/*
	 * PUBLIC METHODS
	 */
	

	/**
	 * Set the display mode for tracks. The {@link #refresh()} method must be called to refresh
	 * the display.
	 */
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		this.trackDisplayMode = mode;
		this.trackDisplayDepth = displayDepth;
	}	
	
	public void setTrackGraph(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		this.trackGraph = trackGraph;
		this.tracks = new ConnectivityInspector<Spot, DefaultEdge>(trackGraph).connectedSets();
		this.trackColors = new HashMap<Set<Spot>, Color>(tracks.size());
		int counter = 0;
		int ntracks = tracks.size();
		for(Set<Spot> track : tracks) {
			trackColors.put(track, colorMap.getPaint((float) counter / (ntracks-1)));
			counter++;
		}
	}
	
	public void setSpots(TreeMap<Integer, List<Spot>> spots) {
		this.spots = spots;
	}
	
	public void setSpotsToShow(TreeMap<Integer, List<Spot>> spotsToShow) {
		this.spotsToShow = spotsToShow;
	}

	
	/*
	 * ABSTRACT METHODS
	 */
	
	/**
	 * Prepare this displayer and render it according to its concrete implementation.
	 */
	public abstract void render();
	
	/**
	 * Color all displayed spots according to the feature given. 
	 * If feature is <code>null</code>, then the default color is 
	 * used. The {@link #refresh()} method must be called for display.
	 */
	public abstract void setColorByFeature(final Feature feature);
	
	
	public abstract void refresh();
	
}
