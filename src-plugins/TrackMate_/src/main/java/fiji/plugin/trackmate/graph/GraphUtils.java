package fiji.plugin.trackmate.graph;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackGraphModel;

public class GraphUtils {


	/**
	 * @return a new graph built with the same structure that of the source model, where every
	 * vertex is an int[] of size 1, with as element 
	 * :
	 * <ul>
	 * 	<li> 1 if the vertex is a leaf (no successor)
	 * 	<li> 0 otherwise
	 * </ul>
	 */
	public static SimpleDirectedWeightedGraph<int[], DefaultWeightedEdge> leafTree(final TrackGraphModel model) {
		
		final DirectedNeighborIndex<Spot, DefaultWeightedEdge> cache = model.getDirectedNeighborIndex();
		SpotFunction<int[]> function = new SpotFunction<int[]>() {
			@Override
			public int[] apply(Spot spot) {
				if (cache.successorsOf(spot).size() == 0) {
					return new int[] { 1 };
				} else {
					return new int[] { 0 };
				}
			}
		};

		SimpleDirectedWeightedGraph<int[], DefaultWeightedEdge> leafTree = model.apply(function);
		return leafTree;
		
	}
	
}
