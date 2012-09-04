package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.io.TmXmlKeys_v12.*;
import static fiji.plugin.trackmate.util.TMUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readFloatAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readIntAttribute;
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

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.tracking.FastLAPTracker;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings;

/**
 * A compatibility xml loader than can load TrackMate xml file saved for version
 * prior to 1.3. In the code, we keep the previous vocable of "segmenter"...
 * The code here is extremely pedestrian; we deal with all particular cases
 * explicitly, and convert on the fly to v1.3 classes. 
 * @author Jean-Yves Tinevez - 2012
 */
public class TmXmlReader_v12<T extends RealType<T> & NativeType<T>> extends TmXmlReader<T>{

	/** Stores error messages when reading parameters. */
	private String errorMessage;

	/*
	 * CONSTRUCTORS
	 */


	public TmXmlReader_v12(File file, Logger logger, TrackMate_<T> plugin) {
		super(file, logger, plugin);
	}

	public TmXmlReader_v12(File file, Logger logger) {
		super(file, logger);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Return a {@link TrackMateModel} from all the information stored in this file.
	 * Fields not set in the field will be <code>null</code> in the model.
	 * @throws DataConversionException
	 */
	public TrackMateModel<T> getModel() {
		TrackMateModel<T> model = new TrackMateModel<T>();
		// Settings
		Settings<T> settings = getSettings();
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
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = readTrackGraph();
		if (null != graph) {
			model.setGraph(graph);
		}

		// Track Filters
		List<FeatureFilter> trackFilters = getTrackFeatureFilters();
		model.getSettings().setTrackFilters(trackFilters);
		// Filtered tracks
		Set<Integer> filteredTrackIndices = getFilteredTracks();
		if (null != filteredTrackIndices) {
			model.setVisibleTrackIndices(filteredTrackIndices, false);
			model.setTrackSpots(readTrackSpots(graph));
			model.setTrackEdges(readTrackEdges(graph));
		}
		// Track features
		readTrackFeatures(model.getFeatureModel());

		// Return
		return model;
	}


	/**
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	@Override
	public FeatureFilter getInitialFilter()  {
		if (!parsed)
			parse();
		Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		if (null == itEl)
			return null;
		String feature  = itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
		double value     = readFloatAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
		boolean isAbove = readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
		FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	@Override
	public List<FeatureFilter> getSpotFeatureFilters() {
		if (!parsed)
			parse();
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		@SuppressWarnings("unchecked")
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			String feature  = ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			double value     = readFloatAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			boolean isAbove = readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
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
	@Override
	public Settings<T> getSettings() {
		if (!parsed)
			parse();
		Settings<T> settings = new Settings<T>();
		// Basic settings
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null != settingsEl) {
			settings.xstart = readIntAttribute(settingsEl, SETTINGS_XSTART_ATTRIBUTE_NAME, logger, 1);
			settings.xend   = readIntAttribute(settingsEl, SETTINGS_XEND_ATTRIBUTE_NAME, logger, 512);
			settings.ystart = readIntAttribute(settingsEl, SETTINGS_YSTART_ATTRIBUTE_NAME, logger, 1);
			settings.yend   = readIntAttribute(settingsEl, SETTINGS_YEND_ATTRIBUTE_NAME, logger, 512);
			settings.zstart = readIntAttribute(settingsEl, SETTINGS_ZSTART_ATTRIBUTE_NAME, logger, 1);
			settings.zend   = readIntAttribute(settingsEl, SETTINGS_ZEND_ATTRIBUTE_NAME, logger, 10);
			settings.tstart = readIntAttribute(settingsEl, SETTINGS_TSTART_ATTRIBUTE_NAME, logger, 1);
			settings.tend   = readIntAttribute(settingsEl, SETTINGS_TEND_ATTRIBUTE_NAME, logger, 10);
		}
		// Image info settings
		Element infoEl  = root.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx             = readFloatAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger);
			settings.dy             = readFloatAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger);
			settings.dz             = readFloatAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger);
			settings.dt             = readFloatAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, logger);
			settings.width          = readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME, logger, 512);
			settings.height         = readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME, logger, 512);
			settings.nslices        = readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME, logger, 1);
			settings.nframes        = readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME, logger, 1);
			settings.spaceUnits     = infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME);
			settings.timeUnits      = infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME);
			settings.imageFileName  = infoEl.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
			settings.imageFolder    = infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		}
		return settings;
	}

	@Override
	public void getDetectorSettings(Settings<T> settings) {
		if (!parsed)
			parse();

		// We have to parse the settings element to fetch the target channel
		int targetChannel = 1;
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null != settingsEl) {
			targetChannel = readIntAttribute(settingsEl, SETTINGS_SEGMENTATION_CHANNEL_ATTRIBUTE_NAME, logger);
		}
		
		// Get back to segmenter element
		Element element = root.getChild(SEGMENTER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}
		
		// Deal with segmenter
		String segmenterKey;
		String segmenterClassName = element.getAttributeValue(SEGMENTER_CLASS_ATTRIBUTE_NAME);
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
				segmenterKey = ManualDetectorFactory.NAME;
			} else {
				logger.error("\nUnknown segmenter: "+segmenterClassName+".\n");
				logger.error("Substituting default.\n");
				segmenterKey = LogDetectorFactory.DETECTOR_KEY;
			}
		}
		DetectorProvider<T> provider = plugin.getDetectorProvider();
		boolean ok = provider.select(segmenterKey);
		if (!ok) {
			logger.error(provider.getErrorMessage());
			logger.error("substituting default detector.\n");
		}
		settings.detectorFactory = provider.getDetectorFactory();

		// Deal with segmenter settings
		Map<String, Object> ds = new HashMap<String, Object>();

		String segmenterSettingsClassName = element.getAttributeValue(SEGMENTER_SETTINGS_CLASS_ATTRIBUTE_NAME);

		if (null == segmenterSettingsClassName) {

			logger.error("\nSegmenter settings class is not present,\n");
			logger.error("substituting default settings values.\n");
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
	@Override
	public void getTrackerSettings(Settings<T> settings) {
		if (!parsed)
			parse();
		Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		// Deal with tracker
		String trackerName;
		String trackerClassName = element.getAttributeValue(TRACKER_CLASS_ATTRIBUTE_NAME);

		if (null == trackerClassName) {
			logger.error("\nTracker class is not present.\n");
			logger.error("Substituting default.\n");
			trackerName = SimpleFastLAPTracker.NAME;

		} else {

			if (trackerClassName.equals("fiji.plugin.trackmate.tracking.SimpleFastLAPTracker") || 
					trackerClassName.equals("fiji.plugin.trackmate.tracking.SimpleLAPTracker")) {
				trackerName = SimpleFastLAPTracker.NAME; // convert to simple fast version

			} else if (trackerClassName.equals("fiji.plugin.trackmate.tracking.FastLAPTracker") || 
					trackerClassName.equals("fiji.plugin.trackmate.tracking.LAPTracker")) {
				trackerName = FastLAPTracker.NAME; // convert to fast version

			} else if (trackerClassName.equals("fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker")) {
				trackerName = NearestNeighborTracker.NAME;

			} else {
				logger.error("\nUnknown tracker: "+trackerClassName+".\n");
				logger.error("Substituting default.\n");
				trackerName = SimpleFastLAPTracker.NAME;
			}
		}
		settings.tracker = trackerName;

		// Deal with tracker settings
		{
			// To start with, we get a settings suitable for the tracker we have found
			TrackerSettings<T> ts = plugin.getTrackerFactory().getDefaultSettings(trackerName);
			String trackerSettingsClassName = element.getAttributeValue(TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME);
			if (null == trackerSettingsClassName) {

				logger.error("\nTracker settings class is not present,\n");
				logger.error("substituting default one.\n");

			} else {

				// All LAP trackers
				if (trackerSettingsClassName.equals("fiji.plugin.trackmate.tracking.LAPTrackerSettings"))  {

					if (ts.getClass().equals(TrackerKeys.class)) {

						// The saved class matched, we can updated the settings created above with the file content
						ts.unmarshall(element);

					} else {

						// They do not match. We DO NOT give priority to what has been saved. That way we always
						// have something that works (when invoking the process methods of the plugin).

						logger.error("\nTracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
								ts.getClass().getName()+"),\n");
						logger.error("substituting default values.\n");
					}

				} else if (trackerSettingsClassName.equals("fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings"))  {

					if (ts.getClass().equals(NearestNeighborTrackerSettings.class)) {

						// The saved class matched, we can updated the settings created above with the file content
						ts.unmarshall(element);

					} else {

						// They do not match. We DO NOT give priority to what has been saved. That way we always
						// have something that works (when invoking the process methods of the plugin).

						logger.error("\nTracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
								ts.getClass().getName()+"),\n");
						logger.error("substituting default values.\n");
					}

				} else {

					logger.error("\nTracker settings class ("+trackerSettingsClassName+") is unknown,\n");
					logger.error("substituting default one.\n");

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
	@Override
	public SpotCollection getAllSpots() {
		if (!parsed) {
			parse();
		}

		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;

		// Retrieve children elements for each frame
		@SuppressWarnings("unchecked")
		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);

		// Determine total number of spots
		int nspots = 0;
		for (Element currentFrameContent : frameContent) {
			nspots += currentFrameContent.getChildren(SPOT_ELEMENT_KEY).size();
		}

		// Instantiate cache
		cache = new ConcurrentHashMap<Integer, Spot>(nspots);

		int currentFrame = 0;
		ArrayList<Spot> spotList;
		SpotCollection allSpots = new SpotCollection();

		for (Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
			@SuppressWarnings("unchecked")
			List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY);
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
	@SuppressWarnings("unchecked")
	@Override
	public SpotCollection getFilteredSpots()  {
		if (!parsed) {
			parse();
		}

		Element selectedSpotCollection = root.getChild(FILTERED_SPOT_ELEMENT_KEY);
		if (null == selectedSpotCollection)
			return null;

		if (null == cache)
			getAllSpots(); // build it if it's not here

		int currentFrame = 0;
		int ID;
		ArrayList<Spot> spotList;
		List<Element> spotContent;
		SpotCollection spotSelection = new SpotCollection();
		List<Element> frameContent = selectedSpotCollection.getChildren(FILTERED_SPOT_COLLECTION_ELEMENT_KEY);

		for (Element currentFrameContent : frameContent) {
			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
			spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			// Loop over all spot element
			for (Element spotEl : spotContent) {
				// Find corresponding spot in cache
				ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
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
	@SuppressWarnings("unchecked")
	public List<Set<Spot>> readTrackSpots(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		// Retrieve all spots from the graph
		final Set<Spot> spots = graph.vertexSet();

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
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
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME, logger);


			// Instantiate current track
			HashSet<Spot> track = new HashSet<Spot>(2*trackElements.size()); // approx

			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME, logger);

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
	@SuppressWarnings("unchecked")
	public List<Set<DefaultWeightedEdge>> readTrackEdges(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph) {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		// Retrieve all spots from the graph
		final Set<Spot> spots = graph.vertexSet();

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
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
			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME, logger);

			// Instantiate current track
			HashSet<DefaultWeightedEdge> track = new HashSet<DefaultWeightedEdge>(edgeElements.size());

			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME, logger);

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
	public Set<Integer> getFilteredTracks() {
		Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY);
		if (null == filteredTracksElement)
			return null;

		// Work because the track splitting from the graph is deterministic
		@SuppressWarnings("unchecked")
		List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY);
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (Element indexElement : elements) {
			Integer trackID = readIntAttribute(indexElement, TRACK_ID_ATTRIBUTE_NAME, logger);
			if (null != trackID) {
				filteredTrackIndices.add(trackID);
			}
		}
		return filteredTrackIndices;
	}

	@Override
	public ImagePlus getImage()  {
		if (!parsed)
			parse();
		Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY);
		if (null == imageInfoElement)
			return null;
		String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
		String folder   = imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
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


	/*
	 * PRIVATE METHODS
	 */

	private Spot createSpotFrom(Element spotEl) {
		int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
		Spot spot = new SpotImp(ID);

		@SuppressWarnings("unchecked")
		List<Attribute> atts = spotEl.getAttributes();
		atts.remove(SPOT_ID_ATTRIBUTE_NAME);

		String name = spotEl.getAttributeValue(SPOT_NAME_ATTRIBUTE_NAME);
		if (null == name || name.equals(""))
			name = "ID"+ID;
		spot.setName(name);
		atts.remove(SPOT_NAME_ATTRIBUTE_NAME);

		for (Attribute att : atts) {
			if (att.getName().equals(SPOT_NAME_ATTRIBUTE_NAME) || att.getName().equals(SPOT_ID_ATTRIBUTE_NAME)) {
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
			double val = Double.parseDouble(str);
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
			int val = Integer.parseInt(str);
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
}
