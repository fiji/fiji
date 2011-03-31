package fiji.plugin.trackmate.tracking;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.InfoTextable;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import mpicbg.imglib.algorithm.Algorithm;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * 
 * @author Nicholas Perry
 *
 */
public interface SpotTracker extends Algorithm, InfoTextable {

	
	/**
	 * Set the spot collection to link. Not setting this field will generate an error 
	 */
	public void setSpots(SpotCollection spots);
	
	/**
	 * Set the settings that control the behavior of this tracker.
	 */
	public void setSettings(TrackerSettings settings);

	/**
	 * Create a new default {@link TrackerSettings} object suited 
	 * to this tracker.
	 */
	public TrackerSettings createSettings();
	
	/**
	 * Returns the final tracks computed, as a directed Graph of spots.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getTrackGraph();

	/**
	 * Set the logger used to echo log messages.
	 */
	void setLogger(Logger logger);
	
	
	/**
	 * Return the name of the tracker
	 * @return
	 */
	@Override
	public String toString();
}
