package fiji.plugin.trackmate.features.track;

import java.util.Set;

import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.TrackMateModel;

public interface TrackFeatureAnalyzer {
	
	/**
	 * Score a collection of tracks.
	 */
	public void process(final TrackMateModel model);
	
	/**
	 * Return the features this analyzer computes.
	 */
	public Set<TrackFeature> getFeatures();	
	
}
