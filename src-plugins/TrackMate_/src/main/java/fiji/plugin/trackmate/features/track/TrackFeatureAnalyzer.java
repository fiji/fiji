package fiji.plugin.trackmate.features.track;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureAnalyzer;

public interface TrackFeatureAnalyzer<T extends RealType<T> & NativeType<T>> extends FeatureAnalyzer {
	
	/**
	 * Score a collection of tracks.
	 */
	public void process(final TrackMateModel<T> model);
	
}
