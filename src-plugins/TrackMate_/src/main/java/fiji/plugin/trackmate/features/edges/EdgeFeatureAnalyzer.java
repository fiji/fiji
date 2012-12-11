package fiji.plugin.trackmate.features.edges;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.features.FeatureModel;

public interface EdgeFeatureAnalyzer {
	
	/**
	 * Score a particular link between two spots. 
	 * The results must be stored in the {@link FeatureModel}.
	 */
	public void process(final DefaultWeightedEdge edge);

}
