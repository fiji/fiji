package fiji.plugin.trackmate.io;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import loci.formats.FormatException;
import loci.plugins.in.ImagePlusReader;
import loci.plugins.in.ImportProcess;
import loci.plugins.in.ImporterOptions;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jgrapht.Graph;
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

	/**
	 * Initialize this reader to read the file given in argument. No actual parsing is made at construction.
	 */
	public TmXmlReader(File file) {
		this.file = file;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	/**
	 * Parse the file to create a JDom {@link Document}. This method must be called before using any of
	 * the other getter methods.
	 */
	public void parse() throws JDOMException,  IOException {
		SAXBuilder sb = new SAXBuilder();
		document = sb.build(file);
		root = document.getRootElement();
	}
	
	/**
	 * Return the list of all spots stored in this file.
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 * @return  a {@link TreeMap} of spot list, index by frame number (one list of spot per frame, frame number
	 * is the key of the treemap). Return <code>null</code> if the spot section does is not present in the file.
	 */
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
	
	/**
	 * Return the spot selection stored in this file, taken from the list of all spots, given in argument.
	 * <p>
	 * The {@link Spot} objects in this list will be the same that of the main list given in argument. 
	 * If a spot ID referenced in the file is in the selection but not in the list given in argument,
	 * it is simply ignored, and not added to the selection list. That way, it is certain that all spots
	 * belonging to the selection list also belong to the global list. 
	 * @param allSpots  the list of all spots, from which this selection is made 
	 * @return  a {@link TreeMap} of spot list, index by frame number (one list of spot per frame, frame number
	 * is the key of the treemap). Each spot of this list belongs also to the  given list.
	 * Return <code>null</code> if the spot selection section does is not present in the file.
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 */
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
	
	/**
	 * Return the {@link Graph} mapping spot linking as tracks. The graph vertices are made of the selected spot
	 * list given in argument. Edges are formed from the file data.
	 * @param selectedSpots  the spot selection from which tracks area made 
	 * @return  a {@link SimpleGraph} encompassing spot linking, or <code>null</code> if the track section does is
	 * not present in the file.
	 * @throws DataConversionException  if the attribute values are not formatted properly in the file.
	 */
	@SuppressWarnings("unchecked")
	public SimpleGraph<Spot, DefaultEdge> getTracks(TreeMap<Integer, List<Spot>> selectedSpots) throws DataConversionException {
		
		Element allTracksElement = root.getChild(TRACK_COLLECTION_ELEMENT_KEY);
		if (null == allTracksElement)
			return null;
		
		// Add all spots to the graph
		SimpleGraph<Spot, DefaultEdge> trackGraph = new SimpleGraph<Spot, DefaultEdge>(DefaultEdge.class);
		for(int frame : selectedSpots.keySet())
			for(Spot spot : selectedSpots.get(frame))
				trackGraph.addVertex(spot);		
		Set<Spot> spots = trackGraph.vertexSet();

		// Load tracks
		List<Element> trackElements = allTracksElement.getChildren(TRACK_ELEMENT_KEY);
		List<Element> edgeElements;
		int sourceID, targetID;
		Spot sourceSpot, targetSpot;
		boolean sourceFound, targetFound;
		for (Element trackElement : trackElements) {
			edgeElements = trackElement.getChildren(TRACK_EDGE_ELEMENT_KEY);
			for (Element edgeElement : edgeElements) {
				// Get source and target ID for this edge
				sourceID = edgeElement.getAttribute(TRACK_EDGE_SOURCE_ATTRIBUTE_NAME).getIntValue();
				targetID = edgeElement.getAttribute(TRACK_EDGE_TARGET_ATTRIBUTE_NAME).getIntValue();
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
						trackGraph.addEdge(sourceSpot, targetSpot);
						break;
					}
				}
			}
		}
		
		return trackGraph;
	}
	
	public ImagePlus getImage() throws IOException, FormatException  {
		Element imageInfoElement = root.getChild(IMAGE_ELEMENT_KEY);
		if (null == imageInfoElement)
			return null;
		String filename = imageInfoElement.getAttribute(IMAGE_FILENAME_ATTRIBUTE_NAME).getValue();
		String folder 	= imageInfoElement.getAttribute(IMAGE_FOLDER_ATTRIBUTE_NAME).getValue();
		File imageFile = new File(folder, filename);
		if (!imageFile.exists() || !imageFile.canRead())
			return null;

		ImporterOptions options = new ImporterOptions();
		options.loadOptions();
		options.parseArg(imageFile.getAbsolutePath());
		options.checkObsoleteOptions();

		ImportProcess process = new ImportProcess(options);
		process.execute();
		
		ImagePlusReader reader = new ImagePlusReader(process);
		ImagePlus[] imps = reader.openImagePlus();
		process.getReader().close();
	    return imps[0];

	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private static Spot createSpotFrom(Element spotEl) throws DataConversionException {
		int ID = spotEl.getAttribute(SPOT_ID_ATTRIBUTE_NAME).getIntValue();
		Spot spot = new SpotImp(ID);
		for (Feature feature : Feature.values()) {
			Attribute att = spotEl.getAttribute(feature.name());
			if (null == att)
				continue;
			spot.putFeature(feature, att.getFloatValue());
		}
		return spot;
	}
}
