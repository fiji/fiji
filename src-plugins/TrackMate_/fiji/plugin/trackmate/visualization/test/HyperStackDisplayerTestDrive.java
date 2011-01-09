package fiji.plugin.trackmate.visualization.test;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer;


public class HyperStackDisplayerTestDrive {

	public static void main(String[] args) throws JDOMException, IOException {
		
		File file = new File("E:/Users/JeanYves/Desktop/data/Celegans-5pc_17timepoints_bis.xml");
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		ij.ImageJ.main(args);

		TreeMap<Integer, List<Spot>> spots = reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots = reader.getSpotSelection(spots);
		SimpleGraph<Spot, DefaultEdge> trackGraph = reader.getTracks(selectedSpots);
		Settings settings = reader.getSettings();
		ImagePlus imp = reader.getImage();
		settings.imp = imp;
		
		SpotDisplayer displayer = new HyperStackDisplayer(settings);
		displayer.setSpots(spots);
		displayer.render();
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(trackGraph);
		displayer.setRadiusDisplayRatio(0.5f);
	}
	
}
