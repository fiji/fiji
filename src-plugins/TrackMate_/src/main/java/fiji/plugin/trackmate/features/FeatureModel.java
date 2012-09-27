package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeatureAnalyzerFactory;
import fiji.plugin.trackmate.TrackFeatureAnalyzerFactory;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * This class represents the part of the {@link TrackMateModel} that is in charge 
 * of dealing with spot features and track features.
 * @author Jean-Yves Tinevez, 2011
 *
 */
public class FeatureModel <T extends RealType<T> & NativeType<T>> {

	/*
	 * FIELDS
	 */

	/** The list of spot features that are available for the spots of this model. */
	private List<String> spotFeatures;
	/** The map of the spot feature names. */
	private Map<String, String> spotFeatureNames;
	/** The map of the spot feature abbreviated names. */
	private Map<String, String> spotFeatureShortNames;
	/** The map of the spot feature dimension. */
	private Map<String, Dimension> spotFeatureDimensions;

	private ArrayList<String> trackFeatures = new ArrayList<String>();
	private HashMap<String, String> trackFeatureNames = new HashMap<String, String>();
	private HashMap<String, String> trackFeatureShortNames = new HashMap<String, String>();
	private HashMap<String, Dimension> trackFeatureDimensions = new HashMap<String, Dimension>();
	/**
	 * Feature storage. We use a List of Map as a 2D Map. The list maps each
	 * track to its feature map. We use the same index that for
	 * {@link #trackEdges} and {@link #trackSpots}. The feature map maps each
	 * track feature to its float value for the selected track.
	 */
	protected List<Map<String, Double>> trackFeatureValues;

	private TrackMateModel<T> model;

	protected SpotFeatureAnalyzerFactory<T> spotAnalyzerFactory;

	protected TrackFeatureAnalyzerFactory<T> trackAnalyzerFactory;


	/*
	 * CONSTRUCTOR
	 */

	public FeatureModel(TrackMateModel<T> model) {
		this.model = model;
		// To initialize the spot features with the basic features:
		setSpotFeatureFactory(null);
	}


	/*
	 * METHODS
	 */

	/*
	 * SPOT FEATURES
	 */
	

	/**
	 * Calculate given features for the all detected spots of this model,
	 * according to the {@link Settings} set in the model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. Since a {@link SpotFeatureAnalyzer} can compute more than a feature
	 * at once, spots might received more data than required.
	 */
	public void computeSpotFeatures(final List<String> features) {
		computeSpotFeatures(model.getSpots(), features);
	}

	/**
	 * Calculate given features for the all filtered spots of this model,
	 * according to the {@link Settings} set in this model.
	 */
	public void computeSpotFeatures(final String feature) {
		ArrayList<String> features = new ArrayList<String>(1);
		features.add(feature);
		computeSpotFeatures(features);
	}

	/**
	 * Calculate given features for the given spots, according to the
	 * {@link Settings} set in this model.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * images. Since a {@link SpotFeatureAnalyzer} can compute more than a feature
	 * at once, spots might received more data than required.
	 */
	public void computeSpotFeatures(final SpotCollection toCompute, final List<String> features) {

		/* 0 - Determine what analyzers are needed */
		final List<SpotFeatureAnalyzer<T>> selectedAnalyzers = new ArrayList<SpotFeatureAnalyzer<T>>(); // We want to keep ordering
		for (String feature : features) {
			for (String analyzer : spotAnalyzerFactory.getAvailableSpotFeatureAnalyzers()) {
				if (spotAnalyzerFactory.getFeatures(analyzer).contains(feature) && !selectedAnalyzers.contains(analyzer)) {
					selectedAnalyzers.add(spotAnalyzerFactory.getSpotFeatureAnalyzer(analyzer));
				}
			}
		}
		
		computeSpotFeaturesAgent(toCompute, selectedAnalyzers);
	}

	/**
	 * Calculate all features for the given spot collection. Does nothing
	 * if the {@link #spotAnalyzerFactory} was not set, which is typically the 
	 * case when the iniModules() method of the plugin has not been called. 
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. 
	 */
	public void computeSpotFeatures(final SpotCollection toCompute) {
		if (null == spotAnalyzerFactory)
			return;
		List<String> analyzerNames = spotAnalyzerFactory.getAvailableSpotFeatureAnalyzers();
		List<SpotFeatureAnalyzer<T>> spotFeatureAnalyzers = new ArrayList<SpotFeatureAnalyzer<T>>(analyzerNames.size());
		for (String analyzerName : analyzerNames) {
			spotFeatureAnalyzers.add(spotAnalyzerFactory.getSpotFeatureAnalyzer(analyzerName));
		}
		computeSpotFeaturesAgent(toCompute, spotFeatureAnalyzers);
	}


