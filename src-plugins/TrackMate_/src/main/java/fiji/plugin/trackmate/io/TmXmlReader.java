package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.util.TMUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readIntAttribute;
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

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.FeatureModel;
import fiji.plugin.trackmate.tracking.SpotTracker;


public class TmXmlReader <T extends RealType<T> & NativeType<T>> implements TmXmlKeys {

	protected static final boolean DEBUG = false;
	//	private static final boolean useMultithreading = TrackMate_.DEFAULT_USE_MULTITHREADING; // Not yet

	protected Document document = null;
	protected File file;
	protected Element root;
	protected Logger logger;
	/** The plugin instance to operate on. This must be provided, in the case we want to load 
	 * a file created with a subclass of {@link TrackMate_} (e.g. with new factories) so that 
	 * correct detectors, etc... can be instantiated from the extended plugin.
	 */
	protected final TrackMate_<T> plugin;
	/** A map of all spots loaded. We need this for performance, since we need to recreate 
	 * both the filtered spot collection and the tracks graph from the same spot objects 
	 * that the main spot collection.. In the file, they are referenced by their {@link Spot#ID()},
	 * and parsing them all to retrieve the one with the right ID is a drag.
	 * We made this cache a {@link ConcurrentHashMap} because we hope to load large data in a 
	 * multi-threaded way.
	 */
	protected ConcurrentHashMap<Integer, Spot> cache;
	/** A flag to indicate whether we already parsed the file. */
	protected boolean parsed = false;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Initialize this reader to read the file given in argument. No actual parsing is made at construction.
	 * <p>
	 * A plugin instance must be provided, and this instance must be initialized with providers
	 * (as in wehen calling {@link TrackMate_#initModules()}. This in the case we want to load 
	 * a file created with a subclass of {@link TrackMate_} (e.g. with new factories) so that 
	 * correct detectors, etc... can be instantiated from the extended plugin. 
	 * But the passed instance is left untouched by the loading process.
	 */
	public TmXmlReader(File file, TrackMate_<T> plugin, Logger logger) {
		this.file = file;
		this.logger = logger;
		this.plugin = plugin;
	}
	
	public TmXmlReader(File file, TrackMate_<T> plugin) {
		this(file, plugin, Logger.VOID_LOGGER);
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Parse the file to create a JDom {@link Document}. This method must be called before using any of
	 * the other getter methods.
	 */
	public void parse() {
		SAXBuilder sb = new SAXBuilder();
		try {
			document = sb.build(file);
		} catch (JDOMException e) {
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"
					+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()
					+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
		}
		root = document.getRootElement();
		parsed = true;
	}
	
	/**
	 * @return the version string stored in the file.
	 */
	public String getVersion() {
		return root.getAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME).getValue();
	}

	/**
	 * Return a {@link TrackMateModel} from all the information stored in this file.
	 * Fields not set in the field will be <code>null</code> in the model. 
	 * @throws DataConversionException 
	 */
	public TrackMateModel<T> getModel() {
		if (!parsed)
			parse();

		TrackMateModel<T> model = plugin.getModel();
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
		SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = readTrackGraph();
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



	public void readTrackFeatures(FeatureModel<T> featureModel) {
		if (!parsed)
			parse();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return;

		featureModel.initFeatureMap();

		// Load tracks
		@SuppressWarnings("unchecked")
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		for (Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TRACK_ID_ATTRIBUTE_NAME).getIntValue();
			} catch (DataConversionException e1) {
				logger.error("Found a track with invalid trackID. Skipping.\n");
				continue;
			}

			@SuppressWarnings("unchecked")
			List<Attribute> attributes = trackElement.getAttributes();
			for(Attribute attribute : attributes) {

				String attName = attribute.getName();
				if (attName.equals(TRACK_ID_ATTRIBUTE_NAME)) { // Skip trackID attribute
					continue;
				}

				Double attVal = Double.NaN;
				try {
					attVal = attribute.getDoubleValue();
				} catch (DataConversionException e) {
					logger.error("Track "+trackID+": Cannot read the feature "+attName+" value. Skipping.\n");
					continue;
				}

				featureModel.putTrackFeature(trackID, attName, attVal); 

			}


		}

	}


	/**
	 * Return the initial threshold on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 */
	public FeatureFilter getInitialFilter()  {
		if (!parsed)
			parse();

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
			String feature 	= ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			Double value 	= readDoubleAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			boolean isAbove	= readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
			FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
			featureThresholds.add(ft);
		}
		return featureThresholds;
	}

