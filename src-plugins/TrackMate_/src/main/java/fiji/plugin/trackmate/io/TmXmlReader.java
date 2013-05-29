package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.IOUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntAttribute;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYSER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYSER_KEY_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.ANALYZER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_TEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_TSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_XEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_XSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_YEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_YSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ZEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ZSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.DETECTOR_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.EDGE_ANALYSERS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.EDGE_FEATURES_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_DECLARATIONS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_DIMENSION_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_NAME_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.FEATURE_SHORT_NAME_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTERED_TRACK_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ABOVE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_FEATURE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FILTER_VALUE_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.FRAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_STATE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ATTRIBUTE;
import static fiji.plugin.trackmate.io.TmXmlKeys.GUI_VIEW_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FILENAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_FOLDER_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NFRAMES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_NSLICES_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.INITIAL_SPOT_FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.MODEL_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPATIAL_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ANALYSERS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FEATURES_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FRAME_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ID_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_NAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.TIME_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACKER_SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ANALYSERS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_EDGE_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_FEATURES_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_ID_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.TRACK_NAME_ATTRIBUTE_NAME;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Logger.StringBuilderLogger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.EdgeAnalyzerProvider;
import fiji.plugin.trackmate.providers.SpotAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackAnalyzerProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.visualization.TrackMateModelView;


public class TmXmlReader {

	protected static final boolean DEBUG = true;

