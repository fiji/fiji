package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft.FourierConvolution;
import net.imglib2.algorithm.math.PickImagePeaks;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.detection.subpixel.QuadraticSubpixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization;
import fiji.plugin.trackmate.detection.subpixel.SubPixelLocalization.LocationType;

public class LogSegmenter <T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */

	private final static String BASE_ERROR_MESSAGE = "LogSegmenter: ";
	private LogSegmenterSettings settings;

	/*
	 * CONSTRUCTORS
	 */

	public LogSegmenter() {
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * METHODS
	 */

	@Override
	public SpotSegmenter<T> createNewSegmenter() {
		return new LogSegmenter<T>();
	}

	@Override
	public void setTarget(Img<T> image, float[] calibration, SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);
		this.settings = (LogSegmenterSettings) settings;
	}

	@Override
	public SegmenterSettings createDefaultSettings() {
		return new LogSegmenterSettings();
	}

	@Override
	public boolean process() {

		// Deal with median filter:
		Img<T> intermediateImage = img;
		if (settings.useMedianFilter) {
			intermediateImage = applyMedianFilter(intermediateImage);
			if (null == intermediateImage) {
				return false;
			}
		}

		float radius = settings.expectedRadius;
		float sigma = (float) (radius / Math.sqrt(img.numDimensions())); // optimal sigma for LoG approach and dimensionality
		ImgFactory<FloatType> factory = new ArrayImgFactory<FloatType>();
		Img<FloatType> gaussianKernel = FourierConvolution.createGaussianKernel(factory, sigma, img.numDimensions());
		final FourierConvolution<T, FloatType> fConvGauss = new FourierConvolution<T, FloatType>(intermediateImage, gaussianKernel);
		if (!fConvGauss.checkInput() || !fConvGauss.process()) {
			errorMessage = baseErrorMessage + "Fourier convolution with Gaussian failed:\n" + fConvGauss.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvGauss.getResult();

		Img<FloatType> laplacianKernel = createLaplacianKernel();
		final FourierConvolution<T, FloatType> fConvLaplacian = new FourierConvolution<T, FloatType>(intermediateImage, laplacianKernel);
		if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
			errorMessage = baseErrorMessage + "Fourier Convolution with Laplacian failed:\n" + fConvLaplacian.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvLaplacian.getResult();	

		PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(intermediateImage);
		double[] suppressionRadiuses = new double[img.numDimensions()];
		for (int i = 0; i < img.numDimensions(); i++) 
			suppressionRadiuses[i] = radius / calibration[i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels
		peakPicker.setAllowBorderPeak(true);

		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}

		// Get peaks location and values
		final ArrayList<int[]> centers = peakPicker.getPeakList();
		final RandomAccess<T> cursor = intermediateImage.randomAccess();
		// Prune values lower than threshold
		List<SubPixelLocalization<T>> dogPeaks = new ArrayList<SubPixelLocalization<T>>();
		final List<T> pruned_values = new ArrayList<T>();
		final LocationType specialPoint = LocationType.MAX;
		for (int i = 0; i < centers.size(); i++) {
			int[] center = centers.get(i);
			cursor.setPosition(center);
			T value = cursor.get().copy();
			if (value.getRealFloat() < settings.threshold) {
				break; // because peaks are sorted, we can exit loop here
			}
			SubPixelLocalization<T> peak = new SubPixelLocalization<T>(center, value, specialPoint);
			dogPeaks.add(peak);
			pruned_values.add(value);
		}

		// Do sub-pixel localization
		if (settings.doSubPixelLocalization ) {
			// Create localizer and apply it to the list
			final QuadraticSubpixelLocalization<T> locator = new QuadraticSubpixelLocalization<T>(intermediateImage, dogPeaks);
			locator.setNumThreads(1); // Since the calls to a segmenter  are already multi-threaded.
			locator.setCanMoveOutside(true);
			if ( !locator.checkInput() || !locator.process() )	{
				errorMessage = baseErrorMessage + locator.getErrorMessage();
				return false;
			}
			dogPeaks = locator.getDoGPeaks();
		}

		// Create spots
		spots.clear();
		for (int j = 0; j < dogPeaks.size(); j++) {

			DifferenceOfGaussianPeak<T> dogPeak = dogPeaks.get(j); 
			float[] coords = new float[3];
			if (settings.doSubPixelLocalization) {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogPeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogPeak.getPosition(i) * calibration[i];
			}
			Spot spot = new SpotImp(coords);
			spot.putFeature(Spot.QUALITY, pruned_values.get(j).getRealFloat());
			spot.putFeature(Spot.RADIUS, settings.expectedRadius);
			spots.add(spot);
		}

		return true;
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This segmenter applies a LoG (Laplacian of Gaussian) filter <br>" +
				"to the image, with a sigma suited to the blob estimated size. <br>" +
				"Calculations are made in the Fourier space. The maxima in the <br>" +
				"filtered image are searched for, and maxima too close from each <br>" +
				"other are suppressed. A quadratic fitting scheme allows to do <br>" +
				"sub-pixel localization. " +
				"</html>";	
	}

	@Override
	public String toString() {
		return "LoG segmenter";
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



}
