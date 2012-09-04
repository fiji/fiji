package fiji.plugin.trackmate.tracking;

import net.imglib2.algorithm.OutputAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * <p>
 * A SpotTracker algorithm is simply expected to <b>create</b> a new {@link SimpleWeightedGraph}
 * from the spot collection help in the {@link TrackMateModel} that is given to it. We 
 * use a weighted graph, though the weights themselves are not used for subsequent steps. It is 
 * suggested to use edge weight to report the cost of a link. 
 */
public interface SpotTracker extends  OutputAlgorithm<SimpleWeightedGraph<Spot, DefaultWeightedEdge>> {

	/** @return a unique String identifier for this tracker. */
	public String getKey();
}
