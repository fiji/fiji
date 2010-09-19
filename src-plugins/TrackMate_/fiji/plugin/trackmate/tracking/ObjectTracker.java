package fiji.plugin.trackmate.tracking;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Spot;
import mpicbg.imglib.algorithm.Algorithm;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * 
 * @author Nicholas Perry
 *
 */
public interface ObjectTracker extends Algorithm {

	/**
	 * Returns the final tracks computed, as a directed Graph of spots.
	 */
	public SimpleGraph<Spot, DefaultEdge> getTrackGraph();
	
}
