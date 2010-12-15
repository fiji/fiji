package fiji.plugin.trackmate.visualization.test;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.jdom.JDOMException;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.JGraphLayout;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.io.TmXmlReader;

public class TrackVisualizerTestDrive extends JFrame {

	private static final long serialVersionUID = 1L;
	private static final String 	FILE_NAME_1 = "Celegans-5pc_17timepoints.xml";
	private static final File 		SPLITTING_CASE_1 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_1).getFile());
	private static final String 	FILE_NAME_2 = "FakeTracks.xml";
	private static final File 		SPLITTING_CASE_2 = new File(TrackVisualizerTestDrive.class.getResource(FILE_NAME_2).getFile());
	private static final Dimension 	DEFAULT_SIZE = new Dimension( 530, 320 );

	
	
	public static void main(String[] args) throws JDOMException, IOException {
		
		TrackVisualizerTestDrive tdtd = new TrackVisualizerTestDrive();
		tdtd.init();
		tdtd.setVisible(true);
		
	}


	private JGraphModelAdapter<Spot, DefaultEdge> jgAdapter;		
		
	public void init() throws JDOMException, IOException {

		TmXmlReader reader = new TmXmlReader(SPLITTING_CASE_2);
		reader.parse();
		
		// Load objects 
		TreeMap<Integer, List<Spot>> allSpots 		= reader.getAllSpots();
		TreeMap<Integer, List<Spot>> selectedSpots 	= reader.getSpotSelection(allSpots);
		SimpleGraph<Spot, DefaultEdge> tracks = reader.getTracks(selectedSpots);
		
		
		
//		ListenableUndirectedGraph<Spot, DefaultEdge> g = new ListenableUndirectedGraph<Spot, DefaultEdge>(tracks);
		
		jgAdapter = new JGraphModelAdapter<Spot, DefaultEdge>(
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
						return new DefaultGraphCell(s) {
							private static final long serialVersionUID = 1L;
							@Override
							public String toString() {
								Spot s = (Spot) userObject;
								return "t = "+s.getFeature(Feature.POSITION_T);
							}
						};
					}
				});
		JGraph jgraph = new JGraph(jgAdapter);
		JScrollPane scrollPane = new JScrollPane(jgraph);
		
		
        getContentPane().add(scrollPane);
        setSize( DEFAULT_SIZE );
        


        Object[] roots = selectedSpots.get(selectedSpots.keySet().iterator().next()).toArray();
        JGraphFacade facade = new JGraphFacade(jgraph, roots);
        JGraphLayout layout = new JGraphHierarchicalLayout();
        layout.run(facade); // Run the layout on the facade. Note that layouts do not implement the Runnable interface, to avoid confusion 
        Map nested = facade.createNestedMap(false, false); // Obtain a map of the resulting attribute changes from the facade 
        jgraph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph 



		
	}
	
	
}
