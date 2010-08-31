package fiji.plugin.nperry;

import ij.ImagePlus;
import ij.ImageStack;

import java.util.Arrays;
import java.util.Collection;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;

/**
 * List of static utilities for the {@link Embryo_Tracker} plugin
 */
public class Utils {
	
	/**
	 * Return a 3D stack or a 2D slice as an {@link Image} corresponding to the frame number <code>iFrame</code>
	 * in the given 4D or 3D {@link ImagePlus}.
	 * @param imp  the 4D or 3D source ImagePlus
	 * @param iFrame  the frame number to extract, 0-based
	 * @return  a 3D or 2D {@link Image} with the single timepoint required 
	 */
	public static <T extends RealType<T>> Image<T> getSingleFrameAsImage(ImagePlus imp, int iFrame) {
		ImageStack frame = imp.createEmptyStack();
		ImageStack stack = imp.getImageStack();
		int numSlices = imp.getNSlices();
		
		// ...create the slice by combining the ImageProcessors, one for each Z in the stack.
		for (int j = 1; j <= numSlices; j++) 
			frame.addSlice(Integer.toString(j + (iFrame * numSlices)), stack.getProcessor(j + (iFrame * numSlices)));
		
		ImagePlus ipSingleFrame = new ImagePlus("Frame " + Integer.toString(iFrame + 1), frame);
		return ImagePlusAdapter.wrap(ipSingleFrame);
	}
	
	
	

	
	
	
	/*
	 * PRIVATE METHODS
	 */

	
	/**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array. Taken from commons-math.
	 */
	public static final double getPercentile(final double[] values, final double p) {

		final int size = values.length;
		if ((p > 1) || (p <= 0)) {
            throw new IllegalArgumentException("invalid quantile value: " + p);
        }
        if (size == 0) {
            return Double.NaN;
        }
        if (size == 1) {
            return values[0]; // always return single value for n = 1
        }
        double n = (double) size;
        double pos = p * (n + 1);
        double fpos = Math.floor(pos);
        int intPos = (int) fpos;
        double dif = pos - fpos;
        double[] sorted = new double[size];
        System.arraycopy(values, 0, sorted, 0, size);
        Arrays.sort(sorted);

        if (pos < 1) {
            return sorted[0];
        }
        if (pos >= n) {
            return sorted[size - 1];
        }
        double lower = sorted[intPos - 1];
        double upper = sorted[intPos];
        return lower + dif * (upper - lower);
	}
	
	
	/** 
	 * Returns <code>[range, min, max]</code> of the given double array.
	 * @return A double[] of length 3, where index 0 is the range, index 1 is the min, and index 2 is the max.
	 */
	public static final double[] getRange(final double[] data) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double value;
		for (int i = 0; i < data.length; i++) {
			value = data[i];
			if (value < min) min = value;
			if (value > max) max = value;
		}		
		return new double[] {(max-min), min, max};
	}

	/**
	 * Return the feature values of this Spot collection as a new double array.
	 */
	public static final double[] getFeature(final Collection<Spot> spots, final Feature feature) {
		final double[] values = new double[spots.size()];
		int index = 0;
		for(Spot spot : spots) {
			values[index] = spot.getFeature(feature);
			index++;
		}
		return values;
	}
	
	/**
	 * Return the optimal bin number for a histogram of the data given in array, using the 
	 * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
	 */
	public static final int getNBins(final double[] values) {
		final int size = values.length;
		final double q1 = Utils.getPercentile(values, 0.25);
		final double q3 = Utils.getPercentile(values, 0.75);
		final double iqr = q3 - q1;
		final double binWidth = 2 * iqr * Math.pow(size, -0.33);
		final double[] range = getRange(values);
		return (int) ( range[0] / binWidth + 1 ); 
	}
	

	/**
	 * Create a histogram from the data given.
	 */
	public static final int[] histogram(final double data[], final int nBins) {
		final double[] range = Utils.getRange(data);
		final double binWidth = range[0]/nBins;
		final int[] hist = new int[nBins];
		int index;

		for (int i = 0; i < data.length; i++) {
			index = Math.min((int) Math.floor((data[i] - range[1]) / binWidth), nBins - 1);
			hist[index]++;
		}
		return hist;
	}
	
	/**
	 * Create a histogram from the data given, with a default number of bins given by {@link #getNBins(double[])}.
	 * @param data
	 * @return
	 */
	public static final int[] histogram(final double data[]) {
		return histogram(data, getNBins(data));
	}
	
	/**
	 * Return a threshold for the given data, using an Otsu histogram thresholding method.
	 */
	public static final double otsuThreshold(double[] data) {
		return otsuThreshold(data, getNBins(data));
	}
	
	/**
	 * Return a threshold for the given data, using an Otsu histogram thresholding method with a given bin number.
	 */
	public static final double otsuThreshold(double[] data, int nBins) {
		final int[] hist = histogram(data, nBins);
		final int thresholdIndex = otsuThresholdIndex(hist, data.length);
		double[] range = getRange(data);
		double binWidth = range[0] / nBins;
		return 	range[1] + binWidth * thresholdIndex;
	}
	
	/**
	 * Given a histogram array <code>hist</code>, built with an initial amount of <code>nPoints</code>
	 * data item, this method return the bin index that thresholds the histogram in 2 classes. 
	 * The threshold is performed using the Otsu Threshold Method, 
	 * {@link http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html}.
	 * @param hist  the histogram array
	 * @param nPoints  the number of data items this histogram was built on
	 * @return the bin index of the histogram that thresholds it
	 */
	public static final int otsuThresholdIndex(final int[] hist, final int nPoints)	{
		int total = nPoints;

		double sum = 0;
		for (int t = 0 ; t < hist.length ; t++) 
			sum += t * hist[t];

		double sumB = 0;
		int wB = 0;
		int wF = 0;

		double varMax = 0;
		int threshold = 0;

		for (int t = 0 ; t < hist.length ; t++) {
			wB += hist[t];               // Weight Background
			if (wB == 0) continue;

			wF = total - wB;                 // Weight Foreground
			if (wF == 0) break;

			sumB += (float) (t * hist[t]);

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (float)wB * (float)wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > varMax) {
				varMax = varBetween;
				threshold = t;
			}
		}
		return threshold;
	}
}
