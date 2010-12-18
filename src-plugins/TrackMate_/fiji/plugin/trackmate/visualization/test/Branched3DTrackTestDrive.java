package fiji.plugin.trackmate.visualization.test;

import ij3d.Image3DUniverse;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;


public class Branched3DTrackTestDrive {
	
	private static final String 	FILE_NAME_1 = "Celegans-5pc_17timepoints.xml";
	private static final File 		SPLITTING_CASE_1 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_1).getFile());
	private static final float 		RADIUS = 3.0f; 
	
	
	public static void main(String[] args) throws JDOMException, IOException {
		
		TmXmlReader reader = new TmXmlReader(SPLITTING_CASE_1);
		reader.parse();
		
		// Load objects 
		TreeMap<Integer, List<Spot>> allSpots 		= reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots 	= reader.getSpotSelection(allSpots);
		SimpleGraph<Spot, DefaultEdge> tracks = reader.getTracks(selectedSpots);
		
		// Launch ImageJ
		ij.ImageJ.main(args);
		
		// Render them
		final Image3DUniverse universe = new Image3DUniverse();
		final SpotDisplayer3D displayer = new SpotDisplayer3D(universe, RADIUS);
		displayer.setSpots(selectedSpots);
		displayer.render();
		universe.show();
		displayer.setTrackGraph(tracks);
		
	}

}
