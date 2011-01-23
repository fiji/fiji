package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.jdom.JDOMException;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.test.LAPTrackerTestDrive;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.DisplayerType;
import fiji.plugin.trackmate.visualization.test.Branched3DTrackTestDrive;

public class TrackVisualizerTestDrive {

	private static final long serialVersionUID = 1L;
	private static final String 	FILE_NAME_1 = "Test2.xml";
	private static final File 		CASE_1 = new File(LAPTrackerTestDrive.class.getResource(FILE_NAME_1).getFile());
	private static final String 	FILE_NAME_2 = "FakeTracks.xml";
	private static final File 		CASE_2 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_2).getFile());
	private static final String 	FILE_NAME_3 ="Celegans-5pc_17timepoints.xml";
	private static final File 		CASE_3 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_3).getFile());
	private static final String 	FILE_NAME_4 ="Celegans-5pc.xml";
	private static final File 		CASE_4 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_4).getFile());
	private static final String 	FILE_NAME_5 ="SwimmingAlgae.xml";
	private static final File 		CASE_5 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_5).getFile());
	
	
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(CASE_3);
		reader.parse();
		
		// Load objects 
		TreeMap<Integer, List<Spot>> allSpots 		= reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots 	= reader.getSpotSelection(allSpots);
		final SimpleWeightedGraph<Spot, DefaultWeightedEdge> tracks = reader.getTracks(selectedSpots);
		ImagePlus imp = reader.getImage();
		Settings settings = reader.getSettings();
		settings.imp = imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		// Instantiate displayer
		@SuppressWarnings("rawtypes")
		TrackMateModelInterface model = new TrackMate_();
		model.setSettings(settings);
//		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.THREEDVIEWER_DISPLAYER, model);
		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(DisplayerType.HYPERSTACK_DISPLAYER, model);
		displayer.render();
		displayer.setSpots(allSpots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(tracks);
		displayer.refresh();
		
		// Update icons
		if (null != imp) {
			SpotIconGrabber grabber = new SpotIconGrabber(settings);
			grabber.updateIcon(allSpots);
		}
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(tracks);
		frame.setVisible(true);

		frame.getJGraph().addGraphSelectionListener(new GraphSelectionListener() {

			private HashSet<Spot> highlightedSpots = new  HashSet<Spot>();
			private HashSet<DefaultWeightedEdge> highlightedEdges = new HashSet<DefaultWeightedEdge>();

			@Override
			public void valueChanged(GraphSelectionEvent event) {
				Object[] cells = event.getCells();
				for(Object cell : cells) {
					if (cell instanceof SpotCell) {
						SpotCell spotCell = (SpotCell) cell;
						if (event.isAddedCell(cell)) 
							highlightedSpots.add(spotCell.getSpot());
						else
							highlightedSpots.remove(spotCell.getSpot());
					} else if (cell instanceof TrackEdgeCell) {
						TrackEdgeCell edgeCell = (TrackEdgeCell) cell;
						if (event.isAddedCell(cell)) 
							highlightedEdges.add(edgeCell.getEdge());
						else
							highlightedEdges.remove(edgeCell.getEdge());
					}
				}
				displayer.highlightEdges(highlightedEdges);
				displayer.highlightSpots(highlightedSpots);
			}
		});


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
