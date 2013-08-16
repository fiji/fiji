package imagescience.feature;

import imagescience.image.Axes;
import imagescience.image.ByteImage;
import imagescience.image.Coordinates;
import imagescience.image.Dimensions;
import imagescience.image.FloatImage;
import imagescience.image.Image;
import imagescience.utility.FMath;
import imagescience.utility.ImageScience;
import imagescience.utility.Messenger;
import imagescience.utility.Progressor;
import imagescience.utility.Timer;

/** Computes image statistics. */
public class Statistics {
	
	/** The minimum value in the image. */
	public final static int MINIMUM = 1;
	
	/** The maximum value in the image. */
	public final static int MAXIMUM = 2;
	
	/** The mean of the values in the image. */
	public final static int MEAN = 4;
	
	/** The median of the values in the image. */
	public final static int MEDIAN = 8;
	
	/** The number of elements in the image. */
	public final static int ELEMENTS = 16;
	
	/** The mass or sum of the values in the image. */
	public final static int MASS = 32;
	
	/** The variance or second-order moment about the mean of the values in the image. */
	public final static int VARIANCE = 64;
	
	/** The mode or most frequently occurring value in the image. */
	public final static int MODE = 128;
	
	/** The standard deviation from the mean of the values in the image. */
	public final static int SDEVIATION = 256;
	
	/** The average absolute deviation from the mean of the values in the image. */
	public final static int ADEVIATION = 512;
	
	/** The L1-norm or sum of the magnitudes of the values in the image. */
	public final static int L1NORM = 1024;
	
	/** The L2-norm or zero-mean standard deviation of the values in the image.*/
	public final static int L2NORM = 2048;
	
	/** The Fisher skewness or third-order moment about the mean of the values in the image. */
	public final static int SKEWNESS = 4096;
	
	/** The Fisher kurtosis or fourth-order moment about the mean of the values in the image. */
	public final static int KURTOSIS = 8192;
	
	private double dMinimum, dMaximum, dMean, dMedian, dElements, dMass,
	dVariance, dMode, dSDeviation, dADeviation, dL1norm, dL2norm,
	dSkewness, dKurtosis;
	
	private final static int BINS = 100000;
	
	/** Default constructor. */
	public Statistics() { }
	
	/** Returns the value of the requested statistic.
		
		@param statistic the statistic whose value is to be returned. Must be one of the static fields of this class.
		
		@return the value of the requested statistic as computed in the last call to any of the {@code run()} methods.
		
		@exception IllegalArgumentException if {@code statistic} is not one of the static fields of this class.
	*/
	public double get(final int statistic) {
		
		switch (statistic) {
			case MINIMUM: return dMinimum;
			case MAXIMUM: return dMaximum;
			case MEAN: return dMean;
			case MEDIAN: return dMedian;
			case ELEMENTS: return dElements;
			case MASS: return dMass;
			case VARIANCE: return dVariance;
			case MODE: return dMode;
			case SDEVIATION: return dSDeviation;
			case ADEVIATION: return dADeviation;
			case L1NORM: return dL1norm;
			case L2NORM: return dL2norm;
			case SKEWNESS: return dSkewness;
			case KURTOSIS: return dKurtosis;
			default: throw new IllegalArgumentException("Non-supported statistic");
		}
	}
	
	/** Computes the statistics of an image. For a {@link FloatImage}, the median and mode are not computed exactly but are estimated with an accuracy of +/- 0.0005 percent of the dynamic range of the image.
		
		@param image the image whose statistics are to be computed.
		
		@exception NullPointerException if {@code image} is {@code null}.
	*/
	public void run(final Image image) {
		
		final Dimensions dims = image.dimensions();
		final Coordinates min = new Coordinates();
		final Coordinates max = new Coordinates(dims.x-1,dims.y-1,dims.z-1,dims.t-1,dims.c-1);
		run(image,min,max);
	}
	
