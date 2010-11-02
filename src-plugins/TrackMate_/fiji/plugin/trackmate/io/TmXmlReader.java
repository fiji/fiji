package fiji.plugin.trackmate.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class TmXmlReader implements TmXmlKeys {

	
	private Document document = null;
	private File file;
	private Element root;

	/*
	 * CONSTRUCTOR
	 */


	public TmXmlReader(File file) {
		this.file = file;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	public void parse() throws JDOMException,  IOException {
		SAXBuilder sb = new SAXBuilder();
		document = sb.build(file);
		root = document.getRootElement();
	}
	
	@SuppressWarnings("unchecked")
	public TreeMap<Integer, List<Spot>> getAllSpots() throws DataConversionException {
		Element spotCollection = root.getChild(SPOT_COLLECTION_ELEMENT_KEY);
		if (null == spotCollection)
			return null;
		
		List<Element> frameContent = spotCollection.getChildren(SPOT_FRAME_COLLECTION_ELEMENT_KEY);
		int currentFrame = 0;
		ArrayList<Spot> spotList;
		TreeMap<Integer, List<Spot>> allSpots = new TreeMap<Integer, List<Spot>>();
		
		for (Element currentFrameContent : frameContent) {
			
			currentFrame = currentFrameContent.getAttribute(FRAME_ATTRIBUTE_NAME).getIntValue();
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
	
	
	@SuppressWarnings("unchecked")
	public TreeMap<Integer, List<Spot>> getSpotSelection(TreeMap<Integer, List<Spot>> allSpots) throws DataConversionException {
		Element selectedSpotCollection = root.getChild(SELECTED_SPOT_ELEMENT_KEY);
		if (null == selectedSpotCollection)
			return null;
		
		int currentFrame = 0;
		int ID;
		ArrayList<Spot> spotList;
		List<Element> spotContent;
		List<Spot> spotsThisFrame;
		TreeMap<Integer, List<Spot>> spotSelection = new TreeMap<Integer, List<Spot>>();
		List<Element> frameContent = selectedSpotCollection.getChildren(SELECTED_SPOT_COLLECTION_ELEMENT_KEY);
		
		for (Element currentFrameContent : frameContent) {
			currentFrame = currentFrameContent.getAttribute(FRAME_ATTRIBUTE_NAME).getIntValue();
			// Get spot list from main list
			spotsThisFrame = allSpots.get(currentFrame);
			if (null == spotsThisFrame)
				continue;
			
			spotContent = currentFrameContent.getChildren(SPOT_ID_ELEMENT_KEY);
			spotList = new ArrayList<Spot>(spotContent.size());
			// Loop over all spot element
			for (Element spotEl : spotContent) {
				ID = spotEl.getAttribute(SPOT_ID_ATTRIBUTE_NAME).getIntValue();
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
	
	public SimpleGraph<Spot, DefaultEdge> getTracks(TreeMap<Integer, List<Spot>> selectedSpots) {
		
		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;
		
		// Add all spots to the graph
		SimpleGraph<Spot, DefaultEdge> trackGraph = new SimpleGraph<Spot, DefaultEdge>(DefaultEdge.class);
		for(int frame : selectedSpots.keySet())
			for(Spot spot : selectedSpots.get(frame))
				trackGraph.addVertex(spot);

		// Load tracks
//		TODO 
		
		return trackGraph;
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private Spot createSpotFrom(Element spotEl) throws DataConversionException {
		int ID = spotEl.getAttribute(SPOT_ID_ATTRIBUTE_NAME).getIntValue();
		Spot spot = new SpotImp(ID);
		for (Feature feature : Feature.values()) {
			Attribute att = spotEl.getAttribute(feature.name());
			spot.putFeature(feature, att.getFloatValue());
		}
		return spot;
	}
}
