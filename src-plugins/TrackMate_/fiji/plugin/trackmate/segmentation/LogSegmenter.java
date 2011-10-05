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

public class LogSegmenter <T extends RealType<T> > extends AbstractSpotSegmenter<T> {

	/** The goal diameter of blobs in <b>pixels</b> following down-sizing. The image will be 
	 * down-sized such that the blob has this diameter (or smaller) in all directions. 
	 * 10 pixels was chosen because trial and error showed that it gave good results.*/
	public final static float GOAL_DOWNSAMPLED_BLOB_DIAM = 10f;

	private final static String BASE_ERROR_MESSAGE = "LogSegmenter: ";

	/** We smooth more than needed to discard secondary minima. */ 
	private static final float SMOOTH_FACTOR = 2;

	private float sigma;
	private Image<FloatType> laplacianKernel;
	private Image<FloatType> gaussianKernel;
	private LogSegmenterSettings settings;

	/*
	 * CONSTRUCTORS
	 */

	public LogSegmenter() {
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * PUBLIC METHODS
	 */
	
	public SpotSegmenter<T> createNewSegmenter() {
		return new LogSegmenter<T>();
	};

	@Override
	public void setTarget(Image<T> image, float[] calibration,	SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);

		createLaplacianKernel(); // instantiate laplacian kernel if needed
		createGaussianKernel();

		float radius = settings.expectedRadius;
		sigma = (float) (SMOOTH_FACTOR * 2 * radius / Math.sqrt(img.getNumDimensions())); // optimal sigma for LoG approach and dimensionality

		this.settings = (LogSegmenterSettings) settings;
	}


	@Override
	public SegmenterSettings createDefaultSettings() {
		return new LogSegmenterSettings();
	}


	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean process() {

		/* 0 - Get settings
		 */

		float threshold 			= settings.threshold;
		float radius 				= settings.expectedRadius;

		/* 1 - 	Downsample to improve run time. The image is downsampled by the 
		 * 		factor necessary to achieve a resulting blob size of about 10 pixels 
		 * 		in diameter in all dimensions. */

		final float[] downsampleFactors = createDownsampledDim(calibration, 2 * radius); // factors for x,y,z that we need for scaling image down;

		final int dim[] = img.getDimensions();
		for (int j = 0; j < dim.length; j++)
			dim[j] = (int) (dim[j] / downsampleFactors[j]);

		final DownSample<T> downsampler = new DownSample<T>(img, dim, 0.5f, 0.5f);	// optimal sigma is defined by 0.5f, as mentioned here: http://pacific.mpi-cbg.de/wiki/index.php/Downsample
		if (!downsampler.checkInput() || !downsampler.process()) {
			errorMessage = baseErrorMessage + "Failed to down-sample source image:\n"  + downsampler.getErrorMessage();
			return false;
		}
		Image<T> intermediateImage = downsampler.getResult();


		/* 2 - 	Apply a median filter, to get rid of salt and pepper noise which could be 
		 * 		mistaken for maxima in the algorithm (only applied if requested by user explicitly) */

		// Deal with median filter:
		intermediateImage = applyMedianFilter(intermediateImage);;
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
		final List<float[]> centeredExtrema = findExtrema.getRegionalExtremaCenters(false);

		/* 4.5 - Grab extrema value to work as a quality feature. */
		final List<Float> extremaValues = new ArrayList<Float>(centeredExtrema.size());
		int[] roundCoords = new int[centeredExtrema.get(0).length];
		LocalizableByDimCursor<T> cursor = intermediateImage.createLocalizableByDimCursor();
		for (float[] coords : centeredExtrema) {
			for (int i = 0; i < roundCoords.length; i++)
				roundCoords[i] = Math.round(coords[i]);
			cursor.setPosition(roundCoords);
			extremaValues.add(cursor.getType().getRealFloat());
		}

		// Create spots
		TreeMap<Float, Spot> spotQuality = new TreeMap<Float, Spot>();
		spots = convertToSpots(centeredExtrema, calibration, downsampleFactors);
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
				"large spot sizes (>&nbsp;~20 pixels), at the cost of preision in localization. " +
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
	 * Return the down-sampling factors that should be applied to the image so that 
	 * the diameter given (in physical units) would have a pixel size (diameter) set
	 * by the static field {@link GOAL_DOWNSAMPLED_BLOB_DIAM}.
	 * @param calibration  the physical calibration (pixel size)
	 * @param diam  the physical object diameter
	 * @return  a float array of down-sampling factors, for usage in {@link DownSample}
	 * @see #downSampleByFactor(Image, float[])
	 */
	private static float[] createDownsampledDim(final float[] calibration, final float diameter) {
		float goal = GOAL_DOWNSAMPLED_BLOB_DIAM;
		int numDim = calibration.length;
		float widthFactor;
		if ( (diameter / calibration[0]) > goal) {
			widthFactor = (diameter / calibration[0]) / goal; // scale down to reach goal size
		} else{
			widthFactor = 1; // do not scale down
		}
		float heightFactor;
		if ( (diameter / calibration[1]) > goal) {
			heightFactor = (diameter / calibration[1]) / goal;
		} else {
			heightFactor = 1;
		}
		float depthFactor;
		if ( (numDim == 3 && (diameter / calibration[2]) > goal) ) {
			depthFactor = (diameter / calibration[2]) / goal; 
		} else {
			depthFactor = 1;								
		}
		float downsampleFactors[];
		if (numDim ==3)
			downsampleFactors = new float[]{widthFactor, heightFactor, depthFactor};
		else
			downsampleFactors = new float[]{widthFactor, heightFactor};
		return downsampleFactors;
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
	private static List<Spot> convertToSpots(List< float[] > coords, float[] calibration, float[] downsampleFactors) {
		ArrayList<Spot> spots = new ArrayList<Spot>();
		Iterator< float[] > itr = coords.iterator();
		while (itr.hasNext()) {
			float[] coord = itr.next();
			float[] calibrated = new float[3];
			for (int i = 0; i < calibration.length; i++) 
				calibrated[i] = coord[i] * calibration[i] * downsampleFactors[i];
			SpotImp spot = new SpotImp(calibrated);
			spots.add(spot);
		}
		return spots;
	}


}
