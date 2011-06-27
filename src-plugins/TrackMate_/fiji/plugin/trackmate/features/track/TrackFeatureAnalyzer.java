package fiji.plugin.trackmate.features.track;

import java.util.Set;

import fiji.plugin.trackmate.TrackCollection;
import fiji.plugin.trackmate.TrackFeature;

public interface TrackFeatureAnalyzer {
	
	/**
	 * Score a collection of tracks.
	 */
	public void process(final TrackCollection tracks);
	
	/**
	 * Return the features this analyzer computes.
	 */
	public Set<TrackFeature> getFeatures();	
	
}
