package fiji.plugin.trackmate.io;

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
import static fiji.plugin.trackmate.io.TmXmlKeys.ROOT_ELEMENT_KEY;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;

public class TmXmlWriter implements Algorithm, Benchmark  {

	/*
	 * FIELD
	 */

	private final Element root;
	private final Logger logger;
	private final TrackMate_ plugin;
	private final TrackMateModel model;
	private final String log;
	private long processingTime;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Create a new XML file write for the specified TrackMate plugin.
	 * No log is added to the file.
	 *  
	 * @param plugin the plugin to write to XML. 
	 */
	public TmXmlWriter(final TrackMate_ plugin) {
		this(plugin, null);
	}

	/**
	 * Create a new XML file write for the specified TrackMate plugin.
	 * This constructor will cause the specified log string to be appended to the file
	 * as plain text content.
	 *  
	 * @param plugin the plugin to write to XML. 
	 * @param log  the log text to add to the file.
	 */
	public TmXmlWriter(TrackMate_ plugin, String log) {
		this.root = new Element(ROOT_ELEMENT_KEY);
		root.setAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME, fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_VERSION);
		this.logger = new Logger.StringBuilderLogger();
		this.plugin = plugin;
		this.log = log;
		this.model = plugin.getModel();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public boolean checkInput() {
		return true;
	}

	@Override
	public boolean process() {
		long start = System.currentTimeMillis();
		
		echoLog();
		echoImageInfo();
		echoBaseSettings();
		echoDetectorSettings();
		echoInitialSpotFilter();
		echoSpotFilters();
		echoTrackerSettings();
		echoTrackFilters();
		echoTracks(); // dense stuff is put at the end of file
		echoFilteredTracks();
		echoAllSpots();
		echoFilteredSpots();

		long end = System.currentTimeMillis();
		processingTime = end - start;

		return true;
	}

	@Override
	public String getErrorMessage() {
		return logger.toString();
	}

