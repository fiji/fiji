package fiji.plugin.trackmate.visualization.test;

import ij3d.Image3DUniverse;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.SpotDisplayer3D;


public class Branched3DTrackTestDrive {
	
	private static final String 	FILE_NAME_1 = "Celegans-5pc_17timepoints.xml";
	private static final File 		SPLITTING_CASE_1 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_1).getFile());
	
	public static void main(String[] args) throws JDOMException, IOException {
		
		TmXmlReader reader = new TmXmlReader(SPLITTING_CASE_1);
		reader.parse();
		
		// Load objects 
		SpotCollection allSpots 		= reader.getAllSpots();
		SpotCollection selectedSpots 	= reader.getSpotSelection(allSpots);
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> tracks = reader.getTracks(selectedSpots);
		
		// Launch ImageJ
		ij.ImageJ.main(args);
		
		// Render them
		final Image3DUniverse universe = new Image3DUniverse();
		final SpotDisplayer3D displayer = new SpotDisplayer3D(universe);
		displayer.setSpots(selectedSpots);
		displayer.render();
		universe.show();
		displayer.setTrackGraph(tracks);
	}
}
