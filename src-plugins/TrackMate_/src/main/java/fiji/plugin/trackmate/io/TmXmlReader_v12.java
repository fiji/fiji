package fiji.plugin.trackmate.io;


import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readFloatAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntAttribute;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ABOVE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_FEATURE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_VALUE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTERED_SPOT_COLLECTION_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTERED_SPOT_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTERED_TRACK_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTER_ABOVE_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTER_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTER_FEATURE_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FILTER_VALUE_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.FRAME_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_FILENAME_v12_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_FOLDER_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_HEIGHT_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_NFRAMES_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_NSLICES_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_TIME_UNITS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.IMAGE_WIDTH_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.INITIAL_SPOT_FILTER_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SEGMENTER_CLASS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SEGMENTER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SEGMENTER_SETTINGS_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_SEGMENTATION_CHANNEL_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_TEND_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_TSTART_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_XEND_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_XSTART_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_YEND_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_YSTART_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_ZEND_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SETTINGS_ZSTART_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_COLLECTION_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_FILTER_COLLECTION_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_FRAME_COLLECTION_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_ID_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_ID_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.SPOT_NAME_v12_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACKER_CLASS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACKER_SETTINGS_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_COLLECTION_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_EDGE_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_EDGE_SOURCE_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_EDGE_TARGET_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_ID_ATTRIBUTE_NAME_v12;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.TRACK_ID_ELEMENT_KEY_v12;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.util.NumberParser;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

/**
 * A compatibility xml loader than can load TrackMate xml file saved for version
 * prior to 1.3. In the code, we keep the previous vocable of "segmenter"...
 * The code here is extremely pedestrian; we deal with all particular cases
 * explicitly, and convert on the fly to v1.3 classes. 
 * @author Jean-Yves Tinevez - 2012
 */
public class TmXmlReader_v12 extends TmXmlReader {

	/*
	 * XML KEY_v12S FOR V 1.2
	 */
	
	private static final String TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12				= "allowed";
	// Alternative costs & blocking
	private static final String TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME_v12 		= "alternatecostfactor";
	private static final String TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME_v12			= "cutoffpercentile";
	private static final String TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME_v12				= "blockingvalue";
	// Cutoff elements
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT				= "TimeCutoff";
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME_v12				= "value";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT			= "DistanceCutoff";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME_v12			= "value";
	private static final String TRACKER_SETTINGS_FEATURE_ELEMENT					= "FeatureCondition";
	private static final String TRACKER_SETTINGS_LINKING_ELEMENT					= "LinkingCondition";
	private static final String TRACKER_SETTINGS_GAP_CLOSING_ELEMENT				= "GapClosingCondition";
	private static final String TRACKER_SETTINGS_MERGING_ELEMENT					= "MergingCondition";
	private static final String TRACKER_SETTINGS_SPLITTING_ELEMENT					= "SplittingCondition";
	// Nearest meighbor tracker
	private static final String MAX_LINKING_DISTANCE_ATTRIBUTE = "maxdistance";
	
	/** Stores error messages when reading parameters. */
	private String errorMessage;

	/*
	 * CONSTRUCTORS
	 */


