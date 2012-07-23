package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.features.track.TrackFeatureAnalyzer;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A factory for the track analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures(String)}. The names and dimension of these 
 * features are also specified in 3 maps: {@link #getFeatureNames(String)}, {@link #getFeatureShortNames(String)}
 * and {@link #getFeatureDimensions(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class TrackFeatureAnalyzerFactory <T extends RealType<T> & NativeType<T>> {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant track analyzer classes.  */
	protected List<String> names;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This factory provides the GUI with the model trackFeatureAnalyzers currently available in the 
	 * TrackMate plugin. Each trackFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the trackFeatureAnalyzer.
	 * <p>
	 * If you want to add custom trackFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom trackFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public TrackFeatureAnalyzerFactory() {
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
		names.add(TrackDurationAnalyzer.NAME);
	}

	/**
	 * @return a new instance of the target trackFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public TrackFeatureAnalyzer<T> getTrackFeatureAnalyzer(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new TrackDurationAnalyzer<T>();
		default:
			return null;
		}
	}

	/**
	 * @return a list of the trackFeatureAnalyzer names available through this factory.
	 */
	public List<String> getAvailableTrackFeatureAnalyzers() {
		return names;
	}


	/**
	 * @return the list of features the target analyzer generates, or <code>null</code>
	 * if the analyzer is unknown to this factory.
	 */
	public List<String> getFeatures(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return TrackDurationAnalyzer.FEATURES;
		default:
			return null;
		}
	}

	/**
	 * @return the map of short names for any feature, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, String> getFeatureShortNames(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return TrackDurationAnalyzer.FEATURE_SHORT_NAMES;
		default:
			return null;
		}
	}

	/**
	 * @return the map of names for any feature, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, String> getFeatureNames(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return TrackDurationAnalyzer.FEATURE_NAMES;
		default:
			return null;
		}
	}

	/**
	 * @return the map of feature dimension, for the target analyzer, 
	 * or <code>null</code> if the analyzer is unknown to this factory.
	 */
	public Map<String, Dimension> getFeatureDimensions(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return TrackDurationAnalyzer.FEATURE_DIMENSIONS;
		default:
			return null;
		}
	}

}
