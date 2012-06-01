package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.*;
import static fiji.plugin.trackmate.util.TMUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readFloatAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readIntAttribute;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mpicbg.imglib.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureModel;
import fiji.plugin.trackmate.segmentation.LogSegmenter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;
import fiji.plugin.trackmate.tracking.SimpleFastLAPTracker;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;


public class TmXmlReader {
	
	private static final boolean DEBUG = false;


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

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Parse the file to create a JDom {@link Document}. This method must be called before using any of
	 * the other getter methods.
	 */
	public void parse() throws JDOMException,  IOException {
		//		SAXBuilder sb = new LineNumberSAXBuilder();
		SAXBuilder sb = new SAXBuilder();
		document = sb.build(file);
		root = document.getRootElement();
	}

	/**
	 * Return a {@link TrackMateModel} from all the information stored in this file.
	 * Fields not set in the field will be <code>null</code> in the model. 
	 * @throws DataConversionException 
	 */
	public TrackMateModel getModel() {
		TrackMateModel model = new TrackMateModel();
		// Settings
		Settings settings = getSettings();
		getSegmenterSettings(settings);
		getTrackerSettings(settings);
		settings.imp = getImage();
		model.setSettings(settings);

		// Spot Filters
		List<FeatureFilter> spotFilters = getSpotFeatureFilters();
		FeatureFilter initialFilter = getInitialFilter();
		model.setInitialSpotFilterValue(initialFilter.value);
		model.setSpotFilters(spotFilters);
		// Spots
		SpotCollection allSpots = getAllSpots();
		SpotCollection filteredSpots = getFilteredSpots(allSpots);
		model.setSpots(allSpots, false);
		model.setFilteredSpots(filteredSpots, false);
		// Tracks
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = readTrackGraph(filteredSpots);
		if (null != graph) {
			model.setGraph(graph);
		}
		// Track Filters
		List<FeatureFilter> trackFilters = getTrackFeatureFilters();
		model.setTrackFilters(trackFilters);
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

	
	
	public void readTrackFeatures(FeatureModel featureModel) {
		
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

				Float attVal = Float.NaN;
				try {
					attVal = attribute.getFloatValue();
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
		Element itEl = root.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		if (null == itEl)
			return null;
		String feature 	= itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
		Float value 	= readFloatAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
		boolean isAbove	= readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
		FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * Return <code>null</code> if the spot feature filters data cannot be found in the file.
	 */
	@SuppressWarnings("unchecked")
	public List<FeatureFilter> getSpotFeatureFilters() {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			String feature 	= ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			Float value 	= readFloatAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
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
	@SuppressWarnings("unchecked")
	public List<FeatureFilter> getTrackFeatureFilters() {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = root.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		if (null == ftCollectionEl)
			return null;
		List<Element> ftEls = ftCollectionEl.getChildren(FILTER_ELEMENT_KEY);
		for (Element ftEl : ftEls) {
			String feature 	= ftEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
			Float value 	= readFloatAttribute(ftEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
			boolean isAbove	= readBooleanAttribute(ftEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
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
	public Settings getSettings() {
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
			settings.segmentationChannel = readIntAttribute(settingsEl, SETTINGS_SEGMENTATION_CHANNEL_ATTRIBUTE_NAME, logger, 1);
		}
		// Image info settings
		Element infoEl 	= root.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx				= readFloatAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger);
			settings.dy				= readFloatAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger);
			settings.dz				= readFloatAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger);
			settings.dt				= readFloatAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, logger);
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
	 * Update the given {@link Settings} object with the {@link SegmenterSettings} and {@link SpotSegmenter} fields
	 * named {@link Settings#segmenterSettings} and {@link Settings#segmenter} read within the XML file
	 * this reader is initialized with.
	 * <p>
	 * If the segmenter settings XML element is not present in the file, the {@link Settings} 
	 * object is not updated. If the segmenter settings or the segmenter info can be read, 
	 * but cannot be understood (most likely because the class the XML refers to is unknown) 
	 * then a default object is substituted.  
	 *   
	 * @param settings  the base {@link Settings} object to update.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void getSegmenterSettings(Settings settings) {
		Element element = root.getChild(SEGMENTER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		// Deal with segmenter
		SpotSegmenter<? extends RealType<?>> segmenter;
		String segmenterClassName = element.getAttributeValue(SEGMENTER_CLASS_ATTRIBUTE_NAME);
		if (null == segmenterClassName) {
			logger.error("Segmenter class is not present.\n");
			logger.error("Substituting default.\n");
			segmenter = new LogSegmenter();
		} else {
			try {
				segmenter = (SpotSegmenter) Class.forName(segmenterClassName).newInstance();
			} catch (InstantiationException e) {
				logger.error("Unable to instantiate segmenter class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				segmenter = new LogSegmenter();
			} catch (IllegalAccessException e) {
				logger.error("Unable to instantiate segmenter class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				segmenter = new LogSegmenter();
			} catch (ClassNotFoundException e) {
				logger.error("Unable to find segmenter class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				segmenter = new LogSegmenter();
			}
		}
		settings.segmenter = segmenter;

		// Deal with segmenter settings
		SegmenterSettings ss = segmenter.createDefaultSettings();
		String segmenterSettingsClassName = element.getAttributeValue(SEGMENTER_SETTINGS_CLASS_ATTRIBUTE_NAME);

		if (null == segmenterSettingsClassName) {

			logger.error("Segmenter settings class is not present,\n");
			logger.error("substituting default one.\n");

		} else {

			if (segmenterSettingsClassName.equals(ss.getClass().getName())) {

				// The saved class matched, we can updated the settings created above with the file content
				ss.unmarshall(element);

			} else {

				// They do not match. We DO NOT give priority to what has been saved. That way we always
				// have something that works (when invoking the process methods of the plugin).

				logger.error("Tracker settings class ("+segmenterSettingsClassName+") does not match tracker requirements (" +
						ss.getClass().getName()+"),\n");
				logger.error("substituting default one.\n");

			}
		}
		settings.segmenterSettings = ss;

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
	public void getTrackerSettings(Settings settings) {
		Element element = root.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == element) {
			return;
		}

		// Deal with tracker
		SpotTracker tracker;
		String trackerClassName = element.getAttributeValue(TRACKER_CLASS_ATTRIBUTE_NAME);
		if (null == trackerClassName) {
			logger.error("Tracker class is not present.\n");
			logger.error("Substituting default.\n");
			tracker = new SimpleFastLAPTracker();
		} else {
			try {
				tracker = (SpotTracker) Class.forName(trackerClassName).newInstance();
			} catch (InstantiationException e) {
				logger.error("Unable to instantiate tracker settings class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				tracker = new SimpleFastLAPTracker();
			} catch (IllegalAccessException e) {
				logger.error("Unable to instantiate tracker settings class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				tracker = new SimpleFastLAPTracker();
			} catch (ClassNotFoundException e) {
				logger.error("Unable to find segmenter settings class: "+e.getLocalizedMessage()+"\n");
				logger.error("Substituting default.\n");
				tracker = new SimpleFastLAPTracker();
			}
		}
		settings.tracker = tracker;

		// Deal with tracker settings
		{
			// To start with, we get a settings suitable for the tracker we have found 
			TrackerSettings ts = tracker.createDefaultSettings();
			String trackerSettingsClassName = element.getAttributeValue(TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME);
			if (null == trackerSettingsClassName) {

				logger.error("Tracker settings class is not present,\n");
				logger.error("substituting default one.\n");

			} else {

				if (trackerSettingsClassName.equals(ts.getClass().getName())) {

					// The saved class matched, we can updated the settings created above with the file content
					ts.unmarshall(element);

				} else {

					// They do not match. We DO NOT give priority to what has been saved. That way we always
					// have something that works (when invoking the process methods of the plugin).

					logger.error("Tracker settings class ("+trackerSettingsClassName+") does not match tracker requirements (" +
							ts.getClass().getName()+"),\n");
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
	@SuppressWarnings("unchecked")
	public SpotCollection getAllSpots() {
		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;

		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);
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
	public SpotCollection getFilteredSpots(SpotCollection allSpots)  {
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
			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
			// Get spot list from main list
			spotsThisFrame = allSpots.get(currentFrame);
			if (null == spotsThisFrame)
				continue;

			spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			// Loop over all spot element
			for (Element spotEl : spotContent) {
				ID = readIntAttribute(spotEl, SPOT_ID_ATTRIBUTE_NAME, logger);
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
	 * Load and build a new graph mapping spot linking as tracks. The graph vertices are made of the selected spot
	 * list given in argument. Edges are formed from the file data.
	 * <p>
	 * The track features are not retrieved by this method. They must be recalculated from the model.
	 * 
	 * @param selectedSpots  the spot selection from which tracks area made 
	 */
	@SuppressWarnings("unchecked")
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> readTrackGraph(final SpotCollection spots) {

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

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TRACK_ID_ATTRIBUTE_NAME, logger);

			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
			for (Element edgeElement : edgeElements) {
				// Get source and target ID for this edge
				sourceID = readIntAttribute(edgeElement, TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, logger);
				targetID = readIntAttribute(edgeElement, TRACK_EDGE_TARGET_ATTRIBUTE_NAME, logger);
				if (null != edgeElement.getAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME))
					weight   	= readDoubleAttribute(edgeElement, TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME, logger);
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
							//							LineNumberElement lne = (LineNumberElement) edgeElement;
							logger.error("Bad edge found for track "+trackID);
							//									+": loop edge at line "+lne.getStartLine()+". Skipping.\n");
							break;
						}
						DefaultWeightedEdge edge = graph.addEdge(sourceSpot, targetSpot);
						if (edge == null) {
							//							LineNumberElement lne = (LineNumberElement) edgeElement;
							logger.error("Bad edge found for track "+trackID);
							//									+": duplicate edge at line "+lne.getStartLine()+". Skipping.\n");
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
