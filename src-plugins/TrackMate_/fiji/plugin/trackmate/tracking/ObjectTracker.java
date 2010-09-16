package fiji.plugin.trackmate.tracking;

import mpicbg.imglib.algorithm.Algorithm;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse images.
 * 
 * @author Nicholas Perry
 *
 */
public interface ObjectTracker extends Algorithm {

	/** Get the final tracks computed by the tracking algorithm. */
	public void getTracks();
	
}