	public TmXmlReader_v12(File file, TrackMate_ plugin) {
		super(file, plugin);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Return a {@link TrackMateModel} from all the information stored in this file.
	 * Fields not set in the field will be <code>null</code> in the model.
	 * @throws DataConversionException
	 */
	public boolean process() {
		
		long start = System.currentTimeMillis();
		
		TrackMateModel model = plugin.getModel();
		// Settings
		Settings settings = getSettings();
		getDetectorSettings(settings);
		getTrackerSettings(settings);
		settings.imp = getImage();
		model.setSettings(settings);

		// Spot Filters
		List<FeatureFilter> spotFilters = getSpotFeatureFilters();
		FeatureFilter initialFilter = getInitialFilter();
		model.getSettings().initialSpotFilterValue = initialFilter.value;
		model.getSettings().setSpotFilters(spotFilters);

		// Spots
		SpotCollection allSpots = getAllSpots();
		SpotCollection filteredSpots = getFilteredSpots();
		model.setSpots(allSpots, false);
		model.setFilteredSpots(filteredSpots, false);

		// Tracks
		readTracks();

		// Track Filters
		List<FeatureFilter> trackFilters = getTrackFeatureFilters();
		model.getSettings().setTrackFilters(trackFilters);

		long end = System.currentTimeMillis();
		processingTime = end - start;
		
		return true;
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	/**
	 * Load the tracks, the track features and the ID of the visible tracks into the model
	 * modified by this reader. 
	 * @return true if the tracks were found in the file, false otherwise.
	 */
	private void readTracks() {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return;

		if (null == cache) 
			getAllSpots(); // build the cache if it's not there

		final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);
		List<Element> edgeElements;

		// A temporary map that maps stored track key to one of its spot
		HashMap<Integer, Spot> savedTrackMap = new HashMap<Integer, Spot>();


		for (Element trackElement : trackElements) {

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);
			// Keep a reference of one of the spot for outside the loop.
			Spot sourceSpot = null; 

			// Iterate over edges
			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY_v12);

			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				int sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME_v12, logger);
				int targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME_v12, logger);

				// Get matching spots from the cache
				sourceSpot = cache.get(sourceID);
				Spot targetSpot = cache.get(targetID);

				// Get weight
				double weight = 0;
				if (null != edgeElement.getAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME_v12)) {
					weight   	= readDoubleAttribute(edgeElement, TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME_v12, logger);
				}

				// Error check
				if (null == sourceSpot) {
					logger.error("Unknown spot ID: "+sourceID);
					continue;
				}
				if (null == targetSpot) {
					logger.error("Unknown spot ID: "+targetID);
					continue;
				}

				if (sourceSpot.equals(targetSpot)) {
					logger.error("Bad link for track " + trackID + ". Source = Target with ID: " + sourceID);
					continue;
				}

				// Add spots to graph and build edge
				graph.addVertex(sourceSpot);
				graph.addVertex(targetSpot);
				DefaultWeightedEdge edge = graph.addEdge(sourceSpot, targetSpot);

				if (edge == null) {
					logger.error("Bad edge found for track "+trackID);
					continue;
				} else {
					graph.setEdgeWeight(edge, weight);
				}
			} // Finished parsing over the edges of the track

			// Store one of the spot in the saved trackID key map
			savedTrackMap.put(trackID, sourceSpot);

		}

		/* Pass the loaded graph to the model. The model will in turn regenerate a new 
		 * map of tracks vs trackID, using the hash as new keys. Because there is a 
		 * good chance that they saved keys and the new keys differ, we must retrieve
		 * the mapping between the two using the retrieve spots.	 */
		final TrackMateModel model = plugin.getModel();
		model.getTrackModel().setGraph(graph);

		// Retrieve the new track map
		Map<Integer, Set<Spot>> newTrackMap = model.getTrackModel().getTrackSpots();

		// Build a map of old key vs new key
		HashMap<Integer, Integer> newKeyMap = new HashMap<Integer, Integer>();
		HashSet<Integer> newKeysToMatch = new HashSet<Integer>(newTrackMap.keySet());
		for (Integer savedKey : savedTrackMap.keySet()) {
			Spot spotToFind = savedTrackMap.get(savedKey);
			for (Integer newKey : newTrackMap.keySet()) {
				Set<Spot> track = newTrackMap.get(newKey);
				if (track.contains(spotToFind)) {
					newKeyMap.put(savedKey, newKey);
					newKeysToMatch.remove(newKey);
					break;
				}
			}
			if (null == newKeyMap.get(savedKey)) {
				logger.error("The track saved with ID = " + savedKey + " and containing the spot " + spotToFind + " has no matching track in the computed model.");
			}
		}

		// Check that we matched all the new keys
		if (!newKeysToMatch.isEmpty()) {
			StringBuilder sb = new StringBuilder("Some of the computed tracks could not be matched to saved tracks:\n");
			for (Integer unmatchedKey : newKeysToMatch) {
				sb.append(" - track with ID " + unmatchedKey + " with spots " + newTrackMap.get(unmatchedKey) + "\n");
			}
			logger.error(sb.toString());
		}

		/* 
		 * Now we know who's who. We can therefore retrieve the saved filtered track index, and 
		 * match it to the proper new track IDs. 
		 */
		Set<Integer> savedFilteredTrackIDs = readFilteredTrackIDs();
		
		// Build a new set with the new trackIDs;
		Set<Integer> newFilteredTrackIDs = new HashSet<Integer>(savedFilteredTrackIDs.size());
		for (Integer savedKey : savedFilteredTrackIDs) {
			Integer newKey = newKeyMap.get(savedKey);
			newFilteredTrackIDs.add(newKey);
		}
		model.getTrackModel().setFilteredTrackIDs(newFilteredTrackIDs, false);

		/* 
		 * We do the same thing for the track features.
		 */
		final FeatureModel fm = model.getFeatureModel();
		Map<Integer, Map<String, Double>> savedFeatureMap = readTrackFeatures();
		for (Integer savedKey : savedFeatureMap.keySet()) {

			Map<String, Double> savedFeatures = savedFeatureMap.get(savedKey);
			Integer newKey = newKeyMap.get(savedKey);
			if (null == newKey) {
				continue;
			}
			for (String feature : savedFeatures.keySet()) {
				fm.putTrackFeature(newKey, feature, savedFeatures.get(feature));
			}

		}
	}
	
	/**
	 * @return the list of {@link FeatureFilter} for tracks stored in this file.
	 * Return <code>null</code> if the track feature filters data cannot be found in the file.
	 */
	private List<FeatureFilter> getTrackFeatureFilters() {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			String feature 	= ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			Double value 	= readDoubleAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			boolean isAbove	= readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}
	
	/**
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	private FeatureFilter getInitialFilter()  {
		Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY_v12);
		if (null == itEl)
			return null;
		String feature  = itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME_v12);
		double value     = readFloatAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME_v12, logger);
		boolean isAbove = readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME_v12, logger);
		FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	private List<FeatureFilter> getSpotFeatureFilters() {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY_v12);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY_v12);
		for (Element ftEl : ftEls) {
			String feature  = ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME_v12);
			double value     = readFloatAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME_v12, logger);
			boolean isAbove = readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME_v12, logger);
			FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}
	
	/**
	 * @return a map of the saved track features, as they appear in the file
	 */
	private Map<Integer,Map<String,Double>> readTrackFeatures() {

		HashMap<Integer, Map<String, Double>> featureMap = new HashMap<Integer, Map<String, Double>>();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return null;

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);
		for (Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TRACK_ID_ATTRIBUTE_NAME_v12).getIntValue();
			} catch (DataConversionException e1) {
				logger.error("Found a track with invalid trackID for " + trackElement + ". Skipping.\n");
				continue;
			}

			HashMap<String, Double> trackMap = new HashMap<String, Double>();

			List<Attribute> attributes = trackElement.getAttributes();
			for(Attribute attribute : attributes) {

				String attName = attribute.getName();
				if (attName.equals(TRACK_ID_ATTRIBUTE_NAME_v12)) { // Skip trackID attribute
					continue;
				}

				Double attVal = Double.NaN;
				try {
					attVal = attribute.getDoubleValue();
				} catch (DataConversionException e) {
					logger.error("Track "+trackID+": Cannot read the feature "+attName+" value. Skipping.\n");
					continue;
				}

				trackMap.put(attName, attVal);

			}

			featureMap.put(trackID, trackMap);
		}

		return featureMap;

	}


	/**
	 * Return the settings for the TrackMate session saved in this file.
	 * <p>
	 * The settings object will have its {@link SegmenterSettings} and {@link TrackerSettings} set default values will be
	 * used.
	 *
	 * @return  a full Settings object
	 * @throws DataConversionException
	 */
	private Settings getSettings() {
		Settings settings = new Settings();
		// Basic settings
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY_v12);
		if (null != settingsEl) {
			settings.xstart = readIntAttribute(settingsEl, SETTINGS_XSTART_ATTRIBUTE_NAME_v12, logger, 1);
			settings.xend   = readIntAttribute(settingsEl, SETTINGS_XEND_ATTRIBUTE_NAME_v12, logger, 512);
			settings.ystart = readIntAttribute(settingsEl, SETTINGS_YSTART_ATTRIBUTE_NAME_v12, logger, 1);
			settings.yend   = readIntAttribute(settingsEl, SETTINGS_YEND_ATTRIBUTE_NAME_v12, logger, 512);
			settings.zstart = readIntAttribute(settingsEl, SETTINGS_ZSTART_ATTRIBUTE_NAME_v12, logger, 1);
			settings.zend   = readIntAttribute(settingsEl, SETTINGS_ZEND_ATTRIBUTE_NAME_v12, logger, 10);
			settings.tstart = readIntAttribute(settingsEl, SETTINGS_TSTART_ATTRIBUTE_NAME_v12, logger, 1);
			settings.tend   = readIntAttribute(settingsEl, SETTINGS_TEND_ATTRIBUTE_NAME_v12, logger, 10);
		}
		// Image info settings
		Element infoEl  = root.getChild(IMAGE_ELEMENT_KEY_v12);
		if (null != infoEl) {
			settings.dx             = readFloatAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME_v12, logger);
			settings.dy             = readFloatAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME_v12, logger);
			settings.dz             = readFloatAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME_v12, logger);
			settings.dt             = readFloatAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME_v12, logger);
			settings.width          = readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME_v12, logger, 512);
			settings.height         = readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME_v12, logger, 512);
			settings.nslices        = readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME_v12, logger, 1);
			settings.nframes        = readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME_v12, logger, 1);
			settings.spaceUnits     = infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME_v12);
			settings.timeUnits      = infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME_v12);
			settings.imageFileName  = infoEl.getAttributeValue(IMAGE_FILENAME_v12_ATTRIBUTE_NAME_v12);
			settings.imageFolder    = infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME_v12);
		}
		return settings;
	}

	private void getDetectorSettings(Settings settings) {

		// We have to parse the settings element to fetch the target channel
		int targetChannel = 1;
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY_v12);
		if (null != settingsEl) {
			targetChannel = readIntAttribute(settingsEl, SETTINGS_SEGMENTATION_CHANNEL_ATTRIBUTE_NAME_v12, logger);
		}
		
		// Get back to segmenter element
		Element element = root.getChild(SEGMENTER_SETTINGS_ELEMENT_KEY_v12);
		if (null == element) {
			return;
		}
		
		// Deal with segmenter
		String segmenterKey;
		String segmenterClassName = element.getAttributeValue(SEGMENTER_CLASS_ATTRIBUTE_NAME_v12);
		if (null == segmenterClassName) {
			logger.error("\nSegmenter class is not present.\n");
			logger.error("Substituting default.\n");
			segmenterKey = LogDetectorFactory.DETECTOR_KEY;
		} else {
			if (segmenterClassName.equals("fiji.plugin.trackmate.segmentation.DogSegmenter")) {
				segmenterKey = DogDetectorFactory.DETECTOR_KEY;
			} else if (segmenterClassName.equals("fiji.plugin.trackmate.segmentation.LogSegmenter")) {
				segmenterKey = LogDetectorFactory.DETECTOR_KEY;
			} else if (segmenterClassName.equals("fiji.plugin.trackmate.segmentation.DownSamplingLogSegmenter")) {
				segmenterKey = DownsampleLogDetectorFactory.DETECTOR_KEY;
			} else if (segmenterClassName.equals("fiji.plugin.trackmate.segmentation.ManualSegmenter")) {
				segmenterKey = ManualDetectorFactory.DETECTOR_KEY;
			} else {
				logger.error("\nUnknown segmenter: "+segmenterClassName+".\n");
				logger.error("Substituting default.\n");
				segmenterKey = LogDetectorFactory.DETECTOR_KEY;
			}
		}
		DetectorProvider provider = plugin.getDetectorProvider();
		boolean ok = provider.select(segmenterKey);
		if (!ok) {
			logger.error(provider.getErrorMessage());
			logger.error("Substituting default detector.\n");
		}
		settings.detectorFactory = provider.getDetectorFactory();

		// Deal with segmenter settings
		Map<String, Object> ds = new HashMap<String, Object>();

		String segmenterSettingsClassName = element.getAttributeValue(SEGMENTER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12);

		if (null == segmenterSettingsClassName) {

			logger.error("\nSegmenter settings class is not present.\n");
			logger.error("Substituting default settings values.\n");
			ds = provider.getDefaultSettings();

		} else {

			// Log segmenter & Dog segmenter
			if (segmenterSettingsClassName.equals("fiji.plugin.trackmate.segmentation.LogSegmenterSettings"))  {

				if (segmenterKey.equals(LogDetectorFactory.DETECTOR_KEY) || segmenterKey.equals(DogDetectorFactory.DETECTOR_KEY)) {

					// The saved class matched, we can update the settings created above with the file content
					ok = readDouble(element, "expectedradius", ds, KEY_RADIUS)
					&& readDouble(element, "threshold", ds, KEY_THRESHOLD)
					&& readBoolean(element, "doSubPixelLocalization",  ds, KEY_DO_SUBPIXEL_LOCALIZATION)
					&& readBoolean(element, "usemedianfilter", ds, KEY_DO_MEDIAN_FILTERING);
					if (!ok) {
						logger.error(errorMessage);
						logger.error("substituting default settings values.\n");
						ds = provider.getDefaultSettings();
					}

				} else {

					// They do not match. We DO NOT give priority to what has been saved. That way we always
					// have something that works (when invoking the process methods of the plugin).

					logger.error("\nDetector settings class ("+segmenterSettingsClassName+") does not match detector requirements (" +
							ds.getClass().getName()+"),\n");
					logger.error("substituting default values.\n");
					ds = provider.getDefaultSettings();
				}

			} else if (segmenterSettingsClassName.equals("fiji.plugin.trackmate.segmentation.DownSampleLogSegmenterSettings"))  {
				// DownSample segmenter

				if (segmenterKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {

					// The saved class matched, we can updated the settings created above with the file content
					ok = readDouble(element, "expectedradius", ds, KEY_RADIUS)
					&& readDouble(element, "threshold", ds, KEY_THRESHOLD)
					&& readInteger(element, "downsamplingfactor", ds, KEY_DOWNSAMPLE_FACTOR);
					if (!ok) {
						logger.error(errorMessage);
						logger.error("substituting default settings values.\n");
						ds = provider.getDefaultSettings();
					}


				} else {

					// They do not match. We DO NOT give priority to what has been saved. That way we always
					// have something that works (when invoking the process methods of the plugin).

					logger.error("\nDetector settings class ("+segmenterSettingsClassName+") does not match detector requirements (" +
							ds.getClass().getName()+"),\n");
					logger.error("substituting default values.\n");
					ds = provider.getDefaultSettings();
				}

			} else if (segmenterSettingsClassName.equals("fiji.plugin.trackmate.segmentation.BasicSegmenterSettings"))  {
				// Manual segmenter

				if (segmenterKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {

					// The saved class matched, we can updated the settings created above with the file content
					ok =  readDouble(element, "expectedradius", ds, KEY_RADIUS);
					if (!ok) {
						logger.error(errorMessage);
						logger.error("substituting default settings values.\n");
						ds = provider.getDefaultSettings();
					}

				} else {

					// They do not match. We DO NOT give priority to what has been saved. That way we always
					// have something that works (when invoking the process methods of the plugin).

					logger.error("\nDetector settings class ("+segmenterSettingsClassName+") does not match tracker requirements (" +
							ds.getClass().getName()+"),\n");
					logger.error("substituting default values.\n");
					ds = provider.getDefaultSettings();
				}

			} else {

				logger.error("\nDetector settings class ("+segmenterSettingsClassName+") is unknown,\n");
				logger.error("substituting default one.\n");
				ds = provider.getDefaultSettings();

			}
		}
		ds.put(KEY_TARGET_CHANNEL, targetChannel);
		settings.detectorSettings = ds;
	}

	/**
	 * Update the given {@link Settings} object with the {@link TrackerSettings} and {@link SpotTracker} fields
	 * named {@link Settings#trackerSettings} and {@link Settings#tracker} read within the XML file
	 * this reader is initialized with.
	 * <p>
	 * If the tracker settings XML element is not present in the file, the {@link Settings}
	 * object is not updated. If the tracker settings or the tracker info can be read,
	 * but cannot be understood (most likely because the class the XML refers to is unknown)
	 * then a default object is substituted.
	 *
	 * @param settings  the base {@link Settings} object to update.
	 */
	private void getTrackerSettings(Settings settings) {
		Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY_v12);
		if (null == element) {
			return;
		}

		// Deal with tracker
		String trackerKey;
		String trackerClassName = element.getAttributeValue(TRACKER_CLASS_ATTRIBUTE_NAME_v12);

		if (null == trackerClassName) {
			logger.error("\nTracker class is not present.\n");
			logger.error("Substituting default.\n");
			trackerKey = SimpleFastLAPTracker.TRACKER_KEY;

		} else {

			if (trackerClassName.equals("fiji.plugin.trackmate.tracking.SimpleFastLAPTracker") || 
					trackerClassName.equals("fiji.plugin.trackmate.tracking.SimpleLAPTracker")) {
				trackerKey = SimpleFastLAPTracker.TRACKER_KEY; // convert to simple fast version

			} else if (trackerClassName.equals("fiji.plugin.trackmate.tracking.FastLAPTracker") || 
					trackerClassName.equals("fiji.plugin.trackmate.tracking.LAPTracker")) {
				trackerKey = FastLAPTracker.TRACKER_KEY; // convert to fast version

			} else if (trackerClassName.equals("fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker")) {
				trackerKey = NearestNeighborTracker.TRACKER_KEY;

			} else {
				logger.error("\nUnknown tracker: "+trackerClassName+".\n");
				logger.error("Substituting default.\n");
				trackerKey = SimpleFastLAPTracker.TRACKER_KEY;
			}
		}
		TrackerProvider provider = plugin.getTrackerProvider();
		boolean ok = provider.select(trackerKey);
		if (!ok) {
			logger.error(provider.getErrorMessage());
			logger.error("Substituting default tracker.\n");
		}
		settings.tracker = provider.getTracker();

		// Deal with tracker settings
		{
			Map<String, Object> ts = new HashMap<String, Object>();
			
			String trackerSettingsClassName = element.getAttributeValue(TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12);

			if (null == trackerSettingsClassName) {

				logger.error("\nTracker settings class is not present.\n");
				logger.error("Substituting default one.\n");
				ts = provider.getDefaultSettings();

			} else {

				// All LAP trackers
				if (trackerSettingsClassName.equals("fiji.plugin.trackmate.tracking.LAPTrackerSettings"))  {

					if (trackerKey.equals(SimpleFastLAPTracker.TRACKER_KEY) 
							|| trackerKey.equals(FastLAPTracker.TRACKER_KEY)) {

						/*
						 *  Read
						 */
						
						double alternativeObjectLinkingCostFactor = readDoubleAttribute(element, TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME_v12, Logger.VOID_LOGGER);
						double cutoffPercentile 			= readDoubleAttribute(element, TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME_v12, Logger.VOID_LOGGER);
						double blockingValue				= readDoubleAttribute(element, TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME_v12, Logger.VOID_LOGGER);
						// Linking
						Element linkingElement = element.getChild(TRACKER_SETTINGS_LINKING_ELEMENT);
						double linkingDistanceCutOff 		= readDistanceCutoffAttribute(linkingElement);
						Map<String, Double> linkingFeaturePenalties = readTrackerFeatureMap(linkingElement);
						// Gap-closing
						Element gapClosingElement 			= element.getChild(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
						boolean allowGapClosing				= readBooleanAttribute(gapClosingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						double gapClosingDistanceCutoff 	= readDistanceCutoffAttribute(gapClosingElement);
						double gapClosingTimeCutoff 		= readTimeCutoffAttribute(gapClosingElement); 
						Map<String, Double> gapClosingFeaturePenalties = readTrackerFeatureMap(gapClosingElement);
						// Splitting
						Element splittingElement	= element.getChild(TRACKER_SETTINGS_SPLITTING_ELEMENT);
						boolean allowSplitting				= readBooleanAttribute(splittingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						double splittingDistanceCutoff		= readDistanceCutoffAttribute(splittingElement);
						@SuppressWarnings("unused")
						double splittingTimeCutoff			= readTimeCutoffAttribute(splittingElement); // IGNORED
						Map<String, Double> splittingFeaturePenalties = readTrackerFeatureMap(splittingElement);
						// Merging
						Element mergingElement 		= element.getChild(TRACKER_SETTINGS_MERGING_ELEMENT);
						boolean allowMerging				= readBooleanAttribute(mergingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						double mergingDistanceCutoff		= readDistanceCutoffAttribute(mergingElement);
						@SuppressWarnings("unused")
						double mergingTimeCutoff			= readTimeCutoffAttribute(mergingElement); // IGNORED
						Map<String, Double> mergingFeaturePenalties = readTrackerFeatureMap(mergingElement);
						
						/*
						 * Store
						 */
						
						ts.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR, alternativeObjectLinkingCostFactor);
						ts.put(KEY_CUTOFF_PERCENTILE, cutoffPercentile);
						ts.put(KEY_BLOCKING_VALUE, blockingValue);
						// Linking
						ts.put(KEY_LINKING_MAX_DISTANCE, linkingDistanceCutOff);
						ts.put(KEY_LINKING_FEATURE_PENALTIES, linkingFeaturePenalties);
						// Gap-closing
						ts.put(KEY_ALLOW_GAP_CLOSING, allowGapClosing);
						ts.put(KEY_GAP_CLOSING_MAX_DISTANCE, gapClosingDistanceCutoff);
						ts.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, (int) (gapClosingTimeCutoff / settings.dt)); // CONVERTED
						ts.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, gapClosingFeaturePenalties);
						// Splitting
						ts.put(KEY_ALLOW_TRACK_SPLITTING, allowSplitting);
						ts.put(KEY_SPLITTING_MAX_DISTANCE, splittingDistanceCutoff);
						ts.put(KEY_SPLITTING_FEATURE_PENALTIES, splittingFeaturePenalties);
						// the rest is IGNORED
						// Merging
						ts.put(KEY_ALLOW_TRACK_MERGING, allowMerging);
						ts.put(KEY_MERGING_MAX_DISTANCE, mergingDistanceCutoff);
						ts.put(KEY_MERGING_FEATURE_PENALTIES, mergingFeaturePenalties);
						// the rest is ignored
						
					} else {

						// They do not match. We DO NOT give priority to what has been saved. That way we always
						// have something that works (when invoking the process methods of the plugin).

						logger.error("\nTracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
								ts.getClass().getName()+"),\n");
						logger.error("substituting default values.\n");
					}

				} else if (trackerSettingsClassName.equals("fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings"))  {

					if (trackerKey.equals(NearestNeighborTracker.TRACKER_KEY)) {

						// The saved class matched, we can updated the settings created above with the file content
						double maxDist = readDoubleAttribute(element, MAX_LINKING_DISTANCE_ATTRIBUTE, Logger.VOID_LOGGER);
						ts.put(KEY_LINKING_MAX_DISTANCE, maxDist);

					} else {

						// They do not match. We DO NOT give priority to what has been saved. That way we always
						// have something that works (when invoking the process methods of the plugin).

						logger.error("\nTracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
								ts.getClass().getName()+"),\n");
						logger.error("substituting default values.\n");
					}

				} else {

					logger.error("\nTracker settings class ("+trackerSettingsClassName+") is unknown.\n");
					logger.error("Substituting default one.\n");

				}
			}
			settings.trackerSettings = ts;
		}
	}

	/**
	 * Return the list of all spots stored in this file.
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 * @return  a {@link SpotCollection}. Return <code>null</code> if the spot section is not present in the file.
	 */
	private SpotCollection getAllSpots() {
		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY_v12);
		if (null == spotCollection)
			return null;

		// Retrieve children elements for each frame
		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY_v12);

		// Determine total number of spots
		int nspots = 0;
		for (Element currentFrameContent : frameContent) {
			nspots += currentFrameContent.getChildren(SPOT_ELEMENT_KEY_v12).size();
		}

		// Instantiate cache
		cache = new ConcurrentHashMap<Integer, Spot>(nspots);

		int currentFrame = 0;
		ArrayList<Spot> spotList;
		SpotCollection allSpots = new SpotCollection();

		for (Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME_v12, logger);
			List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY_v12);
			spotList = new ArrayList<Spot>(spotContent.size());
			for (Element spotElement : spotContent) {
				Spot spot = createSpotFrom(spotElement);
				spotList.add(spot);
				cache.put(spot.ID(), spot);
			}

			allSpots.put(currentFrame, spotList);	
		}
		return allSpots;
	}

	/**
	 * Return the filtered spots stored in this file, taken from the list of all spots, given in argument.
	 * <p>
	 * The {@link Spot} objects in this list will be the same that of the main list given in argument.
	 * If a spot ID referenced in the file is in the selection but not in the list given in argument,
	 * it is simply ignored, and not added to the selection list. That way, it is certain that all spots
	 * belonging to the selection list also belong to the global list.
	 * @param allSpots  the list of all spots, from which this selection is made
	 * @return  a {@link SpotCollection}. Each spot of this collection belongs also to the  given collection.
	 * Return <code>null</code> if the spot selection section does is not present in the file.
	 */
	private SpotCollection getFilteredSpots()  {
		Element selectedSpotCollection = root.getChild(FILTERED_SPOT_ELEMENT_KEY_v12);
		if (null == selectedSpotCollection)
			return null;

		if (null == cache)
			getAllSpots(); // build it if it's not here

		int currentFrame = 0;
		int ID;
		ArrayList<Spot> spotList;
		List<Element> spotContent;
		SpotCollection spotSelection = new SpotCollection();
		List<Element> frameContent = selectedSpotCollection.getChildren(FILTERED_SPOT_COLLECTION_ELEMENT_KEY_v12);

		for (Element currentFrameContent : frameContent) {
			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME_v12, logger);
			spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY_v12);
			spotList = new ArrayList<Spot>(spotContent.size());
			// Loop over all spot element
			for (Element spotEl : spotContent) {
				// Find corresponding spot in cache
				ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME_v12, logger);
				spotList.add(cache.get(ID));
			}

			spotSelection.put(currentFrame, spotList);
		}
		return spotSelection;
	}

	/**
	 * Read the tracks stored in the file as a list of set of spots.
	 * <p>
	 * This methods ensures that the indices of the track in the returned list match the indices
	 * stored in the file. Because we want this list to be made of the same objects used everywhere,
	 * we build it from the track graph that can be built calling {@link #readTrackGraph(SpotCollection)}.
	 * <p>
	 * Each track is returned as a set of spot. The set itself is sorted by increasing time.
	 *
	 * @param graph  the graph to retrieve spot objects from
	 * @return  a list of tracks as a set of spots
	 */
	public List<Set<Spot>> readTrackSpots(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph) {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return null;

		// Retrieve all spots from the graph
		final Set<Spot> spots = graph.vertexSet();

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);
		final int nTracks = trackElements.size();

		// Prepare holder for results
		final ArrayList<Set<Spot>> trackSpots = new ArrayList<Set<Spot>>(nTracks);
		// Fill it with null value so that it is of size nTracks, and we can later put the real tracks
		for (int i = 0; i < nTracks; i++) {
			trackSpots.add(null);
		}

		List<Element> edgeElements;
		int sourceID, targetID;
		boolean sourceFound, targetFound;

		for (Element trackElement : trackElements) {

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);


			// Instantiate current track
			HashSet<Spot> track = new HashSet<Spot>(2*trackElements.size()); // approx

			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY_v12);
			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME_v12, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME_v12, logger);

				// Retrieve corresponding spots from their ID
				targetFound = false;
				sourceFound = false;

				for (Spot spot : spots) {
					if (!sourceFound  && spot.ID() == sourceID) {
						track.add(spot);
						sourceFound = true;
						if (DEBUG) {
							System.out.println("[TmXmlReader] readTrackSpots: in track "+trackID+", found spot "+spot);
							System.out.println("[TmXmlReader] readTrackSpots: the track "+trackID+" has the following spots: "+track);
						}
					}
					if (!targetFound  && spot.ID() == targetID) {
						track.add(spot);
						targetFound = true;
						if (DEBUG) {
							System.out.println("[TmXmlReader] readTrackSpots: in track "+trackID+", found spot "+spot);
							System.out.println("[TmXmlReader] readTrackSpots: the track "+trackID+" has the following spots: "+track);
						}
					}
					if (targetFound && sourceFound) {
						break;
					}
				}

			} // looping over all edges

			trackSpots.set(trackID, track);

			if (DEBUG) {
				System.out.println("[TmXmlReader] readTrackSpots: the track "+trackID+" has the following spots: "+track);
			}


		} // looping over all track elements

		return trackSpots;
	}

	/**
	 * Read the tracks stored in the file as a list of set of edges.
	 * <p>
	 * This methods ensures that the indices of the track in the returned list match the indices
	 * stored in the file. Because we want this list to be made of the same objects used everywhere,
	 * we build it from the track graph that can be built calling {@link #readTrackGraph(SpotCollection)}.
	 * <p>
	 * Each track is returned as a set of edges.
	 *
	 * @param graph  the graph to retrieve spot objects from
	 * @return  a list of tracks as a set of edges
	 */
	public List<Set<DefaultWeightedEdge>> readTrackEdges(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph) {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return null;

		// Retrieve all spots from the graph
		final Set<Spot> spots = graph.vertexSet();

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);
		final int nTracks = trackElements.size();

		// Prepare holder for results
		final ArrayList<Set<DefaultWeightedEdge>> trackEdges = new ArrayList<Set<DefaultWeightedEdge>>(nTracks);
		// Fill it with null value so that it is of size nTracks, and we can later put the real tracks
		for (int i = 0; i < nTracks; i++) {
			trackEdges.add(null);
		}

		List<Element> edgeElements;
		int sourceID, targetID;
		Spot sourceSpot, targetSpot;
		boolean sourceFound, targetFound;

		for (Element trackElement : trackElements) {

			// Get all edge elements
			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY_v12);

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);

			// Instantiate current track
			HashSet<DefaultWeightedEdge> track = new HashSet<DefaultWeightedEdge>(edgeElements.size());

			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME_v12, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME_v12, logger);

				// Retrieve corresponding spots from their ID
				targetFound = false;
				sourceFound = false;
				targetSpot = null;
				sourceSpot = null;

				for (Spot spot : spots) {
					if (!sourceFound  && spot.ID() == sourceID) {
						sourceSpot = spot;
						sourceFound = true;
					}
					if (!targetFound  && spot.ID() == targetID) {
						targetSpot = spot;
						targetFound = true;
					}
					if (targetFound && sourceFound) {
						if (sourceSpot.equals(targetSpot)) {
							logger.error("Bad edge found for track "+trackID+": target spot equals source spot.\n");
							break;
						}

						// Retrieve possible edges from graph
						Set<DefaultWeightedEdge> edges = graph.getAllEdges(sourceSpot, targetSpot);

						if (edges.size() != 1) {
							logger.error("Bad edge found for track "+trackID+": found "+edges.size()+" edges.\n");
							break;
						} else {
							DefaultWeightedEdge edge = edges.iterator().next();
							track.add(edge);
							if (DEBUG) {
								System.out.println("[TmXmlReader] readTrackEdges: in track "+trackID+", found edge "+edge);
							}

						}
						break;
					}

				}
			} // looping over all edges

			trackEdges.set(trackID, track);

		} // looping over all track elements

		return trackEdges;
	}

	/**
	 * Read and return the list of track indices that define the filtered track collection.
	 * @throws DataConversionException
	 */
	private Set<Integer> readFilteredTrackIDs() {
		Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY_v12);
		if (null == filteredTracksElement)
			return null;

		// Work because the track splitting from the graph is deterministic
		List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY_v12);
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (Element indexElement : elements) {
			Integer trackID = readIntAttribute(indexElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);
			if (null != trackID) {
				filteredTrackIndices.add(trackID);
			}
		}
		return filteredTrackIndices;
	}

	private ImagePlus getImage()  {
		Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY_v12);
		if (null == imageInfoElement)
			return null;
		String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_v12_ATTRIBUTE_NAME_v12);
		String folder   = imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME_v12);
		if (null == filename || filename.isEmpty())
			return null;
		if (null == folder || folder.isEmpty())
			folder = file.getParent(); // it is a relative path, then
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead()) {
			// Could not find it to the absolute path. Then we look for the same path of the xml file
			logger.log("Could not find the image in "+folder+". Looking in xml file location...\n");
			folder = file.getParent();
			imageFile = new File(folder, filename);
			if (!imageFile.exists() || !imageFile.canRead()) {
				return null;
			}
		}
		return IJ.openImage(imageFile.getAbsolutePath());
	}

	private Spot createSpotFrom(Element spotEl) {
		int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME_v12, logger);
		Spot spot = new Spot(ID);

		List<Attribute> atts = spotEl.getAttributes();
		atts.remove(SPOT_ID_ATTRIBUTE_NAME_v12);

		String name = spotEl.getAttributeValue(SPOT_NAME_v12_ATTRIBUTE_NAME_v12);
		if (null == name || name.equals(""))
			name = "ID"+ID;
		spot.setName(name);
		atts.remove(SPOT_NAME_v12_ATTRIBUTE_NAME_v12);

		for (Attribute att : atts) {
			if (att.getName().equals(SPOT_NAME_v12_ATTRIBUTE_NAME_v12) || att.getName().equals(SPOT_ID_ATTRIBUTE_NAME_v12)) {
				continue;
			}
			try {
				spot.putFeature(att.getName(), att.getFloatValue());
			} catch (DataConversionException e) {
				logger.error("Cannot read the feature "+att.getName()+" value. Skipping.\n");
			}
		}
		return spot;
	}
	

	private boolean readDouble(final Element element, String attName, Map<String, Object> settings, String mapKey) {
		String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			double val = NumberParser.parseDouble(str);
			settings.put(mapKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as a double value. Got "+str+".";
			return false;
		}
		return true;
	}

	private boolean readInteger(final Element element, String attName, Map<String, Object> settings, String mapKey) {
		String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			int val = NumberParser.parseInteger(str);
			settings.put(mapKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as an integer value. Got "+str+".";
			return false;
		}
		return true;
	}

	private boolean readBoolean(final Element element, String attName, Map<String, Object> settings, String mapKey) {
		String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			boolean val = Boolean.parseBoolean(str);
			settings.put(mapKey, val);
		} catch (NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as an boolean value. Got "+str+".";
			return false;
		}
		return true;
	}

	private static final double readDistanceCutoffAttribute(Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME_v12).getDoubleValue();
		} catch (DataConversionException e) { }
		return val;
	}

	private static final double readTimeCutoffAttribute(Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME_v12).getDoubleValue();
		} catch (DataConversionException e) { }
		return val;
	}

	/**
	 * Look for all the sub-elements of <code>element</code> with the name TRACKER_SETTINGS_FEATURE_ELEMENT, 
	 * fetch the feature attributes from them, and returns them in a map.
	 */
	private static final Map<String, Double> readTrackerFeatureMap(final Element element) {
		Map<String, Double> map = new HashMap<String, Double>();
		List<Element> featurelinkingElements = element.getChildren(TRACKER_SETTINGS_FEATURE_ELEMENT);
		for (Element el : featurelinkingElements) {
			List<Attribute> atts = el.getAttributes();
			for (Attribute att : atts) {
				String feature = att.getName();
				Double cutoff;
				try {
					cutoff = att.getDoubleValue();
				} catch (DataConversionException e) {
					cutoff = 0d;
				}
				map.put(feature, cutoff);
			}
		}
		return map;
	}

}

