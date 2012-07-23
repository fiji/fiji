package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.spot.BlobContrastAndSNR;
import fiji.plugin.trackmate.features.spot.BlobDescriptiveStatistics;
import fiji.plugin.trackmate.features.spot.BlobMorphology;
import fiji.plugin.trackmate.features.spot.RadiusEstimator;
import fiji.plugin.trackmate.features.spot.SpotFeatureAnalyzer;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A factory for the spot analyzers provided in the GUI.
 * <p>
 * Concrete implementation must declare what features they can compute numerically, 
 * using the method {@link #getFeatures(String)}. The names and dimension of these 
 * features are also specified in 3 maps: {@link #getFeatureNames(String)}, {@link #getFeatureShortNames(String)}
 * and {@link #getFeatureDimensions(String)}.
 * <p>
 * Feature key names are for historical reason all capitalized in an enum manner. For instance: POSITION_X,
 * MAX_INTENSITY, etc... They must be suitable to be used as a attribute key in an xml file.
 */
public class SpotFeatureAnalyzerFactory <T extends RealType<T> & NativeType<T>> {


	/** The detector names, in the order they will appear in the GUI.
	 * These names will be used as keys to access relevant spot analyzer classes.  */
	protected List<String> names;

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This factory provides the GUI with the model spotFeatureAnalyzers currently available in the 
	 * TrackMate plugin. Each spotFeatureAnalyzer is identified by a key String, which can be used 
	 * to retrieve new instance of the spotFeatureAnalyzer.
	 * <p>
	 * If you want to add custom spotFeatureAnalyzers to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom spotFeatureAnalyzers and provide this 
	 * extended factory to the {@link TrackMate_} plugin.
	 */
	public SpotFeatureAnalyzerFactory() {
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
		names.add(BlobDescriptiveStatistics.NAME);
		names.add(BlobContrastAndSNR.NAME); // must be after the statistics one
		names.add(RadiusEstimator.NAME);
		names.add(BlobMorphology.NAME);
	}

	/**
	 * @return a new instance of the target spotFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public SpotFeatureAnalyzer<T> getSpotFeatureAnalyzer(String key) {
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return new BlobDescriptiveStatistics<T>();
		case 1:
			return new BlobContrastAndSNR<T>();
		case 2:
			return new RadiusEstimator<T>();
		case 3:
			return new BlobMorphology<T>();
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
		int index = names.indexOf(key);
		if (index < 0) {
			return null;
		}
		switch (index) {
		case 0:
			return BlobDescriptiveStatistics.FEATURES;
		case 1:
			return BlobContrastAndSNR.FEATURES;
		case 2:
			return RadiusEstimator.FEATURES;
		case 3:
			return BlobMorphology.FEATURES;
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
			return BlobDescriptiveStatistics.FEATURE_SHORT_NAMES;
		case 1:
			return BlobContrastAndSNR.FEATURE_SHORT_NAMES;
		case 2:
			return RadiusEstimator.FEATURE_SHORT_NAMES;
		case 3:
			return BlobMorphology.FEATURE_SHORT_NAMES;
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
			return BlobDescriptiveStatistics.FEATURE_NAMES;
		case 1:
			return BlobContrastAndSNR.FEATURE_NAMES;
		case 2:
			return RadiusEstimator.FEATURE_NAMES;
		case 3:
			return BlobMorphology.FEATURE_NAMES;
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
			return BlobDescriptiveStatistics.FEATURE_DIMENSIONS;
		case 1:
			return BlobContrastAndSNR.FEATURE_DIMENSIONS;
		case 2:
			return RadiusEstimator.FEATURE_DIMENSIONS;
		case 3:
			return BlobMorphology.FEATURE_DIMENSIONS;
		default:
			return null;
		}
	}

}
