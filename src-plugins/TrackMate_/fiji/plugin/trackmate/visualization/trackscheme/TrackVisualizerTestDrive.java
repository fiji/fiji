package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jdom.JDOMException;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView.ViewType;

public class TrackVisualizerTestDrive {

	private static final long serialVersionUID = 1L;
	private static final File file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		// Load objects 
		SpotCollection allSpots 		= reader.getAllSpots();
		SpotCollection selectedSpots 	= reader.getFilteredSpots(allSpots);
		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> tracks = reader.getTracks(selectedSpots);
		
		List<Set<Spot>> trackList = new ConnectivityInspector<Spot, DefaultWeightedEdge>(tracks).connectedSets();
		System.out.println("Found "+trackList.size()+" tracks.");// DEBUG
		for(Set<Spot> track : trackList) {
			System.out.println(" - "+track.size()+" spots in track.");// DEBUG
		}
			
		ImagePlus imp = reader.getImage();
		Settings settings = reader.getSettings();
		settings.segmenterSettings = reader.getSegmenterSettings();
		settings.imp = imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		// Instantiate displayer
		TrackMateModel model = reader.getModel();
//		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
		final TrackMateModelView displayer = TrackMateModelView.instantiateView(ViewType.HYPERSTACK_DISPLAYER, model);
		displayer.setSpots(allSpots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(tracks);
		displayer.refresh();
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(model);
		frame.setVisible(true);
		
	}
}
