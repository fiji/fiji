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

import mpicbg.imglib.type.numeric.RealType;

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
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class TmXmlWriter implements TmXmlKeys {
	
	/*
	 * FIELD
	 */
	
	private TrackMate_<? extends RealType<?>> trackmate;

	/*
	 * CONSTRUCTOR
	 */
	
	public TmXmlWriter(TrackMate_<? extends RealType<?>> trackmate) {
		this.trackmate = trackmate;
	}


	/*
	 * PUBLIC METHODS
	 */
	
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		Element root = new Element(ROOT_ELEMENT_KEY);
		// Gather data
		root = echoImageInfo(root);
		root = echoSettings(root);
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
	
	private Element echoSettings(Element root) {
		
		Settings settings = trackmate.getSettings();
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
		
		SegmenterSettings segSettings = settings.segmenterSettings;
		Element segSettingsElement = new Element(SEGMENTER_SETTINGS_ELEMENT_KEY);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_SEGMENTER_TYPE_ATTRIBUTE_NAME, 		segSettings.segmenterType.name());
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME, 		""+segSettings.expectedRadius);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_UNITS_ATTRIBUTE_NAME, 				segSettings.spaceUnits);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME, 			""+segSettings.threshold);
		segSettingsElement.setAttribute(SEGMENTER_SETTINGS_UNITS_ATTRIBUTE_NAME, 				""+segSettings.useMedianFilter);
		root.addContent(segSettingsElement);
		
		return root;
	}
	
	
	private Element echoTracks(Element root) {
		SimpleGraph<Spot, DefaultEdge> trackGraph = trackmate.getTrackGraph();
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
		Settings settings = trackmate.getSettings();
		if (null == settings || null == settings.imp)
			return root;
		Element imEl = new Element(IMAGE_ELEMENT_KEY);
		if (null != settings.imp.getOriginalFileInfo()) {
			imEl.setAttribute(IMAGE_FILENAME_ATTRIBUTE_NAME, 		settings.imp.getOriginalFileInfo().fileName);
			imEl.setAttribute(IMAGE_FOLDER_ATTRIBUTE_NAME, 			settings.imp.getOriginalFileInfo().directory);
		}
		imEl.setAttribute(IMAGE_WIDTH_ATTRIBUTE_NAME, 			""+settings.imp.getWidth());
		imEl.setAttribute(IMAGE_HEIGHT_ATTRIBUTE_NAME, 			""+settings.imp.getHeight());
		imEl.setAttribute(IMAGE_NSLICES_ATTRIBUTE_NAME, 		""+settings.imp.getNSlices());
		imEl.setAttribute(IMAGE_NFRAMES_ATTRIBUTE_NAME, 		""+settings.imp.getNFrames());
		imEl.setAttribute(IMAGE_PIXEL_WIDTH_ATTRIBUTE_NAME, 	""+settings.imp.getCalibration().pixelWidth);
		imEl.setAttribute(IMAGE_PIXEL_HEIGHT_ATTRIBUTE_NAME, 	""+settings.imp.getCalibration().pixelHeight);
		imEl.setAttribute(IMAGE_VOXEL_DEPTH_ATTRIBUTE_NAME, 	""+settings.imp.getCalibration().pixelDepth);
		imEl.setAttribute(IMAGE_TIME_INTERVAL_ATTRIBUTE_NAME, 	""+settings.imp.getCalibration().frameInterval);
		imEl.setAttribute(IMAGE_SPATIAL_UNITS_ATTRIBUTE_NAME,	settings.imp.getCalibration().getUnit());
		imEl.setAttribute(IMAGE_TIME_UNITS_ATTRIBUTE_NAME,		settings.imp.getCalibration().getTimeUnit());
		root.addContent(imEl);
		return root;
	}
	
	private Element echoAllSpots(Element root) {		
		TreeMap<Integer, List<Spot>> allSpots = trackmate.getSpots();
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
		List<FeatureThreshold> featureThresholds = trackmate.getFeatureThresholds();
		
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
		TreeMap<Integer, List<Spot>> selectedSpots =  trackmate.getSelectedSpots();
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
