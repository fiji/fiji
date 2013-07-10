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
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.img.ImgPlus;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotContrastAndSNRAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory;
import fiji.plugin.trackmate.features.spot.SpotMorphologyAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackDurationAnalyzer;
import fiji.plugin.trackmate.gui.descriptors.ConfigureViewsDescriptor;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

/**
 * A compatibility xml loader than can load TrackMate xml file saved for version
 * prior to 2.0. In the code, we keep the previous vocable of "segmenter"...
 * The code here is extremely pedestrian; we deal with all particular cases
 * explicitly, and convert on the fly to v2 classes.
 * @author Jean-Yves Tinevez - 2012
 */
public class TmXmlReader_v12 extends TmXmlReader {

	/*
	 * XML KEY_v12S FOR V 1.2
	 */

	private static final String TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12			= "allowed";
	// Alternative costs & blocking
	private static final String TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME_v12 	= "alternatecostfactor";
	private static final String TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME_v12		= "cutoffpercentile";
	private static final String TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME_v12			= "blockingvalue";
	// Cutoff elements
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT				= "TimeCutoff";
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME_v12			= "value";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT			= "DistanceCutoff";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME_v12		= "value";
	private static final String TRACKER_SETTINGS_FEATURE_ELEMENT					= "FeatureCondition";
	private static final String TRACKER_SETTINGS_LINKING_ELEMENT					= "LinkingCondition";
	private static final String TRACKER_SETTINGS_GAP_CLOSING_ELEMENT				= "GapClosingCondition";
	private static final String TRACKER_SETTINGS_MERGING_ELEMENT					= "MergingCondition";
	private static final String TRACKER_SETTINGS_SPLITTING_ELEMENT					= "SplittingCondition";
	// Nearest meighbor tracker
	private static final String MAX_LINKING_DISTANCE_ATTRIBUTE = "maxdistance";

	// Forgotten features
	private static final ArrayList<String> 			F_FEATURES = new ArrayList<String>(9);
	private static final HashMap<String, String> 	F_FEATURE_NAMES = new HashMap<String, String>(9);
	private static final HashMap<String, String> 	F_FEATURE_SHORT_NAMES = new HashMap<String, String>(9);
	private static final HashMap<String, Dimension> F_FEATURE_DIMENSIONS = new HashMap<String, Dimension>(9);

	private static final String	VARIANCE = "VARIANCE";
	private static final String	KURTOSIS = "KURTOSIS";
	private static final String	SKEWNESS = "SKEWNESS";
	static {
		F_FEATURES.add(VARIANCE);
		F_FEATURES.add(KURTOSIS);
		F_FEATURES.add(SKEWNESS);
		F_FEATURE_NAMES.put(VARIANCE, "Variance");
		F_FEATURE_NAMES.put(KURTOSIS, "Kurtosis");
		F_FEATURE_NAMES.put(SKEWNESS, "Skewness");
		F_FEATURE_SHORT_NAMES.put(VARIANCE, "Var.");
		F_FEATURE_SHORT_NAMES.put(KURTOSIS, "Kurtosis");
		F_FEATURE_SHORT_NAMES.put(SKEWNESS, "Skewness");
		F_FEATURE_DIMENSIONS.put(VARIANCE, Dimension.INTENSITY_SQUARED);
		F_FEATURE_DIMENSIONS.put(KURTOSIS, Dimension.NONE);
		F_FEATURE_DIMENSIONS.put(SKEWNESS, Dimension.NONE);
	}



	/** Stores error messages when reading parameters. */
	private String errorMessage;

	/*
	 * CONSTRUCTORS
	 */


	public TmXmlReader_v12(final File file) {
		super(file);
	}

	/*
	 * PUBLIC METHODS
	 */


