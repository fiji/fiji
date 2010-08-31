package fiji.plugin.nperry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import ij.ImagePlus;
import ij.ImageStack;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.roi.StructuringElement;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;

/**
 * List of static utilities for the {@link Embryo_Tracker} plugin
 */
public class Utils {
	
	/**
	 * Return the down-sampling factors that should be applied to the image so that 
	 * the diameter given (in physical units) would have a pixel size (diameter) set
	 * by the static field {@link Embryo_Tracker#GOAL_DOWNSAMPLED_BLOB_DIAM}.
	 * @param calibration  the physical calibration (pixel size)
	 * @param diam  the physical object diameter
	 * @return  a float array of down-sampling factors, for usage in {@link DownSample}
	 * @see #downSampleByFactor(Image, float[])
	 */
	public static float[] createDownsampledDim(final float[] calibration, final float diam) {
		float goal = Embryo_Tracker.GOAL_DOWNSAMPLED_BLOB_DIAM;
		int numDim = calibration.length;
		float widthFactor;
		if ( (diam / calibration[0]) > goal) {
			widthFactor = (diam / calibration[0]) / goal; // scale down to reach goal size
		} else{
			widthFactor = 1; // do not scale down
		}
		float heightFactor;
		if ( (diam / calibration[1]) > goal) {
			heightFactor = (diam / calibration[1]) / goal;
		} else {
			heightFactor = 1;
		}
		float depthFactor;
		if ( (numDim == 3 && (diam / calibration[2]) > goal) ) {
			depthFactor = (diam / calibration[2]) / goal; 
		} else {
			depthFactor = 1;								
		}
		float downsampleFactors[] = new float[]{widthFactor, heightFactor, depthFactor};
		return downsampleFactors;
	}
	
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
	
	/**
	 * Return a down-sampled copy of the source image, where every dimension has been shrunk 
	 * by the down-sampling factors given in argument.
	 * @param source  the image to down-sample
	 * @param downsampleFactors  the shrinking factor
	 * @return  a down-sampled copy of the source image
	 * @see #createDownsampledDim(float[], float)
	 */
	public static <T extends RealType<T>> Image<T> downSampleByFactor(final Image<T> source, final float[] downsampleFactors) {
		final int dim[] = source.getDimensions();
		for (int j = 0; j < dim.length; j++)
			dim[j] = (int) (dim[j] / downsampleFactors[j]);
	
		final DownSample<T> downsampler = new DownSample<T>(source, dim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
		if (!downsampler.checkInput() || !downsampler.process()) {  // checkInput ensures the input is correct, and process runs the algorithm.
			System.out.println(downsampler.getErrorMessage()); 
	        System.out.println("Bye.");
	        return null;
		}
		return downsampler.getResult();
	}
	
	/**
	 * Takes the down-sampled coordinates of a list of {@link Spots}, and scales them back to be coordinates of the
	 * original image using the downsample factors.
	 * 
	 * @param spots The list of Spots to convert the coordinates for.
	 * @param downsampleFactors The downsample factors used for each dimension.
	 */
	public static void downsampledCoordsToOrigCoords(ArrayList<Spot> spots, float downsampleFactors[]) {
		Iterator<Spot> itr = spots.iterator();
		while (itr.hasNext()) {
			Spot spot = itr.next();
			float[] coords = spot.getCoordinates();
			
			// Undo downsampling
			for (int i = 0; i < coords.length; i++) {
				coords[i] = coords[i] * downsampleFactors[i];
			}
		}
	}
	
	/**
	 * Build a 3x3 square {@link StructuringElement}. The actual structure vary whether we get a 2D image
	 * or a 3D one, this is why the dimension number is required here. 
	 * @param numDim  the number of dimension of the target image 
	 * @return  a square structuring element suitable for the target image dimensionality, <code>null</code>
	 * if the dimensionality is not 2 or 3
	 */
	public static StructuringElement makeSquareStrel(int numDim) {
		StructuringElement strel;		
		// Need to figure out the dimensionality of the image in order to create a StructuringElement of the correct dimensionality (StructuringElement needs to have same dimensionality as the image):
		if (numDim == 3) {  // 3D case
			strel = new StructuringElement(new int[]{3, 3, 1}, "3D Square");  // unoptimized shape for 3D case. Note here that we manually are making this shape (not using a class method). This code is courtesy of Larry Lindsey
			Cursor<BitType> c = strel.createCursor();  // in this case, the shape is manually made, so we have to manually set it, too.
			while (c.hasNext()) { 
			    c.fwd(); 
			    c.getType().setOne(); 
			} 
			c.close(); 
		} else if (numDim == 2)  			// 2D case
			strel = StructuringElement.createCube(2, 3);  // unoptimized shape
		else 
			return null;
		return strel;
	}
	
	/**
	 * Return a new laplacian kernel suitable for convolution, in 2D or 3D. If the dimensionality
	 * given is not 2 or 3, <code>null<code> is returned.
	 */
	public static Image<FloatType> createLaplacianKernel(int numDim) {
		Image<FloatType> laplacianKernel;
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
			quickKernel2D(laplacianArray, laplacianKernel);
		} else 
			return null;
		return laplacianKernel;
	}
	
	
	
	
	
	
	/*
	 * PRIVATE METHODS
	 */

	private static void quickKernel3D(float[][][] vals, Image<FloatType> kern)	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[3];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				for (int k = 0; k < vals[j].length; ++k)
				{
					pos[0] = i;
					pos[1] = j;
					pos[2] = k;
					cursor.setPosition(pos);
					cursor.getType().set(vals[i][j][k]);
				}
			}
		}
		cursor.close();		
	}
	
	/**
	 * Code courtesy of Larry Lindsey. However, it is protected in the DirectConvolution class,
	 * so I reproduced it here.
	 * 
	 * @param vals
	 * @param kern
	 */
	private static void quickKernel2D(float[][] vals, Image<FloatType> kern)	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i)
		{
			for (int j = 0; j < vals[i].length; ++j)
			{
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.getType().set(vals[i][j]);
			}
		}
		cursor.close();		
	}
	
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
