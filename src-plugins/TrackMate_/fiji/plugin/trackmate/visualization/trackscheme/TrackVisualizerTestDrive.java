package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import loci.formats.FormatException;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.test.LAPTrackerTestDrive;
import fiji.plugin.trackmate.visualization.test.Branched3DTrackTestDrive;

public class TrackVisualizerTestDrive {

	private static final long serialVersionUID = 1L;
	private static final String 	FILE_NAME_1 = "Test2.xml";
	private static final File 		CASE_1 = new File(LAPTrackerTestDrive.class.getResource(FILE_NAME_1).getFile());
	private static final String 	FILE_NAME_2 = "FakeTracks.xml";
	private static final File 		CASE_2 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_2).getFile());
	private static final String 	FILE_NAME_3 ="Celegans-5pc_17timepoints.xml";
	private static final File 		CASE_3 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_3).getFile());
	
	
	public static void main(String[] args) throws JDOMException, IOException, FormatException {
	
		TmXmlReader reader = new TmXmlReader(CASE_3);
		reader.parse();
		
		// Load objects 
		TreeMap<Integer, List<Spot>> allSpots 		= reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots 	= reader.getSpotSelection(allSpots);
		SimpleGraph<Spot, DefaultEdge> tracks = reader.getTracks(selectedSpots);
		ImagePlus imp = reader.getImage();
		Settings settings = reader.getSettings();
		settings.imp = imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		// Update icons
		if (null != imp) {
			SpotIconGrabber grabber = new SpotIconGrabber(settings);
			grabber.updateIcon(allSpots);
		}
		
		// Display Track scheme
		TrackSchemeFrame frame = new TrackSchemeFrame(tracks, settings);
        frame.setVisible(true);
        
	}
}