	@Override
	public void readSettings(final Settings settings, final DetectorProvider detectorProvider, final TrackerProvider trackerProvider, final SpotAnalyzerProvider spotAnalyzerProvider,
			final EdgeAnalyzerProvider edgeAnalyzerProvider, final TrackAnalyzerProvider trackAnalyzerProvider) {

		// Settings
		getBaseSettings(settings);
		getDetectorSettings(settings, detectorProvider);
		getTrackerSettings(settings, trackerProvider);
		settings.imp = getImage();

		// Spot Filters
		final List<FeatureFilter> spotFilters = getSpotFeatureFilters();
		final FeatureFilter initialFilter = getInitialFilter();
		settings.initialSpotFilterValue = initialFilter.value;
		settings.setSpotFilters(spotFilters);

		// Track Filters
		final List<FeatureFilter> trackFilters = getTrackFeatureFilters();
		settings.setTrackFilters(trackFilters);

		// Feature analyzers. By default, we add them all.

		final ImgPlus<?> img = TMUtils.rawWraps(settings.imp);
		settings.clearSpotAnalyzerFactories();
		final List<String> spotAnalyzerKeys = spotAnalyzerProvider.getAvailableSpotFeatureAnalyzers();
		for (final String key : spotAnalyzerKeys) {
			final SpotAnalyzerFactory<?> spotFeatureAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
			settings.addSpotAnalyzerFactory(spotFeatureAnalyzer);
		}

		settings.clearEdgeAnalyzers();
		final List<String> edgeAnalyzerKeys = edgeAnalyzerProvider.getAvailableEdgeFeatureAnalyzers();
		for (final String key : edgeAnalyzerKeys) {
			final EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
			settings.addEdgeAnalyzer(edgeAnalyzer);
		}

		settings.clearTrackAnalyzers();
		final List<String> trackAnalyzerKeys = trackAnalyzerProvider.getAvailableTrackFeatureAnalyzers();
		for (final String key : trackAnalyzerKeys) {
			final TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
			settings.addTrackAnalyzer(trackAnalyzer);
		}
	}

	@Override
	public String getGUIState() {
		return ConfigureViewsDescriptor.KEY;
	}

	@Override
	public Collection<TrackMateModelView> getViews(final ViewProvider provider) {
		final Collection<TrackMateModelView> views = new ArrayList<TrackMateModelView>(1);
		views.add(provider.getView(HyperStackDisplayer.NAME));
		return views ;
	}

