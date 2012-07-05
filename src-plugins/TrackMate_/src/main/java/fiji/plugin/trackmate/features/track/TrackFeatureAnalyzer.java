package fiji.plugin.trackmate.features.track;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureAnalyzer;

public interface TrackFeatureAnalyzer extends FeatureAnalyzer {
	
	/**
	 * Score a collection of tracks.
	 */
	public void process(final TrackMateModel model);
	
}
