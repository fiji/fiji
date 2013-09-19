package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.fft.FourierConvolution;
import net.imglib2.algorithm.math.PickImagePeaks;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.subpixel.QuadraticSubpixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization.LocationType;
import fiji.plugin.trackmate.detection.util.MedianFilter3x3;
import fiji.plugin.trackmate.util.TMUtils;

public class LogDetector <T extends RealType<T>  & NativeType<T>> implements SpotDetector<T>, MultiThreaded {

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "LogDetector: ";
	/** The image to segment. Will not modified. */
	protected ImgPlus<T> img;
	protected double radius;
	protected double threshold;
	protected boolean doSubPixelLocalization;
	protected boolean doMedianFilter;
	protected String baseErrorMessage;
	protected String errorMessage;
	/** The list of {@link Spot} that will be populated by this detector. */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/** The processing time in ms. */
	protected long processingTime;
	private int numThreads;

	/*
	 * CONSTRUCTORS
	 */

	public LogDetector(final ImgPlus<T> img, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter) {
		this.img = img;
		this.radius = radius;
		this.threshold = threshold;
		this.doSubPixelLocalization = doSubPixelLocalization;
		this.doMedianFilter = doMedianFilter;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		setNumThreads();
	}

	/*
	 * METHODS
	 */
	
