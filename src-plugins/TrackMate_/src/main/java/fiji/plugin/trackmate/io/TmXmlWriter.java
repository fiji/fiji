package fiji.plugin.trackmate.io;

import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_TEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.*;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_XEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_XSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_YEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_YSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ZEND_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.CROP_ZSTART_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.DETECTOR_SETTINGS_ELEMENT_KEY;
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
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.IMAGE_WIDTH_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.INITIAL_SPOT_FILTER_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.LOG_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.MODEL_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.PLUGIN_VERSION_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.ROOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SETTINGS_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPATIAL_UNITS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FILTER_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_FRAME_COLLECTION_ELEMENT_KEY;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_ID_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.SPOT_NAME_ATTRIBUTE_NAME;
import static fiji.plugin.trackmate.io.TmXmlKeys.TIME_UNITS_ATTRIBUTE_NAME;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.edges.EdgeAnalyzer;
import fiji.plugin.trackmate.features.edges.EdgeTargetAnalyzer;
import fiji.plugin.trackmate.features.spot.SpotAnalyzerFactory;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.providers.DetectorProvider;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TmXmlWriter {

	/*
	 * FIELD
	 */

	private final Element root;
	private final Logger logger;
	private final File file;

	/*
	 * CONSTRUCTORS
	 */

	/**
	 * Creates a new XML file writer for TrackMate. 
	 *
	 * @param file the xml file to write to, will be overwritten.
	 */
	public TmXmlWriter(File file) {
		this.root = new Element(ROOT_ELEMENT_KEY);
		root.setAttribute(PLUGIN_VERSION_ATTRIBUTE_NAME, fiji.plugin.trackmate.TrackMate.PLUGIN_NAME_VERSION);
		this.logger = new Logger.StringBuilderLogger();
		this.file = file;
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Writes the document to the file. Content must be appended first.
	 * @see #appendLog(String)
	 * @see #appendModel(TrackMateModel)
	 * @see #appendSettings(Settings, DetectorProvider, TrackerProvider)
	 */
	public void writeToFile() throws FileNotFoundException, IOException {
		logger.log("  Writing to file.\n");
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(document, new FileOutputStream(file));
	}

	@Override
	public String toString() {
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		StringWriter writer = new StringWriter();
		try {
			outputter.output(document, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}

	/**
	 * Appends the content of a {@link TrackMateModel} to the file generated by this writer.
	 * @param model the {@link TrackMateModel} to write.
	 */
	public void appendModel(TrackMateModel model) {
		Element modelElement = new Element(MODEL_ELEMENT_KEY);
		modelElement.setAttribute(SPATIAL_UNITS_ATTRIBUTE_NAME, model.getSpaceUnits());
		modelElement.setAttribute(TIME_UNITS_ATTRIBUTE_NAME, model.getTimeUnits());
		
		Element featureDeclarationElement = echoFeaturesDeclaration(model);
		modelElement.addContent(featureDeclarationElement);
		
		Element spotElement = echoSpots(model);
		modelElement.addContent(spotElement);
		
		Element trackElement = echoTracks(model);
		modelElement.addContent(trackElement);
		
		Element filteredTrackElement = echoFilteredTracks(model);
		modelElement.addContent(filteredTrackElement);
		
		root.addContent(modelElement);
	}
	
	/**
	 * Appends the content of a {@link Settings} object to the file generated by this writer.
	 * @param settings  the {@link Settings} to write.
	 * @param detectorProvider 
	 */
	public void appendSettings(Settings settings, DetectorProvider detectorProvider, TrackerProvider trackerProvider) {
		Element settingsElement = new Element(SETTINGS_ELEMENT_KEY);
		
		Element imageInfoElement = echoImageInfo(settings);
		settingsElement.addContent(imageInfoElement);
		
		Element cropElement = echoCropSettings(settings);
		settingsElement.addContent(cropElement);
		
		Element detectorElement = echoDetectorSettings(settings, detectorProvider);
		settingsElement.addContent(detectorElement);
		
		Element initFilter = echoInitialSpotFilter(settings);
		settingsElement.addContent(initFilter);
		
		Element spotFiltersElement = echoSpotFilters(settings);
		settingsElement.addContent(spotFiltersElement);
		
		Element trackerElement = echoTrackerSettings(settings, trackerProvider);
		settingsElement.addContent(trackerElement);
		
		Element trackFiltersElement = echoTrackFilters(settings);
		settingsElement.addContent(trackFiltersElement);
		
		Element analyzersElement = echoAnalyzers(settings);
		settingsElement.addContent(analyzersElement);
		
		root.addContent(settingsElement);
	}


	public void appendLog(String log) {
		if (null != log) {
			Element logElement = new Element(LOG_ELEMENT_KEY);
			logElement.addContent(log);
			root.addContent(logElement);
			logger.log("  Added log.\n");
		}
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private Element echoCropSettings(Settings settings) {
		Element settingsElement = new Element(CROP_ELEMENT_KEY);
		settingsElement.setAttribute(CROP_XSTART_ATTRIBUTE_NAME, ""+settings.xstart);
		settingsElement.setAttribute(CROP_XEND_ATTRIBUTE_NAME, ""+settings.xend);
		settingsElement.setAttribute(CROP_YSTART_ATTRIBUTE_NAME, ""+settings.ystart);
		settingsElement.setAttribute(CROP_YEND_ATTRIBUTE_NAME, ""+settings.yend);
		settingsElement.setAttribute(CROP_ZSTART_ATTRIBUTE_NAME, ""+settings.zstart);
		settingsElement.setAttribute(CROP_ZEND_ATTRIBUTE_NAME, ""+settings.zend);
		settingsElement.setAttribute(CROP_TSTART_ATTRIBUTE_NAME, ""+settings.tstart);
		settingsElement.setAttribute(CROP_TEND_ATTRIBUTE_NAME, ""+settings.tend);
		logger.log("  Added crop settings.\n");
		return settingsElement;
	}

	private Element echoDetectorSettings(Settings settings, DetectorProvider provider) {
		Element el = new Element(DETECTOR_SETTINGS_ELEMENT_KEY);
		boolean ok = provider.select(settings.detectorFactory.getKey());
		if (!ok) {
			logger.error(provider.getErrorMessage());
		} else {
			provider.marshall(settings.detectorSettings, el);
		}

		logger.log("  Added detector settings.\n");
		return el;
	}

	private Element echoTrackerSettings(Settings settings, TrackerProvider provider) {
		Element el = new Element(TRACKER_SETTINGS_ELEMENT_KEY);
		
		boolean ok = provider.select(settings.tracker.getKey());
		if (!ok) {
			logger.error(provider.getErrorMessage());
		} else {
			provider.marshall(settings.trackerSettings, el);
		}

		logger.log("  Added tracker settings.\n");
		return el;
	}

	private Element echoTracks(TrackMateModel model) {

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
		logger.log("  Added tracks.\n");
		return allTracksElement;
	}

	private Element echoFilteredTracks(TrackMateModel model) {
		Element filteredTracksElement = new Element(FILTERED_TRACK_ELEMENT_KEY);
		Set<Integer> filteredTrackKeys = model.getTrackModel().getFilteredTrackIDs();
		for (int trackID : filteredTrackKeys) {
			Element trackIDElement = new Element(TRACK_ID_ELEMENT_KEY);
			trackIDElement.setAttribute(TrackIndexAnalyzer.TRACK_ID, ""+trackID);
			filteredTracksElement.addContent(trackIDElement);
		}
		logger.log("  Added filtered tracks.\n");
		return filteredTracksElement;
	}

	private Element echoImageInfo(Settings settings) {
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
		logger.log("  Added image information.\n");
		return imEl;
	}

	private Element echoSpots(TrackMateModel model) {
		SpotCollection spots = model.getSpots();

		Element spotCollectionElement = new Element(SPOT_COLLECTION_ELEMENT_KEY);
		// Store total number of spots
		spotCollectionElement.setAttribute(SPOT_COLLECTION_NSPOTS_ATTRIBUTE_NAME, ""+spots.getNSpots(false));

		for (int frame : spots.keySet()) {

			Element frameSpotsElement = new Element(SPOT_FRAME_COLLECTION_ELEMENT_KEY);
			frameSpotsElement.setAttribute(FRAME_ATTRIBUTE_NAME, ""+frame);

			for (Iterator<Spot> it = spots.iterator(frame, false); it.hasNext();) {
				Element spotElement = marshalSpot(it.next());
				frameSpotsElement.addContent(spotElement);
			}
			spotCollectionElement.addContent(frameSpotsElement);
		}
		logger.log("  Added " + spots.getNSpots(false) + " spots.\n");
		return spotCollectionElement;
	}
	
	private Element echoFeaturesDeclaration(TrackMateModel model) {
		
		FeatureModel fm = model.getFeatureModel();
		Element featuresElement = new Element(FEATURE_DECLARATIONS_ELEMENT_KEY);
		
		// Spots
		Element spotFeaturesElement = new Element(SPOT_FEATURES_ELEMENT_KEY);
		Collection<String> features = fm.getSpotFeatures();
		Map<String, String> featureNames = fm.getSpotFeatureNames();
		Map<String, String> featureShortNames = fm.getSpotFeatureShortNames();
		Map<String, Dimension> featureDimensions = fm.getSpotFeatureDimensions();
		for (String feature : features) {
			Element fel = new Element(FEATURE_ELEMENT_KEY);
			fel.setAttribute(FEATURE_ATTRIBUTE, feature);
			fel.setAttribute(FEATURE_NAME_ATTRIBUTE, featureNames.get(feature));
			fel.setAttribute(FEATURE_SHORT_NAME_ATTRIBUTE, featureShortNames.get(feature));
			fel.setAttribute(FEATURE_DIMENSION_ATTRIBUTE, featureDimensions.get(feature).name());
			spotFeaturesElement.addContent(fel);
		}
		featuresElement.addContent(spotFeaturesElement);
		
		// Edges
		Element edgeFeaturesElement = new Element(EDGE_FEATURES_ELEMENT_KEY);
		features = fm.getEdgeFeatures();
		featureNames = fm.getEdgeFeatureNames();
		featureShortNames = fm.getEdgeFeatureShortNames();
		featureDimensions = fm.getEdgeFeatureDimensions();
		for (String feature : features) {
			Element fel = new Element(FEATURE_ELEMENT_KEY);
			fel.setAttribute(FEATURE_ATTRIBUTE, feature);
			fel.setAttribute(FEATURE_NAME_ATTRIBUTE, featureNames.get(feature));
			fel.setAttribute(FEATURE_SHORT_NAME_ATTRIBUTE, featureShortNames.get(feature));
			fel.setAttribute(FEATURE_DIMENSION_ATTRIBUTE, featureDimensions.get(feature).name());
			edgeFeaturesElement.addContent(fel);
		}
		featuresElement.addContent(edgeFeaturesElement);
		
		// Tracks
		Element trackFeaturesElement = new Element(TRACK_FEATURES_ELEMENT_KEY);
		features = fm.getTrackFeatures();
		featureNames = fm.getTrackFeatureNames();
		featureShortNames = fm.getTrackFeatureShortNames();
		featureDimensions = fm.getTrackFeatureDimensions();
		for (String feature : features) {
			Element fel = new Element(FEATURE_ELEMENT_KEY);
			fel.setAttribute(FEATURE_ATTRIBUTE, feature);
			fel.setAttribute(FEATURE_NAME_ATTRIBUTE, featureNames.get(feature));
			fel.setAttribute(FEATURE_SHORT_NAME_ATTRIBUTE, featureShortNames.get(feature));
			fel.setAttribute(FEATURE_DIMENSION_ATTRIBUTE, featureDimensions.get(feature).name());
			trackFeaturesElement.addContent(fel);
		}
		featuresElement.addContent(trackFeaturesElement);
		
		logger.log("  Added spot, edge and track feature declarations.");
		return featuresElement;
	}


	private Element echoInitialSpotFilter(Settings settings) {
		Element itElement = new Element(INITIAL_SPOT_FILTER_ELEMENT_KEY);
		itElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, Spot.QUALITY);
		itElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, "" + settings.initialSpotFilterValue);
		itElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, "" + true);
		logger.log("  Added initial spot filter.\n");
		return itElement;
	}

	private Element echoSpotFilters(Settings settings) {
		List<FeatureFilter> featureThresholds = settings.getSpotFilters();

		Element filtersElement = new Element(SPOT_FILTER_COLLECTION_ELEMENT_KEY);
		for (FeatureFilter threshold : featureThresholds) {
			Element thresholdElement = new Element(FILTER_ELEMENT_KEY);
			thresholdElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, threshold.feature);
			thresholdElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, threshold.value.toString());
			thresholdElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, ""+threshold.isAbove);
			filtersElement.addContent(thresholdElement);
		}
		logger.log("  Added spot feature filters.\n");
		return filtersElement;
	}

	private Element echoTrackFilters(Settings settings) {
		List<FeatureFilter> filters = settings.getTrackFilters();

		Element trackFiltersElement = new Element(TRACK_FILTER_COLLECTION_ELEMENT_KEY);
		for (FeatureFilter filter : filters) {
			Element thresholdElement = new Element(FILTER_ELEMENT_KEY);
			thresholdElement.setAttribute(FILTER_FEATURE_ATTRIBUTE_NAME, filter.feature);
			thresholdElement.setAttribute(FILTER_VALUE_ATTRIBUTE_NAME, filter.value.toString());
			thresholdElement.setAttribute(FILTER_ABOVE_ATTRIBUTE_NAME, ""+filter.isAbove);
			trackFiltersElement.addContent(thresholdElement);
		}
		logger.log("  Added track feature filters.\n");
		return trackFiltersElement;
	}
	
	private Element echoAnalyzers(Settings settings) {
		Element analyzersElement = new Element(ANALYZER_COLLECTION_ELEMENT_KEY);
		
		// Spot analyzers
		Element spotAnalyzersEl = new Element(SPOT_ANALYSERS_ELEMENT_KEY);
		for (SpotAnalyzerFactory<?> analyzer : settings.getSpotAnalyzerFactories()) {
			Element analyzerEl = new Element(ANALYSER_ELEMENT_KEY);
			analyzerEl.setAttribute(ANALYSER_KEY_ATTRIBUTE, analyzer.getKey());
		}
		analyzersElement.addContent(spotAnalyzersEl);
		
		// Edge analyzers
		Element edgeAnalyzersEl = new Element(EDGE_ANALYSERS_ELEMENT_KEY);
		for (EdgeAnalyzer analyzer : settings.getEdgeAnalyzers()) {
			Element analyzerEl = new Element(ANALYSER_ELEMENT_KEY);
			analyzerEl.setAttribute(ANALYSER_KEY_ATTRIBUTE, analyzer.getKey());
		}
		analyzersElement.addContent(edgeAnalyzersEl);

		// Track analyzers
		Element trackAnalyzersEl = new Element(TRACK_ANALYSERS_ELEMENT_KEY);
		for (TrackAnalyzer analyzer : settings.getTrackAnalyzers()) {
			Element analyzerEl = new Element(ANALYSER_ELEMENT_KEY);
			analyzerEl.setAttribute(ANALYSER_KEY_ATTRIBUTE, analyzer.getKey());
		}
		analyzersElement.addContent(trackAnalyzersEl);

		logger.log("  Added spot, edge and track analyzers.\n");
		return analyzersElement;
	}

	
	
	
	/*
	 * STATIC METHODS
	 */

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
