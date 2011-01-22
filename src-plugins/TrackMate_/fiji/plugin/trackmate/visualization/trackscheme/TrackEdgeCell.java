package fiji.plugin.trackmate.visualization.trackscheme;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;


public class TrackEdgeCell  extends DefaultEdge {

	private static final long serialVersionUID = 2793596686481829376L;
	private DefaultWeightedEdge edge;
	private String label;

	public TrackEdgeCell(final DefaultWeightedEdge edge, final Graph<Spot, DefaultWeightedEdge> graph) {
		super(edge);
		this.edge = edge;
		this.label = String.format("%.1f", graph.getEdgeWeight(edge));
	}
	
	public DefaultWeightedEdge getEdge() {
		return edge;
	}
	
	@Override
	public String toString() {
		return label;
	}
	
}
