package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFactory;
import mpicbg.imglib.algorithm.extremafinder.RegionalExtremaFinder;
import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.algorithm.roi.MedianFilter;
import mpicbg.imglib.algorithm.roi.StructuringElement;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

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
	private StructuringElement strel;


	/*
	 * CONSTRUCTORS
	 */
	
	/**
	 * Instantiate a new blank {@link LogSegmenter} with default settings.
	 */
	public LogSegmenter() {
		settings = new LogSegmenterSettings();
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	
	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Set the image that will be segmented by this algorithm. This resets
	 * the {@link #spots} and {@link #filteredImage} fields. 
	 */
	@Override
	public void setImage(Image<T> image) {
		if  ( (null == img ) || (img.getNumDimensions() != image.getNumDimensions()) ) {
			if (image == null)
				return;
			this.img = image;
			createLaplacianKernel(); // instantiate laplacian kernel if needed
			createGaussianKernel();
			createSquareStrel();
			sigma = (float) (SMOOTH_FACTOR * 2 * radius / Math.sqrt(img.getNumDimensions())); // optimal sigma for LoG approach and dimensionality
		}
		this.spots = null;
		this.intermediateImage = null;
		this.img = image;
	}
		
	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean checkInput() {
		boolean isOk = super.checkInput();		
		if (!isOk)
			return false;
		if (!(settings instanceof LogSegmenterSettings)) {
			errorMessage = baseErrorMessage + "Expected to have a LogSegmenterSettings as settings object, but got a " + settings.getClass().getSimpleName() + ".";
			return false;
		}
		return true;
	}

	@Override
	public boolean process() {
		
		/* 0 - Get settings
		 */
		LogSegmenterSettings lss = (LogSegmenterSettings) settings;
		boolean useMedianFilter 	= lss.useMedianFilter;
		boolean allowEdgeExtrema  	= lss.allowEdgeMaxima;
		float threshold 			= lss.threshold;
		
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
		intermediateImage = downsampler.getResult();
		
		
		/* 2 - 	Apply a median filter, to get rid of salt and pepper noise which could be 
		 * 		mistaken for maxima in the algorithm (only applied if requested by user explicitly) */
		
		if (useMedianFilter) {
			final MedianFilter<T> medFilt = new MedianFilter<T>(intermediateImage, strel, new OutOfBoundsStrategyMirrorFactory<T>()); 
			if (!medFilt.process()) {
				errorMessage = baseErrorMessage + "Failed in applying median filter";
				return false;
			}
			intermediateImage = medFilt.getResult(); 
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
		findExtrema.allowEdgeExtrema(allowEdgeExtrema);
		T thresh = img.createType();
		thresh .setReal(threshold);
		findExtrema.setThreshold(thresh);
		if (!findExtrema.checkInput() || !findExtrema.process()) { 
			errorMessage = baseErrorMessage + "Extrema Finder failed:\n" + findExtrema.getErrorMessage();
			return false;
		}
		final List<float[]> centeredExtrema = findExtrema.getRegionalExtremaCenters(false);
		
		spots = convertToSpots(centeredExtrema, calibration, downsampleFactors);
		return true;
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
	
	private void createSquareStrel() {
		int numDim = img.getNumDimensions();
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
		int index = 0;
		while (itr.hasNext()) {
			float[] coord = itr.next();
			float[] calibrated = new float[3];
			for (int i = 0; i < calibration.length; i++) 
				calibrated[i] = coord[i] * calibration[i] * downsampleFactors[i];
			SpotImp spot = new SpotImp(calibrated);
			spot.setName("Spot "+index);
			index++;
			spots.add(spot);
		}
		return spots;
	}


}
