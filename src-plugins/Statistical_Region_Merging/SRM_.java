import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;
import ij.gui.Roi;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import ij.plugin.filter.PlugInFilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
 * The Statistical Region Merging algorithm is described in
 *
 * R. Nock, F. Nielsen: Statistical Region Merging.
 * IEEE Trans. Pattern Anal. Mach. Intell. 26(11): 1452-1458 (2004)
 */

public class SRM_ implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		// TODO: implement 3D version, let user choose

		GenericDialog gd = new GenericDialog("SRM");
		gd.addNumericField("Q", Q, 2);
		gd.addCheckbox("showAverages", true);
		gd.showDialog();

		if (gd.wasCanceled())
			return;

		Q = (float)gd.getNextNumber();
		boolean showAverages = gd.getNextBoolean();
		srm2D(ip, showAverages).show();
	}

	final float g = 256; // number of different intensity values
	protected float Q = 25; //25; // complexity of the assumed distributions
	protected float delta;

	/*
	 * The predicate: is the difference of the averages of the two
	 * regions R and R' smaller than
	 *
	 * g sqrt(1/2Q (1/|R| + 1/|R'|) ln 2/delta)
	 *
	 * Instead of calculating the square root all the time, we calculate
	 * the factor g^2 / 2Q ln 2/delta, and compare
	 *
	 * (<R> - <R'>)^2 < factor (1/|R| + 1/|R'|)
	 *
	 * instead.
	 */
	protected float factor, logDelta;

	/*
	 * For performance reasons, these are held in w * h arrays
	 */
	float[] average;
	int[] count;
	int[] regionIndex; // if < 0, it is -1 - actual_regionIndex

	/*
	 * The statistical region merging wants to merge regions in a specific
	 * order: for all neighboring pixel pairs, in ascending order of
	 * intensity differences.
	 *
	 * In that order, it is tested if the regions these two pixels belong
	 * to (by construction, the regions must be distinct) should be merged.
	 *
	 * For efficiency, we do it by bucket sorting, because there are only
	 * g + 1 possible differences.
	 *
	 * After sorting, for each difference the pixel pair with the largest
	 * index is stored in neighborBuckets[difference], and every
	 * nextNeighbor[index] points to the pixel pair with the same
	 * difference and the next smaller index (or -1 if there is none).
	 *
	 * The pixel pairs are identified by
	 *
	 *     2 * (x + (w - 1) * y) + direction
	 *
	 * where direction = 0 means "right neighbor", and direction = 1 means
	 * " lower neighbor".  (We do not need "left" or "up", as the order
	 * within the pair is not important.)
	 *
	 * In n dimensions, it must be n * pixel_count, and "direction"
	 * specifies the Cartesian unit vector (axis) determining the neighbor.
	 */
	int[] nextNeighbor, neighborBucket;

	protected ImagePlus srm2D(ImageProcessor ip, boolean showAverages) {
		int w = ip.getWidth(), h = ip.getHeight();

		delta = 1f / (6 * w * h);
		/*
		 * This would be the non-relaxed formula:
		 *
		 * factor = g * g / 2 / Q * (float)Math.log(2 / delta);
		 *
		 * The paper claims that this is more prone to oversegmenting.
		 */
		factor = g * g / 2 / Q;
		logDelta = 2f * (float)Math.log(6 * w * h);

		byte[] pixel = (byte[])ip.getPixels();
		initializeRegions2D(pixel, ip.getWidth(), ip.getHeight());
		initializeNeighbors2D(pixel, w, h);
		mergeAllNeighbors2D(w);

		if (showAverages) {
			for (int i = 0; i < average.length; i++)
				average[i] = average[getRegionIndex(i)];
			ip = new FloatProcessor(w, h, average, null);
		}
		else {
			int regionCount = consolidateRegions();

			if (regionCount > 1<<8) {
				if (regionCount > 1<<16)
					IJ.showMessage("Found " + regionCount
						+ " regions, which does not fit"
						+ " in 16-bit.");
				short[] pixel16 = new short[w * h];
				for (int i = 0; i < pixel16.length; i++)
					pixel16[i] = (short)regionIndex[i];
				ip = new ShortProcessor(w, h, pixel16, null);
			}
			else {
				pixel = new byte[w * h];
				for (int i = 0; i < pixel.length; i++)
					pixel[i] = (byte)regionIndex[i];
				ip = new ByteProcessor(w, h, pixel, null);
			}
		}

		String title = image.getTitle() + " (SRM Q=" + Q + ")";
		return new ImagePlus(title, ip);
	}

	void initializeRegions2D(byte[] pixel, int w, int h) {
		average = new float[w * h];
		count = new int[w * h];
		regionIndex = new int[w * h];

		for (int i = 0; i < average.length; i++) {
			average[i] = pixel[i] & 0xff;
			count[i] = 1;
			regionIndex[i] = i;
		}
	}

	protected void addNeighborPair(int neighborIndex,
			byte[] pixel, int i1, int i2) {
		int difference = Math.abs((pixel[i1] & 0xff)
				- (pixel[i2] & 0xff));
		nextNeighbor[neighborIndex] = neighborBucket[difference];
		neighborBucket[difference] = neighborIndex;
	}

	void initializeNeighbors2D(byte[] pixel, int w, int h) {
		nextNeighbor = new int[2 * w * h];

		// bucket sort
		neighborBucket = new int[256];
		Arrays.fill(neighborBucket, -1);

		for (int j = h - 1; j >= 0; j--)
			for (int i = w - 1; i >= 0; i--) {
				int index = i + w * j;
				int neighborIndex = 2 * index;

				// vertical
				if (j < h - 1)
					addNeighborPair(neighborIndex + 1,
						pixel, index, index + w);

				// horizontal
				if (i < w - 1)
					addNeighborPair(neighborIndex,
						pixel, index, index + 1);
			}
	}

	// recursively find out the region index for this pixel
	int getRegionIndex(int i) {
		i = regionIndex[i];
		while (i < 0)
			i = regionIndex[-1 - i];
		return i;
	}

	// should regions i1 and i2 be merged?
	boolean predicate(int i1, int i2) {
		float difference = average[i1] - average[i2];
		/*
		 * This would be the non-relaxed predicate mentioned in the
		 * paper.
		 *
		 * return difference * difference <
			factor * (1f / count[i1] + 1f / count[i2]);
		 *
		 */
		float log1 = (float)Math.log(1 + count[i1])
			* (g < count[i1] ? g : count[i1]);
		float log2 = (float)Math.log(1 + count[i2])
			* (g < count[i2] ? g : count[i2]);
		return difference * difference <
			.1f * factor * ((log1 + logDelta) / count[i1]
				+ ((log2 + logDelta) / count[i2]));
	}

	void mergeAllNeighbors2D(int w) {
		for (int i = 0; i < neighborBucket.length; i++) {
			int neighborIndex = neighborBucket[i];
			while (neighborIndex >= 0) {
				int i1 = neighborIndex / 2;
				int i2 = i1
					+ (0 == (neighborIndex & 1) ? 1 : w);

				i1 = getRegionIndex(i1);
				i2 = getRegionIndex(i2);

				if (predicate(i1, i2))
					mergeRegions(i1, i2);

				neighborIndex = nextNeighbor[neighborIndex];
			}
		}
	}

	void mergeRegions(int i1, int i2) {
		if (i1 == i2)
			return;
		int mergedCount = count[i1] + count[i2];
		float mergedAverage = (average[i1] * count[i1]
				+ average[i2] * count[i2]) / mergedCount;

		// merge larger index into smaller index
		if (i1 > i2) {
			average[i2] = mergedAverage;
			count[i2] = mergedCount;
			regionIndex[i1] = -1 - i2;
		}
		else {
			average[i1] = mergedAverage;
			count[i1] = mergedCount;
			regionIndex[i2] = -1 - i1;
		}
	}

	int consolidateRegions() {
		/*
		 * By construction, a negative regionIndex will always point
		 * to a smaller regionIndex.
		 *
		 * So we can get away by iterating from small to large and
		 * replacing the positive ones with running numbers, and the
		 * negative ones by the ones they are pointing to (that are
		 * now guaranteed to contain a non-negative index).
		 */
		int count = 0;
		for (int i = 0; i < regionIndex.length; i++)
			if (regionIndex[i] < 0)
				regionIndex[i] =
					regionIndex[-1 - regionIndex[i]];
			else
				regionIndex[i] = count++;
		return count;
	}
}
