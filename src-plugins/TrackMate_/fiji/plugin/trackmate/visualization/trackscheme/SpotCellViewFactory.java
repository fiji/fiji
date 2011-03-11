package fiji.plugin.trackmate.visualization.trackscheme;

import java.io.Serializable;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

import edu.mines.jtk.mesh.Geometry;
import fiji.plugin.trackmate.Spot;


public class SpotCellViewFactory implements JGraphXAdapter.CellFactory<Spot, DefaultWeightedEdge>, Serializable {

	private static final long serialVersionUID = 762434565251709869L;
	private Graph<Spot, DefaultWeightedEdge> jGraphT;

	/*
	 * CONSTRUCTORS
	 */

	public SpotCellViewFactory(final Graph<Spot, DefaultWeightedEdge> jGraphT) {
		this.jGraphT = jGraphT;
	}

	
	/*
	 * METHODS
	 */
	
	@Override
	public mxCell createEdgeCell(DefaultWeightedEdge edge) {
		return new TrackEdgeCell(edge);
	}


	@Override
	public mxCell createVertexCell(Spot spot) {
		return new SpotCell(spot);
	}
	



	public class TrackEdgeCell extends mxCell {

		private static final long serialVersionUID = 2793596686481829376L;
		private DefaultWeightedEdge edge;
		private String label;

		public TrackEdgeCell(final DefaultWeightedEdge edge) {
			super(String.format("%.1f", jGraphT.getEdgeWeight(edge)), new mxGeometry(), "edge");
			this.edge = edge;
			this.label = String.format("%.1f", jGraphT.getEdgeWeight(edge));
			setEdge(true);
			setId(null);
			getGeometry().setRelative(true);
		}
		
		public DefaultWeightedEdge getEdge() {
			return edge;
		}
		
		@Override
		public String toString() {
			return label;
		}
		
	}
	
	public class SpotCell extends mxCell {
		
		private static final long serialVersionUID = 1L;
		private Spot spot;
		
		public SpotCell(Spot spot) {
			super( (spot.getName() == null || spot.getName().equals("")) ? "ID "+spot.ID() : spot.getName(), new mxGeometry(), "spot");
			this.spot = spot;
			setVertex(true);
			setId(null);
			setConnectable(true);
		}
		
		public Spot getSpot() {
			return spot;
		}
		
		@Override
		public String toString() {
			return spot.getName();
		}
	}



}
