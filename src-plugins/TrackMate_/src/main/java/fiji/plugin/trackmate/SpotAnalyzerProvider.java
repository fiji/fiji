package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotRadiusEstimatorFactory;

/**
 * A provider for the spot analyzer factories provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures(String)}. The names and dimension of these 
 * features are also specified in 3 maps: {@link #getFeatureName(String)}, {@link #getFeatureShortName(String)}
 * and {@link #getFeatureDimension(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class SpotAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant spot analyzer classes.  */
	protected List<String> analyzerNames;
	protected Map<String, List<String>> features;
	protected Map<String, String> featureNames;
	protected Map<String,String> featureShortNames;
	protected Map<String, Dimension> featureDimensions;
	protected final TrackMateModel model;

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
	public SpotAnalyzerProvider(TrackMateModel model) {
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
		analyzerNames = new ArrayList<String>(4);
		analyzerNames.add(SpotIntensityAnalyzerFactory.KEY);
		analyzerNames.add(SpotContrastAndSNRAnalyzerFactory.KEY); // must be after the statistics one
		analyzerNames.add(SpotRadiusEstimatorFactory.KEY);
		// features
		features = new HashMap<String, List<String>>();
		features.put(SpotIntensityAnalyzerFactory.KEY, SpotIntensityAnalyzerFactory.FEATURES);
		features.put(SpotContrastAndSNRAnalyzerFactory.KEY, SpotContrastAndSNRAnalyzerFactory.FEATURES);
		features.put(SpotRadiusEstimatorFactory.KEY, SpotRadiusEstimatorFactory.FEATURES);
		// features names
		featureNames = new HashMap<String, String>();
		featureNames.putAll(SpotIntensityAnalyzerFactory.FEATURE_NAMES);
		featureNames.putAll(SpotContrastAndSNRAnalyzerFactory.FEATURE_NAMES);
		featureNames.putAll(SpotRadiusEstimatorFactory.FEATURE_NAMES);
		// features short names
		featureShortNames = new HashMap<String, String>();
		featureShortNames.putAll(SpotIntensityAnalyzerFactory.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(SpotContrastAndSNRAnalyzerFactory.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(SpotRadiusEstimatorFactory.FEATURE_SHORT_NAMES);
		// feature dimensions
		featureDimensions = new HashMap<String, Dimension>();
		featureDimensions.putAll(SpotIntensityAnalyzerFactory.FEATURE_DIMENSIONS);
		featureDimensions.putAll(SpotContrastAndSNRAnalyzerFactory.FEATURE_DIMENSIONS);
		featureDimensions.putAll(SpotRadiusEstimatorFactory.FEATURE_DIMENSIONS);
	}

	/**
	 * @return a new instance of the target spotFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	@SuppressWarnings("rawtypes")
	public SpotFeatureAnalyzerFactory getSpotFeatureAnalyzer(String key) {
		if (key == SpotIntensityAnalyzerFactory.KEY) {
			return new SpotIntensityAnalyzerFactory(model);
		} else if (key == SpotContrastAndSNRAnalyzerFactory.KEY) {
			return new SpotContrastAndSNRAnalyzerFactory(model);
		} else if (key == SpotRadiusEstimatorFactory.KEY) {
			return new SpotRadiusEstimatorFactory(model);
		} else {
			return null;
		}
	}

	/**
	 * @return a list of the spotFeatureAnalyzer names available through this provider.
	 */
	public List<String> getAvailableSpotFeatureAnalyzers() {
		return analyzerNames;
	}


	/**
	 * @return the list of features the target analyzer generates, or <code>null</code>
	 * if the analyzer is unknown to this provider.
	 */
	public List<String> getFeatures(String key) {
		return features.get(key);
	}

	/**
	 * @return the short names of a feature, 
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public String getFeatureShortName(String key) {
		return featureShortNames.get(key);
	}

	/**
	 * @return the name of a feature,  
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public String getFeatureName(String key) {
		return featureNames.get(key);
	}

	/**
	 * @return the dimension of a feature,  
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public Dimension getFeatureDimension(String key) {
		return featureDimensions.get(key);
	}

}
