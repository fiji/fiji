package fiji.plugin.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;

/**
 * This class represents the part of the {@link TrackMateModel} that is in charge 
 * of dealing with spot features and track features.
 * @author Jean-Yves Tinevez, 2011, 2012
 *
 */
public class FeatureModel {

	//	private static final boolean DEBUG = true;

	/*
	 * FIELDS
	 */

	private ArrayList<String> trackFeatures = new ArrayList<String>();
	private HashMap<String, String> trackFeatureNames = new HashMap<String, String>();
	private HashMap<String, String> trackFeatureShortNames = new HashMap<String, String>();
	private HashMap<String, Dimension> trackFeatureDimensions = new HashMap<String, Dimension>();
	/**
	 * Feature storage. We use a Map of Map as a 2D Map. The list maps each
	 * track to its feature map. The feature map maps each
	 * feature to the double value for the specified feature.
	 */
	protected Map<Integer, Map<String, Double>> trackFeatureValues =  new ConcurrentHashMap<Integer, Map<String, Double>>();

	/**
	 * Feature storage for edges.
	 */
	protected ConcurrentHashMap<DefaultWeightedEdge, ConcurrentHashMap<String, Double>> edgeFeatureValues 
	= new ConcurrentHashMap<DefaultWeightedEdge, ConcurrentHashMap<String, Double>>();

	private ArrayList<String> edgeFeatures = new ArrayList<String>();
	private HashMap<String, String> edgeFeatureNames = new HashMap<String, String>();
	private HashMap<String, String> edgeFeatureShortNames = new HashMap<String, String>();
	private HashMap<String, Dimension> edgeFeatureDimensions = new HashMap<String, Dimension>();

	protected SpotAnalyzerProvider spotAnalyzerProvider;
	protected TrackAnalyzerProvider trackAnalyzerProvider;
	protected EdgeAnalyzerProvider edgeAnalyzerProvider;

	private TrackMateModel model;


	/*
	 * CONSTRUCTOR
	 */

	FeatureModel(TrackMateModel model) {
		this.model = model;
	}

	/*
	 * METHODS
	 */
	
	/** 
	 * Set the list of track feature analyzers that will be used to compute track features.
	 * Setting this field will automatically sets the derived fields: {@link #trackFeatures},
	 * {@link #trackFeatureNames}, {@link #trackFeatureShortNames} and {@link #trackFeatureDimensions}.
	 * These fields will be generated from the {@link TrackAnalyzer} content, returned 
	 * by its methods {@link TrackAnalyzer#getFeatures()}, etc... and will be added
	 * in the order given by the list.
	 * 
	 * @see #computeTrackFeatures()
	 */
	public void setTrackAnalyzerProvider(TrackAnalyzerProvider trackAnalyzerProvider) {
		this.trackAnalyzerProvider = trackAnalyzerProvider;
		// Collect all the track feature we will have to deal with
		trackFeatures = new ArrayList<String>();
		for (String analyzer : trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers()) {
			trackFeatures.addAll(trackAnalyzerProvider.getFeaturesForKey(analyzer));
		}
		// Collect track feature metadata
		trackFeatureNames = new HashMap<String, String>();
		trackFeatureShortNames = new HashMap<String, String>();
		trackFeatureDimensions = new HashMap<String, Dimension>();
		for (String trackFeature : trackFeatures) {
			trackFeatureNames.put(trackFeature, trackAnalyzerProvider.getFeatureName(trackFeature));
			trackFeatureShortNames.put(trackFeature, trackAnalyzerProvider.getFeatureShortName(trackFeature));
			trackFeatureDimensions.put(trackFeature, trackAnalyzerProvider.getFeatureDimension(trackFeature));
		}
		this.trackFeatureValues = new ConcurrentHashMap<Integer, Map<String, Double>>();
	}

	public void setEdgeAnalyzerProvider(final EdgeAnalyzerProvider edgeFeatureAnalyzerProvider) {
		this.edgeAnalyzerProvider = edgeFeatureAnalyzerProvider;

		edgeFeatures = new ArrayList<String>();
		for (String analyzer : edgeFeatureAnalyzerProvider.getAvailableEdgeFeatureAnalyzers()) {
			edgeFeatures.addAll(edgeFeatureAnalyzerProvider.getFeatures(analyzer));
		}

		edgeFeatureNames = new HashMap<String, String>();
		edgeFeatureShortNames = new HashMap<String, String>();
		edgeFeatureDimensions = new HashMap<String, Dimension>();
		for (String edgeFeature : edgeFeatures) {
			edgeFeatureNames.put(edgeFeature, edgeFeatureAnalyzerProvider.getFeatureName(edgeFeature));
			edgeFeatureShortNames.put(edgeFeature, edgeFeatureAnalyzerProvider.getFeatureShortName(edgeFeature));
			edgeFeatureDimensions.put(edgeFeature, edgeFeatureAnalyzerProvider.getFeatureDimension(edgeFeature));
		}
	}

	/**
	 * @return a new double array with all the values for the specified track feature.
	 * @param trackFeature the track feature to parse. Throw an {@link IllegalArgumentException}
	 * if the feature is unknown.
	 * @param filteredOnly if <code>true</code>, will only include filtered tracks, 
	 * all the tracks otherwise.
	 */
	public double[] getTrackFeatureValues(String trackFeature, boolean filteredOnly) {
		if (!trackFeatures.contains(trackFeature)) {
			throw new IllegalArgumentException("Unknown track feature: " + trackFeature);
		}
		final Set<Integer> keys;
		if (filteredOnly) {
			keys = model.getTrackModel().getFilteredTrackIDs();
		} else {
			keys = model.getTrackModel().getTrackIDs();
		}
		double[] val = new double[keys.size()];
		int index = 0;
		for (Integer trackID : keys) {
			val[index++] = getTrackFeature(trackID, trackFeature).doubleValue(); 
		}
		return val;
	}
	
