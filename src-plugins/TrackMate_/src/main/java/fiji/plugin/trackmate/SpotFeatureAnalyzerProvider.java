package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures(String)}. The names and dimension of these 
 * features are also specified in 3 maps: {@link #getFeatureNames(String)}, {@link #getFeatureShortNames(String)}
 * and {@link #getFeatureDimensions(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class SpotFeatureAnalyzerProvider <T extends RealType<T> & NativeType<T>> {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant spot analyzer classes.  */
	protected List<String> names;
	protected Map<String, Map<String, String>> featureNames;
	protected Map<String, List<String>> features;
	protected Map<String, Map<String, String>> featureShortNames;
	protected Map<String, Map<String, Dimension>> featureDimensions;
	protected final TrackMateModel<T> model;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model spotFeatureAnalyzers currently available in the 
	 * TrackMate plugin. Each spotFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom spotFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public SpotFeatureAnalyzerProvider(TrackMateModel<T> model) {
		this.model = model;
		registerSpotFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerSpotFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(4);
		names.add(SpotIntensityAnalyzerFactory.KEY);
		names.add(SpotContrastAndSNRAnalyzerFactory.KEY); // must be after the statistics one
		names.add(SpotRadiusEstimatorFactory.KEY);
//		names.add(SpotMorphologyAnalyzerFactory.KEY);
		// features
		features = new HashMap<String, List<String>>();
		features.put(SpotIntensityAnalyzerFactory.KEY, SpotIntensityAnalyzerFactory.FEATURES);
		features.put(SpotContrastAndSNRAnalyzerFactory.KEY, SpotContrastAndSNRAnalyzerFactory.FEATURES);
		features.put(SpotRadiusEstimatorFactory.KEY, SpotRadiusEstimatorFactory.FEATURES);
//		features.put(SpotMorphologyAnalyzerFactory.KEY, SpotMorphologyAnalyzerFactory.FEATURES);
		// features names
		featureNames = new HashMap<String, Map<String,String>>();
		featureNames.put(SpotIntensityAnalyzerFactory.KEY, SpotIntensityAnalyzerFactory.FEATURE_NAMES);
		featureNames.put(SpotContrastAndSNRAnalyzerFactory.KEY, SpotContrastAndSNRAnalyzerFactory.FEATURE_NAMES);
		featureNames.put(SpotRadiusEstimatorFactory.KEY, SpotRadiusEstimatorFactory.FEATURE_NAMES);
//		featureNames.put(SpotMorphologyAnalyzerFactory.KEY, SpotMorphologyAnalyzerFactory.FEATURE_NAMES);
		// features short names
		featureShortNames = new HashMap<String, Map<String,String>>();
		featureShortNames.put(SpotIntensityAnalyzerFactory.KEY, SpotIntensityAnalyzerFactory.FEATURE_SHORT_NAMES);
		featureShortNames.put(SpotContrastAndSNRAnalyzerFactory.KEY, SpotContrastAndSNRAnalyzerFactory.FEATURE_SHORT_NAMES);
		featureShortNames.put(SpotRadiusEstimatorFactory.KEY, SpotRadiusEstimatorFactory.FEATURE_SHORT_NAMES);
//		featureShortNames.put(SpotMorphologyAnalyzerFactory.KEY, SpotMorphologyAnalyzerFactory.FEATURE_SHORT_NAMES);
		// feature dimensions
		featureDimensions = new HashMap<String, Map<String,Dimension>>();
		featureDimensions.put(SpotIntensityAnalyzerFactory.KEY, SpotIntensityAnalyzerFactory.FEATURE_DIMENSIONS);
		featureDimensions.put(SpotContrastAndSNRAnalyzerFactory.KEY, SpotContrastAndSNRAnalyzerFactory.FEATURE_DIMENSIONS);
		featureDimensions.put(SpotRadiusEstimatorFactory.KEY, SpotRadiusEstimatorFactory.FEATURE_DIMENSIONS);
//		featureDimensions.put(SpotMorphologyAnalyzerFactory.KEY, SpotMorphologyAnalyzerFactory.FEATURE_DIMENSIONS);
	}

	/**
	 * @return a new instance of the target spotFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotFeatureAnalyzerFactory<T> getSpotFeatureAnalyzer(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new SpotIntensityAnalyzerFactory<T>(model);
		case 1:
			return new SpotContrastAndSNRAnalyzerFactory<T>(model);
		case 2:
			return new SpotRadiusEstimatorFactory<T>(model);
//		case 3:
//			return new SpotMorphologyAnalyzerFactory<T>(model);
		default:
			return null;
		}
	}

	/**
	 * @return a list of the spotFeatureAnalyzer names available through this factory.
	 */
	public List<String> getAvailableSpotFeatureAnalyzers() {
		return names;
	}


	/**
	 * @return the list of features the target analyzer generates, or <code>null</code>
	 * if the analyzer is unknown to this factory.
	 */
	public List<String> getFeatures(String key) {
		return features.get(key);
	}

	/**
	 * @return the map of short names for any feature, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, String> getFeatureShortNames(String key) {
		return featureShortNames.get(key);
	}

	/**
	 * @return the map of names for any feature, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, String> getFeatureNames(String key) {
		return featureNames.get(key);
	}

	/**
	 * @return the map of feature dimension, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, Dimension> getFeatureDimensions(String key) {
		return featureDimensions.get(key);
	}

}
