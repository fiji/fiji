package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.FeatureThreshold;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.Settings.SegmenterType;
import fiji.plugin.trackmate.Settings.TrackerType;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class TmXmlWriter implements TmXmlKeys {
	
	/*
	 * FIELD
	 */
	
	private TrackMateModelInterface model;

	/*
	 * CONSTRUCTOR
	 */
	
	public TmXmlWriter(TrackMateModelInterface model) {
		this.model = model;
	}


	/*
	 * PUBLIC METHODS
	 */
	
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		Element root = new Element(ROOT_ELEMENT_KEY);
		// Gather data
		root = echoImageInfo(root);
		root = echoBaseSettings(root);
		root = echoSegmenterSettings(root);
		root = echoTrackerSettings(root);
		root = echoThresholds(root);
		root = echoSpotSelection(root);
		root = echoTracks(root);
		root = echoAllSpots(root); // last one because it is a long list
		
		// Write to file
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(document, new FileOutputStream(file));
	}
	
	

	/*
	 * PRIVATE METHODS
	 */
	
	private Element echoBaseSettings(Element root) {
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
		return root;
	}
	
	private Element echoSegmenterSettings(Element root) {
		SegmenterSettings segSettings = model.getSettings().segmenterSettings;
		SegmenterType type = segSettings.segmenterType;
		if (null == type)
			return root;
		Element segSettingsElement = new Element(SEGMENTER_SETTINGS_ELEMENT_KEY);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_SEGMENTER_TYPE_ATTRIBUTE_NAME, 		segSettings.segmenterType.name());
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME, 		""+segSettings.expectedRadius);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_UNITS_ATTRIBUTE_NAME, 				segSettings.spaceUnits);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME, 			""+segSettings.threshold);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME, 			""+segSettings.useMedianFilter);
		root.addContent(segSettingsElement);
		return root;
	}
	
	private Element echoTrackerSettings(Element root) {
		TrackerSettings settings = model.getSettings().trackerSettings;
		TrackerType type = settings.trackerType;
		if (null == type)
			return root;
		Element trackerSettingsElement = new Element(TRACKER_SETTINGS_ELEMENT_KEY);
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_TRACKER_TYPE_ATTRIBUTE_NAME, 		settings.trackerType.name());
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_TIME_UNITS_ATTNAME, 				settings.timeUnits);
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_SPACE_UNITS_ATTNAME, 				settings.spaceUnits);
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME, 	""+settings.alternativeObjectLinkingCostFactor);
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME, 		""+settings.cutoffPercentile);
		trackerSettingsElement.setAttribute(TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME,			""+settings.blockingValue);
		// Linking
		Element linkingElement = new Element(TRACKER_SETTINGS_LINKING_ELEMENT);
		linkingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+settings.linkingDistanceCutOff));
		for(Feature feature : settings.linkingFeatureCutoffs.keySet())
			linkingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature.name(), ""+settings.linkingFeatureCutoffs.get(feature)) );
		trackerSettingsElement.addContent(linkingElement);
		// Gap-closing
		Element gapClosingElement = new Element(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
		gapClosingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+settings.allowGapClosing);
		gapClosingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+settings.gapClosingDistanceCutoff));
		gapClosingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+settings.gapClosingTimeCutoff));
		for(Feature feature : settings.gapClosingFeatureCutoffs.keySet())
			gapClosingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature.name(), ""+settings.gapClosingFeatureCutoffs.get(feature)) );
		trackerSettingsElement.addContent(gapClosingElement);
		// Splitting
		Element splittingElement = new Element(TRACKER_SETTINGS_SPLITTING_ELEMENT);
		splittingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+settings.allowSplitting);
		splittingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+settings.splittingDistanceCutoff));
		splittingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+settings.splittingTimeCutoff));
		for(Feature feature : settings.splittingFeatureCutoffs.keySet())
			splittingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature.name(), ""+settings.splittingFeatureCutoffs.get(feature)) );
		trackerSettingsElement.addContent(splittingElement);
		// Merging
		Element mergingElement = new Element(TRACKER_SETTINGS_MERGING_ELEMENT);
		mergingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+settings.allowMerging);
		mergingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+settings.mergingDistanceCutoff));
		mergingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+settings.mergingTimeCutoff));
		for(Feature feature : settings.mergingFeatureCutoffs.keySet())
			mergingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature.name(), ""+settings.mergingFeatureCutoffs.get(feature)) );
		trackerSettingsElement.addContent(mergingElement);
		// Add to root		
		root.addContent(trackerSettingsElement);
		return root;
	}
	
	private Element echoTracks(Element root) {
		SimpleGraph<Spot, DefaultEdge> trackGraph = model.getTrackGraph();
		if (null == trackGraph)
			return root;
		
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultEdge>(trackGraph).connectedSets();
		Element allTracksElement = new Element(TRACK_COLLECTION_ELEMENT_KEY);
		Element trackElement;
		Element edgeElement;
		HashMap<Set<Spot>, Element> trackElements = new HashMap<Set<Spot>, Element>(tracks.size());
		for(Set<Spot> track : tracks) {
			trackElement = new Element(TRACK_ELEMENT_KEY);
			trackElements.put(track, trackElement);
			allTracksElement.addContent(trackElement);
		}
		
		Set<DefaultEdge> edges = trackGraph.edgeSet();
		Spot source, target;
		Set<Spot> track = null;
		
		for (DefaultEdge edge : edges) {
			
			source = trackGraph.getEdgeSource(edge);
			target = trackGraph.getEdgeTarget(edge);
			for (Set<Spot> t : tracks)
				if (t.contains(source)) {
					track = t;
					break;
				}
				
			edgeElement = new Element(TRACK_EDGE_ELEMENT_KEY);
			edgeElement.setAttribute(TRACK_EDGE_SOURCE_ATTRIBUTE_NAME, ""+source.ID());
			edgeElement.setAttribute(TRACK_EDGE_TARGET_ATTRIBUTE_NAME, ""+target.ID());
			trackElements.get(track).addContent(edgeElement);
		}

		root.addContent(allTracksElement);
		return root;
	}
	
	private Element echoImageInfo(Element root) {
		Settings settings = model.getSettings();
		if (null == settings || null == settings.imp)
			return root;
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
		return root;
	}
	
	private Element echoAllSpots(Element root) {		
		TreeMap<Integer, List<Spot>> allSpots = model.getSpots();
		if (null == allSpots)
			return root;
		List<Spot> spots;
		
		Element spotElement;
		Element frameSpotsElement;
		Element spotCollection = new Element(SPOT_COLLECTION_ELEMENT_KEY);
		
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
		return root;
	}
	
	private Element echoThresholds(Element root) {
		List<FeatureThreshold> featureThresholds = model.getFeatureThresholds();
		
		Element allTresholdElement = new Element(THRESHOLD_COLLECTION_ELEMENT_KEY);
		for (FeatureThreshold threshold : featureThresholds) {
			Element thresholdElement = new Element(THRESHOLD_ELEMENT_KEY);
			thresholdElement.setAttribute(THRESHOLD_FEATURE_ATTRIBUTE_NAME, threshold.feature.name());
			thresholdElement.setAttribute(THRESHOLD_VALUE_ATTRIBUTE_NAME, threshold.value.toString());
			thresholdElement.setAttribute(THRESHOLD_ABOVE_ATTRIBUTE_NAME, ""+threshold.isAbove);
			allTresholdElement.addContent(thresholdElement);
		}
		root.addContent(allTresholdElement);
		return root;
		
	}
	
	private Element echoSpotSelection(Element root) {
		TreeMap<Integer, List<Spot>> selectedSpots =  model.getSelectedSpots();
		if (null == selectedSpots)
			return root;
		List<Spot> spots;
		
		Element spotIDElement, frameSpotsElement;
		Element spotCollection = new Element(SELECTED_SPOT_ELEMENT_KEY);
		
		for(int frame : selectedSpots.keySet()) {
			
			frameSpotsElement = new Element(SELECTED_SPOT_COLLECTION_ELEMENT_KEY);
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
		return root;
	}
	
	private static final Element marshalSpot(final Spot spot) {
		Collection<Attribute> attributes = new ArrayList<Attribute>();
		Attribute IDattribute = new Attribute(SPOT_ID_ATTRIBUTE_NAME, ""+spot.ID());
		attributes.add(IDattribute);
		Float val;
		Attribute featureAttribute;
		for (Feature feature : Feature.values()) {
			val = spot.getFeature(feature);
			if (null == val)
				continue;
			featureAttribute = new Attribute(feature.name(), val.toString());
			attributes.add(featureAttribute);
		}
		
		Element spotElement = new Element(SPOT_ELEMENT_KEY);
		spotElement.setAttributes(attributes);
		return spotElement;
	}

}