	@Override
	public boolean checkInput() {
		if (null == img) {
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if (!(img.numDimensions() == 2 || img.numDimensions() == 3)) {
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + img.numDimensions() +"D.";
			return false;
		}
		return true;
	};
	
	
	@Override
	public boolean process() {
		
		long start = System.currentTimeMillis();

		// Deal with median filter:
		Img<T> intermediateImage = img;
		if (doMedianFilter) {
			intermediateImage = applyMedianFilter(intermediateImage);
			if (null == intermediateImage) {
				return false;
			}
		}

		double sigma = radius / Math.sqrt(img.numDimensions()); // optimal sigma for LoG approach and dimensionality
		// Turn it in pixel coordinates
		final double[] calibration = TMUtils.getSpatialCalibration(img);
		double[] sigmas = new double[img.numDimensions()];
		for (int i = 0; i < sigmas.length; i++) {
			sigmas[i] = sigma / calibration[i];
		}
		
		ImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		Img<FloatType> gaussianKernel = FourierConvolution.createGaussianKernel(factory, sigmas);
		FourierConvolution<T, FloatType> fConvGauss;
		try {
			fConvGauss = new FourierConvolution<T, FloatType>(intermediateImage, gaussianKernel);
		} catch (IncompatibleTypeException e) {
			errorMessage = baseErrorMessage + "Fourier convolution failed: "+e.getMessage();
			return false;
		}

		fConvGauss.setNumThreads(numThreads);
		if (!fConvGauss.checkInput() || !fConvGauss.process()) {
			errorMessage = baseErrorMessage + "Fourier convolution with Gaussian failed:\n" + fConvGauss.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvGauss.getResult();

		Img<FloatType> laplacianKernel = createLaplacianKernel();
		FourierConvolution<T, FloatType> fConvLaplacian;
		try {
			fConvLaplacian = new FourierConvolution<T, FloatType>(intermediateImage, laplacianKernel);
		} catch (IncompatibleTypeException e) {
			errorMessage = baseErrorMessage + "Fourier convolution failed: "+e.getMessage();
			return false;
		}
		
		fConvLaplacian.setNumThreads(numThreads);
		if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
			errorMessage = baseErrorMessage + "Fourier Convolution with Laplacian failed:\n" + fConvLaplacian.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvLaplacian.getResult();	

		PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(intermediateImage);
		double[] suppressionRadiuses = new double[img.numDimensions()];
		for (int i = 0; i < img.numDimensions(); i++) 
			suppressionRadiuses[i] = radius / calibration [i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels
		peakPicker.setAllowBorderPeak(true);

		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}

		try {
			Thread.sleep(0);
		} catch (InterruptedException e) {
			return false;
		}

		// Get peaks location and values
		final ArrayList<long[]> centers = peakPicker.getPeakList();
		final RandomAccess<T> cursor = intermediateImage.randomAccess();
		// Prune values lower than threshold
		List<SubPixelLocalization<T>> peaks = new ArrayList<SubPixelLocalization<T>>();
		final List<T> pruned_values = new ArrayList<T>();
		final LocationType specialPoint = LocationType.MAX;
		for (int i = 0; i < centers.size(); i++) {
			long[] center = centers.get(i);
			cursor.setPosition(center);
			T value = cursor.get().copy();
			if (value.getRealDouble() < threshold) {
				break; // because peaks are sorted, we can exit loop here
			}
			SubPixelLocalization<T> peak = new SubPixelLocalization<T>(center, value, specialPoint);
			peaks.add(peak);
			pruned_values.add(value);
		}

		// Do sub-pixel localization
		if (doSubPixelLocalization && !peaks.isEmpty()) {
			// Create localizer and apply it to the list. The list object will be updated
			final QuadraticSubpixelLocalization<T> locator = new QuadraticSubpixelLocalization<T>(intermediateImage, peaks);
			locator.setNumThreads(numThreads);
			locator.setCanMoveOutside(true);
			if ( !locator.checkInput() || !locator.process() )	{
				errorMessage = baseErrorMessage + locator.getErrorMessage();
				return false;
			}
		}

		// Create spots
		spots.clear();
		for (int j = 0; j < peaks.size(); j++) {

			SubPixelLocalization<T> peak = peaks.get(j); 
			double[] coords = new double[3];
			for (int i = 0; i < img.numDimensions(); i++) {
				coords[i] = peak.getDoublePosition(i) * calibration[i];
			}
			Spot spot = new Spot(coords);
			spot.putFeature(Spot.QUALITY, peak.getValue().getRealDouble());
			spot.putFeature(Spot.RADIUS, radius);
			spots.add(spot);
		}

		long end = System.currentTimeMillis();
		processingTime = end - start;
		
		return true;
	}


	/*
	 * PRIVATE METHODS
	 */


	private Img<FloatType> createLaplacianKernel() {
		final ImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		int numDim = img.numDimensions();
		Img<FloatType> laplacianKernel = null;
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.create(new int[]{3, 3, 3}, new FloatType());
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.create(new int[]{3, 3}, new FloatType());
			quickKernel2D(laplacianArray, laplacianKernel);
		} 
		return laplacianKernel;
	}
	
	/**
	 * Apply a simple 3x3 median filter to the target image.
	 */
	protected Img<T> applyMedianFilter(final Img<T> image) {
		final MedianFilter3x3<T> medFilt = new MedianFilter3x3<T>(image); 
		if (!medFilt.checkInput() || !medFilt.process()) {
			errorMessage = baseErrorMessage + "Failed in applying median filter";
			return null;
		}
		return medFilt.getResult(); 
	}

	/*
	 * STATIC METHODS
	 */


	private static void quickKernel2D(float[][] vals, Img<FloatType> kern)	{
		final RandomAccess<FloatType> cursor = kern.randomAccess();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i) 
			for (int j = 0; j < vals[i].length; ++j) {
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.get().set(vals[i][j]);
			}
	}

	private static void quickKernel3D(float[][][] vals, Img<FloatType> kern)	{
		final RandomAccess<FloatType> cursor = kern.randomAccess();
		final int[] pos = new int[3];

		for (int i = 0; i < vals.length; ++i) 
			for (int j = 0; j < vals[i].length; ++j) 
				for (int k = 0; k < vals[j].length; ++k) {
					pos[0] = i;
					pos[1] = j;
					pos[2] = k;
					cursor.setPosition(pos);
					cursor.get().set(vals[i][j][k]);
				}
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}
		
	@Override
	public String getErrorMessage() {
		return errorMessage ;
	}
	

	@Override
	public long getProcessingTime() {
		return processingTime;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}


}
