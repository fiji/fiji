package fiji.plugin.trackmate.features;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;

/**
 * A class dedicated to centralizing the calculation of the numerical features of tracks,
 * through {@link TrackAnalyzer}s.  
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class TrackFeatureCalculator extends MultiThreadedBenchmarkAlgorithm {

	private static final String BASE_ERROR_MSG = "[TrackFeatureCalculator] ";
	private final Settings settings;
	private final TrackMateModel model;

	public TrackFeatureCalculator(final TrackMateModel model, final Settings settings) {
		this.settings = settings;
		this.model = model;
	}
	
	/*
	 * METHODS
	 */
	
	@Override
	public boolean checkInput() {
		if (null == model) {
			errorMessage = BASE_ERROR_MSG + "Model object is null.";
			return false;
		}
		if (null == settings) {
			errorMessage = BASE_ERROR_MSG + "Settings object is null.";
			return false;
		}
		return true;
	}
	
	/**
	 * Calculates the track features configured in the {@link Settings} 
	 * for all the tracks of this model.
	 */
	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		
		// Clean
		model.getFeatureModel().clearTrackFeatures();

		// Declare what you do.
		for (TrackAnalyzer analyzer : settings.getTrackAnalyzers()) {
			Collection<String> features = analyzer.getFeatures();
			Map<String, String> featureNames = analyzer.getFeatureNames();
			Map<String, String> featureShortNames = analyzer.getFeatureShortNames();
			Map<String, Dimension> featureDimensions = analyzer.getFeatureDimensions();
			model.getFeatureModel().declareTrackFeatures(features, featureNames, featureShortNames, featureDimensions);
		}
		
		// Do it.
		computeTrackFeaturesAgent(model.getTrackModel().trackIDs(false), settings.getTrackAnalyzers(), true);
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}
	
	/**
	 * Calculates all the track features configured in the {@link Settings} object 
	 * for the specified tracks. 
	 */
	public void computeTrackFeatures(final Collection<Integer> trackIDs, boolean doLogIt) {
		List<TrackAnalyzer> trackFeatureAnalyzers = settings.getTrackAnalyzers();
		computeTrackFeaturesAgent(trackIDs, trackFeatureAnalyzers, doLogIt);
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Calculate all features for the tracks with the given IDs.
	 */
	private void computeTrackFeaturesAgent(final Collection<Integer> trackIDs, final List<TrackAnalyzer> analyzers, boolean doLogIt) {
		final Logger logger = model.getLogger();
		if (doLogIt) {
			logger.log("Computing track features:\n", Logger.BLUE_COLOR);		
		}
		
		for (TrackAnalyzer analyzer : analyzers) {
			analyzer.setNumThreads(numThreads);
			if (analyzer.isLocal()) {
				analyzer.process(trackIDs);
			} else {
				analyzer.process(model.getTrackModel().trackIDs(false));
			}
			if (doLogIt)
				logger.log("  - " + analyzer.getKey() + " in " + analyzer.getProcessingTime() + " ms.\n");
		}
	}
}
