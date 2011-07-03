package fiji.plugin.trackmate.tracking;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import mpicbg.imglib.algorithm.Algorithm;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * <p>
 * A SpotTracker algorithm is simply expected to update the {@link TrackMateModel} it is 
 * given at creation time using its methods to add edges. 
 * 
 * @author Nicholas Perry
 *
 */
public interface SpotTracker extends Algorithm {
	
	/**
	 * Set the logger used to echo log messages.
	 */
	void setLogger(Logger logger);
	
	/**
	 * Return the graph containing the link resulting from the process of this tracker.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getResult();
	
}