	@Override
	public Model getModel() {
		final Model model = new Model();

		// Spots
		final SpotCollection allSpots = getAllSpots();
		final Map<Integer, Set<Integer>> filteredIDs = getFilteredSpotsIDs();
		if (null != filteredIDs) {
			for (final Integer frame : filteredIDs.keySet()) {
				for (final Integer ID : filteredIDs.get(frame)) {
					cache.get(ID).putFeature(SpotCollection.VISIBLITY, SpotCollection.ONE);
				}
			}
		}
		model.setSpots(allSpots, false);

		// Tracks
		readTracks(model);

		// Physical units
		final Element infoEl  = root.getChild(IMAGE_ELEMENT_KEY_v12);
		if (null != infoEl) {
			final String spaceUnits = infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME_v12);
			final String timeUnits = infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME_v12);
			model.setPhysicalUnits(spaceUnits, timeUnits);
		}

		// Features
		declareDefaultFeatures(model.getFeatureModel());

		return model;
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * We must initialize the model with feature declarations that match the feature we retrieved
	 * from the file.
	 */
	private void declareDefaultFeatures(final FeatureModel fm) {
		// Spots:
		fm.declareSpotFeatures(Spot.FEATURES, Spot.FEATURE_NAMES, Spot.FEATURE_SHORT_NAMES, Spot.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(SpotContrastAndSNRAnalyzerFactory.FEATURES, SpotContrastAndSNRAnalyzerFactory.FEATURE_NAMES,
				SpotContrastAndSNRAnalyzerFactory.FEATURE_SHORT_NAMES, SpotContrastAndSNRAnalyzerFactory.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(SpotMorphologyAnalyzerFactory.FEATURES, SpotMorphologyAnalyzerFactory.FEATURE_NAMES,
				SpotMorphologyAnalyzerFactory.FEATURE_SHORT_NAMES, SpotMorphologyAnalyzerFactory.FEATURE_DIMENSIONS);

		fm.declareSpotFeatures(SpotIntensityAnalyzerFactory.FEATURES, SpotIntensityAnalyzerFactory.FEATURE_NAMES,
				SpotIntensityAnalyzerFactory.FEATURE_SHORT_NAMES, SpotIntensityAnalyzerFactory.FEATURE_DIMENSIONS);
		fm.declareSpotFeatures(F_FEATURES, F_FEATURE_NAMES, F_FEATURE_SHORT_NAMES, F_FEATURE_DIMENSIONS);

		// Edges: no edge features in v1.2

		// Tracks:
		fm.declareTrackFeatures(TrackDurationAnalyzer.FEATURES, TrackDurationAnalyzer.FEATURE_NAMES,
				TrackDurationAnalyzer.FEATURE_SHORT_NAMES, TrackDurationAnalyzer.FEATURE_DIMENSIONS);
	}

	/**
	 * Load the tracks, the track features and the ID of the visible tracks into the model
	 * modified by this reader.
	 * @param model
	 * @return true if the tracks were found in the file, false otherwise.
	 */
	private void readTracks(final Model model) {

		final Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return;

		if (null == cache)
			getAllSpots(); // build the cache if it's not there

		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		// Load tracks
		final List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);

		final Map<Integer, Set<Spot>> trackSpots = new HashMap<Integer, Set<Spot>>(trackElements.size());
		final Map<Integer, Set<DefaultWeightedEdge>> trackEdges = new HashMap<Integer, Set<DefaultWeightedEdge>>(trackElements.size());
		final Map<Integer, String> trackNames = new HashMap<Integer, String>(trackElements.size());

		for (final Element trackElement : trackElements) {


			// Get track ID as it is saved on disk
			final int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);

			// Iterate over edges
			final List<Element> edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY_v12);

			final Set<Spot> spots = new HashSet<Spot>(edgeElements.size());
			final Set<DefaultWeightedEdge> edges = new HashSet<DefaultWeightedEdge>(edgeElements.size());

			for (final Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				final int sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME_v12, logger);
				final int targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME_v12, logger);

				// Get matching spots from the cache
				final Spot sourceSpot = cache.get(sourceID);
				final Spot targetSpot = cache.get(targetID);

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
				final DefaultWeightedEdge edge = graph.addEdge(sourceSpot, targetSpot);

				if (edge == null) {
					logger.error("Bad edge found for track "+trackID);
					continue;
				} else {
					graph.setEdgeWeight(edge, weight);
				}

				// Add to current track sets
				spots.add(sourceSpot);
				spots.add(targetSpot);
				edges.add(edge);


			} // Finished parsing over the edges of the track

			// Store one of the spot in the saved trackID key map
			trackSpots.put(trackID, spots);
			trackEdges.put(trackID, edges);
			trackNames.put(trackID, "Track_" + trackID); // Default name
		}

		final Map<Integer, Boolean> trackVisibility = new HashMap<Integer, Boolean>(trackElements.size());
		final Set<Integer> savedFilteredTrackIDs = readFilteredTrackIDs();
		for (final Integer id : savedFilteredTrackIDs) {
			trackVisibility.put(id, Boolean.TRUE);
		}
		final Set<Integer> ids = new HashSet<Integer>(trackSpots.keySet());
		ids.removeAll(savedFilteredTrackIDs);
		for (final Integer id : ids) {
			trackVisibility.put(id, Boolean.FALSE);
		}

		/*
		 * Pass all of this to the model
		 */
		model.getTrackModel().from(graph, trackSpots, trackEdges, trackVisibility, trackNames);

		/*
		 * We do the same thing for the track features.
		 */
		final FeatureModel fm = model.getFeatureModel();
		final Map<Integer, Map<String, Double>> savedFeatureMap = readTrackFeatures();
		for (final Integer savedKey : savedFeatureMap.keySet()) {

			final Map<String, Double> savedFeatures = savedFeatureMap.get(savedKey);
			for (final String feature : savedFeatures.keySet()) {
				fm.putTrackFeature(savedKey, feature, savedFeatures.get(feature));
			}

		}
	}

	/**
	 * @return the list of {@link FeatureFilter} for tracks stored in this file.
	 * Return <code>null</code> if the track feature filters data cannot be found in the file.
	 */
	private List<FeatureFilter> getTrackFeatureFilters() {
		final List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		final Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		final List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (final Element ftEl : ftEls) {
			final String feature 	= ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			final Double value 	= readDoubleAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			final boolean isAbove	= readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	/**
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	private FeatureFilter getInitialFilter()  {
		final Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY_v12);
		if (null == itEl)
			return null;
		final String feature  = itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME_v12);
		final double value     = readFloatAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME_v12, logger);
		final boolean isAbove = readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME_v12, logger);
		final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	private List<FeatureFilter> getSpotFeatureFilters() {
		final List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		final Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY_v12);
		if (null == ftCollectionEl)
			return null;
		final List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY_v12);
		for (final Element ftEl : ftEls) {
			final String feature  = ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME_v12);
			final double value     = readFloatAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME_v12, logger);
			final boolean isAbove = readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME_v12, logger);
			final FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	/**
	 * @return a map of the saved track features, as they appear in the file
	 */
	private Map<Integer,Map<String,Double>> readTrackFeatures() {

		final HashMap<Integer, Map<String, Double>> featureMap = new HashMap<Integer, Map<String, Double>>();

		final Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY_v12);
		if (null == allTracksElement)
			return null;

		// Load tracks
		final List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY_v12);
		for (final Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TRACK_ID_ATTRIBUTE_NAME_v12).getIntValue();
			} catch (final DataConversionException e1) {
				logger.error("Found a track with invalid trackID for " + trackElement + ". Skipping.\n");
				continue;
			}

			final HashMap<String, Double> trackMap = new HashMap<String, Double>();

			final List<Attribute> attributes = trackElement.getAttributes();
			for(final Attribute attribute : attributes) {

				final String attName = attribute.getName();
				if (attName.equals(TRACK_ID_ATTRIBUTE_NAME_v12)) { // Skip trackID attribute
					continue;
				}

				Double attVal = Double.NaN;
				try {
					attVal = attribute.getDoubleValue();
				} catch (final DataConversionException e) {
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
	private void getBaseSettings(final Settings settings) {
		// Basic settings
		final Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY_v12);
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
		final Element infoEl  = root.getChild(IMAGE_ELEMENT_KEY_v12);
		if (null != infoEl) {
			settings.dx             = readFloatAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME_v12, logger);
			settings.dy             = readFloatAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME_v12, logger);
			settings.dz             = readFloatAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME_v12, logger);
			settings.dt             = readFloatAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME_v12, logger);
			settings.width          = readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME_v12, logger, 512);
			settings.height         = readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME_v12, logger, 512);
			settings.nslices        = readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME_v12, logger, 1);
			settings.nframes        = readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME_v12, logger, 1);
			settings.imageFileName  = infoEl.getAttributeValue(IMAGE_FILENAME_v12_ATTRIBUTE_NAME_v12);
			settings.imageFolder    = infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME_v12);
		}
	}

	private void getDetectorSettings(final Settings settings, final DetectorProvider provider) {

		// We have to parse the settings element to fetch the target channel
		int targetChannel = 1;
		final Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY_v12);
		if (null != settingsEl) {
			targetChannel = readIntAttribute(settingsEl, SETTINGS_SEGMENTATION_CHANNEL_ATTRIBUTE_NAME_v12, logger);
		}

		// Get back to segmenter element
		final Element element = root.getChild(SEGMENTER_SETTINGS_ELEMENT_KEY_v12);
		if (null == element) {
			return;
		}

		// Deal with segmenter
		String segmenterKey;
		final String segmenterClassName = element.getAttributeValue(SEGMENTER_CLASS_ATTRIBUTE_NAME_v12);
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
		boolean ok = provider.select(segmenterKey);
		if (!ok) {
			logger.error(provider.getErrorMessage());
			logger.error("Substituting default detector.\n");
		}
		settings.detectorFactory = provider.getDetectorFactory();

		// Deal with segmenter settings
		Map<String, Object> ds = new HashMap<String, Object>();

		final String segmenterSettingsClassName = element.getAttributeValue(SEGMENTER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12);

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
					// have something that works (when invoking the process methods of the trackmate).

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
					// have something that works (when invoking the process methods of the trackmate).

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
					// have something that works (when invoking the process methods of the trackmate).

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
	private void getTrackerSettings(final Settings settings, final TrackerProvider provider) {
		final Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY_v12);
		if (null == element) {
			return;
		}

		// Deal with tracker
		String trackerKey;
		final String trackerClassName = element.getAttributeValue(TRACKER_CLASS_ATTRIBUTE_NAME_v12);

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
		final boolean ok = provider.select(trackerKey);
		if (!ok) {
			logger.error(provider.getErrorMessage());
			logger.error("Substituting default tracker.\n");
		}
		settings.tracker = provider.getTracker();

		// Deal with tracker settings
		{
			Map<String, Object> ts = new HashMap<String, Object>();

			final String trackerSettingsClassName = element.getAttributeValue(TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME_v12);

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

						final double alternativeObjectLinkingCostFactor = readDoubleAttribute(element, TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME_v12, Logger.VOID_LOGGER);
						final double cutoffPercentile 			= readDoubleAttribute(element, TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME_v12, Logger.VOID_LOGGER);
						final double blockingValue				= readDoubleAttribute(element, TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME_v12, Logger.VOID_LOGGER);
						// Linking
						final Element linkingElement = element.getChild(TRACKER_SETTINGS_LINKING_ELEMENT);
						final double linkingDistanceCutOff 		= readDistanceCutoffAttribute(linkingElement);
						final Map<String, Double> linkingFeaturePenalties = readTrackerFeatureMap(linkingElement);
						// Gap-closing
						final Element gapClosingElement 			= element.getChild(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
						final boolean allowGapClosing				= readBooleanAttribute(gapClosingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						final double gapClosingDistanceCutoff 	= readDistanceCutoffAttribute(gapClosingElement);
						final double gapClosingTimeCutoff 		= readTimeCutoffAttribute(gapClosingElement);
						final Map<String, Double> gapClosingFeaturePenalties = readTrackerFeatureMap(gapClosingElement);
						// Splitting
						final Element splittingElement	= element.getChild(TRACKER_SETTINGS_SPLITTING_ELEMENT);
						final boolean allowSplitting				= readBooleanAttribute(splittingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						final double splittingDistanceCutoff		= readDistanceCutoffAttribute(splittingElement);
						@SuppressWarnings("unused")
						final
						double splittingTimeCutoff			= readTimeCutoffAttribute(splittingElement); // IGNORED
						final Map<String, Double> splittingFeaturePenalties = readTrackerFeatureMap(splittingElement);
						// Merging
						final Element mergingElement 		= element.getChild(TRACKER_SETTINGS_MERGING_ELEMENT);
						final boolean allowMerging				= readBooleanAttribute(mergingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME_v12, Logger.VOID_LOGGER);
						final double mergingDistanceCutoff		= readDistanceCutoffAttribute(mergingElement);
						@SuppressWarnings("unused")
						final
						double mergingTimeCutoff			= readTimeCutoffAttribute(mergingElement); // IGNORED
						final Map<String, Double> mergingFeaturePenalties = readTrackerFeatureMap(mergingElement);

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
						// have something that works (when invoking the process methods of the trackmate).

						logger.error("\nTracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
								ts.getClass().getName()+"),\n");
						logger.error("substituting default values.\n");
					}

				} else if (trackerSettingsClassName.equals("fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings"))  {

					if (trackerKey.equals(NearestNeighborTracker.TRACKER_KEY)) {

						// The saved class matched, we can updated the settings created above with the file content
						final double maxDist = readDoubleAttribute(element, MAX_LINKING_DISTANCE_ATTRIBUTE, Logger.VOID_LOGGER);
						ts.put(KEY_LINKING_MAX_DISTANCE, maxDist);

					} else {

						// They do not match. We DO NOT give priority to what has been saved. That way we always
						// have something that works (when invoking the process methods of the trackmate).

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
		final Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY_v12);
		if (null == spotCollection)
			return null;

		// Retrieve children elements for each frame
		final List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY_v12);

		// Determine total number of spots
		int nspots = 0;
		for (final Element currentFrameContent : frameContent) {
			nspots += currentFrameContent.getChildren(SPOT_ELEMENT_KEY_v12).size();
		}

		// Instantiate cache
		cache = new ConcurrentHashMap<Integer, Spot>(nspots);

		int currentFrame = 0;
		ArrayList<Spot> spotList;
		final SpotCollection allSpots = new SpotCollection();

		for (final Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME_v12, logger);
			final List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY_v12);
			spotList = new ArrayList<Spot>(spotContent.size());
			for (final Element spotElement : spotContent) {
				final Spot spot = createSpotFrom(spotElement);
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
	private Map<Integer, Set<Integer>>  getFilteredSpotsIDs()  {
		final Element selectedSpotCollection = root.getChild(FILTERED_SPOT_ELEMENT_KEY_v12);
		if (null == selectedSpotCollection)
			return null;

		final List<Element> frameContent = selectedSpotCollection.getChildren(FILTERED_SPOT_COLLECTION_ELEMENT_KEY_v12);
		final Map<Integer, Set<Integer>> visibleIDs = new HashMap<Integer, Set<Integer>>(frameContent.size());

		for (final Element currentFrameContent : frameContent) {
			final int currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME_v12, logger);
			final List<Element> spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY_v12);
			final HashSet<Integer> IDs = new HashSet<Integer>(spotContent.size());
			// Loop over all spot element
			for (final Element spotEl : spotContent) {
				// Find corresponding spot in cache
				final int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME_v12, logger);
				IDs.add(ID);
			}

			visibleIDs.put(currentFrame, IDs);
		}
		return visibleIDs;
	}

	/**
	 * Read and return the list of track indices that define the filtered track collection.
	 * @throws DataConversionException
	 */
	private Set<Integer> readFilteredTrackIDs() {
		final Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY_v12);
		if (null == filteredTracksElement)
			return null;

		// Work because the track splitting from the graph is deterministic
		final List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY_v12);
		final HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (final Element indexElement : elements) {
			final Integer trackID = readIntAttribute(indexElement, TRACK_ID_ATTRIBUTE_NAME_v12, logger);
			if (null != trackID) {
				filteredTrackIndices.add(trackID);
			}
		}
		return filteredTrackIndices;
	}

	private ImagePlus getImage()  {
		final Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY_v12);
		if (null == imageInfoElement)
			return null;
		final String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_v12_ATTRIBUTE_NAME_v12);
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

	private Spot createSpotFrom(final Element spotEl) {
		final int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME_v12, logger);
		final Spot spot = new Spot(ID);

		final List<Attribute> atts = spotEl.getAttributes();
		atts.remove(SPOT_ID_ATTRIBUTE_NAME_v12);

		String name = spotEl.getAttributeValue(SPOT_NAME_v12_ATTRIBUTE_NAME_v12);
		if (null == name || name.equals(""))
			name = "ID"+ID;
		spot.setName(name);
		atts.remove(SPOT_NAME_v12_ATTRIBUTE_NAME_v12);

		for (final Attribute att : atts) {
			if (att.getName().equals(SPOT_NAME_v12_ATTRIBUTE_NAME_v12) || att.getName().equals(SPOT_ID_ATTRIBUTE_NAME_v12)) {
				continue;
			}
			spot.putFeature(att.getName(), Double.valueOf(att.getValue()));
		}
		return spot;
	}


	private boolean readDouble(final Element element, final String attName, final Map<String, Object> settings, final String mapKey) {
		final String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			final double val = Double.parseDouble(str);
			settings.put(mapKey, val);
		} catch (final NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as a double value. Got "+str+".";
			return false;
		}
		return true;
	}

	private boolean readInteger(final Element element, final String attName, final Map<String, Object> settings, final String mapKey) {
		final String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			final int val = Integer.parseInt(str);
			settings.put(mapKey, val);
		} catch (final NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as an integer value. Got "+str+".";
			return false;
		}
		return true;
	}

	private boolean readBoolean(final Element element, final String attName, final Map<String, Object> settings, final String mapKey) {
		final String str = element.getAttributeValue(attName);
		if (null == str) {
			errorMessage = "Attribute "+attName+" could not be found in XML element.";
			return false;
		}
		try {
			final boolean val = Boolean.parseBoolean(str);
			settings.put(mapKey, val);
		} catch (final NumberFormatException nfe) {
			errorMessage = "Could not read "+attName+" attribute as an boolean value. Got "+str+".";
			return false;
		}
		return true;
	}

	private static final double readDistanceCutoffAttribute(final Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME_v12).getDoubleValue();
		} catch (final DataConversionException e) { }
		return val;
	}

	private static final double readTimeCutoffAttribute(final Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME_v12).getDoubleValue();
		} catch (final DataConversionException e) { }
		return val;
	}

	/**
	 * Look for all the sub-elements of <code>element</code> with the name TRACKER_SETTINGS_FEATURE_ELEMENT,
	 * fetch the feature attributes from them, and returns them in a map.
	 */
	private static final Map<String, Double> readTrackerFeatureMap(final Element element) {
		final Map<String, Double> map = new HashMap<String, Double>();
		final List<Element> featurelinkingElements = element.getChildren(TRACKER_SETTINGS_FEATURE_ELEMENT);
		for (final Element el : featurelinkingElements) {
			final List<Attribute> atts = el.getAttributes();
			for (final Attribute att : atts) {
				final String feature = att.getName();
				Double cutoff;
				try {
					cutoff = att.getDoubleValue();
				} catch (final DataConversionException e) {
					cutoff = 0d;
				}
				map.put(feature, cutoff);
			}
		}
		return map;
	}

}

