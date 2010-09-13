package fiji.plugin.spottracker.tracking;

import mpicbg.imglib.algorithm.Algorithm;

/**
 * This interface should be used when creating algorithms for linking objects across
 * multiple frames in time-lapse microscopy.
 * 
 * @author Nicholas Perry
 *
 */
public interface ObjectTracker extends Algorithm {

	/** Get the final tracks computed by the tracking algorithm. */
	public void getTracks();

	public static class Settings {
		/** To throw out spurious segments, only include track segments with a length strictly larger
		 * than this value. */
		public static final int MINIMUM_SEGMENT_LENGTH = 3; 
		/** The maximum distance away two Spots in consecutive frames can be in order 
		 * to be linked. */
		public static final double MAX_DIST_OBJECTS = 15.0d;	// TODO make user input
		/** The factor used to create d and b in the paper, the alternative costs to linking
		 * objects. */
		public static final double ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;	// TODO make user input
		/** The maximum distance away two Segments can be in order 
		 * to be linked. */
		public static final double MAX_DIST_SEGMENTS = 15.0d;	// TODO make user input
		/** The maximum number of frames apart two segments can be 'gap closed.' */
		public static final int GAP_CLOSING_TIME_WINDOW = 2; // TODO make user input.
		/** The minimum and maximum allowable intensity ratio cutoffs for merging and splitting. */
		public static final double[] INTENSITY_RATIO_CUTOFFS = new double[] {0.5d, 4d}; // TODO make user input.
		/** The percentile used to calculate d and b cutoffs in the paper. */
		public static final double CUTOFF_PERCENTILE = 0.9d;	// TODO make user input.
		

	}
	
}
