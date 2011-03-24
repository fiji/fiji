package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jdom.JDOMException;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.DisplayerType;
import fiji.plugin.trackmate.visualization.test.Branched3DTrackTestDrive;

public class TrackVisualizerTestDrive {

	private static final long serialVersionUID = 1L;
	private static final String 	FILE_NAME_2 = "FakeTracks.xml";
	private static final File 		CASE_2 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_2).getFile());
	private static final File		CELEGANS_2HOURS = new File("/Volumes/Data/Data/Confocal_LSM700/10-01-21/10-01-21-after-removal2.xml");
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(CASE_2);
		reader.parse();
		
		// Load objects 
		SpotCollection allSpots 		= reader.getAllSpots();
		SpotCollection selectedSpots 	= reader.getSpotSelection(allSpots);
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
		TrackMateModelInterface model = new TrackMate_();
		model.setSettings(settings);
//		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
		displayer.render();
		displayer.setSpots(allSpots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(tracks);
		displayer.refresh();
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(tracks, settings);
		frame.setVisible(true);
		
		// Listeners
		new SpotSelectionManager(displayer, frame);

		frame.addGraphListener(new GraphListener<Spot, DefaultWeightedEdge>() {

			@Override
			public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
				System.out.println("Removed a spot");
			}
			
			@Override
			public void vertexAdded(GraphVertexChangeEvent<Spot> e) {
				System.out.println("Added a spot");
			}
			
			@Override
			public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				displayer.setTrackGraph(frame.getTrackModel());
				displayer.refresh();
			}
			
			@Override
			public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				displayer.setTrackGraph(frame.getTrackModel());
				displayer.refresh();
			}
		});
		
		
		
	}
}
