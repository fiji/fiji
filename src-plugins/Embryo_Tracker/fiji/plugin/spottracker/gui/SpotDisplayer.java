package fiji.plugin.spottracker.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import fiji.plugin.spottracker.Feature;
import fiji.plugin.spottracker.Spot;

public abstract class SpotDisplayer {

	/** The default display radius. */
	protected static final float DEFAULT_DISPLAY_RADIUS = 5;
	
	/** The display radius. */
	protected float radius = DEFAULT_DISPLAY_RADIUS;
	/** The spot collections emanating from segmentation. */
	protected TreeMap<Integer,Collection<Spot>> spots;
	/** The default color to paint the spots in. */ 
	protected Color color = new Color(1f, 0, 1f);


	
	
	
	/*
	 * ABSTRACT METHODS
	 */
	
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
	
	/**
	 * Return the subset of spots of this displayer that satisfy the threshold conditions given
	 * in argument.
	 */
	protected TreeMap<Integer,Collection<Spot>> threshold(final Feature[] features, double[] thresholds, boolean[] isAboves) {
		if (null == features || null == thresholds || null == isAboves)
			return spots;
		
		double threshold;
		boolean isAbove;
		Feature feature;
		Float val;
		Collection<Spot> spotThisFrame;
		TreeMap<Integer,Collection<Spot>> spotsToshow = new TreeMap<Integer, Collection<Spot>>();

		for (int key : spots.keySet()) {
			
			spotThisFrame = spots.get(key);
			ArrayList<Spot> blobToShow = new ArrayList<Spot>(spotThisFrame);
			ArrayList<Spot> blobToHide = new ArrayList<Spot>(spotThisFrame.size());

			Spot blob;

			for (int i = 0; i < features.length; i++) {

				threshold = thresholds[i];
				feature = features[i];
				isAbove = isAboves[i];

				blobToHide.clear();
				if (isAbove) {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getFeature(feature);
						if (null == val)
							continue;
						if ( val < threshold) {
							blobToHide.add(blob);
						}
					}

				} else {
					for (int j = 0; j < blobToShow.size(); j++) {
						blob = blobToShow.get(j);
						val = blob.getFeature(feature);
						if (null == val)
							continue;
						if ( val > threshold) {
							blobToHide.add(blob); 
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
