package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.features.track.TrackLocationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackSpeedStatisticsAnalyzer;

/**
 * A provider for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeaturesForKey(String)}. The names and dimension of these 
 * features are also stored and provided: {@link #getFeatureName(String)}, {@link #getFeatureShortName(String)}
 * and {@link #getFeatureDimension(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class TrackAnalyzerProvider {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant track analyzer classes.  */
	protected List<String> names;
	/** Map of the features per analyzer. */
	protected Map<String, List<String>> features;
	/** Map a feature to its name. */
	protected Map<String,String> featureNames;
	/** Map a feature to its short name. */
	protected Map<String, String> featureShortNames;
	/** Map a feature to its dimension. */
	protected Map<String, Dimension> featureDimensions;
	/** The target model to operate on. */
	protected final TrackMateModel model;
	/** The {@link TrackIndexAnalyzer} is the only analyzer we do not re-instantiate 
	 * at every {@link #getTrackFeatureAnalyzer(String)} call, for it has an internal state 
	 * useful for lazy computation of track features. */
	protected final TrackIndexAnalyzer trackIndexAnalyzer;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the model trackFeatureAnalyzers currently available in the 
	 * TrackMate plugin. Each trackFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public TrackAnalyzerProvider(TrackMateModel model) {
		this.model = model;
		registerTrackFeatureAnalyzers();
		this.trackIndexAnalyzer = new TrackIndexAnalyzer(model);
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard trackFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerTrackFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(4);
		names.add(TrackBranchingAnalyzer.KEY);
		names.add(TrackDurationAnalyzer.KEY);
		names.add(TrackSpeedStatisticsAnalyzer.KEY);
		names.add(TrackLocationAnalyzer.KEY);
		names.add(TrackIndexAnalyzer.KEY);
		// features
		features = new HashMap<String, List<String>>();
		features.put(TrackBranchingAnalyzer.KEY, TrackBranchingAnalyzer.FEATURES);
		features.put(TrackDurationAnalyzer.KEY, TrackDurationAnalyzer.FEATURES);
		features.put(TrackSpeedStatisticsAnalyzer.KEY, TrackSpeedStatisticsAnalyzer.FEATURES);
		features.put(TrackLocationAnalyzer.KEY, TrackLocationAnalyzer.FEATURES);
		features.put(TrackIndexAnalyzer.KEY, TrackIndexAnalyzer.FEATURES);
		// features names
		featureNames = new HashMap<String, String>();
		featureNames.putAll(TrackSpeedStatisticsAnalyzer.FEATURE_NAMES);
		featureNames.putAll(TrackDurationAnalyzer.FEATURE_NAMES);
		featureNames.putAll(TrackBranchingAnalyzer.FEATURE_NAMES);
		featureNames.putAll(TrackLocationAnalyzer.FEATURE_NAMES);
		featureNames.putAll(TrackIndexAnalyzer.FEATURE_NAMES);
		// features short names
		featureShortNames = new HashMap<String, String>();
		featureShortNames.putAll(TrackSpeedStatisticsAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(TrackDurationAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(TrackBranchingAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(TrackLocationAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(TrackIndexAnalyzer.FEATURE_SHORT_NAMES);
		// feature dimensions
		featureDimensions = new HashMap<String, Dimension>();
		featureDimensions.putAll(TrackSpeedStatisticsAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(TrackDurationAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(TrackBranchingAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(TrackLocationAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(TrackIndexAnalyzer.FEATURE_DIMENSIONS);
	}

	/**
	 * @return a new instance of the target trackFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackAnalyzer getTrackFeatureAnalyzer(String key) {
		if (key == TrackDurationAnalyzer.KEY) {
			return new TrackDurationAnalyzer(model);
		} else if (key == TrackBranchingAnalyzer.KEY) {
			return new TrackBranchingAnalyzer(model);
		} else if (key == TrackSpeedStatisticsAnalyzer.KEY) {
			return new TrackSpeedStatisticsAnalyzer(model);
		} else if (key == TrackLocationAnalyzer.KEY) {
			return new TrackLocationAnalyzer(model);
		} else if (key == TrackIndexAnalyzer.KEY) {
			return trackIndexAnalyzer;
		} else {
			return null;
		}
	}

	/**
	 * @return a list of the trackFeatureAnalyzer names available through this provider.
	 */
	public List<String> getAvailableTrackFeatureAnalyzers() {
		return names;
	}


	/**
	 * @return the list of features the analyzer with the specified key 
	 * generates, or <code>null</code> if the analyzer is unknown to this provider.
	 */
	public List<String> getFeaturesForKey(String key) {
		return features.get(key);
	}
	
	/**
	 * @return the key of the analyzer that can generate the specified feature,
	 * or <code>null</code> if this feature is unknown to this provider.
	 */
	public String getKeyForAnalyzer(String feature) {
		for (String key : features.keySet()) {
			if (features.get(key).contains(feature)) {
				return key;
			}
		}
		return null;
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