	protected Document document = null;
	protected final File file;
	/** A map of all spots loaded. We need this for performance, since we need to recreate 
	 * both the filtered spot collection and the tracks graph from the same spot objects 
	 * that the main spot collection.. In the file, they are referenced by their {@link Spot#ID()},
	 * and parsing them all to retrieve the one with the right ID is a drag.
	 * We made this cache a {@link ConcurrentHashMap} because we hope to load large data in a 
	 * multi-threaded way.
	 */
	protected ConcurrentHashMap<Integer, Spot> cache;
	protected StringBuilderLogger logger = new StringBuilderLogger();
	protected final Element root;
	/** If <code>false</code>, an error occured during reading. 
	 * @see #getErrorMessage(). */
	private boolean ok = true;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Initialize this reader to read the file given in argument. 
	 */
	public TmXmlReader(File file) {
		this.file = file;
		SAXBuilder sb = new SAXBuilder();
		Element r = null;
		try {
			document = sb.build(file);
			r  = document.getRootElement();
		} catch (JDOMException e) {
			ok = false;
			logger.error("Problem parsing "+file.getName()+", it is not a valid TrackMate XML file.\nError message is:\n"
					+e.getLocalizedMessage()+'\n');
		} catch (IOException e) {
			logger.error("Problem reading "+file.getName()
					+".\nError message is:\n"+e.getLocalizedMessage()+'\n');
			ok = false;
		}
		this.root = r;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Returns  the log text saved in the file, or <code>null</code> if log
	 * text was not saved.
	 */
	public String getLog() {
		Element logElement = root.getChild(LOG_ELEMENT_KEY);
		if (null != logElement) {
			return logElement.getTextTrim();
		} else {
			return "";
		}
	}
	
	/**
	 * Returns the GUI state saved in the file. 
	 * @return the saved GUI state, as a string.
	 */
	public String getGUIState() {
		Element guiel = root.getChild(GUI_STATE_ELEMENT_KEY);
		if (null != guiel) {
			String guiState = guiel.getAttributeValue(GUI_STATE_ATTRIBUTE);
			if (null == guiState) {
				logger.error("Could not find GUI state attribute.\n");
				ok = false;
			}
			return guiState;
			
		} else {
			logger.error("Could not find GUI state element.\n");
			ok = false;
			return null;
		}
	}
	
	/**
	 * Returns the collection of views that were saved in this file. The views returned
	 * are not rendered yet.
	 * @param provider  the {@link ViewProvider} to instantiate the view. Each saved 
	 * view must be known by the specified provider.
	 * @return the collection of views.
	 * @see TrackMateModelView#render()
	 */
	public Collection<TrackMateModelView> getViews(ViewProvider provider) {
		Element guiel = root.getChild(GUI_STATE_ELEMENT_KEY);
		if (null != guiel) {
			
			List<Element> children = guiel.getChildren(GUI_VIEW_ELEMENT_KEY);
			Collection<TrackMateModelView> views = new ArrayList<TrackMateModelView>(children.size());

			for (Element child : children) {
				String viewKey = child.getAttributeValue(GUI_VIEW_ATTRIBUTE);
				if (null == viewKey) {
					logger.error("Could not find view key attribute for element " + child +".\n");
					ok = false;
				} else {
					TrackMateModelView view = provider.getView(viewKey);
					if (null == view) {
						logger.error("Unknown view for key " + viewKey +".\n");
						ok = false;
					} else {
						views.add(view);
					}
				}
			}
			return views;
			
		} else {
			logger.error("Could not find GUI state element.\n");
			ok = false;
			return null;
		}
	}


	/**
	 * Returns the model saved in the file, or <code>null</code> if a saved model 
	 * cannot be found in the xml file. 
	 * @return a new {@link TrackMateModel}.
	 */
	public TrackMateModel getModel() {
		Element modelElement = root.getChild(MODEL_ELEMENT_KEY);
		if (null == modelElement) {
			return null;
		} 
		TrackMateModel model = new TrackMateModel();

		// Physical units
		String spaceUnits = modelElement.getAttributeValue(SPATIAL_UNITS_ATTRIBUTE_NAME);
		String timeUnits  = modelElement.getAttributeValue(TIME_UNITS_ATTRIBUTE_NAME);
		model.setPhysicalUnits(spaceUnits, timeUnits);

		// Feature declarations
		readFeatureDeclarations(modelElement, model);

		// Spots
		SpotCollection spots = getSpots(modelElement);
		model.setSpots(spots, false);

		// Tracks
		if (!readTracks(modelElement, model)) {
			ok = false;
		}
		
		// Track features
		
		try {
			Map<Integer, Map<String, Double>> savedFeatureMap = readTrackFeatures(modelElement);
			for (Integer savedKey : savedFeatureMap.keySet()) {

				Map<String, Double> savedFeatures = savedFeatureMap.get(savedKey);
				for (String feature : savedFeatures.keySet()) {
					model.getFeatureModel().putTrackFeature(savedKey, feature, savedFeatures.get(feature));
				}
			}
		} catch (RuntimeException re) {
			logger.error("Problem populating track features:\n");
			logger.error(re.getMessage());
			ok = false;
		}
		
		// That's it
		return model;
	}


	/**
	 * Reads the settings element of the file, and sets the fields of the specified 
	 * {@link Settings} object according to the xml file content.
	 * The provided {@link Settings} object is left untouched if the settings element
	 * cannot be found in the file.
	 * @param settings the {@link Settings} object to flesh out.
	 * @param detectorProvider the detector provider, required to configure the settings with
	 * a correct {@link SpotDetectorFactory}. If <code>null</code>, will skip reading detector
	 * parameters. 
	 * @param trackerProvider the tracker provider, required to configure the settings with a 
	 * correct {@link SpotTracker}. If <code>null</code>, will skip reading tracker parameters.
	 * @param spotAnalyzerProvider the spot analyzer provider, required to instantiates the saved
	 * {@link SpotAnalyzerFactory}s. If <code>null</code>, will skip reading spot analyzers.
	 * @param edgeAnalyzerProvider the edge analyzer provider, required to instantiates the saved
	 * {@link EdgeAnalyzer}s. If <code>null</code>, will skip reading edge analyzers.
	 * @param trackAnalyzerProvider the track analyzer provider, required to instantiates the saved
	 * {@link TrackAnalyzer}s. If <code>null</code>, will skip reading track analyzers.
	 */
	public void readSettings(Settings settings, 
			DetectorProvider detectorProvider, TrackerProvider trackerProvider, 
			SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, 
			TrackAnalyzerProvider trackAnalyzerProvider) {
		Element settingsElement = root.getChild(SETTINGS_ELEMENT_KEY);
		if (null == settingsElement) {
			return;
		} 

		// Base
		getBaseSettings(settingsElement, settings);

		// Image
		settings.imp = getImage(settingsElement);

		// Detector
		if (null != detectorProvider) {
			getDetectorSettings(settingsElement, settings, detectorProvider);
		}

		// Tracker
		if (null != trackerProvider) {
			getTrackerSettings(settingsElement, settings, trackerProvider);
		}

		// Spot Filters
		FeatureFilter initialFilter = getInitialFilter(settingsElement);
		if (null != initialFilter) {
			settings.initialSpotFilterValue = initialFilter.value;
		}
		List<FeatureFilter> spotFilters = getSpotFeatureFilters(settingsElement);
		settings.setSpotFilters(spotFilters);

		// Track Filters
		List<FeatureFilter> trackFilters = getTrackFeatureFilters(settingsElement);
		settings.setTrackFilters(trackFilters);

		// Features analyzers
		readAnalyzers(settingsElement, settings, spotAnalyzerProvider, edgeAnalyzerProvider, trackAnalyzerProvider);
	}


	/**
	 * Returns the version string stored in the file.
	 */
	public String getVersion() {
		return root.getAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME).getValue();
	}

