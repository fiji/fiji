package fiji.plugin.spottracker.tracking;

import mpicbg.imglib.algorithm.Algorithm;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse microscopy.
 * 
 * @author nperry
 *
 */
public interface ObjectTracker extends Algorithm {

	public void getTracks();

}
