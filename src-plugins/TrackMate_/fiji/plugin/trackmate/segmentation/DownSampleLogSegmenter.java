package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFactory;
import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFinder;
import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.util.TMUtils;

public class DownSampleLogSegmenter <T extends RealType<T> > extends AbstractSpotSegmenter<T> {

	private final static String BASE_ERROR_MESSAGE = "DownSampleLogSegmenter: ";

	private float sigma;
	private Image<FloatType> laplacianKernel;
	private Image<FloatType> gaussianKernel;
	private DownSampleLogSegmenterSettings settings;

	/*
	 * CONSTRUCTORS
	 */

	public DownSampleLogSegmenter() {
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	public SpotSegmenter<T> createNewSegmenter() {
		return new DownSampleLogSegmenter<T>();
	};

	@Override
	public void setTarget(Image<T> image, float[] calibration,	SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);

		this.settings = (DownSampleLogSegmenterSettings) settings;
		float radius = this.settings.expectedRadius;
		this.sigma = (float) (radius / Math.sqrt(img.getNumDimensions()) / this.settings.downSamplingFactor); // optimal sigma for LoG approach and dimensionality

		createLaplacianKernel(); // instantiate laplacian kernel if needed
		createGaussianKernel();
	}

	@Override
	public boolean checkInput() {
		if (!super.checkInput())
			return false;
		if (settings instanceof DownSampleLogSegmenterSettings) {
			return true;
		} else {
			errorMessage = baseErrorMessage + "Bad settings class. Expected DownSampleLogSegmenterSettings, got "+settings.getClass()+".\n";
			return false;
		}
	}
	
	
	@Override
	public SegmenterSettings createDefaultSettings() {
		return new DownSampleLogSegmenterSettings();
	}


	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean process() {

		/* 0 - Get settings
		 */

		float threshold 			= settings.threshold;
		float downSampleFactor 		= settings.downSamplingFactor;

		/* 1 - 	Downsample to improve run time. */

		final int dim[] = img.getDimensions();
		for (int j = 0; j < dim.length; j++)
			dim[j] = (int) (dim[j] / downSampleFactor);

		final DownSample<T> downsampler = new DownSample<T>(img, dim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
		if (!downsampler.checkInput() || !downsampler.process()) {
			errorMessage = baseErrorMessage + "Failed to down-sample source image:\n"  + downsampler.getErrorMessage();
			return false;
		}
		Image<T> intermediateImage = downsampler.getResult();

		/* 2 - 	Apply a median filter, to get rid of salt and pepper noise which could be 
		 * 		mistaken for maxima in the algorithm (only applied if requested by user explicitly) */

		// Deal with median filter:
		if (settings.useMedianFilter) {
			intermediateImage = applyMedianFilter(intermediateImage);
			if (null == intermediateImage) {
				return false;
			}
		}

		/* 3 - 	Apply the LoG filter - current homemade implementation  */

		final FourierConvolution<T, FloatType> fConvGauss = new FourierConvolution<T, FloatType>(intermediateImage, gaussianKernel);
		if (!fConvGauss.checkInput() || !fConvGauss.process()) {
			errorMessage = baseErrorMessage + "Fourier convolution with Gaussian failed:\n" + fConvGauss.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvGauss.getResult();

		final FourierConvolution<T, FloatType> fConvLaplacian = new FourierConvolution<T, FloatType>(intermediateImage, laplacianKernel);
		if (!fConvLaplacian.checkInput() || !fConvLaplacian.process()) {
			errorMessage = baseErrorMessage + "Fourier Convolution with Laplacian failed:\n" + fConvLaplacian.getErrorMessage() ;
			return false;
		}
		intermediateImage = fConvLaplacian.getResult();	

		/* 4 - Find extrema of newly convoluted image */

		final RegionalExtremaFactory<T> extremaFactory = new RegionalExtremaFactory<T>(intermediateImage);
		final RegionalExtremaFinder<T> findExtrema = extremaFactory.createRegionalMaximaFinder(true);
		findExtrema.allowEdgeExtrema(false);
		T thresh = img.createType();
		thresh.setReal(threshold);
		findExtrema.setThreshold(thresh);
		if (!findExtrema.checkInput() || !findExtrema.process()) { 
			errorMessage = baseErrorMessage + "Extrema finder failed:\n" + findExtrema.getErrorMessage();
			return false;
		}
		final List<float[]> centeredExtrema = findExtrema.getRegionalExtremaCenters();

		/* 4.5 - Grab extrema value to work as a quality feature. */
		final List<Float> extremaValues = new ArrayList<Float>(centeredExtrema.size());
		int[] roundCoords = new int[centeredExtrema.get(0).length];
		LocalizableByDimCursor<T> cursor = intermediateImage.createLocalizableByDimCursor();
		for (float[] coords : centeredExtrema) {
			for (int i = 0; i < roundCoords.length; i++) {
				roundCoords[i] = Math.round(coords[i]);
			}
			cursor.setPosition(roundCoords);
			extremaValues.add(cursor.getType().getRealFloat());
		}

		// Create spots
		TreeMap<Float, Spot> spotQuality = new TreeMap<Float, Spot>();
		spots = convertToSpots(centeredExtrema, calibration, downSampleFactor);
		for (int i = 0; i < spots.size(); i++) {
			spots.get(i).putFeature(Spot.QUALITY, extremaValues.get(i));
			spots.get(i).putFeature(Spot.RADIUS, settings.expectedRadius);
			spotQuality.put(extremaValues.get(i), spots.get(i));
		}

		// Prune spots too close to each other
		spots = TMUtils.suppressSpots(spots, Spot.QUALITY);

		return true;
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This segmenter is basically identical to the LoG segmenter, except <br>" +
				"that images are downsampled before filtering, giving it a small <br>" +
				"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
				"large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " +
				"</html>";
	}
	
	@Override
	public String toString() {
		return "Downsampled LoG segmenter";
	}


	/*
	 * PRIVATE METHODS
	 */


	private void createLaplacianKernel() {
		final ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		int numDim = img.getNumDimensions();
		if (numDim == 3) {
			final float laplacianArray[][][] = new float[][][]{ { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} }, { {-1/18,-1/18,-1/18}, {-1/18,1,-1/18}, {-1/18,-1/18,-1/18} }, { {0,-1/18,0},{-1/18,-1/18,-1/18},{0,-1/18,0} } }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3, 3}, "Laplacian");
			quickKernel3D(laplacianArray, laplacianKernel);
		} else if (numDim == 2) {
			final float laplacianArray[][] = new float[][]{ {-1/8,-1/8,-1/8},{-1/8,1,-1/8},{-1/8,-1/8,-1/8} }; // laplace kernel found here: http://en.wikipedia.org/wiki/Discrete_Laplace_operator
			laplacianKernel = factory.createImage(new int[]{3, 3}, "Laplacian");
			quickKernel2D(laplacianArray, laplacianKernel);
		} 
	}

	private void createGaussianKernel() {
		ImageFactory<FloatType> factory = new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		gaussianKernel = FourierConvolution.getGaussianKernel(factory, sigma, img.getNumDimensions());
	}

	/*
	 * STATIC METHODS
	 */

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

	/**
	 * Create a {@link Spot} ArrayList from a list of down-sampled pixel coordinates. 
	 * Internally, we use the {@link SpotImp} concrete implementation of Featurable.
	 * <p>
	 * Since the spot 
	 * coordinates are expected to be in physical units, a calibration array
	 * is used to translate the pixel coordinates in physical coordinates.
	 * <p>
	 * Also, we assume we have been working on a down-sampled image, so we must
	 * scale the coordinates back.
	 * @param  coords  the coordinates in pixel units
	 * @param  calibration  the calibration array that stores pixel size
	 * @param  downsampleFactors  the array containing the factors used to down-sample the source image
	 * @return  Spot list  with coordinates in physical units of the source image
	 */
	private static List<Spot> convertToSpots(List< float[] > coords, float[] calibration, float downSampleFactor) {
		ArrayList<Spot> spots = new ArrayList<Spot>();
		Iterator< float[] > itr = coords.iterator();
		while (itr.hasNext()) {
			float[] coord = itr.next();
			float[] calibrated = new float[3];
			for (int i = 0; i < calibration.length; i++) 
				calibrated[i] = coord[i] * calibration[i] * downSampleFactor;
			SpotImp spot = new SpotImp(calibrated);
			spots.add(spot);
		}
		return spots;
	}


}
