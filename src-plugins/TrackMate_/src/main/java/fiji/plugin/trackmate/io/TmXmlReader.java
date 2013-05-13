package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntAttribute;
import static fiji.plugin.trackmate.io.TmXmlKeys.DETECTOR_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTERED_SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTERED_SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTERED_TRACK_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ABOVE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_FEATURE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_VALUE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FRAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FILENAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FOLDER_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NFRAMES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NSLICES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_TIME_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.INITIAL_SPOT_FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_TEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_TSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_XEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_XSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_YEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_YSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ZEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ZSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FRAME_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ID_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ID_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_NAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACKER_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_EDGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ID_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_NAME_ATTRIBUTE_NAME;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Logger.StringBuilderLogger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.tracking.SpotTracker;


public class TmXmlReader implements Algorithm, Benchmark {

	protected static final boolean DEBUG = true;

	protected Document document = null;
	protected File file;
	protected Element root;
	/** The plugin instance to operate on. This must be provided, in the case we want to load 
	 * a file created with a subclass of {@link TrackMate_} (e.g. with new factories) so that 
	 * correct detectors, etc... can be instantiated from the extended plugin.
	 */
	protected final TrackMate_ plugin;
	/** A map of all spots loaded. We need this for performance, since we need to recreate 
	 * both the filtered spot collection and the tracks graph from the same spot objects 
	 * that the main spot collection.. In the file, they are referenced by their {@link Spot#ID()},
	 * and parsing them all to retrieve the one with the right ID is a drag.
	 * We made this cache a {@link ConcurrentHashMap} because we hope to load large data in a 
	 * multi-threaded way.
	 */
	protected ConcurrentHashMap<Integer, Spot> cache;

	protected long processingTime;
	protected StringBuilderLogger logger = new StringBuilderLogger();

