package fiji.plugin.trackmate.features.edges;

import java.util.Collection;

import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;

public interface EdgeAnalyzer extends Benchmark {

	/**
	 * Score a collection of link between two spots. 
	 * The results must be stored in the {@link FeatureModel}.
	 * <p>
	 * Note: ideally concrete implementation should work in a multi-threaded fashion
	 * for performance reason, when possible.
	 * 
	 * @author Jean-Yves Tinevez
	 */
	public void process(final Collection<DefaultWeightedEdge> edges);
	
	/**
	 * @return <code>true</code> if this analyzer is a local analyzer. That is: a modification that
	 * affects only one edge requires the edge features to be re-calculated only for
	 * this edge. If <code>false</code>, any model modification involving an edge will trigger
	 * a recalculation over the whole track this edge belong to.
	 */
	public boolean isLocal();

}
