package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class TmXmlWriter <T extends RealType<T> & NativeType<T>> {

	/*
	 * FIELD
	 */

	private TrackMateModel<T> model;
	private Element root;
	private Logger logger;
	private TrackMate_<T> plugin;

	/*
	 * CONSTRUCTORS
	 */

	public TmXmlWriter(TrackMateModel<T> model, Logger logger, TrackMate_<T> plugin) {
		this.model = model;
		this.root = new Element(ROOT_ELEMENT_KEY);
		root.setAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME, fiji.plugin.trackmate.TrackMate_.PLUGIN_NAME_VERSION);
		if (null == logger) 
			logger = Logger.VOID_LOGGER;
		this.logger = logger;
		this.plugin = plugin;
	}
	
	public TmXmlWriter(TrackMateModel<T> model, Logger logger) {
		this(model,logger, new TrackMate_<T>());
	}
	
	public TmXmlWriter(TrackMate_<T> plugin) {
		this(plugin.getModel(), plugin.getModel().getLogger(), plugin);
	}
	
	/*
	 * PUBLIC METHODS
	 */


	/**
	 * Append the image info to the root {@link Document}.
	 */
	public void appendBasicSettings() {
		echoBaseSettings();
		echoImageInfo();		
	}

	/**
	 * Append the {@link DetectorSettings} to the {@link Document}.
	 */
	public void appendDetectorSettings() {
		echoDetectorSettings();
	}

	/**
	 * Append the {@link TrackerSettings} to the {@link Document}.
	 */
	public void appendTrackerSettings() {
		echoTrackerSettings();
	}

	/**
	 * Append the initial threshold on quality to the {@link Document}.
	 */
	public void appendInitialSpotFilter() {
		echoInitialSpotFilter(model.getSettings().initialSpotFilterValue);
	}

	/**
	 * Append the list of spot {@link FeatureFilter} to the {@link Document}.
	 */
	public void appendSpotFilters() {
		echoSpotFilters();
	}

	/**
	 * Append the list of track {@link FeatureFilter} to the {@link Document}.
	 */
	public void appendTrackFilters() {
		echoTrackFilters();
	}

	/**
	 * Append the spot collection to the  {@link Document}.
	 */
	public void appendSpots() {
		echoAllSpots();
	}

	/**
	 * Append the filtered spot collection to the  {@link Document}.	
	 */
	public void appendFilteredSpots() {
		echoSpotSelection();
	}

	/**
	 * Append the tracks to the  {@link Document}.
	 */
	public void appendTracks() {
		echoTracks();
	}

	/**
	 * Append the filtered tracks to the  {@link Document}.
	 */
	public void appendFilteredTracks() {
		echoFilteredTracks();
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

	private void echoBaseSettings() {
		Settings<T> settings = model.getSettings();
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
		logger.log("  Appending base settings.\n");
		return;
	}

	private void echoDetectorSettings() {
		Element el = new Element(DETECTOR_SETTINGS_ELEMENT_KEY);
		if (null != model.getSettings().detectorFactory  && null != model.getSettings().detectorSettings) {
			DetectorProvider<T> provider = plugin.getDetectorProvider();
			boolean ok = provider.select(model.getSettings().detectorFactory.getKey());
			if (!ok) {
				logger.error(provider.getErrorMessage());
			} else {
				provider.marshall(model.getSettings().detectorSettings, el);
			}
		}
		root.addContent(el);
		return;
	}

	private void echoTrackerSettings() {
		Element element = new Element(TRACKER_SETTINGS_ELEMENT_KEY);
		if (null != model.getSettings().tracker) {
			element.setAttribute(TRACKER_ATTRIBUTE_NAME, model.getSettings().tracker);
		}
		TrackerSettings<T> settings = model.getSettings().trackerSettings;
		if (null != settings) {
			element.setAttribute(TRACKER_SETTINGS_CLASS_ATTRIBUTE_NAME, settings.getClass().getName());
			settings.marshall(element);
			logger.log("  Appending tracker settings.\n");
		} else {
			logger.log("  Tracker settings are null.\n");
		}
		// Add to root		
		root.addContent(element);
		return;
	}

	private void echoTracks() {
		if (model.getNTracks() == 0)
			return;

		Element allTracksElement = new Element(TRACK_COLLECTION_ELEMENT_KEY);

		List<Set<DefaultWeightedEdge>> trackEdges = model.getTrackEdges();

		for (int trackIndex = 0; trackIndex < trackEdges.size(); trackIndex++) {
			Set<DefaultWeightedEdge> track = trackEdges.get(trackIndex);

			Element trackElement = new Element(TRACK_ELEMENT_KEY);
			// Echo attributes and features
			trackElement.setAttribute(TRACK_ID_ATTRIBUTE_NAME, ""+trackIndex);
			for(String feature : model.getFeatureModel().getTrackFeatureValues().keySet()) {
				Double val = model.getFeatureModel().getTrackFeature(trackIndex, feature);
				if (null == val) {
					continue;
				}
				trackElement.setAttribute(feature, val.toString());
			}

			// Echo edges
			for (DefaultWeightedEdge edge : track) {

				Spot source = model.getEdgeSource(edge);
				Spot target = model.getEdgeTarget(edge);
				double weight = model.getEdgeWeight(edge);

				Element edgeElement = new Element(TRACK_EDGE_ELEMENT_KEY);
				edgeElement.setAttribute(TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, ""+source.ID());
				edgeElement.setAttribute(TRACK_EDGE_TARGET_ATTRIBUTE_NAME, ""+target.ID());
				edgeElement.setAttribute(TRACK_EDGE_WEIGHT_ATTRIBUTE_NAME, ""+weight);

				trackElement.addContent(edgeElement);
			}
			allTracksElement.addContent(trackElement);
		}
		root.addContent(allTracksElement);
		logger.log("  Appending tracks.\n");
		return;
	}

	private void echoFilteredTracks() {
		if (model.getVisibleTrackIndices() == null) {
			return;
		}

		Element filteredTracksElement = new Element(FILTERED_TRACK_ELEMENT_KEY);
		Set<Integer> indices = model.getVisibleTrackIndices();
		for(int trackIndex : indices) {
			Element trackIDElement = new Element(TRACK_ID_ELEMENT_KEY);
			trackIDElement.setAttribute(TRACK_ID_ATTRIBUTE_NAME, ""+trackIndex);
			filteredTracksElement.addContent(trackIDElement);
		}
		root.addContent(filteredTracksElement);
		logger.log("  Appending filtered tracks.\n");
	}

	private void echoImageInfo() {
		Settings<T> settings = model.getSettings();
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
		logger.log("  Appending image information.\n");
		return;
	}

	private void echoAllSpots() {		
		SpotCollection allSpots = model.getSpots();
		if (null == allSpots)
			return;
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
		logger.log("  Appending spots.\n");
		return;
	}

	private void echoInitialSpotFilter(final Double qualityThreshold) {
		Element itElement = new Element(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		itElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, Spot.QUALITY);
		itElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, ""+qualityThreshold);
		itElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, ""+true);
		root.addContent(itElement);
		logger.log("  Appending initial spot filter.\n");
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
		logger.log("  Appending spot feature filters.\n");
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
		logger.log("  Appending track feature filters.\n");
		return;
	}

	private void echoSpotSelection() {
		SpotCollection selectedSpots =  model.getFilteredSpots();
		if (null == selectedSpots)
			return;
		List<Spot> spots;

		Element spotIDElement, frameSpotsElement;
		Element spotCollection = new Element(FILTERED_SPOT_ELEMENT_KEY);

		for(int frame : selectedSpots.keySet()) {

			frameSpotsElement = new Element(FILTERED_SPOT_COLLECTION_ELEMENT_KEY);
			frameSpotsElement.setAttribute(FRAME_ATTRIBUTE_NAME, ""+frame);
			spots = selectedSpots.get(frame);

			for(Spot spot : spots) {
				spotIDElement = new Element(SPOT_ID_ELEMENT_KEY);
				spotIDElement.setAttribute(SPOT_ID_ATTRIBUTE_NAME, ""+spot.ID());
				frameSpotsElement.addContent(spotIDElement);
			}
			spotCollection.addContent(frameSpotsElement);
		}

		root.addContent(spotCollection);
		logger.log("  Appending spot selection.\n");
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
