package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.HashMap;

import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.view.mxGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackCollection;

public class JGraphXAdapter extends mxGraph implements GraphListener<Spot, DefaultWeightedEdge> {

	private HashMap<Spot, mxCell> 					vertexToCellMap 	= new HashMap<Spot, mxCell>();
	private HashMap<DefaultWeightedEdge, mxCell> 	edgeToCellMap 		= new HashMap<DefaultWeightedEdge, mxCell>();
	private HashMap<mxCell, Spot>					cellToVertexMap		= new HashMap<mxCell, Spot>();
	private HashMap<mxCell, DefaultWeightedEdge>	cellToEdgeMap		= new HashMap<mxCell, DefaultWeightedEdge>();
	private TrackCollection tracks;

	/*
	 * CONSTRUCTOR
	 */

	public JGraphXAdapter(final TrackCollection tracks) {
		super();
		this.tracks = tracks;
		tracks.addGraphListener(this);
		insertTrackCollection(tracks);
	}

	/*
	 * METHODS
	 */

	public void addJGraphTVertex(Spot vertex) {
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

	public void addJGraphTEdge(DefaultWeightedEdge edge) {
		getModel().beginUpdate();
		try {
			Spot source = tracks.getEdgeSource(edge);
			Spot target = tracks.getEdgeTarget(edge);				
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

	public HashMap<Spot, mxCell> getVertexToCellMap() {
		return vertexToCellMap;
	}

	public HashMap<DefaultWeightedEdge, mxCell> getEdgeToCellMap() {
		return edgeToCellMap;
	}

	public HashMap<mxCell, DefaultWeightedEdge> getCellToEdgeMap() {
		return cellToEdgeMap;
	}

	public HashMap<mxCell, Spot> getCellToVertexMap() {
		return cellToVertexMap;
	}

	/*
	 * GRAPH LISTENER
	 */


	@Override
	public void vertexAdded(GraphVertexChangeEvent<Spot> e) {
		addJGraphTVertex(e.getVertex());
	}

	@Override
	public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
		mxCell cell = vertexToCellMap.remove(e.getVertex());
		removeCells(new Object[] { cell } );
	}

	@Override
	public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
		addJGraphTEdge(e.getEdge());
	}

	@Override
	public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
		mxCell cell = edgeToCellMap.remove(e.getEdge());
		removeCells(new Object[] { cell } );
	}


	/*
	 * PRIVATE METHODS
	 */


	private void insertTrackCollection(final TrackCollection tracks) {		
		getModel().beginUpdate();
		try {
			for (Spot vertex : tracks.vertexSet())
				addJGraphTVertex(vertex);
			for (DefaultWeightedEdge edge : tracks.edgeSet())
				addJGraphTEdge(edge);
		} finally {
			getModel().endUpdate();
		}



	}
}