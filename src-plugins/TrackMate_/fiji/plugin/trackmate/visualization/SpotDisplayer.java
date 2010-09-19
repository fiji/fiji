package fiji.plugin.trackmate.visualization;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.jfree.chart.renderer.InterpolatePaintScale;
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
	/** The track collections emanating from tracking. They are represented as a graph of Spot,
	 * which must be the same objects as for {@link #spots}. */
	protected SimpleGraph<Spot, DefaultEdge> trackGraph;
	/** The default color to paint the spots in. */ 
	protected Color color = DEFAULT_COLOR;
	/** If true, tracks will be displayed. */
	protected boolean displayTracks;
	/** The Spot lists emanating from segmentation, indexed by the frame index as Integer. */
	protected TreeMap<Integer, List<Spot>> spots;


	
	public void setDisplayTracks(boolean displayTrack) {
		this.displayTracks = displayTrack;
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
	
	
	/*
	 * PROTECTED METHODS
	 */
	
	public void setTrackGraph(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		this.trackGraph = trackGraph;
	}
	
	public void setSpots(TreeMap<Integer, List<Spot>> spots) {
		this.spots = spots;
	}
	
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
