package fiji.plugin.trackmate.visualization.test;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;


public class HyperStackDisplayerTestDrive {

	public static void main(String[] args) throws JDOMException, IOException {
		
//		File file = new File("E:/Users/JeanYves/Desktop/data/Celegans-5pc_17timepoints_bis.xml");
		File file = new File("/Users/tinevez/Desktop/Celegans-5pc_17timepoints_bis.xml");
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		ij.ImageJ.main(args);

		SpotCollection spots = reader.getAllSpots();
		SpotCollection selectedSpots = reader.getSpotSelection(spots);
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph = reader.getTracks(selectedSpots);
		Settings settings = reader.getSettings();
		ImagePlus imp = reader.getImage();
		settings.imp = imp;
		
		
		SpotDisplayer displayer = new HyperStackDisplayer(settings);
		displayer.setSpots(spots);
		displayer.render();
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(trackGraph);
		displayer.setDisplayTrackMode(TrackDisplayMode.LOCAL_WHOLE_TRACKS, 5);
		displayer.setRadiusDisplayRatio(0.5f);
	}
	
}