	private String log;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Initialize this reader to read the file given in argument. 
	 * <p>
	 * A plugin instance must be provided, and this instance must be initialized with providers
	 * (as in when calling {@link TrackMate_#initModules()}. This in the case we want to load 
	 * a file created with a subclass of {@link TrackMate_} (e.g. with new factories) so that 
	 * correct detectors, etc... can be instantiated from the extended plugin.
	 * <p>
	 * The given plugin instance will be modified by this class, upon calling the {@link #process()}
	 * method 
	 */
	public TmXmlReader(File file, TrackMate_ plugin) {
		this.file = file;
		this.plugin = plugin;
		parse();
	}

	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * @return  the log text saved in the specified file, or <code>null</code> if log
	 * text was not saved. Must be called after {@link #process()}.
	 */
	public String getLogText() {
		return log;
	}

	@Override
	public boolean process() {

		long start = System.currentTimeMillis();

		TrackMateModel model = plugin.getModel();
		
		// Text log
		log = getLog();
		
		// Settings
		Settings settings = getSettings();
		getDetectorSettings(settings);
		getTrackerSettings(settings);
		settings.imp = getImage();
		model.setSettings(settings);

		// Spot Filters
		FeatureFilter initialFilter = getInitialFilter();
		if (null != initialFilter) {
			model.getSettings().initialSpotFilterValue = initialFilter.value;
		} else {
			model.getSettings().initialSpotFilterValue = null; // So that everyone knows we did NOT find it in the file
		}
		List<FeatureFilter> spotFilters = getSpotFeatureFilters();
		model.getSettings().setSpotFilters(spotFilters);
		// Spots
		SpotCollection allSpots = getAllSpots();
		SpotCollection filteredSpots = getFilteredSpots();
		model.setSpots(allSpots, true);
		model.setFilteredSpots(filteredSpots, true);
		// Tracks, filtered tracks and track features all at once
		if (!readTracks()) {
			return false;
		}

		// Track Filters
		List<FeatureFilter> trackFilters = getTrackFeatureFilters();
		model.getSettings().setTrackFilters(trackFilters);

		long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	/**
	 * @return the version string stored in the file.
	 */
	public String getVersion() {
		return root.getAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME).getValue();
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public boolean checkInput() {
		if (null == document) {
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage() {
		return logger.toString();
	}

	/*
	 * PRIVATE METHODS
	 */

	private String getLog() {
		Element logElement = root.getChild(LOG_ELEMENT_KEY);
		if (null != logElement) {
			return logElement.getTextTrim();
		} else {
			return "";
		}
	}

	
	private ImagePlus getImage()  {
		Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY);
		if (null == imageInfoElement)
			return null; // value will still be null
		String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
		String folder 	= imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		if (null == filename || filename.isEmpty())
			return null;
		if (null == folder || folder.isEmpty())
			folder = file.getParent(); // it is a relative path, then
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead()) {
			// Could not find it to the absolute path. Then we look for the same path of the xml file
			folder = file.getParent();
			imageFile = new File(folder, filename);
			if (!imageFile.exists() || !imageFile.canRead()) {
				return null;
			}
		}
		return IJ.openImage(imageFile.getAbsolutePath());
	}


	/**
	 * Parse the file to create a JDom {@link Document}. This method is called at construction.
	 */
	private void parse() {
		SAXBuilder sb = new SAXBuilder();
		try {
			document = sb.build(file);
			root = document.getRootElement();
		} catch (JDOMException e) {
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"
					+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()
					+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
		}
	}

	/**
	 * @return a map of the saved track features, as they appear in the file
	 */
	private Map<Integer,Map<String,Double>> readTrackFeatures() {

		HashMap<Integer, Map<String, Double>> featureMap = new HashMap<Integer, Map<String, Double>>();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		for (Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TrackIndexAnalyzer.TRACK_ID).getIntValue();
			} catch (DataConversionException e1) {
				logger.error("Found a track with invalid trackID for " + trackElement + ". Skipping.\n");
				continue;
			}

			HashMap<String, Double> trackMap = new HashMap<String, Double>();

			List<Attribute> attributes = trackElement.getAttributes();
			for(Attribute attribute : attributes) {

				String attName = attribute.getName();
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
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	private FeatureFilter getInitialFilter()  {

		Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		if (null == itEl)
			return null;
		String feature 	= itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
		Double value 	= readDoubleAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
		boolean isAbove	= readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
		FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	private List<FeatureFilter> getSpotFeatureFilters() {

		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
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
	 * Return the settings for the TrackMate session saved in this file.
	 * <p>
	 * The settings object will have its {@link DetectorSettings} and {@link TrackerSettings} set default values will be
	 * used.
	 * 
	 * @return  a full Settings object
	 * @throws DataConversionException 
	 */
	private Settings getSettings() {
		Settings settings = new Settings();
		// Basic settings
		Element settingsEl = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null != settingsEl) {
			settings.xstart = readIntAttribute(settingsEl, SETTINGS_XSTART_ATTRIBUTE_NAME, logger, 1);
			settings.xend 	= readIntAttribute(settingsEl, SETTINGS_XEND_ATTRIBUTE_NAME, logger, 512);
			settings.ystart = readIntAttribute(settingsEl, SETTINGS_YSTART_ATTRIBUTE_NAME, logger, 1);
			settings.yend 	= readIntAttribute(settingsEl, SETTINGS_YEND_ATTRIBUTE_NAME, logger, 512);
			settings.zstart = readIntAttribute(settingsEl, SETTINGS_ZSTART_ATTRIBUTE_NAME, logger, 1);
			settings.zend 	= readIntAttribute(settingsEl, SETTINGS_ZEND_ATTRIBUTE_NAME, logger, 10);
			settings.tstart = readIntAttribute(settingsEl, SETTINGS_TSTART_ATTRIBUTE_NAME, logger, 1);
			settings.tend 	= readIntAttribute(settingsEl, SETTINGS_TEND_ATTRIBUTE_NAME, logger, 10);
			//			settings.detectionChannel = readIntAttribute(settingsEl, SETTINGS_DETECTION_CHANNEL_ATTRIBUTE_NAME, logger, 1);
		}
		// Image info settings
		Element infoEl 	= root.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx				= readDoubleAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger);
			settings.dy				= readDoubleAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger);
			settings.dz				= readDoubleAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger);
			settings.dt				= readDoubleAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, logger);
			settings.width			= readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME, logger, 512);
			settings.height			= readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME, logger, 512);
			settings.nslices		= readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME, logger, 1);
			settings.nframes		= readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME, logger, 1);
			settings.spaceUnits		= infoEl.getAttributeValue(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME);
			settings.timeUnits		= infoEl.getAttributeValue(IMAGE_TIME_UNITS_ATTRIBUTE_NAME);
			settings.imageFileName	= infoEl.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
			settings.imageFolder	= infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		}
		return settings;
	}

	/**
	 * Update the given {@link Settings} object with the {@link SpotDetectorFactory} and settings map fields
	 * named {@link Settings#detectorFactory}  and {@link Settings#detectorSettings} read within the XML file
	 * this reader is initialized with. 
	 * <p>
	 * As a side effect, this method also configure the {@link DetectorProvider} stored in 
	 * the passed {@link TrackMate_} plugin for the found target detector factory. 
	 * <p>
	 * If the detector settings XML element is not present in the file, the {@link Settings} 
	 * object is not updated. 

	 * @param settings  the base {@link Settings} object to update.
	 */
	private void getDetectorSettings(Settings settings) {
		Element element = root.getChild(DETECTOR_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		DetectorProvider provider = plugin.getDetectorProvider();
		Map<String, Object> ds = new HashMap<String, Object>(); 
		// All the hard work is delegated to the provider. 
		boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			return;
		}

		settings.detectorSettings = ds;
		settings.detectorFactory = provider.getDetectorFactory();
	}

	/**
	 * Update the given {@link Settings} object with {@link SpotTracker} proper settings map fields 
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
		Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		TrackerProvider provider = plugin.getTrackerProvider();
		Map<String, Object> ds = new HashMap<String, Object>(); 
		// All the hard work is delegated to the provider. 
		boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			return;
		}

		settings.trackerSettings = ds;
		settings.tracker = provider.getTracker();
	}

	/**
	 * Read the list of all spots stored in this file.
	 * <p>
	 * Internally, this methods also builds the cache field, which will be required by the
	 * following methods:
	 * <ul>
	 * 	<li> {@link #getFilteredSpots()}
	 * 	<li> {@link #readTracks()}
	 * 	<li> {@link #readTrackEdges(SimpleDirectedWeightedGraph)}
	 * 	<li> {@link #readTrackSpots(SimpleDirectedWeightedGraph)}
	 * </ul>
	 * It is therefore sensible to call this method first, just afther {@link #parse()}ing the file.
	 * If not called, this method will be called anyway by the other methods to build the cache.
	 * 
	 * @return  a {@link SpotCollection}. Return <code>null</code> if the spot section is not present in the file.
	 */
	private SpotCollection getAllSpots() {
		// Root element for collection
		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;

		// Retrieve children elements for each frame
		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);

		// Determine total number of spots
		int nspots = readIntAttribute(spotCollection, SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME, Logger.VOID_LOGGER);
		if (nspots == 0) {
			// Could not find it or read it. Determine it by quick sweeping through children element
			for (Element currentFrameContent : frameContent) {
				nspots += currentFrameContent.getChildren(SPOT_ELEMENT_KEY).size();
			}
		}

		// Instantiate cache
		cache = new ConcurrentHashMap<Integer, Spot>(nspots);

		// Load collection and build cache
		int currentFrame = 0;
		ArrayList<Spot> spotList;
		SpotCollection allSpots = new SpotCollection();

		for (Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
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
	 * Read the filtered spots stored in this file, taken from the list of all spots, given in argument.
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
	 * Load the tracks, the track features and the ID of the filtered tracks into the model
	 * modified by this reader. 
	 * @return 
	 * @return true if reading tracks was successsful, false otherwise.
	 */
	private boolean readTracks() {

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return true;

		if (null == cache) 
			getAllSpots(); // build the cache if it's not there

		final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		List<Element> edgeElements;

		// A temporary map that maps stored track key to one of its spot
		HashMap<Integer, Spot> savedTrackMap = new HashMap<Integer, Spot>(trackElements.size());
		HashMap<Integer, String> savedTrackNames = new HashMap<Integer, String>(trackElements.size());
		
		// The list of edge features. that we will set.
		final FeatureModel fm = plugin.getModel().getFeatureModel();
		List<String> edgeIntFeatures = new ArrayList<String>();// TODO is there a better way?
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_SOURCE_ID);
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_TARGET_ID);
		List<String> edgeDoubleFeatures = fm.getEdgeFeatures();
		edgeDoubleFeatures.removeAll(edgeIntFeatures);

		for (Element trackElement : trackElements) {

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TrackIndexAnalyzer.TRACK_ID, logger);
			String trackName = trackElement.getAttributeValue(TRACK_NAME_ATTRIBUTE_NAME);
			if (null == trackName) {
				trackName = "Unnamed";
			}
			// Keep a reference of one of the spot for outside the loop.
			Spot sourceSpot = null; 

			// Iterate over edges
			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);

			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				int sourceID = readIntAttribute(edgeElement, EdgeTargetAnalyzer.SPOT_SOURCE_ID, logger);
				int targetID = readIntAttribute(edgeElement, EdgeTargetAnalyzer.SPOT_TARGET_ID, logger);

				// Get matching spots from the cache
				sourceSpot = cache.get(sourceID);
				Spot targetSpot = cache.get(targetID);

				// Get weight
				double weight = 0;
				if (null != edgeElement.getAttribute(EdgeTargetAnalyzer.EDGE_COST)) {
					weight = readDoubleAttribute(edgeElement, EdgeTargetAnalyzer.EDGE_COST, logger);
				}

				// Error check
				if (null == sourceSpot) {
					logger.error("Unknown spot ID: "+sourceID + "\n");
					return false;
				}
				if (null == targetSpot) {
					logger.error("Unknown spot ID: "+targetID + "\n");
					return false;
				}

				if (sourceSpot.equals(targetSpot)) {
					logger.error("Bad link for track " + trackID + ". Source = Target with ID: " + sourceID + "\n");
					return false;
				}

				// Add spots to graph and build edge
				graph.addVertex(sourceSpot);
				graph.addVertex(targetSpot);
				DefaultWeightedEdge edge = graph.addEdge(sourceSpot, targetSpot);

				if (edge == null) {
					logger.error("Bad edge found for track " + trackID + "\n");
					return false;
				} else {
					graph.setEdgeWeight(edge, weight);
					
					// Put edge features
					for (String feature : edgeDoubleFeatures) {
						double val = readDoubleAttribute(edgeElement, feature, logger);
						fm.putEdgeFeature(edge, feature, val);
					}
					for (String feature : edgeIntFeatures) {
						double val = (double) readIntAttribute(edgeElement, feature, logger);
						fm.putEdgeFeature(edge, feature, val);
					}
					
				}
			} // Finished parsing over the edges of the track

			// Store one of the spot in the saved trackID key map
			savedTrackMap.put(trackID, sourceSpot);
			savedTrackNames.put(trackID, trackName);

		}

		/* Pass the loaded graph to the model. The model will in turn regenerate a new 
		 * map of tracks vs trackID, using the hash as new keys. Because there is a 
		 * good chance that they saved keys and the new keys differ, we must retrieve
		 * the mapping between the two using the retrieve spots.	 */
		final TrackMateModel model = plugin.getModel();
		model.getTrackModel().setGraph(graph);

		// Retrieve the new track map
		Map<Integer, Set<Spot>> newTrackMap = model.getTrackModel().getTrackSpots();

		// Build a map of saved key vs new key
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
				logger.error("The track saved with ID = " + savedKey + " and containing the spot " + spotToFind + " has no matching track in the computed model.\n");
				return false;
			}
		}

		// Check that we matched all the new keys
		if (!newKeysToMatch.isEmpty()) {
			StringBuilder sb = new StringBuilder("Some of the computed tracks could not be matched to saved tracks:\n");
			for (Integer unmatchedKey : newKeysToMatch) {
				sb.append(" - track with ID " + unmatchedKey + " with spots " + newTrackMap.get(unmatchedKey) + "\n");
			}
			logger.error(sb.toString());
			return false;
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
		try {
			Map<Integer, Map<String, Double>> savedFeatureMap = readTrackFeatures();
			for (Integer savedKey : savedFeatureMap.keySet()) {

				Map<String, Double> savedFeatures = savedFeatureMap.get(savedKey);
				for (String feature : savedFeatures.keySet()) {
					Integer newKey = newKeyMap.get(savedKey);
					fm.putTrackFeature(newKey, feature, savedFeatures.get(feature));
				}
			}
		} catch (RuntimeException re) {
			logger.error("Problem populating track features:\n");
			logger.error(re.getMessage());
			return false;
		}

		/*
		 * We can name correctly the tracks
		 */

		try {
			for (Integer savedTrackID : savedTrackNames.keySet()) {
				Integer newKey = newKeyMap.get(savedTrackID);
				model.getTrackModel().setTrackName(newKey, savedTrackNames.get(savedTrackID));
			}
		} catch (RuntimeException rte) {
			logger.error("Problem setting track names:\n");
			logger.error(rte.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Read and return the list of track indices that define the filtered track collection.
	 * @throws DataConversionException 
	 */
	private Set<Integer> readFilteredTrackIDs() {
		Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY);
		if (null == filteredTracksElement)
			return null;

		// We double-check that all trackID in the filtered list exist in the track list
		// First, prepare a sorted array of all track IDs
		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		int[] IDs = new int[trackElements.size()];
		int index = 0;
		for (Element trackElement : trackElements) {
			int trackID = readIntAttribute(trackElement, TrackIndexAnalyzer.TRACK_ID, logger);
			IDs[index] = trackID;
			index++;
		}
		Arrays.sort(IDs);

		List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY);
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (Element indexElement : elements) {
			Integer trackID = readIntAttribute(indexElement, TrackIndexAnalyzer.TRACK_ID, logger);
			if (null != trackID) {

				// Check if this one exist in the list
				int search = Arrays.binarySearch(IDs, trackID);
				if (search < 0) {
					logger.error("Invalid filtered track index: "+trackID+". Track ID does not exist.\n");
				} else {
					filteredTrackIndices.add(trackID);
				}
			}
		}
		return filteredTrackIndices;
	}

	private Spot createSpotFrom(final Element spotEl) {
		int ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
		Spot spot = new Spot(ID);

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


}