	/** 
	 * Set the list of spot feature analyzers that will be used to compute spot features.
	 * Setting this field will automatically sets the derived fields: {@link #spotFeatures},
	 * {@link #spotFeatureNames}, {@link #spotFeatureShortNames} and {@link #spotFeatureDimensions}.
	 * These fields will be generated from the {@link SpotFeatureAnalyzer} content, returned 
	 * by its methods {@link SpotFeatureAnalyzer#getFeatures()}, etc... and will be added
	 * in the order given by the list.
	 * 
	 * @see #updateFeatures(List) 
	 * @see #updateFeatures(Spot)
	 */
	public void setSpotFeatureFactory(final SpotFeatureAnalyzerFactory<T> spotAnalyzerFactory) {
		this.spotAnalyzerFactory = spotAnalyzerFactory;

		spotFeatures = new ArrayList<String>();
		spotFeatureNames = new HashMap<String, String>();
		spotFeatureShortNames = new HashMap<String, String>();
		spotFeatureDimensions = new HashMap<String, Dimension>();

		// Add the basic features
		spotFeatures.addAll(Spot.FEATURES);
		spotFeatureNames.putAll(Spot.FEATURE_NAMES);
		spotFeatureShortNames.putAll(Spot.FEATURE_SHORT_NAMES);
		spotFeatureDimensions.putAll(Spot.FEATURE_DIMENSIONS);

		// Features from analyzers
		if (null != spotAnalyzerFactory) {
			List<String> analyzers = spotAnalyzerFactory.getAvailableSpotFeatureAnalyzers();
			for(String analyzer : analyzers) {
				spotFeatures.addAll(spotAnalyzerFactory.getFeatures(analyzer));
				spotFeatureNames.putAll(spotAnalyzerFactory.getFeatureNames(analyzer));
				spotFeatureShortNames.putAll(spotAnalyzerFactory.getFeatureShortNames(analyzer));
				spotFeatureDimensions.putAll(spotAnalyzerFactory.getFeatureDimensions(analyzer));
			}
		}
	}

	/**
	 * Return the list of the spot features that are dealt with in this model.
	 */
	public List<String> getSpotFeatures() {
		return spotFeatures;
	}

	/**
	 * Return the name mapping of the spot features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getSpotFeatureNames() {
		return spotFeatureNames;
	}

	/**
	 * Return the short name mapping of the spot features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getSpotFeatureShortNames() {
		return spotFeatureShortNames;
	}

	/**
	 * Return the dimension mapping of the spot features that are dealt with in this model.
	 * @return
	 */
	public Map<String, Dimension> getSpotFeatureDimensions() {
		return spotFeatureDimensions;
	}

	/** 
	 * Set the list of track feature analyzers that will be used to compute track features.
	 * Setting this field will automatically sets the derived fields: {@link #trackFeatures},
	 * {@link #trackFeatureNames}, {@link #trackFeatureShortNames} and {@link #trackFeatureDimensions}.
	 * These fields will be generated from the {@link TrackFeatureAnalyzer} content, returned 
	 * by its methods {@link TrackFeatureAnalyzer#getFeatures()}, etc... and will be added
	 * in the order given by the list.
	 * 
	 * @see #computeTrackFeatures()
	 */
	public void setTrackFeatureFactory(TrackFeatureAnalyzerFactory<T> trackAnalyzerFactory) {
		this.trackAnalyzerFactory = trackAnalyzerFactory;

		trackFeatures = new ArrayList<String>();
		trackFeatureNames = new HashMap<String, String>();
		trackFeatureShortNames = new HashMap<String, String>();
		trackFeatureDimensions = new HashMap<String, Dimension>();

		for (String analyzer : trackAnalyzerFactory.getAvailableTrackFeatureAnalyzers()) {
			trackFeatures.addAll(trackAnalyzerFactory.getFeatures(analyzer));
			trackFeatureNames.putAll(trackAnalyzerFactory.getFeatureNames(analyzer));
			trackFeatureShortNames.putAll(trackAnalyzerFactory.getFeatureShortNames(analyzer));
			trackFeatureDimensions.putAll(trackAnalyzerFactory.getFeatureDimensions(analyzer));
		}
	}



	/**
	 * Return a map of feature values for the spot collection held
	 * by this instance. Each feature maps a double array, with 1 element per
	 * {@link Spot}, all pooled together.
	 */
	public Map<String, double[]> getSpotFeatureValues() {
		return TMUtils.getSpotFeatureValues(model.getSpots(), spotFeatures, model.getLogger());
	}
	
