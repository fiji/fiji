package fiji.plugin.trackmate.tracking;

import java.util.Map;

import net.imglib2.algorithm.OutputAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * <p>
 * A SpotTracker algorithm is simply expected to <b>create</b> a new {@link SimpleWeightedGraph}
 * from the spot collection help in the {@link Model} that is given to it. We
 * use a simple weighted graph:
 * <ul>
 * 	<li> Though the weights themselves are not used for subsequent steps, it is
 * suggested to use edge weight to report the cost of a link.
 * 	<li> The graph is undirected, however, some link direction can be retrieved later on
 * using the {@link Spot#FRAME} feature. The {@link SpotTracker} implementation
 * does not have to deal with this; only undirected edges are created.
 * 	<li> Several links between two spots are not permitted.
 * 	<li> A link with the same spot for source and target is not allowed.
 * 	<li> A link with the source spot and the target spot in the same frame
 * is not allowed. This must be enforced by implementations.
 * </ul>
 */
public interface SpotTracker extends  OutputAlgorithm<SimpleWeightedGraph<Spot, DefaultWeightedEdge>> {

	/** @return a unique String identifier for this tracker. */
	public String getKey();

	/**
	 * Set the spot collection and the settings map to use with this tracker.
	 * In the spot collection, only visible spots are considered for tracking.
	 */
	public void setTarget(final SpotCollection spots, final Map<String, Object> settings);
}
