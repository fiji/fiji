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
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;


public class Branched3DTrackTestDrive {
	
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");

	public static void main(String[] args) throws JDOMException, IOException {
		
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		// Load objects 
		SpotCollection allSpots 		= reader.getAllSpots();
		SpotCollection selectedSpots 	= reader.getFilteredSpots(allSpots);
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
