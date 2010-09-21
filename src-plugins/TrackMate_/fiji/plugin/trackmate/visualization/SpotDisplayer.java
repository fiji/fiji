package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.ArrayList;
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
	protected TreeMap<Integer, List<Spot>> spots;

	/** The display track mode. */
	protected TrackDisplayMode trackDisplayMode = TrackDisplayMode.DO_NOT_DISPLAY;
	/** The display depth: how many track segments will be shown on the track display. */
	protected int trackDisplayDepth = Integer.MAX_VALUE; // by default, show all	

	
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
	
	
	public void setDisplayTrackMode(TrackDisplayMode mode, int displayDepth) {
		this.trackDisplayMode = mode;
		this.trackDisplayDepth = displayDepth;
		refresh();
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
	 * used.
	 */
	public abstract void setColorByFeature(final Feature feature);
	
	/**
	 * Change the visibility of each spot according to the thresholds specified in argument.
	 */
	public abstract void refresh(final Feature[] features, double[] thresholds, boolean[] isAboves);
	
	/**
	 * Make all spots visible.
	 */
	public abstract void resetTresholds();
	
	
	public abstract void refresh();
	
	/*
	 * PROTECTED METHODS
	 */
	
	/**
	 * Return the subset of spots of this displayer that satisfy the threshold conditions given
	 * in argument. The collection returned is a new instance.
	 */
	protected TreeMap<Integer,List<Spot>> threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		if (null == features || null == thresholds || null == isAboves)
			return spots;
		
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
		List<Spot> spotThisFrame;
		TreeMap<Integer, List<Spot>> spotsToshow = new TreeMap<Integer, List<Spot>>();

		for (int key : spots.keySet()) {
			
			spotThisFrame = spots.get(key);
			ArrayList<Spot> blobToShow = new ArrayList<Spot>(spotThisFrame);
			ArrayList<Spot> blobToHide = new ArrayList<Spot>(spotThisFrame.size());
			Spot spot;

			for (int i = 0; i < features.length; i++) {

				threshold = thresholds[i];
				feature = features[i];
				isAbove = isAboves[i];

				blobToHide.clear();
				if (isAbove) {
					for (int j = 0; j < blobToShow.size(); j++) {
						spot = blobToShow.get(j);
						val = spot.getFeature(feature);
						if (null == val)
							continue;
						if ( val < threshold) {
							blobToHide.add(spot);
						}
					}

				} else {
					for (int j = 0; j < blobToShow.size(); j++) {
						spot = blobToShow.get(j);
						val = spot.getFeature(feature);
						if (null == val)
							continue;
						if ( val > threshold) {
							blobToHide.add(spot); 
						}
					}

				}
				blobToShow.removeAll(blobToHide); // no need to treat them multiple times
			} // loop over features to threshold
			spotsToshow.put(key, blobToShow);
		} // loop over time points
		return spotsToshow;
	}


	
}
