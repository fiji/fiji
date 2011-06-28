package fiji.plugin.trackmate.tracking;

import mpicbg.imglib.algorithm.Algorithm;
import fiji.plugin.trackmate.Logger;
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
	
	
}
