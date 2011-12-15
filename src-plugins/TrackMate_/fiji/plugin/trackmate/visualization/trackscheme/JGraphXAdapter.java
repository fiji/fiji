package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.HashMap;
import java.util.Set;

import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class JGraphXAdapter extends mxGraph implements GraphListener<Spot, DefaultWeightedEdge> {

	private HashMap<Spot, mxCell> 					vertexToCellMap 	= new HashMap<Spot, mxCell>();
	private HashMap<DefaultWeightedEdge, mxICell> 	edgeToCellMap 		= new HashMap<DefaultWeightedEdge, mxICell>();
	private HashMap<mxICell, Spot>					cellToVertexMap		= new HashMap<mxICell, Spot>();
	private HashMap<mxICell, DefaultWeightedEdge>	cellToEdgeMap		= new HashMap<mxICell, DefaultWeightedEdge>();
	private TrackMateModel tmm;

	/*
	 * CONSTRUCTOR
	 */

	public JGraphXAdapter(final TrackMateModel tmm) {
		super();
		this.tmm = tmm;
		insertTrackCollection(tmm);
	}

	/*
	 * METHODS
	 */
	
	/**
	 * Overridden method so that when a label is changed, we change the target spot's name.
	 */
	@Override
	public void cellLabelChanged(Object cell, Object value, boolean autoSize) {
		model.beginUpdate();
		try {
			Spot spot = cellToVertexMap.get(cell);
			if (null == spot)
				return;
			String str = (String) value;
			spot.setName(str);
			getModel().setValue(cell, str);

			if (autoSize) {
				cellSizeUpdated(cell, false);
			}
		} finally {
			model.endUpdate();
		}
	}

	public mxCell addJGraphTVertex(Spot vertex) {
		if (vertexToCellMap.containsKey(vertex)) {
			// cell for Spot already existed, skip creation and return original cell.
			return vertexToCellMap.get(vertex);
		}
		mxCell cell = null;
		getModel().beginUpdate();
		try {
			cell = new mxCell(vertex, new mxGeometry(), "");
			cell.setVertex(true);
			cell.setId(null);
			cell.setValue(vertex.getName());
			addCell(cell, defaultParent);
			vertexToCellMap.put(vertex, cell);
			cellToVertexMap.put(cell, vertex);
		} finally {
			getModel().endUpdate();
		}
		return cell;
	}

	public mxICell addJGraphTEdge(DefaultWeightedEdge edge) {
		if (edgeToCellMap.containsKey(edge)) {
			// cell for edge already existed, skip creation and return original cell.
			return edgeToCellMap.get(edge);
		}
		mxCell cell = null;
		getModel().beginUpdate();
		try {
			Spot source = tmm.getEdgeSource(edge);
			Spot target = tmm.getEdgeTarget(edge);				
			cell = new mxCell(edge);
			cell.setEdge(true);
			cell.setId(null);
			cell.setValue(String.format("%.1f", tmm.getEdgeWeight(edge)));
			cell.setGeometry(new mxGeometry());
			cell.getGeometry().setRelative(true);
			addEdge(cell, defaultParent, vertexToCellMap.get(source),  vertexToCellMap.get(target), null);
			edgeToCellMap.put(edge, cell);
			cellToEdgeMap.put(cell, edge);
		} finally {
			getModel().endUpdate();
		}
		return cell;
	}
	
	public void mapEdgeToCell(DefaultWeightedEdge edge, mxICell cell) {
		cellToEdgeMap.put(cell, edge);
		edgeToCellMap.put(edge, cell);
	}
	
	public Spot getSpotFor(mxICell cell) {
		return cellToVertexMap.get(cell);
	}
	
	public DefaultWeightedEdge getEdgeFor(mxICell cell) {
		return cellToEdgeMap.get(cell);
	}
	
	public mxICell getCellFor(Spot spot) {
		return vertexToCellMap.get(spot);
	}
	
	public mxICell getCellFor(DefaultWeightedEdge edge) {
		return edgeToCellMap.get(edge);
	}
	
	public Set<mxICell> getVertexCells() {
		return cellToVertexMap.keySet();
	}
	
	public void removeMapping(Spot spot) {
		mxICell cell = vertexToCellMap.remove(spot);
		cellToVertexMap.remove(cell);
	}
	
	public void removeMapping(DefaultWeightedEdge edge) {
		mxICell cell = edgeToCellMap.remove(edge);
		cellToEdgeMap.remove(cell);
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
		mxICell cell = edgeToCellMap.remove(e.getEdge());
		removeCells(new Object[] { cell } );
	}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Only insert spot and edges belonging to visible tracks. 
	 * Any other spot or edges will be ignored by the whole trackscheme
	 * framework, and if they are needed, they will have to be imported "by hand".
	 */
	private void insertTrackCollection(final TrackMateModel tmm) {		
		model.beginUpdate();
		try {
			for (int i : tmm.getVisibleTrackIndices()) {
				for (Spot vertex : tmm.getTrackSpots(i)) {
					addJGraphTVertex(vertex);
				}
				for (DefaultWeightedEdge edge : tmm.getTrackEdges(i)) {
					addJGraphTEdge(edge);
				}
			}
		} finally {
			model.endUpdate();
		}
	}



}