	/**
	 * Return the list of {@link FeatureFilter} for tracks stored in this file.
	 * Return <code>null</code> if the track feature filters data cannot be found in the file.
	 */
	public List<FeatureFilter> getTrackFeatureFilters() {
		if (!parsed)
			parse();

		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		@SuppressWarnings("unchecked")
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
	public Settings<T> getSettings() {
		if (!parsed)
			parse();

		Settings<T> settings = new Settings<T>();
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
	public void getDetectorSettings(Settings<T> settings) {
		if (!parsed)
			parse();

		Element element = root.getChild(DETECTOR_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}
		
		DetectorProvider<T> provider = plugin.getDetectorProvider();
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
	public void getTrackerSettings(Settings<T> settings) {
		if (!parsed)
			parse();

		Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}
		
		TrackerProvider<T> provider = plugin.getTrackerProvider();
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
	 * Return the list of all spots stored in this file.
	 * <p>
	 * Internally, this methods also builds the cache field, which will be required by the
	 * following methods:
	 * <ul>
	 * 	<li> {@link #getFilteredSpots()}
	 * 	<li> {@link #readTrackGraph()}
	 * 	<li> {@link #readTrackEdges(SimpleDirectedWeightedGraph)}
	 * 	<li> {@link #readTrackSpots(SimpleDirectedWeightedGraph)}
	 * </ul>
	 * It is therefore sensible to call this method first, just afther {@link #parse()}ing the file.
	 * If not called, this method will be called anyway by the other methods to build the cache.
	 * 
	 * @return  a {@link SpotCollection}. Return <code>null</code> if the spot section is not present in the file.
	 */
	@SuppressWarnings("unchecked")
	public SpotCollection getAllSpots() {
		if (!parsed)
			parse();

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
	public SpotCollection getFilteredSpots()  {
		if (!parsed)
			parse();

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
	 * Load and build a new graph mapping spot linking as tracks. The graph vertices are made of the selected spot
	 * list given in argument. Edges are formed from the file data.
	 * <p>
	 * The track features are not retrieved by this method. They must be recalculated from the model.
	 * 
	 * @param selectedSpots  the spot selection from which tracks area made 
	 */
	@SuppressWarnings("unchecked")
	public SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> readTrackGraph() {
		if (!parsed)
			parse();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		if (null == cache) 
			getAllSpots(); // build the cache if it's not there

		final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		List<Element> edgeElements;
		int sourceID, targetID;
		Spot sourceSpot, targetSpot;
		double weight = 0;

		for (Element trackElement : trackElements) {

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME, logger);

			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME, logger);

				// Get matching spots from the cache
				sourceSpot = cache.get(sourceID);
				targetSpot = cache.get(targetID);

				// Get weight
				if (null != edgeElement.getAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME))
					weight   	= readDoubleAttribute(edgeElement, TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME, logger);
				else 
					weight  	= 0;

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
					logger.error("Bad edge found for track "+trackID);
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
			}
		}
		return graph;
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
	public List<Set<Spot>> readTrackSpots(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		if (!parsed)
			parse();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		if (null == cache)
			getAllSpots(); // build the cache if it's not there

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
		Spot sourceSpot, targetSpot;

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

				// Get spots from the cache
				sourceSpot = cache.get(sourceID);
				targetSpot = cache.get(targetID);

				// Add them to current track
				track.add(sourceSpot);
				track.add(targetSpot);

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
	public List<Set<DefaultWeightedEdge>> readTrackEdges(final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph) {
		if (!parsed)
			parse();

		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;

		if (null == cache)
			getAllSpots(); // build cache if it's not there

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

				// Get spots from cache
				sourceSpot = cache.get(sourceID);
				targetSpot = cache.get(targetID);

				// Retrieve possible edges from graph
				Set<DefaultWeightedEdge> edges = graph.getAllEdges(sourceSpot, targetSpot);

				// Add edges to track
				if (edges.size() != 1) {
					logger.error("Bad edge found for track "+trackID+": found "+edges.size()+" edges.\n");
					continue;
				} else {
					DefaultWeightedEdge edge = edges.iterator().next();
					track.add(edge);
					if (DEBUG) {
						System.out.println("[TmXmlReader] readTrackEdges: in track "+trackID+", found edge "+edge);
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
		if (!parsed)
			parse();

		Element filteredTracksElement = root.getChild(FILTERED_TRACK_ELEMENT_KEY);
		if (null == filteredTracksElement)
			return null;

		// We double-check that all trackID in the filtered list exist in the track list
		// First, prepare a sorted array of all track IDs
		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		@SuppressWarnings("unchecked")
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		int[] IDs = new int[trackElements.size()];
		int index = 0;
		for (Element trackElement : trackElements) {
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME, logger);
			IDs[index] = trackID;
			index++;
		}
		Arrays.sort(IDs);

		@SuppressWarnings("unchecked")
		List<Element> elements = filteredTracksElement.getChildren(TRACK_ID_ELEMENT_KEY);
		HashSet<Integer> filteredTrackIndices = new HashSet<Integer>(elements.size());
		for (Element indexElement : elements) {
			Integer trackID = readIntAttribute(indexElement, TRACK_ID_ATTRIBUTE_NAME, logger);
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

	public ImagePlus getImage()  {
		if (!parsed)
			parse();

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
}