	/**
	 * Returns an explanatory message about the last unsuccessful read attempt.
	 * @return an error message.
	 * @see #isReadingOk()
	 */
	public String getErrorMessage() {
		return logger.toString();
	}
	
	/**
	 * Returns <code>true</code> if the last reading method call happened 
	 * without any warning or error, <code>false</code> otherwise. 
	 * @return <code>true</code> if reading was ok.
	 * @see #getErrorMessage()
	 */
	public boolean isReadingOk() {
		return ok;
	}

	/*
	 * PRIVATE METHODS
	 */



	private ImagePlus getImage(Element settingsElement)  {
		Element imageInfoElement = settingsElement.getChild(IMAGE_ELEMENT_KEY);
		String filename = imageInfoElement.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
		String folder 	= imageInfoElement.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		if (null == filename || filename.isEmpty()) {
			logger.error("Cannot find image file name in xml file.\n");
			ok = false;
			return null;
		}
		if (null == folder || folder.isEmpty()) {
			folder = file.getParent(); // it is a relative path, then
		}
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead()) {
			// Could not find it to the absolute path. Then we look for the same path of the xml file
			folder = file.getParent();
			imageFile = new File(folder, filename);
			if (!imageFile.exists() || !imageFile.canRead()) {
				logger.error("Cannot read image file: " + imageFile + ".\n");
				ok = false;
				return null;
			}
		}
		return IJ.openImage(imageFile.getAbsolutePath());
	}



	/**
	 * Returns a map of the saved track features, as they appear in the file
	 */
	private Map<Integer, Map<String,Double>> readTrackFeatures(Element modelElement) {

		HashMap<Integer, Map<String, Double>> featureMap = new HashMap<Integer, Map<String, Double>>();

		Element allTracksElement = modelElement.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement) {
			logger.error("Cannot find the track collection in file.\n");
			ok = false;
			return null;
		}

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		for (Element trackElement : trackElements) {

			int trackID = -1;
			try {
				trackID = trackElement.getAttribute(TrackIndexAnalyzer.TRACK_ID).getIntValue();
			} catch (DataConversionException e1) {
				logger.error("Found a track with invalid trackID for " + trackElement + ". Skipping.\n");
				ok = false;
				continue;
			}

			HashMap<String, Double> trackMap = new HashMap<String, Double>();

			List<Attribute> attributes = trackElement.getAttributes();
			for(Attribute attribute : attributes) {

				String attName = attribute.getName();
				if (attName.equals(TRACK_NAME_ATTRIBUTE_NAME)) { // Skip trackID attribute
					continue;
				}
				
				
				Double attVal = Double.NaN;
				try {
					attVal = attribute.getDoubleValue();
				} catch (DataConversionException e) {
					logger.error("Track "+trackID+": Cannot read the feature "+attName+" value. Skipping.\n");
					ok = false;
					continue;
				}

				trackMap.put(attName, attVal);

			}

			featureMap.put(trackID, trackMap);
		}

		return featureMap;

	}


	/**
	 * Return the initial filter value on quality stored in this file.
	 * Return <code>null</code> if the initial threshold data cannot be found in the file.
	 * @param settingsElement the settings {@link Element} to read from.
	 * @return the initial filter, as a {@link FeatureFilter}. 
	 */
	private FeatureFilter getInitialFilter(Element settingsElement)  {
		Element itEl = settingsElement.getChild(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		String feature 	= itEl.getAttributeValue(FILTER_FEATURE_ATTRIBUTE_NAME);
		Double value 	= readDoubleAttribute(itEl, FILTER_VALUE_ATTRIBUTE_NAME, logger);
		boolean isAbove	= readBooleanAttribute(itEl, FILTER_ABOVE_ATTRIBUTE_NAME, logger);
		FeatureFilter ft = new FeatureFilter(feature, value, isAbove);
		return ft;
	}


	/**
	 * Return the list of {@link FeatureFilter} for spots stored in this file.
	 * @param settingsElement the settings {@link Element} to read from.
	 * @return a list of {@link FeatureFilter}s.
	 */
	private List<FeatureFilter> getSpotFeatureFilters(Element settingsElement) {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = settingsElement.getChild(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
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
	 * Returns the list of {@link FeatureFilter} for tracks stored in this file.
	 * @param settingsElement the settings {@link Element} to read from. 
	 * @return a list of {@link FeatureFilter}s.
	 */
	private List<FeatureFilter> getTrackFeatureFilters(Element settingsElement) {
		List<FeatureFilter> featureThresholds = new ArrayList<FeatureFilter>();
		Element ftCollectionEl = settingsElement.getChild(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
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
	 * Set the base settings of the provided {@link Settings} object, extracted from the specified 
	 * {@link Element}.
	 * @param settingsElement the settings {@link Element} to read parameters from. 
	 * @return  a new {@link Settings} object.
	 */
	private void getBaseSettings(Element settingsElement, Settings settings) {
		// Basic settings
		Element settingsEl = settingsElement.getChild(CROP_ELEMENT_KEY);
		if (null != settingsEl) {
			settings.xstart = readIntAttribute(settingsEl, CROP_XSTART_ATTRIBUTE_NAME, logger, 1);
			settings.xend 	= readIntAttribute(settingsEl, CROP_XEND_ATTRIBUTE_NAME, logger, 512);
			settings.ystart = readIntAttribute(settingsEl, CROP_YSTART_ATTRIBUTE_NAME, logger, 1);
			settings.yend 	= readIntAttribute(settingsEl, CROP_YEND_ATTRIBUTE_NAME, logger, 512);
			settings.zstart = readIntAttribute(settingsEl, CROP_ZSTART_ATTRIBUTE_NAME, logger, 1);
			settings.zend 	= readIntAttribute(settingsEl, CROP_ZEND_ATTRIBUTE_NAME, logger, 10);
			settings.tstart = readIntAttribute(settingsEl, CROP_TSTART_ATTRIBUTE_NAME, logger, 1);
			settings.tend 	= readIntAttribute(settingsEl, CROP_TEND_ATTRIBUTE_NAME, logger, 10);
			//			settings.detectionChannel = readIntAttribute(settingsEl, CROP_DETECTION_CHANNEL_ATTRIBUTE_NAME, logger, 1);
		}
		// Image info settings
		Element infoEl 	= settingsElement.getChild(IMAGE_ELEMENT_KEY);
		if (null != infoEl) {
			settings.dx				= readDoubleAttribute(infoEl, IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, logger);
			settings.dy				= readDoubleAttribute(infoEl, IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, logger);
			settings.dz				= readDoubleAttribute(infoEl, IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, logger);
			settings.dt				= readDoubleAttribute(infoEl, IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, logger);
			settings.width			= readIntAttribute(infoEl, IMAGE_WIDTH_ATTRIBUTE_NAME, logger, 512);
			settings.height			= readIntAttribute(infoEl, IMAGE_HEIGHT_ATTRIBUTE_NAME, logger, 512);
			settings.nslices		= readIntAttribute(infoEl, IMAGE_NSLICES_ATTRIBUTE_NAME, logger, 1);
			settings.nframes		= readIntAttribute(infoEl, IMAGE_NFRAMES_ATTRIBUTE_NAME, logger, 1);
			settings.imageFileName	= infoEl.getAttributeValue(IMAGE_FILENAME_ATTRIBUTE_NAME);
			settings.imageFolder	= infoEl.getAttributeValue(IMAGE_FOLDER_ATTRIBUTE_NAME);
		}
	}

	/**
	 * Update the given {@link Settings} object with the {@link SpotDetectorFactory} and settings map fields
	 * named {@link Settings#detectorFactory}  and {@link Settings#detectorSettings} read within the XML file
	 * this reader is initialized with. 
	 * <p>
	 * As a side effect, this method also configure the {@link DetectorProvider}. 
	 *  
	 * @param settingsElement the Element in which the {@link Settings} parameters are stored.   
	 * @param settings  the base {@link Settings} object to update.
	 * @param provider  a {@link DetectorProvider}, required to read detector parameters. 
	 */
	private void getDetectorSettings(Element settingsElement, Settings settings, DetectorProvider provider) {
		Element element = settingsElement.getChild(DETECTOR_SETTINGS_ELEMENT_KEY);
		Map<String, Object> ds = new HashMap<String, Object>(); 
		// All the hard work is delegated to the provider. 
		boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			this.ok = false;
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
	 * If the tracker settings or the tracker info can be read, 
	 * but cannot be understood (most likely because the class the XML refers to is unknown) 
	 * then a default object is substituted.
	 *   
	 * @param settingsElement the {@link Element} in which the tracker parameters are stored.
	 * @param settings  the base {@link Settings} object to update.
	 * @param trackerProvider the {@link TrackerProvider}, required to read the tracker
	 * parameters. 
	 */
	private void getTrackerSettings(Element settingsElement, Settings settings, TrackerProvider provider) {
		Element element = settingsElement.getChild(TRACKER_SETTINGS_ELEMENT_KEY);
		Map<String, Object> ds = new HashMap<String, Object>(); 
		// All the hard work is delegated to the provider. 
		boolean ok = provider.unmarshall(element, ds);

		if (!ok) {
			logger.error(provider.getErrorMessage());
			this.ok = false;
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
	 * 	<li> {@link #readTracks()}
	 * 	<li> {@link #readTrackEdges(SimpleDirectedWeightedGraph)}
	 * 	<li> {@link #readTrackSpots(SimpleDirectedWeightedGraph)}
	 * </ul>
	 * It is therefore sensible to call this method first, just after {@link #parse()}ing the file.
	 * If not called, this method will be called anyway by the other methods to build the cache.
	 * 
	 * @param modelElement the {@link Element} in which the model content was written. 
	 * @return  a new {@link SpotCollection}. 
	 */
	private SpotCollection getSpots(Element modelElement) {
		// Root element for collection
		Element spotCollection = modelElement.getChild(SPOT_COLLECTION_ELEMENT_KEY);

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
		Map<Integer, Set<Spot>> content = new HashMap<Integer, Set<Spot>>(frameContent.size());
		for (Element currentFrameContent : frameContent) {

			currentFrame = readIntAttribute(currentFrameContent, FRAME_ATTRIBUTE_NAME, logger);
			List<Element> spotContent = currentFrameContent.getChildren(SPOT_ELEMENT_KEY);
			Set<Spot> spotSet = new HashSet<Spot>(spotContent.size());
			for (Element spotElement : spotContent) {
				Spot spot = createSpotFrom(spotElement);
				spotSet.add(spot);
				cache.put(spot.ID(), spot);
			}
			content.put(currentFrame, spotSet);
		}
		SpotCollection allSpots = SpotCollection.fromMap(content);
		return allSpots;
	}

	/**
	 * Load the tracks, the track features and the ID of the filtered tracks into the model
	 * modified by this reader. 
	 * @return 
	 * @return true if reading tracks was successful, false otherwise.
	 */
	private boolean readTracks(Element modelElement, TrackMateModel model) {

		Element allTracksElement = modelElement.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);

		// What we have to flesh out from the file
		final SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		final Map<Integer, Set<Spot>> connectedVertexSet = new HashMap<Integer, Set<Spot>>(trackElements.size());
		final Map<Integer, Set<DefaultWeightedEdge>> connectedEdgeSet = new HashMap<Integer, Set<DefaultWeightedEdge>>(trackElements.size());
		final Map<Integer, String> savedTrackNames = new HashMap<Integer, String>(trackElements.size());

		// The list of edge features. that we will set.
		final FeatureModel fm = model.getFeatureModel();
		List<String> edgeIntFeatures = new ArrayList<String>();// TODO is there a better way?
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_SOURCE_ID);
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_TARGET_ID);
		Collection<String> edgeDoubleFeatures = fm.getEdgeFeatures();
		edgeDoubleFeatures.removeAll(edgeIntFeatures);

		for (Element trackElement : trackElements) {

			// Get track ID as it is saved on disk
			int trackID = readIntAttribute(trackElement, TrackIndexAnalyzer.TRACK_ID, logger);
			String trackName = trackElement.getAttributeValue(TRACK_NAME_ATTRIBUTE_NAME);
			if (null == trackName) {
				trackName = "Unnamed";
			}

			// Iterate over edges & spots
			List<Element> edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
			Set<DefaultWeightedEdge> edges = new HashSet<DefaultWeightedEdge>(edgeElements.size());
			Set<Spot> spots = new HashSet<Spot>(edgeElements.size());

			for (Element edgeElement : edgeElements) {

				// Get source and target ID for this edge
				int sourceID = readIntAttribute(edgeElement, EdgeTargetAnalyzer.SPOT_SOURCE_ID, logger);
				int targetID = readIntAttribute(edgeElement, EdgeTargetAnalyzer.SPOT_TARGET_ID, logger);

				// Get matching spots from the cache
				Spot sourceSpot = cache.get(sourceID);
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
				
				// Add spots to connected set. We might add the same spot twice (because we iterate over edges)
				// but this is fine for we use a set.
				spots.add(sourceSpot);
				spots.add(targetSpot);

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
				
				// Adds the edge to the set
				edges.add(edge);
				
			} // Finished parsing over the edges of the track

			// Store one of the spot in the saved trackID key map
			connectedVertexSet.put(trackID, spots);
			connectedEdgeSet.put(trackID, edges);
			savedTrackNames.put(trackID, trackName);
		}

		/* 
		 * Now on to the visibility.
		 */
		Set<Integer> savedFilteredTrackIDs = readFilteredTrackIDs(modelElement);
		Map<Integer, Boolean> visibility = new HashMap<Integer, Boolean>(connectedEdgeSet.size());
		Set<Integer> ids = new HashSet<Integer>(connectedEdgeSet.keySet());
		for (Integer id : savedFilteredTrackIDs) {
			visibility.put(id, Boolean.TRUE);
		}
		ids.removeAll(savedFilteredTrackIDs);
		for (Integer id : ids) {
			visibility.put(id, Boolean.FALSE);
		}
		
		
		/*
		 * Pass read results to model.
		 */
		model.getTrackModel().from(graph, connectedVertexSet, connectedEdgeSet, visibility, savedTrackNames);

		return true;
	}

	/**
	 * Read and return the list of track indices that define the filtered track collection.
	 * @throws DataConversionException 
	 */
	private Set<Integer> readFilteredTrackIDs(Element modelElement) {
		Element filteredTracksElement = modelElement.getChild(FILTERED_TRACK_ELEMENT_KEY);
		if (null == filteredTracksElement) {
			logger.error("Could not find the filtered track IDs in file.\n");
			ok = false;
			return null;
		}

		// We double-check that all trackID in the filtered list exist in the track list
		// First, prepare a sorted array of all track IDs
		Element allTracksElement = modelElement.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement) {
			logger.error("Could not find the track collection in file.\n");
			ok = false;
			return null;
		}
		
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
					ok = false;
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
			spot.putFeature(att.getName(), Double.valueOf(att.getValue()));
		}
		return spot;
	}

	private void readFeatureDeclarations(Element modelElement, TrackMateModel model) {

		FeatureModel fm = model.getFeatureModel();
		Element featuresElement = modelElement.getChild(FEATURE_DECLARATIONS_ELEMENT_KEY);
		if (null == featuresElement) {
			logger.error("Could not find feature declarations in file.\n");
			ok = false;
			return;
		}

		// Spots
		Element spotFeaturesElement = featuresElement.getChild(SPOT_FEATURES_ELEMENT_KEY);
		if (null == spotFeaturesElement) {
			logger.error("Could not find spot feature declarations in file.\n");
			ok = false;

		} else {

			List<Element> children = spotFeaturesElement.getChildren(FEATURE_ELEMENT_KEY);
			Collection<String> features = new ArrayList<String>(children.size());
			Map<String, String> featureNames = new HashMap<String, String>(children.size());
			Map<String, String> featureShortNames = new HashMap<String, String>(children.size());
			Map<String, Dimension> featureDimensions = new HashMap<String, Dimension>(children.size());
			for (Element child : children) {
				readSingleFeatureDeclaration(child, features, featureNames, featureShortNames, featureDimensions);
			}
			fm.declareSpotFeatures(features, featureNames, featureShortNames, featureDimensions);
		}

		// Edges
		Element edgeFeaturesElement = featuresElement.getChild(EDGE_FEATURES_ELEMENT_KEY);
		if (null == edgeFeaturesElement) {
			logger.error("Could not find edge feature declarations in file.\n");
			ok = false;

		} else {

			List<Element> children = edgeFeaturesElement.getChildren(FEATURE_ELEMENT_KEY);
			Collection<String> features = new ArrayList<String>(children.size());
			Map<String, String> featureNames = new HashMap<String, String>(children.size());
			Map<String, String> featureShortNames = new HashMap<String, String>(children.size());
			Map<String, Dimension> featureDimensions = new HashMap<String, Dimension>(children.size());
			for (Element child : children) {
				readSingleFeatureDeclaration(child, features, featureNames, featureShortNames, featureDimensions);
			}
			fm.declareEdgeFeatures(features, featureNames, featureShortNames, featureDimensions);
		}

		// Tracks
		Element trackFeaturesElement = featuresElement.getChild(TRACK_FEATURES_ELEMENT_KEY);
		if (null == trackFeaturesElement) {
			logger.error("Could not find track feature declarations in file.\n");
			ok = false;

		} else {

			List<Element> children = trackFeaturesElement.getChildren(FEATURE_ELEMENT_KEY);
			Collection<String> features = new ArrayList<String>(children.size());
			Map<String, String> featureNames = new HashMap<String, String>(children.size());
			Map<String, String> featureShortNames = new HashMap<String, String>(children.size());
			Map<String, Dimension> featureDimensions = new HashMap<String, Dimension>(children.size());
			for (Element child : children) {
				readSingleFeatureDeclaration(child, features, featureNames, featureShortNames, featureDimensions);
			}
			fm.declareTrackFeatures(features, featureNames, featureShortNames, featureDimensions);
		}
	}

	private void readAnalyzers(Element settingsElement, Settings settings, 
			SpotAnalyzerProvider spotAnalyzerProvider, EdgeAnalyzerProvider edgeAnalyzerProvider, 
			TrackAnalyzerProvider trackAnalyzerProvider) {

		Element analyzersEl = settingsElement.getChild(ANALYZER_COLLECTION_ELEMENT_KEY);
		if (null == analyzersEl) {
			logger.error("Could not find the feature analyzer element.\n");
			ok = false;
			return;
		}

		// Spot analyzers
		if (null != spotAnalyzerProvider) {
			Element spotAnalyzerEl = analyzersEl.getChild(SPOT_ANALYSERS_ELEMENT_KEY);
			if (null == spotAnalyzerEl) {
				logger.error("Could not find the spot analyzer element.\n");
				ok = false;

			} else {

				if (settings.imp == null) {
					logger.error("The source image is not loaded; cannot instantiates spot analzers.\n");
					ok = false;

				} else {

					ImgPlus<?> img = ImagePlusAdapter.wrapImgPlus(settings.imp);
					List<Element> children = spotAnalyzerEl.getChildren(ANALYSER_ELEMENT_KEY);
					for (Element child : children) {

						String key = child.getAttributeValue(ANALYSER_KEY_ATTRIBUTE);
						if (null == key) {
							logger.error("Could not find analyzer name for element " + child + ".\n");
							ok = false;
							continue;
						}
						
						SpotAnalyzerFactory<?> spotAnalyzer = spotAnalyzerProvider.getSpotFeatureAnalyzer(key, img);
						if (null == spotAnalyzer) {
							logger.error("Unknown spot analyzer key: " + key + ".\n");
							ok = false;
							
						} else {
							settings.addSpotAnalyzerFactory(spotAnalyzer);
						}

					}

				}

			}
		}

		// Edge analyzers
		if (null != edgeAnalyzerProvider) {
			Element edgeAnalyzerEl = analyzersEl.getChild(EDGE_ANALYSERS_ELEMENT_KEY);
			if (null == edgeAnalyzerEl) {
				logger.error("Could not find the edge analyzer element.\n");
				ok = false;

			} else {

				List<Element> children = edgeAnalyzerEl.getChildren(ANALYSER_ELEMENT_KEY);
				for (Element child : children) {

					String key = child.getAttributeValue(ANALYSER_KEY_ATTRIBUTE);
					if (null == key) {
						logger.error("Could not find analyzer name for element " + child + ".\n");
						ok = false;
						continue;
					}
					
					EdgeAnalyzer edgeAnalyzer = edgeAnalyzerProvider.getEdgeFeatureAnalyzer(key);
					if (null == edgeAnalyzer) {
						logger.error("Unknown edge analyzer key: " + key + ".\n");
						ok = false;
					} else {
						settings.addEdgeAnalyzer(edgeAnalyzer);
					}
				}
			}
		}
		
		// Track analyzers
		if (null != trackAnalyzerProvider) {
			Element trackAnalyzerEl = analyzersEl.getChild(TRACK_ANALYSERS_ELEMENT_KEY);
			if (null == trackAnalyzerEl) {
				logger.error("Could not find the track analyzer element.\n");
				ok = false;

			} else {

				List<Element> children = trackAnalyzerEl.getChildren(ANALYSER_ELEMENT_KEY);
				for (Element child : children) {

					String key = child.getAttributeValue(ANALYSER_KEY_ATTRIBUTE);
					if (null == key) {
						logger.error("Could not find analyzer name for element " + child + ".\n");
						ok = false;
						continue;
					}
					
					TrackAnalyzer trackAnalyzer = trackAnalyzerProvider.getTrackFeatureAnalyzer(key);
					if (null == trackAnalyzer) {
						logger.error("Unknown track analyzer key: " + key + ".\n");
						ok = false;
					} else {
						settings.addTrackAnalyzer(trackAnalyzer);
					}
				}
			}
		}
	}


	private void readSingleFeatureDeclaration(Element child, Collection<String> features, 
			Map<String, String> featureNames, Map<String, String> featureShortNames, Map<String, Dimension> featureDimensions) {

		String feature 				= child.getAttributeValue(FEATURE_ATTRIBUTE);
		if (null == feature) {
			logger.error("Could not find feature declaration for element " + child + ".\n");
			ok = false;
			return;
		}
		String featureName 			= child.getAttributeValue(FEATURE_NAME_ATTRIBUTE);
		if (null == featureName) {
			logger.error("Could not find name for feature " + feature + ".\n");
			ok = false;
			return;
		}
		String featureShortName 	= child.getAttributeValue(FEATURE_SHORT_NAME_ATTRIBUTE);
		if (null == featureShortName) {
			logger.error("Could not find short name for feature " + feature + ".\n");
			ok = false;
			return;
		}
		Dimension featureDimension 	= Dimension.valueOf(child.getAttributeValue(FEATURE_DIMENSION_ATTRIBUTE));
		if (null == featureDimension) {
			logger.error("Could not find dimension for feature " + feature + ".\n");
			ok = false;
			return;
		}

		features.add(feature);
		featureNames.put(feature, featureName);
		featureShortNames.put(feature, featureShortName);
		featureDimensions.put(feature, featureDimension);
	}


}
