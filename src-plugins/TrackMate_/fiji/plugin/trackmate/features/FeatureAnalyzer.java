package fiji.plugin.trackmate.features;

import java.util.Collection;
import java.util.Map;

import fiji.plugin.trackmate.Dimension;

public interface FeatureAnalyzer {

	/**
	 * Return the features this analyzer generates.
	 * @see #getFeatureDimensions()
	 * @see #getFeatureNames()
	 * @see #getFeatureShortNames()
	 */
	public Collection<String> getFeatures();	
	
	/**
	 * Return the short names of all the features this analyzer generates.
	 * @see #getFeatures()
	 * @see #getFeatureNames()
	 * @see #getFeatureDimensions()
	 */
	public Map<String, String> getFeatureShortNames();

	/**
	 * Return the long names of all the features this analyzer generates.
	 * @see #getFeatures()
	 * @see #getFeatureShortNames()
	 * @see #getFeatureDimensions()
	 */
	public Map<String, String> getFeatureNames();

	/**
	 * Return the dimension of all the features this analyzer generates.
	 * @see #getFeatures()
	 * @see #getFeatureNames()
	 * @see #getFeatureShortNames()
	 */
	public Map<String, Dimension> getFeatureDimensions();
}
