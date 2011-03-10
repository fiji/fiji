package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.HashMap;

import javax.swing.JFrame;

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
	
	private ListenableGraph<V, E> jgtGraph;
	private HashMap<V, mxCell> vertexMap 	= new HashMap<V, mxCell>();
	private HashMap<E, mxCell> edgeMap 		= new HashMap<E, mxCell>();
	private CellFactory<V, E> cellFactory;

	private static final int DEFAULT_WIDTH = 80;
	private static final int DEFAULT_HEIGHT = 30;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public JGraphXAdapter(final ListenableGraph<V, E> jgtGraph, CellFactory<V, E> factory) {
		super();
		this.jgtGraph = jgtGraph;
		this.cellFactory 		= factory;
		
		jgtGraph.addGraphListener(this);
		
		getModel().beginUpdate();
		try {
			for (V vertex : jgtGraph.vertexSet())
				addJGraphTVertex(vertex);
			for (E edge : jgtGraph.edgeSet())
				addJGraphTEdge(edge);
		} finally {
			getModel().endUpdate();
		}
	}
	
	/*
	 * METHODS
	 */
	
	public void addJGraphTVertex(V vertex) {
		getModel().beginUpdate();
		try {
			mxCell cell = cellFactory.createVertexCell(vertex);
			addCell(cell, defaultParent);
			vertexMap.put(vertex, cell);
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void addJGraphTEdge(E edge) {
		getModel().beginUpdate();
		try {
			V source = jgtGraph.getEdgeSource(edge);
			V target = jgtGraph.getEdgeTarget(edge);				
			mxCell cell = cellFactory.createEdgeCell(edge);
			addEdge(cell, defaultParent, vertexMap.get(source),  vertexMap.get(target), null);
			edgeMap.put(edge, cell);
		} finally {
			getModel().endUpdate();
		}
	}
	
	public void setCellFactory(CellFactory<V, E> factory) {
		this.cellFactory = factory;
	}
	
	public mxCell getCellForVertex(V vertex) {
		return vertexMap.get(vertex);
	}
	
	public mxCell getCellForEdge(E edge) {
		return edgeMap.get(edge);
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
		mxCell cell = vertexMap.get(e.getVertex());
		removeCells(new Object[] { cell } );
	}

	@Override
	public void edgeAdded(GraphEdgeChangeEvent<V, E> e) {
		addJGraphTEdge(e.getEdge());
	}

	@Override
	public void edgeRemoved(GraphEdgeChangeEvent<V, E> e) {
		mxCell cell = edgeMap.get(e.getEdge());
		removeCells(new Object[] { cell } );
	}
	
	/*
	 * INNER CLASSES	
	 */
	
	public static interface CellFactory<VV, EE> {
		
		 /**
         * Creates an edge cell that contains its respective JGraphT edge.
         *
         * @param jGraphTEdge a JGraphT edge to be contained.
         *
         * @return an edge cell that contains its respective JGraphT edge.
         */
        public mxCell createEdgeCell(EE jGraphTEdge);

        /**
         * Creates a vertex cell that contains its respective JGraphT vertex.
         *
         * @param jGraphTVertex a JGraphT vertex to be contained.
         *
         * @return a vertex cell that contains its respective JGraphT vertex.
         */
        public mxCell createVertexCell(VV jGraphTVertex);
	}
	
	
	public static class DefaultCellFactory<VV, EE> implements CellFactory<VV, EE> {

		private GeometryFactory geometryFactory;

		public  DefaultCellFactory(GeometryFactory geometryFactory) {
			this.geometryFactory = geometryFactory;
		}
		
		@Override
		public mxCell createEdgeCell(EE jGraphTEdge) {
			mxCell cell = new mxCell(jGraphTEdge, new mxGeometry(), null);
			cell.setId(jGraphTEdge.toString());
			cell.setEdge(true);
			cell.getGeometry().setRelative(true);
			return cell;
		}

		@Override
		public mxCell createVertexCell(VV vertex) {
			mxGeometry geometry = geometryFactory.nextGeometry();
			mxCell cell = new mxCell(vertex, geometry, null);
			cell.setId(vertex.toString());
			cell.setVertex(true);
			cell.setConnectable(true);
			return cell;
		}
		
	}
	
	public static class GeometryFactory {
		
		private double x = -  2 * DEFAULT_WIDTH;
		private double y = 0;
		
		public mxGeometry nextGeometry() {
			x += 2 * DEFAULT_WIDTH;
			if (x > 400) {
				y += 2 * DEFAULT_HEIGHT;
				x = 0;
			}
			return new mxGeometry(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
		}
		
	}
	

	
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
        
        GeometryFactory gf = new GeometryFactory();
        JGraphXAdapter<String, DefaultEdge> graph = new JGraphXAdapter<String, DefaultEdge>(g, new DefaultCellFactory<String, DefaultEdge>(gf));
        
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


	}


	

	
}