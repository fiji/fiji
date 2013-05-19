package fiji.plugin.trackmate.graph;

import java.util.Set;

import org.jgrapht.alg.DirectedNeighborIndex;
import org.jgrapht.graph.SimpleDirectedGraph;

public class RecursiveCumSum<V, E> {

	private final DirectedNeighborIndex<V, E> cache;
	private final Function2<V, V> function;

	public RecursiveCumSum(final SimpleDirectedGraph<V, E> graph, final Function2<V, V> function) {
		this.cache = new DirectedNeighborIndex<V, E>(graph);
		this.function = function;
	}
	
	public V apply(V current) {
		
		Set<V> children = cache.successorsOf(current);
		
		if (children.size() == 0) {
			// It is a leaf
			return current;
		} else {
			
			V val = current;
			for (V child : children) {
				function.compute(val, apply(child), val);
			}
			return val;
			
		}
		
	}

}
