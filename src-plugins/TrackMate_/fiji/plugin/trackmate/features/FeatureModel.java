package fiji.plugin.trackmate.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.multithreading.SimpleMultiThreading;
import mpicbg.imglib.type.numeric.RealType;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
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
public class FeatureModel {

	/*
	 * FIELDS
	 */

	/** 
	 * The list of spot feature analyzer that will be used to compute spot features.
	 * Setting this field will automatically sets the derived fields: {@link #spotFeatures},
	 * {@link #spotFeatureNames}, {@link #spotFeatureShortNames} and {@link #spotFeatureDimensions}
	 * @see #updateFeatures(List) 
	 * @see #updateFeatures(Spot)
	 */
	protected List<SpotFeatureAnalyzer> spotFeatureAnalyzers = new ArrayList<SpotFeatureAnalyzer>();

	/** The list of spot features that are available for the spots of this model. */
	private List<String> spotFeatures;
	/** The map of the spot feature names. */
	private Map<String, String> spotFeatureNames;
	/** The map of the spot feature abbreviated names. */
	private Map<String, String> spotFeatureShortNames;
	/** The map of the spot feature dimension. */
	private Map<String, Dimension> spotFeatureDimensions;

	protected List<TrackFeatureAnalyzer> trackFeatureAnalyzers = new ArrayList<TrackFeatureAnalyzer>();
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
	protected List<Map<String, Float>> trackFeatureValues;

	private TrackMateModel model;


	/*
	 * CONSTRUCTOR
	 */

	public FeatureModel(TrackMateModel model) {
		this.model = model;
		// To initialize the spot features with the basic features:
		setSpotFeatureAnalyzers(new ArrayList<SpotFeatureAnalyzer>());
	}


	/*
	 * METHODS
	 */

	/*
	 * SPOT FEATURES
	 */
	

	/**
	 * Calculate given features for the all segmented spots of this model,
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
		final List<SpotFeatureAnalyzer> selectedAnalyzers = new ArrayList<SpotFeatureAnalyzer>(); // We want to keep ordering
		for (String feature : features) {
			for (SpotFeatureAnalyzer analyzer : spotFeatureAnalyzers) {
				if (analyzer.getFeatures().contains(feature) && !selectedAnalyzers.contains(analyzer)) {
					selectedAnalyzers.add(analyzer);
				}
			}
		}
		
		computeSpotFeaturesAgent(toCompute, selectedAnalyzers);
	}

	/**
	 * Calculate all features for the given spot collection.
	 * <p>
	 * Features are calculated for each spot, using their location, and the raw
	 * image. 
	 */
	public void computeSpotFeatures(final SpotCollection toCompute) {
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
	public void setSpotFeatureAnalyzers(List<SpotFeatureAnalyzer> featureAnalyzers) {
		this.spotFeatureAnalyzers = featureAnalyzers;

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
		for(SpotFeatureAnalyzer analyzer : spotFeatureAnalyzers) {
			spotFeatures.addAll(analyzer.getFeatures());
			spotFeatureNames.putAll(analyzer.getFeatureNames());
			spotFeatureShortNames.putAll(analyzer.getFeatureShortNames());
			spotFeatureDimensions.putAll(analyzer.getFeatureDimensions());
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
	public void setTrackFeatureAnalyzers(List<TrackFeatureAnalyzer> featureAnalyzers) {
		this.trackFeatureAnalyzers = featureAnalyzers;

		trackFeatures = new ArrayList<String>();
		trackFeatureNames = new HashMap<String, String>();
		trackFeatureShortNames = new HashMap<String, String>();
		trackFeatureDimensions = new HashMap<String, Dimension>();

		for(TrackFeatureAnalyzer analyzer : trackFeatureAnalyzers) {
			trackFeatures.addAll(analyzer.getFeatures());
			trackFeatureNames.putAll(analyzer.getFeatureNames());
			trackFeatureShortNames.putAll(analyzer.getFeatureShortNames());
			trackFeatureDimensions.putAll(analyzer.getFeatureDimensions());
		}
	}



	/**
	 * Return a map of feature values for the spot collection held
	 * by this instance. Each feature maps a double array, with 1 element per
	 * {@link Spot}, all pooled together.
	 */
	public Map<String, double[]> getSpotFeatureValues() {
		return TMUtils.getSpotFeatureValues(model.getSpots().values(), spotFeatures);
	}
	
	/**
	 * The method in charge of computing spot features with the given {@link SpotFeatureAnalyzer}s, fot the
	 * given {@link SpotCollection}.
	 * @param toCompute
	 * @param analyzers
	 */
	private void computeSpotFeaturesAgent(final SpotCollection toCompute, final List<SpotFeatureAnalyzer> analyzers) {

		final Settings settings = model.getSettings();
		final Logger logger = model.getLogger();
		
		// Can't compute any spot feature without an image to compute on.
		if (settings.imp == null)
			return;

		final List<Integer> frameSet = new ArrayList<Integer>(toCompute.keySet());
		final int numFrames = frameSet.size();
		final float[] calibration = settings.getCalibration();

		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger progress = new AtomicInteger(0);
		final Thread[] threads = SimpleMultiThreading.newThreads();

		
		/* Prepare stack for use with Imglib. This time, since the spot
		 * coordinates are with respect to the top-left corner of the image, we
		 * must not generate a cropped version of the image, but a full
		 * snapshot. */
		final Settings uncroppedSettings = new Settings();
		uncroppedSettings.xstart = 1;
		uncroppedSettings.xend = settings.imp.getWidth();
		uncroppedSettings.ystart = 1;
		uncroppedSettings.yend = settings.imp.getHeight();
		uncroppedSettings.zstart = 1;
		uncroppedSettings.zend = settings.imp.getNSlices();
		// Set the target channel for feature calculation. For now, we simple take the current one. // TODO: be more flexible 
		final int targetChannel = settings.imp.getChannel() - 1;

		// Prepare the thread array
		for (int ithread = 0; ithread < threads.length; ithread++) {

			threads[ithread] = new Thread(
					"TrackMate spot feature calculating thread " + (1 + ithread) + "/" + threads.length) {

				public void run() {

					for (int index = ai.getAndIncrement(); index < numFrames; index = ai.getAndIncrement()) {

						int frame = frameSet.get(index);
						List<Spot> spotsThisFrame = toCompute.get(frame);
						Image<? extends RealType<?>> img = TMUtils.getSingleFrameAsImage(settings.imp, frame, targetChannel, uncroppedSettings);

						for (SpotFeatureAnalyzer analyzer : analyzers) {
							analyzer.setTarget(img, calibration);
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

	public void putTrackFeature(final int trackIndex, final String feature, final Float value) {
		trackFeatureValues.get(trackIndex).put(feature, value);
	}

	public Float getTrackFeature(final int trackIndex, final String feature) {
		return trackFeatureValues.get(trackIndex).get(feature);
	}

	public Map<String, double[]> getTrackFeatureValues() {
		final Map<String, double[]> featureValues = new HashMap<String, double[]>();
		Float val;
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
	private void initFeatureMap() {
		this.trackFeatureValues = new ArrayList<Map<String, Float>>(model.getNTracks());
		for (int i = 0; i < model.getNTracks(); i++) {
			Map<String, Float> featureMap = new HashMap<String, Float>(trackFeatures.size());
			trackFeatureValues.add(featureMap);
		}
	}

	/**
	 * Calculate all features for the tracks in this model.
	 */
	public void computeTrackFeatures() {
		initFeatureMap();
		for (TrackFeatureAnalyzer analyzer : trackFeatureAnalyzers)
			analyzer.process(model);
	}


}