	/** Computes the statistics of an image in a rectangular region of interest. For a {@link FloatImage}, the median and mode are not computed exactly, but are estimated with an accuracy of +/- 0.0005 percent of the dynamic range of the image.
		
		@param image the image whose statistics are to be computed.
		
		@param min contains for each dimension the minimum coordinate of the rectangular region of interest.
		
		@param max contains for each dimension the maximum coordinate of the rectangular region of interest.
		
		@exception IllegalArgumentException if the region of interest as determined by {@code min} and {@code max} does not fall entirely within the image, or if any of the {@code min} coordinates is larger than its corresponding {@code max} coordinate.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public void run(final Image image, final Coordinates min, final Coordinates max) {
		
		final ByteImage mask = new ByteImage(new Dimensions());
		mask.set(new Coordinates(),1);
		run(image,min,max,mask);
	}
	
	/** Computes the statistics of an image in a masked rectangular region of interest. For a {@link FloatImage}, the median and mode are not computed exactly, but are estimated with an accuracy of +/- 0.0005 percent of the dynamic range of the image.
		
		@param image the image whose statistics are to be computed.
		
		@param min contains for each dimension the minimum coordinate of the rectangular region of interest.
		
		@param max contains for each dimension the maximum coordinate of the rectangular region of interest.
		
		@param mask the mask to be used within the region of interest determined by {@code min} and {@code max}. The method includes all image elements whose corresponding mask value is nonzero. The position of the (zeroth element of the) mask relative to the image is determined by {@code min}. In principle, the size of the mask must be equal to the size determined by {@code min} and {@code max}. Alternatively, to be able to be more memory efficient, the size of the mask may be equal to 1 in any dimension. In that case, the mask is extended (not actually, but implicitly by the algorithm) by value repetition, so that it (again) spans the entire range given by the {@code min} and the {@code max} coordinate for that dimension (this explains the need for the otherwise superfluous {@code max} coordinate).
		
		@exception IllegalArgumentException if the region of interest as determined by {@code min} and {@code max} does not fall entirely within the image, if any of the {@code min} coordinates is larger than its corresponding {@code max} coordinate, or if the size of the mask in any dimension does not match the size determined by the {@code min} and {@code max} coordinate for that dimension or, alternatively, is not equal to 1.
		
		@exception NullPointerException if any of the parameters is {@code null}.
	*/
	public void run(final Image image, final Coordinates min, final Coordinates max, final Image mask) {
		
		messenger.log(ImageScience.prelude()+"Statistics");
		
		final Timer timer = new Timer();
		timer.messenger.log(messenger.log());
		timer.start();
		
		// Initialize:
		final int xsize = max.x - min.x + 1;
		final int ysize = max.y - min.y + 1;
		final int zsize = max.z - min.z + 1;
		final int tsize = max.t - min.t + 1;
		final int csize = max.c - min.c + 1;
		final Dimensions idims = image.dimensions();
		final Dimensions mdims = mask.dimensions();
		if (min.x > max.x || min.x < 0 || max.x >= idims.x ||
			min.y > max.y || min.y < 0 || max.y >= idims.y ||
			min.z > max.z || min.z < 0 || max.z >= idims.z ||
			min.t > max.t || min.t < 0 || max.t >= idims.t ||
			min.c > max.c || min.c < 0 || max.c >= idims.c)
			throw new IllegalArgumentException("Invalid min and max coordinates for region of interest");
		if (!(mdims.x == xsize || mdims.x == 1) ||
			!(mdims.y == ysize || mdims.y == 1) ||
			!(mdims.z == zsize || mdims.z == 1) ||
			!(mdims.t == tsize || mdims.t == 1) ||
			!(mdims.c == csize || mdims.c == 1))
			throw new IllegalArgumentException("Invalid mask size for region of interest");
		
		final Coordinates ic = new Coordinates(); ic.x = min.x;
		final Coordinates mc = new Coordinates();
		final double[] ia = new double[xsize];
		final double[] ma = new double[mdims.x];
		image.axes(Axes.X); mask.axes(Axes.X);
		
		final int pmc = (mdims.c == 1) ? 0 : 1;
		final int pmt = (mdims.t == 1) ? 0 : 1;
		final int pmz = (mdims.z == 1) ? 0 : 1;
		final int pmy = (mdims.y == 1) ? 0 : 1;
		final int pmx = (mdims.x == 1) ? 0 : 1;
		
		messenger.log("Computing statistics");
		messenger.status("Computing statistics...");
		progressor.steps(2*csize*tsize*zsize);
		progressor.start();
		
		// Compute statistics:
		dElements=0.0;
		double dSum1=0.0, dSum2=0.0, dSum3=0.0, dSum4=0.0, dASum=0.0;
		dMinimum = Double.MAX_VALUE; dMaximum = -Double.MAX_VALUE;
		for (ic.c=min.c, mc.c=0; ic.c<=max.c; ++ic.c, mc.c+=pmc)
			for (ic.t=min.t, mc.t=0; ic.t<=max.t; ++ic.t, mc.t+=pmt)
				for (ic.z=min.z, mc.z=0; ic.z<=max.z; ++ic.z, mc.z+=pmz) {
					for (ic.y=min.y, mc.y=0; ic.y<=max.y; ++ic.y, mc.y+=pmy) {
						image.get(ic,ia); mask.get(mc,ma);
						for (int ix=0, mx=0; ix<xsize; ++ix, mx+=pmx) {
							if (ma[mx] != 0) {
								++dElements;
								final double val1 = ia[ix];
								final double val2 = val1*val1;
								dSum1 += val1;
								dSum2 += val2;
								dSum3 += val1*val2;
								dSum4 += val2*val2;
								dASum += Math.abs(val1);
								if (val1 < dMinimum) dMinimum = val1;
								if (val1 > dMaximum) dMaximum = val1;
							}
						}
					}
					progressor.step();
				}
		dMass = dSum1;
		dMean = dSum1/dElements;
		final double dMean2 = dMean*dMean;
		dVariance = (dSum2/dElements) - dMean2;
		dSDeviation = Math.sqrt(dVariance);
		dL1norm = dASum;
		dL2norm = Math.sqrt(dSum2);
		dSkewness = ((dSum3 - 3.0*dMean*dSum2)/dElements + 2.0*dMean*dMean2)/(dVariance*dSDeviation);
		dKurtosis = (((dSum4 - 4.0*dMean*dSum3 + 6.0*dMean2*dSum2)/dElements - 3.0*dMean2*dMean2)/(dVariance*dVariance)-3.0);
		
		double dADevSum=0.0;
		final double dRange = dMaximum - dMinimum;
		final double dScale = (dRange == 0.0) ? 1 : dRange;
		final int[] hist = new int[BINS+1];
		final double dNBins = BINS;
		for (ic.c=min.c, mc.c=0; ic.c<=max.c; ++ic.c, mc.c+=pmc)
			for (ic.t=min.t, mc.t=0; ic.t<=max.t; ++ic.t, mc.t+=pmt)
				for (ic.z=min.z, mc.z=0; ic.z<=max.z; ++ic.z, mc.z+=pmz) {
					for (ic.y=min.y, mc.y=0; ic.y<=max.y; ++ic.y, mc.y+=pmy) {
						image.get(ic,ia); mask.get(mc,ma);
						for (int ix=0, mx=0; ix<xsize; ++ix, mx+=pmx) {
							if (ma[mx] != 0) {
								++hist[(int)(((ia[ix] - dMinimum)/dScale)*dNBins)];
								dADevSum += Math.abs(ia[ix] - dMean);
							}
						}
					}
					progressor.step();
				}
		dADeviation = dADevSum/dElements;
		hist[BINS-1] += hist[BINS];
		int iMax = 0, iCum = 0;
		final int iElements = (int)dElements;
		final int iHalfway = (iElements%2 == 0) ? (iElements/2) : (iElements/2 + 1);
		for (int i=0; i<BINS; ++i) {
			final int iVal = hist[i];
			if (iVal >= iMax) { iMax = iVal; dMode = i; }
			if (iCum < iHalfway) { dMedian = i; iCum += iVal; }
		}
		dMode = dMinimum + ((dMode + 0.5)/dNBins)*dRange;
		dMedian = dMinimum + ((dMedian + 0.5)/dNBins)*dRange;
		if (!(image instanceof FloatImage)) {
			dMode = FMath.round(dMode);
			dMedian = FMath.round(dMedian);
		}
		
		messenger.status("");
		progressor.stop();
		timer.stop();
	}
	
	/** The object used for message displaying. */
	public final Messenger messenger = new Messenger();
	
	/** The object used for progress displaying. */
	public final Progressor progressor = new Progressor();
	
}
