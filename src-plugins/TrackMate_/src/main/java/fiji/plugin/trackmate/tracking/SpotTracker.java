package fiji.plugin.trackmate.tracking;

import java.util.Map;

import net.imglib2.algorithm.OutputAlgorithm;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * <p>
 * A SpotTracker algorithm is simply expected to <b>create</b> a new {@link SimpleDirectedWeightedGraph}
 * from the spot collection help in the {@link TrackMateModel} that is given to it. We 
 * use a simple directed weighted graph:
 * <ul>
 * 	<li> Though the weights themselves are not used for subsequent steps, it is 
 * suggested to use edge weight to report the cost of a link.
 * 	<li> The link direction serves to indicate forward in time, so that the source of a link
 * is always at an earlier frame than its target *but there might be several frames between them).
 * This is important, as this property will be used elsewhere.
 * 	<li> Several links between two spots are not permitted.
 * 	<li> A link with the same spot for source and target is not allowed.  
 * </ul>
 */
public interface SpotTracker extends  OutputAlgorithm<SimpleDirectedWeightedGraph<Spot, DefaultWeightedEdge>> {

	/** @return a unique String identifier for this tracker. */
	public String getKey();
	
	/** Set the settings map to use with this tracker. */
	public void setSettings(final Map<String, Object> settings);
}
