package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.algorithm.MultiThreadedBenchmarkAlgorithm;
import net.imglib2.multithreading.SimpleMultiThreading;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.SpotAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;

/**
 * A class dedicated to centralizing the calculation of the numerical features of spots,
 * through {@link SpotAnalyzer}s.  
 * @author Jean-Yves Tinevez - 2013
 *
 */
public class SpotFeatureCalculator extends MultiThreadedBenchmarkAlgorithm {

	private static final String BASE_ERROR_MSG = "[SpotFeatureCalculator] ";
	private final Settings settings;
	private final TrackMateModel model;

	public SpotFeatureCalculator(final TrackMateModel model, final Settings settings) {
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
	 * Calculates the spot features configured in the {@link Settings} 
	 * for all the spots of this model,
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Since a {@link SpotAnalyzer} can compute more than a feature
	 * at once, spots might received more data than required.
	 */
	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		
		// Clean
		model.getFeatureModel().clearSpotFeatures();
		
		// Declare what you do.
		for (SpotAnalyzerFactory<?> factory : settings.getSpotAnalyzerFactories()) {
			Collection<String> features = factory.getFeatures();
			Map<String, String> featureNames = factory.getFeatureNames();
			Map<String, String> featureShortNames = factory.getFeatureShortNames();
			Map<String, Dimension> featureDimensions = factory.getFeatureDimensions();
			model.getFeatureModel().declareSpotFeatures(features, featureNames, featureShortNames, featureDimensions);
		}
		
		// Do it.
		computeSpotFeaturesAgent(model.getSpots(), settings.getSpotAnalyzerFactories(), true);
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		return true;
	}

	/**
	 * Calculates all the spot features configured in the {@link Settings} object 
	 * for the specified spot collection. 
	 * Features are calculated for each spot, using their location, and the raw
	 * image. 
	 */
	public void computeSpotFeatures(final SpotCollection toCompute, boolean doLogIt) {
		List<SpotAnalyzerFactory<?>> spotFeatureAnalyzers = settings.getSpotAnalyzerFactories();
		computeSpotFeaturesAgent(toCompute, spotFeatureAnalyzers, doLogIt);
	}

	/**
	 * The method in charge of computing spot features with the given {@link SpotAnalyzer}s, for the
	 * given {@link SpotCollection}.
	 * @param toCompute
	 * @param analyzers
	 */
	private void computeSpotFeaturesAgent(final SpotCollection toCompute, final List<SpotAnalyzerFactory<?>> analyzerFactories, boolean doLogIt) {

		final Logger logger;
		if (doLogIt) {
			logger = model.getLogger();
		}
		else {
			logger = Logger.VOID_LOGGER;
		}

		// Can't compute any spot feature without an image to compute on.
		if (settings.imp == null)
			return;
		
		// Do it.
		final List<Integer> frameSet = new ArrayList<Integer>(toCompute.keySet());
		final int numFrames = frameSet.size();

		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		final Thread[] threads = SimpleMultiThreading.newThreads(numThreads);

		int tc = 0;
		if (settings != null && settings.detectorSettings != null) {
			// Try to extract it from detector settings target channel
			Map<String, Object> ds = settings.detectorSettings;
			Object obj = ds.get(KEY_TARGET_CHANNEL);
			if (null != obj && obj instanceof Integer) {
				tc = ((Integer) obj) - 1;
			}
		}
		final int targetChannel = tc;

		// Prepare the thread array
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread("TrackMate spot feature calculating thread " + (1 + ithread) + "/" + threads.length) {

				public void run() {

					for (int index = ai.getAndIncrement(); index < numFrames; index = ai.getAndIncrement()) {

						int frame = frameSet.get(index);
						for (SpotAnalyzerFactory<?> factory : analyzerFactories) {
							SpotAnalyzer<?> analyzer = factory.getAnalyzer(frame, targetChannel);
							analyzer.process();
						}

						logger.setProgress(progress.incrementAndGet() / (float) numFrames);
					} // Finished looping over frames
				}
			};
		}
		logger.setStatus("Calculating " + toCompute.getNSpots(false) + " spots features...");
		logger.setProgress(0);

		SimpleMultiThreading.startAndJoin(threads);

		logger.setProgress(1);
		logger.setStatus("");
	}

}