	/**
	 * Write the document to the given file.
	 */
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		logger.log("  Writing to file.\n");
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(document, new FileOutputStream(file));
	}

	@Override
	public String toString() {
		String str = "";

		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		StringWriter writer = new StringWriter();
		try {
			outputter.output(document, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		str = writer.toString();
		return str;
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private void echoLog() {
		if (null != log) {
			Element logElement = new Element(LOG_ELEMENT_KEY);
			logElement.addContent(log);
			root.addContent(logElement);
			logger.log("  Added log.\n");
		}
	}


	private void echoBaseSettings() {
		Settings settings = model.getSettings();
		Element settingsElement = new Element(SETTINGS_ELEMENT_KEY);
		settingsElement.setAttribute(SETTINGS_XSTART_ATTRIBUTE_NAME, ""+settings.xstart);
		settingsElement.setAttribute(SETTINGS_XEND_ATTRIBUTE_NAME, ""+settings.xend);
		settingsElement.setAttribute(SETTINGS_YSTART_ATTRIBUTE_NAME, ""+settings.ystart);
		settingsElement.setAttribute(SETTINGS_YEND_ATTRIBUTE_NAME, ""+settings.yend);
		settingsElement.setAttribute(SETTINGS_ZSTART_ATTRIBUTE_NAME, ""+settings.zstart);
		settingsElement.setAttribute(SETTINGS_ZEND_ATTRIBUTE_NAME, ""+settings.zend);
		settingsElement.setAttribute(SETTINGS_TSTART_ATTRIBUTE_NAME, ""+settings.tstart);
		settingsElement.setAttribute(SETTINGS_TEND_ATTRIBUTE_NAME, ""+settings.tend);
		root.addContent(settingsElement);
		logger.log("  Added base settings.\n");
	}

	private void echoDetectorSettings() {
		Element el = new Element(DETECTOR_SETTINGS_ELEMENT_KEY);
		if (null == model.getSettings().detectorFactory) {
			return; // and write nothing
		}
		DetectorProvider provider = plugin.getDetectorProvider();
		boolean ok = provider.select(model.getSettings().detectorFactory.getKey());
		if (!ok) {
			logger.error(provider.getErrorMessage());
		} else {
			provider.marshall(model.getSettings().detectorSettings, el);
		}

		root.addContent(el);
		logger.log("  Added detector settings.\n");
	}

	private void echoTrackerSettings() {
		Element el = new Element(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null == model.getSettings().tracker) {
			return; // and write nothing
		}
		
		TrackerProvider provider = plugin.getTrackerProvider();
		boolean ok = provider.select(model.getSettings().tracker.getKey());
		if (!ok) {
			logger.error(provider.getErrorMessage());
		} else {
			provider.marshall(model.getSettings().trackerSettings, el);
		}

		root.addContent(el);
		logger.log("  Added tracker settings.\n");
	}

	private void echoTracks() {
		if (model.getTrackModel().getNTracks() == 0)
			return;

		Element allTracksElement = new Element(TRACK_COLLECTION_ELEMENT_KEY);

		Map<Integer, Set<DefaultWeightedEdge>> trackEdges = model.getTrackModel().getTrackEdges();
		
		// Prepare track features for writing: we separate ints from doubles 
		List<String> trackIntFeatures = new ArrayList<String>();
		trackIntFeatures.add(TrackIndexAnalyzer.TRACK_ID);
		trackIntFeatures.add(TrackIndexAnalyzer.TRACK_INDEX); // TODO is there a better way?
		List<String> trackDoubleFeatures = new ArrayList<String>(model.getFeatureModel().getTrackFeatures());
		trackDoubleFeatures.removeAll(trackIntFeatures);
		
		// Same thing for edge features
		List<String> edgeIntFeatures = new ArrayList<String>();// TODO is there a better way?
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_SOURCE_ID);
		edgeIntFeatures.add(EdgeTargetAnalyzer.SPOT_TARGET_ID);
		List<String> edgeDoubleFeatures = new ArrayList<String>(model.getFeatureModel().getEdgeFeatures());
		edgeDoubleFeatures.removeAll(edgeIntFeatures);
		
		for (int trackID : trackEdges.keySet()) {
			Set<DefaultWeightedEdge> track = trackEdges.get(trackID);

			Element trackElement = new Element(TRACK_ELEMENT_KEY);
			trackElement.setAttribute(TRACK_NAME_ATTRIBUTE_NAME, model.getTrackModel().getTrackName(trackID));
			
			for(String feature : trackDoubleFeatures) {
				Double val = model.getFeatureModel().getTrackFeature(trackID, feature);
				trackElement.setAttribute(feature, val.toString());
			}
			
			for(String feature : trackIntFeatures) {
				int val = model.getFeatureModel().getTrackFeature(trackID, feature).intValue();
				trackElement.setAttribute(feature, ""+val);
			}

			// Echo edges
			if (track.size() == 0) {
				/* Special case: the track has only one spot in it, therefore no edge. 
				 * It just should not be, since the model never returns a track with less
				 * than one edge. So we skip writing it. */
				continue;

			} else {
				

				for (DefaultWeightedEdge edge : track) {

					Element edgeElement = new Element(TRACK_EDGE_ELEMENT_KEY);
					for(String feature : edgeDoubleFeatures) {
						Double val = model.getFeatureModel().getEdgeFeature(edge, feature);
						edgeElement.setAttribute(feature, val.toString());
					}
					for(String feature : edgeIntFeatures) {
						int val = model.getFeatureModel().getEdgeFeature(edge, feature).intValue();
						edgeElement.setAttribute(feature, ""+val);
					}

					trackElement.addContent(edgeElement);
				}
			}
			allTracksElement.addContent(trackElement);
		}
		root.addContent(allTracksElement);
		logger.log("  Added tracks.\n");
		return;
	}

	private void echoFilteredTracks() {
		if (model.getTrackModel().getFilteredTrackIDs() == null) {
			return;
		}

		Element filteredTracksElement = new Element(FILTERED_TRACK_ELEMENT_KEY);
		Set<Integer> filteredTrackKeys = model.getTrackModel().getFilteredTrackIDs();
		for (int trackID : filteredTrackKeys) {
			Element trackIDElement = new Element(TRACK_ID_ELEMENT_KEY);
			trackIDElement.setAttribute(TrackIndexAnalyzer.TRACK_ID, ""+trackID);
			filteredTracksElement.addContent(trackIDElement);
		}
		root.addContent(filteredTracksElement);
		logger.log("  Added filtered tracks.\n");
	}

	private void echoImageInfo() {
		Settings settings = model.getSettings();
		if (null == settings || null == settings.imp)
			return;
		Element imEl = new Element(IMAGE_ELEMENT_KEY);
		imEl.setAttribute(IMAGE_FILENAME_ATTRIBUTE_NAME, 		settings.imageFileName);
		imEl.setAttribute(IMAGE_FOLDER_ATTRIBUTE_NAME, 			settings.imageFolder);
		imEl.setAttribute(IMAGE_WIDTH_ATTRIBUTE_NAME, 			""+settings.width);
		imEl.setAttribute(IMAGE_HEIGHT_ATTRIBUTE_NAME, 			""+settings.height);
		imEl.setAttribute(IMAGE_NSLICES_ATTRIBUTE_NAME, 		""+settings.nslices);
		imEl.setAttribute(IMAGE_NFRAMES_ATTRIBUTE_NAME, 		""+settings.nframes);
		imEl.setAttribute(IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, 	""+settings.dx);
		imEl.setAttribute(IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, 	""+settings.dy);
		imEl.setAttribute(IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, 	""+settings.dz);
		imEl.setAttribute(IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, 	""+settings.dt);
		imEl.setAttribute(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME,	settings.spaceUnits);
		imEl.setAttribute(IMAGE_TIME_UNITS_ATTRIBUTE_NAME,		settings.timeUnits);
		root.addContent(imEl);
		logger.log("  Added image information.\n");
		return;
	}

	private void echoAllSpots() {		
		SpotCollection allSpots = model.getSpots();
		if (null == allSpots || allSpots.isEmpty())
			return; // and write nothing
		List<Spot> spots;

		Element spotElement;
		Element frameSpotsElement;
		Element spotCollection = new Element(SPOT_COLLECTION_ELEMENT_KEY);
		// Store total number of spots
		spotCollection.setAttribute(SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME, ""+allSpots.getNSpots());

		for(int frame : allSpots.keySet()) {

			frameSpotsElement = new Element(SPOT_FRAME_COLLECTION_ELEMENT_KEY);
			frameSpotsElement.setAttribute(FRAME_ATTRIBUTE_NAME, ""+frame);
			spots = allSpots.get(frame);

			for (Spot spot : spots) {
				spotElement = marshalSpot(spot);
				frameSpotsElement.addContent(spotElement);
			}
			spotCollection.addContent(frameSpotsElement);
		}
		root.addContent(spotCollection);
		logger.log("  Added spots.\n");
		return;
	}

	private void echoInitialSpotFilter() {
		Double filterVal = model.getSettings().initialSpotFilterValue;
		if (null == filterVal) {
			return; // and write nothing
		}
		Element itElement = new Element(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		itElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, Spot.QUALITY);
		itElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, "" + filterVal);
		itElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, "" + true);
		root.addContent(itElement);
		logger.log("  Added initial spot filter.\n");
		return;
	}

	private void echoSpotFilters() {
		List<FeatureFilter> featureThresholds = model.getSettings().getSpotFilters();

		Element allTresholdElement = new Element(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		for (FeatureFilter threshold : featureThresholds) {
			Element thresholdElement = new Element(FILTER_ELEMENT_KEY);
			thresholdElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, threshold.feature);
			thresholdElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, threshold.value.toString());
			thresholdElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, ""+threshold.isAbove);
			allTresholdElement.addContent(thresholdElement);
		}
		root.addContent(allTresholdElement);
		logger.log("  Added spot feature filters.\n");
		return;
	}

	private void echoTrackFilters() {
		List<FeatureFilter> featureThresholds = model.getSettings().getTrackFilters();

		Element allTresholdElement = new Element(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		for (FeatureFilter threshold : featureThresholds) {
			Element thresholdElement = new Element(FILTER_ELEMENT_KEY);
			thresholdElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, threshold.feature);
			thresholdElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, threshold.value.toString());
			thresholdElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, ""+threshold.isAbove);
			allTresholdElement.addContent(thresholdElement);
		}
		root.addContent(allTresholdElement);
		logger.log("  Added track feature filters.\n");
		return;
	}

	private void echoFilteredSpots() {
		SpotCollection filteredSpots =  model.getFilteredSpots();
		if (null == filteredSpots || filteredSpots.isEmpty())
			return;
		List<Spot> spots;

		Element spotIDElement, frameSpotsElement;
		Element spotCollection = new Element(FILTERED_SPOT_ELEMENT_KEY);

		for(int frame : filteredSpots.keySet()) {

			frameSpotsElement = new Element(FILTERED_SPOT_COLLECTION_ELEMENT_KEY);
			frameSpotsElement.setAttribute(FRAME_ATTRIBUTE_NAME, ""+frame);
			spots = filteredSpots.get(frame);

			for(Spot spot : spots) {
				spotIDElement = new Element(SPOT_ID_ELEMENT_KEY);
				spotIDElement.setAttribute(SPOT_ID_ATTRIBUTE_NAME, ""+spot.ID());
				frameSpotsElement.addContent(spotIDElement);
			}
			spotCollection.addContent(frameSpotsElement);
		}

		root.addContent(spotCollection);
		logger.log("  Added spot selection.\n");
		return;
	}

	private static final Element marshalSpot(final Spot spot) {
		Collection<Attribute> attributes = new ArrayList<Attribute>();
		Attribute IDattribute = new Attribute(SPOT_ID_ATTRIBUTE_NAME, ""+spot.ID());
		attributes.add(IDattribute);
		Attribute nameAttribute = new Attribute(SPOT_NAME_ATTRIBUTE_NAME, spot.getName());
		attributes.add(nameAttribute);
		Double val;
		Attribute featureAttribute;
		for (String feature : spot.getFeatures().keySet()) {
			val = spot.getFeature(feature);
			if (null == val)
				continue;
			featureAttribute = new Attribute(feature, val.toString());
			attributes.add(featureAttribute);
		}

		Element spotElement = new Element(SPOT_ELEMENT_KEY);
		spotElement.setAttributes(attributes);
		return spotElement;
	}


}
