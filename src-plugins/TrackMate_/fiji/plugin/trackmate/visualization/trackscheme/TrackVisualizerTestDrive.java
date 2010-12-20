package fiji.plugin.trackmate.visualization.trackscheme;

import ij.ImagePlus;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import loci.formats.FormatException;

import org.jdom.JDOMException;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphLayoutCache;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.test.LAPTrackerTestDrive;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;
import fiji.plugin.trackmate.visualization.test.Branched3DTrackTestDrive;

public class TrackVisualizerTestDrive extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String 	FILE_NAME_1 = "Test2.xml";
	private static final File 		CASE_1 = new File(LAPTrackerTestDrive.class.getResource(FILE_NAME_1).getFile());
	private static final String 	FILE_NAME_2 = "FakeTracks.xml";
	private static final File 		CASE_2 = new File(Branched3DTrackTestDrive.class.getResource(FILE_NAME_2).getFile());
	private static final Dimension 	DEFAULT_SIZE = new Dimension( 530, 320 );
	private static final String 	FILE_NAME_3 ="Celegans-5pc_17timepoints.xml";
	private static final File 		CASE_3 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_3).getFile());
	
	
	public static void main(String[] args) throws JDOMException, IOException, FormatException {
		
		TrackVisualizerTestDrive tdtd = new TrackVisualizerTestDrive();
		tdtd.init(args);
		tdtd.setVisible(true);
		
	}
		
	public void init(String[] args) throws JDOMException, IOException, FormatException {

		TmXmlReader reader = new TmXmlReader(CASE_3);
		reader.parse();
		
		// Load objects 
		TreeMap<Integer, List<Spot>> allSpots 		= reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots 	= reader.getSpotSelection(allSpots);
		SimpleGraph<Spot, DefaultEdge> tracks = reader.getTracks(selectedSpots);
		ImagePlus imp = reader.getImage();
		
		// Stuff not saved yet
		float[] calibration = null; // new float[] {1, 1, 1};
		float radius = 15;
		
		// Launch ImageJ and display
		ij.ImageJ.main(args);
		imp.show();
		SpotDisplayer2D displayer = new SpotDisplayer2D(imp, radius, calibration);
		displayer.setSpots(selectedSpots);
		displayer.render();
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(tracks);
		displayer.refresh();
		
		JGraphModelAdapter<Spot, DefaultEdge> jgAdapter = new JGraphModelAdapter<Spot, DefaultEdge>(
				tracks,
				JGraphModelAdapter.createDefaultVertexAttributes(), 
				JGraphModelAdapter.createDefaultEdgeAttributes(tracks),
				new CellFactory<Spot, DefaultEdge>() {

					@Override
					public org.jgraph.graph.DefaultEdge createEdgeCell(DefaultEdge e) {
						return new org.jgraph.graph.DefaultEdge();
					}

					@Override
					public DefaultGraphCell createVertexCell(Spot s) {
						return new SpotCell(s);
					}
				});
		
		
		SpotCellViewFactory factory = new SpotCellViewFactory(imp, radius, calibration);
		
		GraphLayoutCache graphLayout = new GraphLayoutCache(jgAdapter, factory);
		JGraph jgraph = new JGraph(jgAdapter, graphLayout);

		JScrollPane scrollPane = new JScrollPane(jgraph);
		
        getContentPane().add(scrollPane);
        setSize( DEFAULT_SIZE );
        
        JGraphFacade facade = new JGraphFacade(jgraph);
        JGraphLayout layout = new JGraphTimeLayout(tracks, jgAdapter);
        
        layout.run(facade);
        Map nested = facade.createNestedMap(false, false); // Obtain a map of the resulting attribute changes from the facade 
        jgraph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph 
	}
}
