package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian.SpecialPoint;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

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
	public void setTarget(Image<T> image, float[] calibration, SegmenterSettings settings) {
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
		Image<T> intermediateImage = img;
		if (settings.useMedianFilter) {
			intermediateImage = applyMedianFilter(intermediateImage);
			if (null == intermediateImage) {
				return false;
			}
		}

		float radius = settings.expectedRadius;
		float sigma = (float) (radius / Math.sqrt(img.getNumDimensions())); // optimal sigma for LoG approach and dimensionality
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		Image<FloatType> gaussianKernel = FourierConvolution.getGaussianKernel(factory, sigma, img.getNumDimensions());
		final FourierConvolution<T, FloatType> fConvGauss = new FourierConvolution<T, FloatType>(intermediateImage, gaussianKernel);
		if (!fConvGauss.checkInput() || !fConvGauss.process()) {
			errorMessage = baseErrorMessage + "Fourier convolution with Gaussian failed:\n" + fConvGauss.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvGauss.getResult();

		Image<FloatType> laplacianKernel = createLaplacianKernel();
		final FourierConvolution<T, FloatType> fConvLaplacian = new FourierConvolution<T, FloatType>(intermediateImage, laplacianKernel);
		if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
			errorMessage = baseErrorMessage + "Fourier Convolution with Laplacian failed:\n" + fConvLaplacian.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvLaplacian.getResult();	


		PickImagePeaks<T> peakPicker = new PickImagePeaks<T>(intermediateImage);
		double[] suppressionRadiuses = new double[img.getNumDimensions()];
		for (int i = 0; i < img.getNumDimensions(); i++) 
			suppressionRadiuses[i] = radius / calibration[i];
		peakPicker.setSuppression(suppressionRadiuses); // in pixels

		if (!peakPicker.checkInput() || !peakPicker.process()) {
			errorMessage = baseErrorMessage +"Could not run the peak picker algorithm:\n" + peakPicker.getErrorMessage();
			return false;
		}

		// Get peaks location and values
		final ArrayList<int[]> peaks = peakPicker.getPeakList();
		final LocalizableByDimCursor<T> cursor = intermediateImage.createLocalizableByDimCursor();
		// Prune values lower than threshold
		List<DifferenceOfGaussianPeak<T>> dogPeaks = new ArrayList<DifferenceOfGaussianPeak<T>>();
		final List<T> pruned_values = new ArrayList<T>();
		final SpecialPoint specialPoint = SpecialPoint.MAX;
		for (int i = 0; i < peaks.size(); i++) {
			int[] peak = peaks.get(i);
			cursor.setPosition(peak);
			T value = cursor.getType().copy();
			if (value.getRealFloat() < settings.threshold) {
				break; // because peaks are sorted, we can exit loop here
			}
			DifferenceOfGaussianPeak<T> dogPeak = new DifferenceOfGaussianPeak<T>(peak, value, specialPoint);
			dogPeaks.add(dogPeak);
			pruned_values.add(value);
		}
		
		// Do sub-pixel localization
		if (settings.doSubPixelLocalization ) {
			// Create localizer and apply it to the list
			final SubpixelLocalization<T> locator = new SubpixelLocalization<T>(intermediateImage, dogPeaks);
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
				for (int i = 0; i < img.getNumDimensions(); i++) 
					coords[i] = dogPeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.getNumDimensions(); i++) 
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
				"to the image, with a sigma suited to the blob estimated size.<br>" +
				"Calculations are made in the Fourier space. The maxima in the <br>" +
				"filtered image are searched for, and maxima too close from each <br>" +
				"other are suppressed. " +
				"</html>";	
	}

	@Override
	public String toString() {
		return "LoG segmenter";
	}
	
	/*
	 * PRIVATE METHODS
	 */


	private Image<FloatType> createLaplacianKernel() {
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		int numDim = img.getNumDimensions();
		Image<FloatType> laplacianKernel = null;
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
			quickKernel2D(laplacianArray, laplacianKernel);
		} 
		return laplacianKernel;
	}

	/*
	 * STATIC METHODS
	 */


	private static void quickKernel2D(float[][] vals, Image<FloatType> kern)	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[2];

		for (int i = 0; i < vals.length; ++i) 
			for (int j = 0; j < vals[i].length; ++j) {
				pos[0] = i;
				pos[1] = j;
				cursor.setPosition(pos);
				cursor.getType().set(vals[i][j]);
			}
		cursor.close();		
	}

	private static void quickKernel3D(float[][][] vals, Image<FloatType> kern)	{
		final LocalizableByDimCursor<FloatType> cursor = kern.createLocalizableByDimCursor();
		final int[] pos = new int[3];

		for (int i = 0; i < vals.length; ++i) 
			for (int j = 0; j < vals[i].length; ++j) 
				for (int k = 0; k < vals[j].length; ++k) {
					pos[0] = i;
					pos[1] = j;
					pos[2] = k;
					cursor.setPosition(pos);
					cursor.getType().set(vals[i][j][k]);
				}
		cursor.close();		
	}



}
