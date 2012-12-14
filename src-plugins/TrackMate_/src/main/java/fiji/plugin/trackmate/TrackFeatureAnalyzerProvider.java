package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;

/**
 * A provider for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures(String)}. The names and dimension of these 
 * features are also stored and provided: {@link #getFeatureName(String)}, {@link #getFeatureShortName(String)}
 * and {@link #getFeatureDimension(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class TrackFeatureAnalyzerProvider <T extends RealType<T> & NativeType<T>> {


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
	public TrackFeatureAnalyzerProvider() {
		registerTrackFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard trackFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerTrackFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(1);
		names.add(TrackDurationAnalyzer.KEY);
		// features
		features = new HashMap<String, List<String>>();
		features.put(TrackDurationAnalyzer.KEY, TrackDurationAnalyzer.FEATURES);
		// features names
		featureNames = new HashMap<String, String>();
		featureNames.putAll(TrackDurationAnalyzer.FEATURE_NAMES);
		// features short names
		featureShortNames = new HashMap<String, String>();
		featureShortNames.putAll(TrackDurationAnalyzer.FEATURE_SHORT_NAMES);
		// feature dimensions
		featureDimensions = new HashMap<String, Dimension>();
		featureDimensions.putAll(TrackDurationAnalyzer.FEATURE_DIMENSIONS);
	}

	/**
	 * @return a new instance of the target trackFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackFeatureAnalyzer<T> getTrackFeatureAnalyzer(String key) {
		if (key == TrackDurationAnalyzer.KEY) {
			return new TrackDurationAnalyzer<T>();
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
