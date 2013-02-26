package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTimeLocationAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;

/**
 * A provider for the edge analyzers provided in the GUI.
 */
public class EdgeAnalyzerProvider {


	protected final TrackMateModel model;
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
	public EdgeAnalyzerProvider(TrackMateModel model) {
		this.model = model;
		registerEdgeFeatureAnalyzers();
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard spotFeatureAnalyzers shipped with TrackMate.
	 */
	protected void registerEdgeFeatureAnalyzers() {
		// Names
		names = new ArrayList<String>(3);
		names.add(EdgeVelocityAnalyzer.KEY);
		names.add(EdgeTimeLocationAnalyzer.KEY);
		names.add(EdgeTargetAnalyzer.KEY);
		// features
		features = new HashMap<String, List<String>>();
		features.put(EdgeTimeLocationAnalyzer.KEY, EdgeTimeLocationAnalyzer.FEATURES);
		features.put(EdgeVelocityAnalyzer.KEY, EdgeVelocityAnalyzer.FEATURES);
		features.put(EdgeTargetAnalyzer.KEY, EdgeTargetAnalyzer.FEATURES);
		// features names
		featureNames = new HashMap<String, String>();
		featureNames.putAll(EdgeTimeLocationAnalyzer.FEATURE_NAMES);
		featureNames.putAll(EdgeVelocityAnalyzer.FEATURE_NAMES);
		featureNames.putAll(EdgeTargetAnalyzer.FEATURE_NAMES);
		// features short names
		featureShortNames = new HashMap<String, String>();
		featureShortNames.putAll(EdgeTimeLocationAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(EdgeVelocityAnalyzer.FEATURE_SHORT_NAMES);
		featureShortNames.putAll(EdgeTargetAnalyzer.FEATURE_SHORT_NAMES);
		// feature dimensions
		featureDimensions = new HashMap<String, Dimension>();
		featureDimensions.putAll(EdgeTimeLocationAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(EdgeVelocityAnalyzer.FEATURE_DIMENSIONS);
		featureDimensions.putAll(EdgeTargetAnalyzer.FEATURE_DIMENSIONS);
	}

	/**
	 * @return a new instance of the target edgeFeatureAnalyzer identified by the key parameter. 
	 * If the key is unknown to this factory, <code>null</code> is returned. 
	 */
	public EdgeAnalyzer getEdgeFeatureAnalyzer(String key) {
		if (key == EdgeTargetAnalyzer.KEY) {
			return new EdgeTargetAnalyzer(model);
		} else if (key == EdgeVelocityAnalyzer.KEY) {
			return new EdgeVelocityAnalyzer(model);
		} else if (key == EdgeTimeLocationAnalyzer.KEY) {
			return new EdgeTimeLocationAnalyzer(model);
		} else {
			return null;
		}
	}

	/**
	 * @return a list of the edgeFeatureAnalyzer names available through this provider.
	 */
	public List<String> getAvailableEdgeFeatureAnalyzers() {
		return names;
	}

	/** 
	 * @return  the list of features an analyzer can compute, or <code>null</code> if the 
	 * analyzer is unknown to this provider
	 * @param analyzer  the analyzer key String
	 */
	public List<String> getFeatures(String analyzer) {
		if (analyzer == EdgeTargetAnalyzer.KEY) {
			return EdgeTargetAnalyzer.FEATURES;
		} else if (analyzer == EdgeVelocityAnalyzer.KEY) {
			return EdgeVelocityAnalyzer.FEATURES;
		} else if (analyzer == EdgeTimeLocationAnalyzer.KEY) {
			return EdgeTimeLocationAnalyzer.FEATURES;
		} else {
			return null;
		}
	}
	
	/**
	 * @return the short name of the given feature, 
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public String getFeatureShortName(String key) {
		return featureShortNames.get(key);
	}

	/**
	 * @return the name of the given feature, 
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public String getFeatureName(String key) {
		return featureNames.get(key);
	}

	/**
	 * @return the dimension of the target feature, 
	 * or <code>null</code> if the feature is unknown to this provider.
	 */
	public Dimension getFeatureDimension(String key) {
		return featureDimensions.get(key);
	}
}
