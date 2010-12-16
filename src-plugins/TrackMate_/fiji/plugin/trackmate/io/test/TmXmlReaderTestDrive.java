package fiji.plugin.trackmate.io.test;

import ij.ImagePlus;
import ij.gui.NewImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import loci.formats.FormatException;

import org.jdom.DataConversionException;
import org.jdom.JDOMException;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.test.LAPTrackerTestDrive;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;

public class TmXmlReaderTestDrive {

	private static final String FILE_NAME = "Test1.xml";
	private static final File file = new File(LAPTrackerTestDrive.class.getResource(FILE_NAME).getFile());
	
	public static void main(String args[]) {
		
		System.out.println("Opening file: "+file.getAbsolutePath());		
		TmXmlReader reader = new TmXmlReader(file);
		
		// Parse
		try {
			reader.parse();
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Image
		ImagePlus imp = null;
		try {
			imp = reader.getImage();
			imp.show();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FormatException e) {
			e.printStackTrace();
		}

		// All spots
		TreeMap<Integer, List<Spot>> spots = null;
		try {
			spots = reader.getAllSpots();
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		ArrayList<Spot> spotList = new ArrayList<Spot>();
		for(int frame : spots.keySet())
			for(Spot spot : spots.get(frame))
				spotList.add(spot);
		System.out.println("Found "+spotList.size()+" spots over "+spots.keySet().size()+" different frames.");
//		for (Spot spot : spotList) {
//			System.out.println(spot.toString());
//		}
		
		// Spot selection
		TreeMap<Integer, List<Spot>> selectedSpots = null;
		try {
			selectedSpots = reader.getSpotSelection(spots);
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		System.out.println("The spot selection contains "+spotList.size()+" spots over "+spots.keySet().size()+" different frames.");

		// Tracks
		SimpleGraph<Spot, DefaultEdge> trackGraph = null;
		try {
			trackGraph = reader.getTracks(selectedSpots);
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		List<Set<Spot>> tracks = new ConnectivityInspector<Spot, DefaultEdge>(trackGraph).connectedSets();		
		System.out.println("Found "+tracks.size()+" tracks.");
		
		// Settings
		System.out.println("Reading settings:");
		try {
			Settings settings = reader.getSettings();
			System.out.println(settings);
			System.out.println(settings.segmenterSettings);
			System.out.println(settings.trackerSettings);
		} catch (DataConversionException e) {
			e.printStackTrace();
		}
		
		// Display
		
		// Create fake image
		Integer[] frames = spots.keySet().toArray(new Integer[0]);
		int minFrame = Integer.MAX_VALUE;
		int maxFrame = Integer.MIN_VALUE;
		for(int i : frames) {
			if (i > maxFrame)
				maxFrame = i;
			if (i < minFrame)
				minFrame = i;
		}
		imp = NewImage.createByteImage("FakeImage", 128, 128, maxFrame+1, NewImage.FILL_BLACK);
		imp.setDimensions(1, 1, maxFrame+1);

		// Instantiate displayer
		SpotDisplayer2D displayer = new SpotDisplayer2D(imp);
		displayer.render();
		displayer.setSpots(spots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.refresh();
		displayer.setTrackGraph(trackGraph);
		
		
	}
	
	
}
