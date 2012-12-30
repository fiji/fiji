package fiji.plugin.trackmate.features.edges;

import java.util.Collection;

import net.imglib2.algorithm.Benchmark;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.FeatureModel;

public interface EdgeFeatureAnalyzer extends Benchmark {

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

}
