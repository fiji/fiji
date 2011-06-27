package fiji.plugin.trackmate.tracking;

import mpicbg.imglib.algorithm.Algorithm;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackCollection;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
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
	 * Returns the final tracks computed, as a track collection.
	 */
	public TrackCollection getTracks();
	
}
