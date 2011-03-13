package fiji.plugin.trackmate.visualization.trackscheme;
 
import java.io.Serializable;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotDisplayer;

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

		public TrackEdgeCell(final DefaultWeightedEdge edge) {
			super(String.format("%.1f", jGraphT.getEdgeWeight(edge)), new mxGeometry(), "");
			String style = "startArrow=none;endArrow=none;strokeWidth=2;strokeColor="+Integer.toHexString(SpotDisplayer.DEFAULT_COLOR.getRGB());
			setStyle(style);
			this.edge = edge;
			setEdge(true);
			setId(null);
			getGeometry().setRelative(true);
		}
		
		public DefaultWeightedEdge getEdge() {
			return edge;
		}
	}
	
	public class SpotCell extends mxCell {
		
		private static final long serialVersionUID = 1L;
		private Spot spot;
		
		public SpotCell(Spot spot) {
			super((spot.getName() == null || spot.getName().equals("")) ? "ID "+spot.ID() : spot.getName(), new mxGeometry(), null);
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
