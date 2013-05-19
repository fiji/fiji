package fiji.plugin.trackmate.features;

import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.Dimension;

public interface FeatureAnalyzer {

	/** 
	 * Returns a unique String identifier for this factory.
	 */
	public String getKey();
	
	/**
	 * Returns the list of features this analyzer can compute.
	 */
	public List<String> getFeatures();
	
	/**
	 * Returns the map of short names for any feature the analyzer
	 * can compute.
	 */
	public Map<String, String> getFeatureShortNames();
	
	/**
	 * Returns the map of names for any feature this analyzer can compute.
	 */
	public Map<String, String> getFeatureNames();
	
	/**
	 * Returns the map of feature dimension this analyzer can compute.
	 */
	public Map<String, Dimension> getFeatureDimensions();
}
