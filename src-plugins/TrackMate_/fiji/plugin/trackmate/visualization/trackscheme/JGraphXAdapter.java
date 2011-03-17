package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.HashMap;

import javax.swing.JFrame;

import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class JGraphXAdapter<V, E> extends mxGraph implements GraphListener<V, E> {
	
	private ListenableGraph<V, E> graphT;
	private HashMap<V, mxCell> 	vertexToCellMap 	= new HashMap<V, mxCell>();
	private HashMap<E, mxCell> 	edgeToCellMap 		= new HashMap<E, mxCell>();
	private HashMap<mxCell, V>	cellToVertexMap		= new HashMap<mxCell, V>();
	private HashMap<mxCell, E>	cellToEdgeMap		= new HashMap<mxCell, E>();
	
	/*
	 * CONSTRUCTOR
	 */
	
	public JGraphXAdapter(final ListenableGraph<V, E> graphT) {
		super();
		this.graphT = graphT;
		
		graphT.addGraphListener(this);
		
		insertJGraphT(graphT);
		
	}
	
	/*
	 * METHODS
	 */
	
	public void addJGraphTVertex(V vertex) {
		getModel().beginUpdate();
		try {
			mxCell cell = new mxCell(vertex);
			cell.setVertex(true);
			cell.setId(null);
			addCell(cell, defaultParent);
			vertexToCellMap.put(vertex, cell);
			cellToVertexMap.put(cell, vertex);
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void addJGraphTEdge(E edge) {
		getModel().beginUpdate();
		try {
			V source = graphT.getEdgeSource(edge);
			V target = graphT.getEdgeTarget(edge);				
			mxCell cell = new mxCell(edge);
			cell.setEdge(true);
			cell.setId(null);
			cell.setGeometry(new mxGeometry());
			cell.getGeometry().setRelative(true);
			addEdge(cell, defaultParent, vertexToCellMap.get(source),  vertexToCellMap.get(target), null);
			edgeToCellMap.put(edge, cell);
			cellToEdgeMap.put(cell, edge);
		} finally {
			getModel().endUpdate();
		}
	}
		
	public HashMap<V, mxCell> getVertexToCellMap() {
		return vertexToCellMap;
	}
	
	public HashMap<E, mxCell> getEdgeToCellMap() {
		return edgeToCellMap;
	}
	
	public HashMap<mxCell, E> getCellToEdgeMap() {
		return cellToEdgeMap;
	}

	public HashMap<mxCell, V> getCellToVertexMap() {
		return cellToVertexMap;
	}
	
	/*
	 * GRAPH LISTENER
	 */
	
	
	@Override
	public void vertexAdded(GraphVertexChangeEvent<V> e) {
		addJGraphTVertex(e.getVertex());
	}

	@Override
	public void vertexRemoved(GraphVertexChangeEvent<V> e) {
		mxCell cell = vertexToCellMap.remove(e.getVertex());
		removeCells(new Object[] { cell } );
	}

	@Override
	public void edgeAdded(GraphEdgeChangeEvent<V, E> e) {
		addJGraphTEdge(e.getEdge());
	}

	@Override
	public void edgeRemoved(GraphEdgeChangeEvent<V, E> e) {
		mxCell cell = edgeToCellMap.remove(e.getEdge());
		removeCells(new Object[] { cell } );
	}
	
	
	/*
	 * PRIVATE METHODS
	 */
	

	private void insertJGraphT(Graph<V, E> graphT) {		
		getModel().beginUpdate();
		try {
			for (V vertex : graphT.vertexSet())
				addJGraphTVertex(vertex);
			for (E edge : graphT.edgeSet())
				addJGraphTEdge(edge);
		} finally {
			getModel().endUpdate();
		}
		
		
		
	}
	
	/*
	 * MAIN METHOD
	 */
	
	
	public static void main(String[] args) {
		// create a JGraphT graph
        ListenableGraph<String, DefaultEdge> g = new ListenableDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        // add some sample data (graph manipulated via JGraphT)
        g.addVertex( "v1" );
        g.addVertex( "v2" );
        g.addVertex( "v3" );
        g.addVertex( "v4" );

        g.addEdge( "v1", "v2" );
        g.addEdge( "v2", "v3" );
        g.addEdge( "v3", "v1" );
        g.addEdge( "v4", "v3" );
        
        JGraphXAdapter<String, DefaultEdge> graph = new JGraphXAdapter<String, DefaultEdge>(g);
        
        
        JFrame frame = new JFrame();
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
		frame.getContentPane().add(graphComponent);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 320);
		frame.setVisible(true);
		
        g.addVertex( "v5" );
        g.addVertex( "v6" );
        g.addVertex( "v7" );
        g.addVertex( "v8" );
        
        graph.getModel().beginUpdate();
        double x = 20, y = 20;
        for (mxCell cell : graph.getVertexToCellMap().values()) {
        	graph.getModel().setGeometry(cell, new mxGeometry(x, y, 20, 20));
        	x += 40;
        	if (x > 200) {
        		x = 20;
        		y += 40;
        	}
        }
        graph.getModel().endUpdate();


	}



	

	
}