	/**
	 * The method in charge of computing spot features with the given {@link SpotFeatureAnalyzer}s, for the
	 * given {@link SpotCollection}.
	 * @param toCompute
	 * @param analyzers
	 */
	private void computeSpotFeaturesAgent(final SpotCollection toCompute, final List<SpotFeatureAnalyzer<T>> analyzers) {

		final Settings<T> settings = model.getSettings();
		final Logger logger = model.getLogger();
		
		// Can't compute any spot feature without an image to compute on.
		if (settings.imp == null)
			return;

		final List<Integer> frameSet = new ArrayList<Integer>(toCompute.keySet());
		final int numFrames = frameSet.size();

		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		final Thread[] threads = SimpleMultiThreading.newThreads();

		
		/* Prepare stack for use with Imglib. This time, since the spot
		 * coordinates are with respect to the top-left corner of the image, we
		 * must not generate a cropped version of the image, but a full
		 * snapshot. */
	
		final ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(settings.imp);
		int targetChannel = 0;
		if (settings != null && settings.detectorSettings != null) {
			// Try to extract it from detector settings target channel
			Map<String, Object> ds = settings.detectorSettings;
			Object obj = ds.get(KEY_TARGET_CHANNEL);
			if (null != obj && obj instanceof Integer) {
				targetChannel = ((Integer) obj) - 1;
			}
		}
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);

		// Prepare the thread array
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread(
					"TrackMate spot feature calculating thread " + (1 + ithread) + "/" + threads.length) {

				@SuppressWarnings({ "rawtypes", "unchecked" })
				public void run() {

					for (int index = ai.getAndIncrement(); index < numFrames; index = ai.getAndIncrement()) {

						int frame = frameSet.get(index);
						List<Spot> spotsThisFrame = toCompute.get(frame);
						
						ImgPlus<T> imgCT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);

						for (SpotFeatureAnalyzer analyzer : analyzers) {
							analyzer.setTarget(imgCT);
							analyzer.process(spotsThisFrame);
						}


						logger.setProgress(progress.incrementAndGet() / (float) numFrames);
					} // Finished looping over frames
				}
			};
		}
		logger.setStatus("Calculating features...");
		logger.setProgress(0);

		SimpleMultiThreading.startAndJoin(threads);

		logger.setProgress(1);
		logger.setStatus("");
	}


	/*
	 * TRACK FEATURES
	 */
	


	/**
	 * Return the list of the track features that are dealt with in this model.
	 */
	public List<String> getTrackFeatures() {
		return trackFeatures;
	}

	/**
	 * Return the name mapping of the track features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getTrackFeatureNames() {
		return trackFeatureNames;
	}

	/**
	 * Return the short name mapping of the track features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getTrackFeatureShortNames() {
		return trackFeatureShortNames;
	}

	/**
	 * Return the dimension mapping of the track features that are dealt with in this model.
	 * @return
	 */
	public Map<String, Dimension> getTrackFeatureDimensions() {
		return trackFeatureDimensions;
	}

	public void putTrackFeature(final int trackIndex, final String feature, final Double value) {
		trackFeatureValues.get(trackIndex).put(feature, value);
		
		// FIXME We store the found feature name if it is not already in the feature name list 
		// This happens when we load a model from a file: the feature name list is empty, 
		// because it is normally initiated from the plugin.
		if (!trackFeatures.contains(feature)) {
			trackFeatures.add(feature);
		}
		
	}

	public Double getTrackFeature(final int trackIndex, final String feature) {
		if (trackIndex >= trackFeatureValues.size()) {
			return null; // Unknown track 
		}
		Map<String, Double> features = trackFeatureValues.get(trackIndex);
		if (null == features) {
			return null;
		}
		return features.get(feature);
	}

	public Map<String, double[]> getTrackFeatureValues() {
		final Map<String, double[]> featureValues = new HashMap<String, double[]>();
		Double val;
		int nTracks = model.getNTracks();
		for (String feature : trackFeatures) {
			// Make a double array to comply to JFreeChart histograms
			boolean noDataFlag = true;
			final double[] values = new double[nTracks];
			for (int i = 0; i < nTracks; i++) {
				val = getTrackFeature(i, feature);
				if (null == val)
					continue;
				values[i] = val;
				noDataFlag = false;
			}

			if (noDataFlag)
				featureValues.put(feature, null);
			else
				featureValues.put(feature, values);
		}
		return featureValues;
	}
	
	/**
	 * Instantiate an empty feature 2D map.
	 */
	public void initFeatureMap() {
		this.trackFeatureValues = new ArrayList<Map<String, Double>>(model.getNTracks());
		for (int i = 0; i < model.getNTracks(); i++) {
			Map<String, Double> featureMap = new HashMap<String, Double>(trackFeatures.size());
			trackFeatureValues.add(featureMap);
		}
	}

	/**
	 * Calculate all features for the tracks in this model.
	 */
	public void computeTrackFeatures() {
		initFeatureMap();
		for (String analyzer : trackAnalyzerFactory.getAvailableTrackFeatureAnalyzers())
			trackAnalyzerFactory.getTrackFeatureAnalyzer(analyzer).process(model);
	}


}
