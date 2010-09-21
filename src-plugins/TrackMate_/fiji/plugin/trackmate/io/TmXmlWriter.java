package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate_;

public class TmXmlWriter implements TmXmlKeys {
	
	/*
	 * FIELD
	 */
	
	private TrackMate_ trackmate;

	/*
	 * CONSTRUCTOR
	 */
	
	public TmXmlWriter(TrackMate_ trackmate) {
		this.trackmate = trackmate;
	}


	/*
	 * PUBLIC METHODS
	 */
	
	public void writeToFile(File file) throws FileNotFoundException, IOException {
		Element root = new Element(ROOT_ELEMENT_KEY);
		// Gather data
		root = echoImageInfo(root);
		root = echoAllSpots(root);
		root = echoThresholds(root);
		root = echoSpotSelection(root);
		
		// Write to file
		Document document = new Document(root);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(document, new FileOutputStream(file));
	}
	
	

	/*
	 * PRIVATE METHODS
	 */
	
	
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
		List<Feature> thresholdFeatures = trackmate.getThresholdFeatures();
		List<Float> thresholdValues = trackmate.getThresholdValues();
		List<Boolean> thresholdAbove = trackmate.getThresholdAbove();
		if (null == thresholdFeatures || null == thresholdValues || null == thresholdAbove)
			return root;
		
		Element allTresholdElement = new Element(THRESHOLD_COLLECTION_ELEMENT_KEY);
		for (int i = 0; i < thresholdAbove.size(); i++) {
			Element thresholdElement = new Element(THRESHOLD_ELEMENT_KEY);
			thresholdElement.setAttribute(THRESHOLD_FEATURE_ATTRIBUTE_NAME, thresholdFeatures.get(i).name());
			thresholdElement.setAttribute(THRESHOLD_VALUE_ATTRIBUTE_NAME, thresholdValues.get(i).toString());
			thresholdElement.setAttribute(THRESHOLD_ABOVE_ATTRIBUTE_NAME, ""+thresholdAbove.get(i));
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
		Element spotCollection = new Element(SELECTED_SPOT_COLLECTION_ELEMENT_KEY);
		
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
