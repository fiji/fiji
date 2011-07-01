package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.fft.FourierConvolution;
import mpicbg.imglib.algorithm.math.PickImagePeaks;
import mpicbg.imglib.container.array.ArrayContainerFactory;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class PeakPickerSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "PeakPickerSegmenter: ";
	
	
	
	/*
	 * CONSTRUCTORS
	 */
	
	public PeakPickerSegmenter(SegmenterSettings segmenterSettings) {
		super(segmenterSettings);
		baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	
	/*
	 * METHODS
	 */
	

	@Override
	public boolean process() {
		
		// Deal with median filter:
		intermediateImage = img;
		if (settings.useMedianFilter)
			if (!applyMedianFilter())
				return false;
		
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
		
		// Create spots
		LocalizableByDimCursor<T> cursor = intermediateImage.createLocalizableByDimCursor();
		ArrayList<int[]> peaks = peakPicker.getPeakList();
		spots.clear();
		for(int[] peak : peaks) {
			cursor.setPosition(peak);
			if (cursor.getType().getRealFloat() < settings.threshold)
				break; // because peaks are sorted, we can exit loop here
			float[] coords = new float[3];
			for (int i = 0; i < img.getNumDimensions(); i++) 
				coords[i] = peak[i] * calibration[i];
			Spot spot = new SpotImp(coords);
			cursor.setPosition(peak);
			spot.putFeature(SpotFeature.QUALITY, cursor.getType().getRealFloat());
			spot.putFeature(SpotFeature.RADIUS, settings.expectedRadius);
			spots.add(spot);
		}
		
		return true;
	}

	
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