	/**
	 * @return a new double array with all the values for the specified edge feature.
	 * @param edgeFeature the track feature to parse. Throw an {@link IllegalArgumentException}
	 * if the feature is unknown.
	 * @param filteredOnly if <code>true</code>, will only include edges in filtered tracks, 
	 * in all the tracks otherwise.
	 */
	public double[] getEdgeFeatureValues(String edgeFeature, boolean filteredOnly) {
		if (!edgeFeatures.contains(edgeFeature)) {
			throw new IllegalArgumentException("Unknown edge feature: " + edgeFeature);
		}
		final Set<Integer> keys;
		if (filteredOnly) {
			keys = model.getTrackModel().getFilteredTrackIDs();
		} else {
			keys = model.getTrackModel().getTrackIDs();
		}
		int nvals = 0;
		for (Integer trackID : keys) {
			nvals += model.getTrackModel().getTrackEdges(trackID).size();
		}
		
		double[] val = new double[nvals];
		int index = 0;
		for (Integer trackID : keys) {
			for (DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {
				val[index++] = getEdgeFeature(edge, edgeFeature).doubleValue(); 
			}
		}
		return val;
	}



	/*
	 * EDGE FEATURES
	 */

	public synchronized void putEdgeFeature(DefaultWeightedEdge edge, final String featureName, final Double featureValue) {
		ConcurrentHashMap<String, Double> map = edgeFeatureValues.get(edge);
		if (null == map) {
			map = new ConcurrentHashMap<String, Double>();
			edgeFeatureValues.put(edge, map);
		}
		map.put(featureName, featureValue);
	}

	public Double getEdgeFeature(DefaultWeightedEdge edge, final String featureName) {
		ConcurrentHashMap<String, Double> map = edgeFeatureValues.get(edge);
		if (null == map) {
			return null;
		}
		return map.get(featureName);
	}

	public List<String> getEdgeFeatures() {
		return edgeFeatures;
	}

	/**
	 * Return the name mapping of the edge features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getEdgeFeatureNames() {
		return edgeFeatureNames;
	}

	/**
	 * Return the short name mapping of the edge features that are dealt with in this model.
	 * @return
	 */
	public Map<String, String> getEdgeFeatureShortNames() {
		return edgeFeatureShortNames;
	}

	/**
	 * Return the dimension mapping of the edge features that are dealt with in this model.
	 * @return
	 */
	public Map<String, Dimension> getEdgeFeatureDimensions() {
		return edgeFeatureDimensions;
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

	public synchronized void putTrackFeature(final Integer trackID, final String feature, final Double value) {
		Map<String, Double> trackFeatureMap = trackFeatureValues.get(trackID);
		if (null == trackFeatureMap) {
			trackFeatureMap = new HashMap<String, Double>(trackFeatures.size());
			trackFeatureValues.put(trackID, trackFeatureMap);
		}
		trackFeatureMap.put(feature, value);
	}

	/**
	 * @return the numerical value of the specified track feature for the specified track.
	 * @param trackID the track ID to quest.
	 * @param feature the desired feature.
	 */
	public Double getTrackFeature(final Integer trackID, final String feature) {
		Map<String, Double> valueMap = trackFeatureValues.get(trackID);
		return valueMap.get(feature);
	}

	public Map<String, double[]> getTrackFeatureValues() {
		final Map<String, double[]> featureValues = new HashMap<String, double[]>();
		Double val;
		int nTracks = model.getTrackModel().getNTracks();
		for (String feature : trackFeatures) {
			// Make a double array to comply to JFreeChart histograms
			boolean noDataFlag = true;
			final double[] values = new double[nTracks];
			int index = 0;
			for (Integer trackID : model.getTrackModel().getTrackIDs()) {
				val = getTrackFeature(trackID, feature);
				if (null == val)
					continue;
				values[index++] = val;
				noDataFlag = false;
			}

			if (noDataFlag)
				featureValues.put(feature, new double[0]); // Empty array to signal no data
			else
				featureValues.put(feature, values);
		}
		return featureValues;
	}

	/**
	 * Calculate all features for the tracks with the given IDs.
	 */
	public void computeTrackFeatures(final Collection<Integer> trackIDs, boolean doLogIt) {
		final Logger logger = model.getLogger();
		if (doLogIt) {
			logger.log("Computing track features:\n", Logger.BLUE_COLOR);		
		}
		// Reset track feature value map
		trackFeatureValues.clear();
		/*
		 *  Compute new track feature. Analyzers will use the #putFeature method to store results,
		 *  which will regenerate the value map.
		 */
		for (String analyzerKey : trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers()) {
			// Compute features
			TrackAnalyzer analyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(analyzerKey);
			analyzer.process(trackIDs);
			if (doLogIt)
				logger.log("  - " + analyzer.toString() + " in " + analyzer.getProcessingTime() + " ms.\n");
		}
	}

	public void computeEdgeFeatures(final Collection<DefaultWeightedEdge> edges, boolean doLogIt) {
		final Logger logger = model.getLogger();
		if (doLogIt) {
			logger.log("Computing edge features:\n", Logger.BLUE_COLOR);		
		}
		for(String key : edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers()) {
			EdgeAnalyzer analyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			analyzer.process(edges);
			if (doLogIt)
				logger.log("  - " + analyzer.toString() + " in " + analyzer.getProcessingTime() + " ms.\n");
		}
	}

}
