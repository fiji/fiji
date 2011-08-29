package fiji.plugin.trackmate.io;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.contrib.input.LineNumberElement;
import org.jdom.contrib.input.LineNumberSAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterType;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.TrackerType;

public class TmXmlReader implements TmXmlKeys {


	private Document document = null;
	private File file;
	private Element root;
	private Logger logger;

	/*
	 * CONSTRUCTOR
	 */

	/**
	 * Initialize this reader to read the file given in argument. No actual parsing is made at construction.
	 */
	public TmXmlReader(File file, Logger logger) {
		this.file = file;
		this.logger = logger;
	}

	public TmXmlReader(File file) {
		this(file, Logger.DEFAULT_LOGGER);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Parse the file to create a JDom {@link Document}. This method must be called before using any of
	 * the other getter methods.
	 */
	public void parse() throws JDOMException,  IOException {
		LineNumberSAXBuilder sb = new LineNumberSAXBuilder();
		document = sb.build(file);
		root = document.getRootElement();
	}

	/**
	 * Return a {@link TrackMateModel} from all the information stored in this file.
	 * Fields not set in the field will be <code>null</code> in the model. 
	 * @throws DataConversionException 
	 */
	public TrackMateModel getModel() throws DataConversionException {
		TrackMateModel model = new TrackMateModel();
		// Settings
		Settings settings = getSettings();
		settings.segmenterSettings = getSegmenterSettings();
		settings.trackerSettings = getTrackerSettings();
		settings.imp = getImage();
		model.setSettings(settings);
		// Spot Filters
		List<FeatureFilter<SpotFeature>> spotFilters = getSpotFeatureFilters();
		FeatureFilter<SpotFeature> initialFilter = getInitialFilter();
		model.setInitialSpotFilterValue(initialFilter.value);
		model.setSpotFilters(spotFilters);
		// Spots
		SpotCollection allSpots = getAllSpots();
		SpotCollection filteredSpots = getFilteredSpots(allSpots);
		model.setSpots(allSpots, false);
		model.setFilteredSpots(filteredSpots, false);
		// Tracks
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = readTracks(filteredSpots);
		if (null != graph)
			model.setGraph(graph);
		// Track Filters
		List<FeatureFilter<TrackFeature>> trackFilters = getTrackFeatureFilters();
		model.setTrackFilters(trackFilters);
		// Filtered tracks
		Set<Integer> filteredTrackIndices = getFilteredTracks();
		if (null != filteredTrackIndices) {
			model.setVisibleTrackIndices(filteredTrackIndices, false);
		}
		// Return
		return model;
	}


	/**
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	public FeatureFilter<SpotFeature> getInitialFilter() throws DataConversionException {
		Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		if (null == itEl)
			return null;
		SpotFeature feature = SpotFeature.valueOf(itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME));
		Float value 	= itEl.getAttribute(FILTER_VALUE_ATTRIBUTE_NAME).getFloatValue();
		boolean isAbove	= itEl.getAttribute(FILTER_ABOVE_ATTRIBUTE_NAME).getBooleanValue();
		FeatureFilter<SpotFeature> ft = new FeatureFilter<SpotFeature>(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	@SuppressWarnings("unchecked")
	public List<FeatureFilter<SpotFeature>> getSpotFeatureFilters() throws DataConversionException {
		List<FeatureFilter<SpotFeature>> featureThresholds = new ArrayList<FeatureFilter<SpotFeature>>();
		Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			SpotFeature feature = SpotFeature.valueOf(ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME));
			Float value 	= ftEl.getAttribute(FILTER_VALUE_ATTRIBUTE_NAME).getFloatValue();
			boolean isAbove	= ftEl.getAttribute(FILTER_ABOVE_ATTRIBUTE_NAME).getBooleanValue();
			FeatureFilter<SpotFeature> ft = new FeatureFilter<SpotFeature>(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	/**
	 * Return the list of {@link FeatureFilter} for tracks stored in this file.
	 * Return <code>null</code> if the track feature filters data cannot be found in the file.
	 */
	@SuppressWarnings("unchecked")
	public List<FeatureFilter<TrackFeature>> getTrackFeatureFilters() throws DataConversionException {
		List<FeatureFilter<TrackFeature>> featureThresholds = new ArrayList<FeatureFilter<TrackFeature>>();
		Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			TrackFeature feature = TrackFeature.valueOf(ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME));
			Float value 	= ftEl.getAttribute(FILTER_VALUE_ATTRIBUTE_NAME).getFloatValue();
			boolean isAbove	= ftEl.getAttribute(FILTER_ABOVE_ATTRIBUTE_NAME).getBooleanValue();
			FeatureFilter<TrackFeature> ft = new FeatureFilter<TrackFeature>(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
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
	public Settings getSettings() throws DataConversionException {
		Settings settings = new Settings();
		// Basic settings
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null != settingsEl) {
			settings.xstart 		= settingsEl.getAttribute(SETTINGS_XSTART_ATTRIBUTE_NAME).getIntValue();
			settings.xend 			= settingsEl.getAttribute(SETTINGS_XEND_ATTRIBUTE_NAME).getIntValue();
			settings.ystart 		= settingsEl.getAttribute(SETTINGS_YSTART_ATTRIBUTE_NAME).getIntValue();
			settings.yend 			= settingsEl.getAttribute(SETTINGS_YEND_ATTRIBUTE_NAME).getIntValue();
			settings.zstart 		= settingsEl.getAttribute(SETTINGS_ZSTART_ATTRIBUTE_NAME).getIntValue();
			settings.zend 			= settingsEl.getAttribute(SETTINGS_ZEND_ATTRIBUTE_NAME).getIntValue();
			settings.tstart 		= settingsEl.getAttribute(SETTINGS_TSTART_ATTRIBUTE_NAME).getIntValue();
			settings.tend 			= settingsEl.getAttribute(SETTINGS_TEND_ATTRIBUTE_NAME).getIntValue();
		}
		// Image info settings
		Element infoEl 	= root.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx				= infoEl.getAttribute(IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME).getFloatValue();
			settings.dy				= infoEl.getAttribute(IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME).getFloatValue();
			settings.dz				= infoEl.getAttribute(IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME).getFloatValue();
			settings.dt				= infoEl.getAttribute(IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME).getFloatValue();
			settings.width			= infoEl.getAttribute(IMAGE_WIDTH_ATTRIBUTE_NAME).getIntValue();
			settings.height			= infoEl.getAttribute(IMAGE_HEIGHT_ATTRIBUTE_NAME).getIntValue();
			settings.nslices		= infoEl.getAttribute(IMAGE_NSLICES_ATTRIBUTE_NAME).getIntValue();
			settings.nframes		= infoEl.getAttribute(IMAGE_NFRAMES_ATTRIBUTE_NAME).getIntValue();
			settings.spaceUnits		= infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME);
			settings.timeUnits		= infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME);
			settings.imageFileName	= infoEl.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
			settings.imageFolder	= infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		}
		return settings;
	}

	public SegmenterSettings getSegmenterSettings() throws DataConversionException {
		SegmenterSettings segSettings = null;
		Element segSettingsEl = root.getChild(SEGMENTER_SETTINGS_ELEMENT_KEY);
		if (null != segSettingsEl) {
			String segmenterTypeStr = segSettingsEl.getAttributeValue(SEGMENTER_SETTINGS_SEGMENTER_TYPE_ATTRIBUTE_NAME);
			SegmenterType segmenterType = SegmenterType.valueOf(segmenterTypeStr);
			segSettings = segmenterType.createSettings();
			segSettings.segmenterType 		= segmenterType;
			segSettings.expectedRadius 		= segSettingsEl.getAttribute(SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME).getFloatValue();
			segSettings.threshold			= segSettingsEl.getAttribute(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME).getFloatValue();
			segSettings.useMedianFilter		= segSettingsEl.getAttribute(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME).getBooleanValue();
			segSettings.spaceUnits			= segSettingsEl.getAttributeValue(SEGMENTER_SETTINGS_UNITS_ATTRIBUTE_NAME);			
		}
		return segSettings;
	}


	public TrackerSettings getTrackerSettings() throws DataConversionException {
		// Tracker settings
		TrackerSettings trackerSettings = null;
		Element trackerSettingsEl = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null != trackerSettingsEl) {
			String trackerTypeStr 			= trackerSettingsEl.getAttributeValue(TRACKER_SETTINGS_TRACKER_TYPE_ATTRIBUTE_NAME);
			TrackerType trackerType 		= TrackerType.valueOf(trackerTypeStr);
			trackerSettings = trackerType.createSettings();
			trackerSettings.trackerType		= trackerType;
			trackerSettings.timeUnits		= trackerSettingsEl.getAttributeValue(TRACKER_SETTINGS_TIME_UNITS_ATTNAME);
			trackerSettings.spaceUnits		= trackerSettingsEl.getAttributeValue(TRACKER_SETTINGS_SPACE_UNITS_ATTNAME);
			trackerSettings.alternativeObjectLinkingCostFactor = trackerSettingsEl.getAttribute(TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME).getDoubleValue();
			trackerSettings.cutoffPercentile = trackerSettingsEl.getAttribute(TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME).getDoubleValue();
			trackerSettings.blockingValue	=  trackerSettingsEl.getAttribute(TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME).getDoubleValue();
			// Linking
			Element linkingElement 			= trackerSettingsEl.getChild(TRACKER_SETTINGS_LINKING_ELEMENT);
			trackerSettings.linkingDistanceCutOff = readDistanceCutoffAttribute(linkingElement);
			trackerSettings.linkingFeaturePenalties = readTrackerFeatureMap(linkingElement);
			// Gap-closing
			Element gapClosingElement		= trackerSettingsEl.getChild(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
			trackerSettings.allowGapClosing	= gapClosingElement.getAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME).getBooleanValue();
			trackerSettings.gapClosingDistanceCutoff 	= readDistanceCutoffAttribute(gapClosingElement);
			trackerSettings.gapClosingTimeCutoff 		= readTimeCutoffAttribute(gapClosingElement); 
			trackerSettings.gapClosingFeaturePenalties 	= readTrackerFeatureMap(gapClosingElement);
			// Splitting
			Element splittingElement		= trackerSettingsEl.getChild(TRACKER_SETTINGS_SPLITTING_ELEMENT);
			trackerSettings.allowSplitting	= splittingElement.getAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME).getBooleanValue();
			trackerSettings.splittingDistanceCutoff		= readDistanceCutoffAttribute(splittingElement);
			trackerSettings.splittingTimeCutoff			= readTimeCutoffAttribute(splittingElement);
			trackerSettings.splittingFeaturePenalties		= readTrackerFeatureMap(splittingElement);
			// Merging
			Element mergingElement 			= trackerSettingsEl.getChild(TRACKER_SETTINGS_MERGING_ELEMENT);
			trackerSettings.allowMerging	= mergingElement.getAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME).getBooleanValue();
			trackerSettings.mergingDistanceCutoff		= readDistanceCutoffAttribute(mergingElement);
			trackerSettings.mergingTimeCutoff			= readTimeCutoffAttribute(mergingElement);
			trackerSettings.mergingFeaturePenalties		= readTrackerFeatureMap(mergingElement);
		}
		return trackerSettings;
	}


	/**
	 * Return the list of all spots stored in this file.
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 * @return  a {@link SpotCollection}. Return <code>null</code> if the spot section is not present in the file.
	 */
	@SuppressWarnings("unchecked")
	public SpotCollection getAllSpots() throws DataConversionException {
		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;

		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);
		int currentFrame = 0;
		ArrayList<Spot> spotList;
		SpotCollection allSpots = new SpotCollection();

		for (Element currentFrameContent : frameContent) {

			currentFrame = currentFrameContent.getAttribute(FRAME_ATTRIBUTE_NAME).getIntValue();
			List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			for (Element spotElement : spotContent) {
				Spot spot = createSpotFrom(spotElement);
				spotList.add(spot);
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
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 */
	@SuppressWarnings("unchecked")
	public SpotCollection getFilteredSpots(SpotCollection allSpots) throws DataConversionException {
		Element selectedSpotCollection = root.getChild(FILTERED_SPOT_ELEMENT_KEY);
		if (null == selectedSpotCollection)
			return null;

		int currentFrame = 0;
		int ID;
		ArrayList<Spot> spotList;
		List<Element> spotContent;
		List<Spot> spotsThisFrame;
		SpotCollection spotSelection = new SpotCollection();
		List<Element> frameContent = selectedSpotCollection.getChildren(FILTERED_SPOT_COLLECTION_ELEMENT_KEY);

		for (Element currentFrameContent : frameContent) {
			currentFrame = currentFrameContent.getAttribute(FRAME_ATTRIBUTE_NAME).getIntValue();
			// Get spot list from main list
			spotsThisFrame = allSpots.get(currentFrame);
			if (null == spotsThisFrame)
				continue;

			spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			// Loop over all spot element
			for (Element spotEl : spotContent) {
				ID = spotEl.getAttribute(SPOT_ID_ATTRIBUTE_NAME).getIntValue();
				// Find corresponding spot in main list
				for (Spot spot : spotsThisFrame) {
					if (ID == spot.ID()) {
						spotList.add(spot);
						break;
					}
				}
			}

			spotSelection.put(currentFrame, spotList);
		}
		return spotSelection;
	}

	/**
	 * Load the graph mapping spot linking as tracks. The graph vertices are made of the selected spot
	 * list given in argument. Edges are formed from the file data.
	 * @param selectedSpots  the spot selection from which tracks area made 
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 */
	@SuppressWarnings("unchecked")
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> readTracks(final SpotCollection spots) throws DataConversionException {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		for (Spot spot : spots)
			graph.addVertex(spot);

				// Load tracks
				List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
				List<Element> edgeElements;
				int sourceID, targetID;
				Spot sourceSpot, targetSpot;
				double weight = 0;
				boolean sourceFound, targetFound;

				for (Element trackElement : trackElements) {
					edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
					for (Element edgeElement : edgeElements) {
						// Get source and target ID for this edge
						sourceID = edgeElement.getAttribute(TRACK_EDGE_SOURCE_ATTRIBUTE_NAME).getIntValue();
						targetID = edgeElement.getAttribute(TRACK_EDGE_TARGET_ATTRIBUTE_NAME).getIntValue();
						if (null != edgeElement.getAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME))
							weight   	= edgeElement.getAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME).getDoubleValue();
						else 
							weight  	= 0;
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
									LineNumberElement lne = (LineNumberElement) edgeElement;
									logger.error("Bad edge found for track "+trackElement.getAttributeValue(TRACK_ID_ATTRIBUTE_NAME)
											+": loop edge at line "+lne.getStartLine()+". Skipping.");
									break;
								}
								DefaultWeightedEdge edge = graph.addEdge(sourceSpot, targetSpot);
								if (edge == null) {
									LineNumberElement lne = (LineNumberElement) edgeElement;
									logger.error("Bad edge found for track "+trackElement.getAttributeValue(TRACK_ID_ATTRIBUTE_NAME)
											+": duplicate edge at line "+lne.getStartLine()+". Skipping.");
									break;
								} else {
									graph.setEdgeWeight(edge, weight);
								}
								break;
							}
						}
					}
				}
				return graph;
	}

	/**
	 * Read and return the list of track indices that define the filtered track collection.
	 * @throws DataConversionException 
	 */
	public Set<Integer> getFilteredTracks() throws DataConversionException {
		Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY);
		if (null == filteredTracksElement)
			return null;

		// Work because the track splitting from the graph is deterministic
		@SuppressWarnings("unchecked")
		List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY);
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (Element indexElement : elements) {
			Integer trackID = indexElement.getAttribute(TRACK_ID_ATTRIBUTE_NAME).getIntValue();
			if (null != trackID) {
				filteredTrackIndices.add(trackID);
			}
		}
		return filteredTrackIndices;
	}

	public ImagePlus getImage()  {
		Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY);
		if (null == imageInfoElement)
			return null;
		String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
		String folder 	= imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		if (null == filename || filename.isEmpty())
			return null;
		if (null == folder || folder.isEmpty())
			folder = file.getParent(); // it is a relative path, then
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead())
			return null;
		return IJ.openImage(imageFile.getAbsolutePath());
	}


	/*
	 * PRIVATE METHODS
	 */



	private static final double readDistanceCutoffAttribute(Element element) throws DataConversionException {
		return element.getChild(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.getAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME).getDoubleValue();
	}

	private static final double readTimeCutoffAttribute(Element element) throws DataConversionException {
		return element.getChild(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.getAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME).getDoubleValue();
	}

	/**
	 * Look for all the sub-elements of <code>element</code> with the name TRACKER_SETTINGS_FEATURE_ELEMENT, 
	 * fetch the feature attributes from them, and returns them in a map.
	 */
	@SuppressWarnings("unchecked")
	private static final Map<SpotFeature, Double> readTrackerFeatureMap(final Element element) throws DataConversionException {
		Map<SpotFeature, Double> map = new HashMap<SpotFeature, Double>();
		List<Element> featurelinkingElements = element.getChildren(TRACKER_SETTINGS_FEATURE_ELEMENT);
		for (Element el : featurelinkingElements) {
			List<Attribute> atts = el.getAttributes();
			for (Attribute att : atts) {
				String featureStr = att.getName();
				SpotFeature feature = SpotFeature.valueOf(featureStr);
				Double cutoff = att.getDoubleValue();
				map.put(feature, cutoff);
			}
		}
		return map;
	}


	private static Spot createSpotFrom(Element spotEl) throws DataConversionException {
		int ID = spotEl.getAttribute(SPOT_ID_ATTRIBUTE_NAME).getIntValue();
		Spot spot = new SpotImp(ID);
		String name = spotEl.getAttributeValue(SPOT_NAME_ATTRIBUTE_NAME);
		if (null == name || name.equals(""))
			name = "ID"+ID;
		spot.setName(name);
		for (SpotFeature feature : SpotFeature.values()) {
			Attribute att = spotEl.getAttribute(feature.name());
			if (null == att)
				continue;
			spot.putFeature(feature, att.getFloatValue());
		}
		return spot;
	}
}
