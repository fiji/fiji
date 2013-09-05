package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussian;
import net.imglib2.algorithm.legacy.scalespace.DifferenceOfGaussianPeak;
import net.imglib2.algorithm.legacy.scalespace.SubpixelLocalization;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorExpWindowingFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

public class DogDetector <T extends RealType<T>  & NativeType<T>> extends LogDetector<T> {

	/*
	 * CONSTANTS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "DogDetector: ";
	
	/*
	 * CONSTRUCTOR
	 */
	
	public DogDetector(final ImgPlus<T> img, final double radius, final double threshold, final boolean doSubPixelLocalization, final boolean doMedianFilter) {
		super(img, radius, threshold, doSubPixelLocalization, doMedianFilter);
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	

	/*
	 * METHODS
	 */
	

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
		
		final double[] calibration = TMUtils.getSpatialCalibration(img);
		
		// First we need an image factory 
		ImgFactory<T> imageFactory = img.factory();
		
		// And the out of bounds strategies for both types. It needs to be a value-oobs, with a constant
		// value of 0; otherwise, we will miss maxima on the border of the image.
		final OutOfBoundsFactory<T, RandomAccessibleInterval<T>> oobs2 = new OutOfBoundsMirrorExpWindowingFactory<T, RandomAccessibleInterval<T>>();
		
		double[] sigma1 = new double[img.numDimensions()];
		double[] sigma2 = new double[img.numDimensions()];
		for (int i = 0; i < sigma2.length; i++) {
			sigma1[i] = 2 / (1+Math.sqrt(img.numDimensions())) *  radius / calibration[i]; // in pixel units 
			sigma2[i] = Math.sqrt(img.numDimensions()) * sigma1[i];
		}
		
		final DifferenceOfGaussian<T> dog = new DifferenceOfGaussian<T>(intermediateImage, imageFactory, oobs2, sigma1, sigma2, threshold, 1.0);
		/* The DogDetector class will be called in a multi-threaded way, so the DifferenceOfGaussianRealNI
		 * does not need to be multi-threaded. On top of that, reports from users on win32 platform 
		 * indicate that multi-threading generates some silent problems, with some frames (first ones
		 * being not present in the final SpotColleciton. 
		 * On 64-bit platforms, I could see that keeping the DogRNI multi-threaded translated by a 
		 * speedup of about 10%, which I sacrifice without hesitation if i can make the trackmate more stable. */
		dog.setNumThreads(1);
		
		// Keep laplace image if needed
		if (doSubPixelLocalization)
			dog.setKeepDoGImg(true);
		
		// Execute
		if ( !dog.checkInput() || !dog.process() )	{
			errorMessage = baseErrorMessage + dog.getErrorMessage();
			return false;
		}
				
		// Get all peaks
		List<DifferenceOfGaussianPeak<FloatType>> list = dog.getPeaks();
		RandomAccess<T> cursor = img.randomAccess();
		
		// Prune non-relevant peaks
		List<DifferenceOfGaussianPeak<FloatType>> pruned_list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : list) {
			if ( (dogpeak.getPeakType() != DifferenceOfGaussian.SpecialPoint.MAX))
				continue;
			cursor.setPosition(dogpeak);
			if (cursor.get().getRealDouble() < threshold)
				continue;
			
			pruned_list.add(dogpeak);
		}
		
		// Deal with sub-pixel localization if required
		if (doSubPixelLocalization && pruned_list.size() > 0) {
			Img<FloatType> laplacian = dog.getDoGImg();
			SubpixelLocalization<FloatType> locator = new SubpixelLocalization<FloatType>(laplacian , pruned_list);
			locator.setNumThreads(1); // Since the calls to a segmenter  are already multi-threaded.
			if ( !locator.checkInput() || !locator.process() )	{
				errorMessage = baseErrorMessage + locator.getErrorMessage();
				return false;
			}
			pruned_list = locator.getDoGPeaks();
		}

		// Create spots
		spots.clear();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : pruned_list) {
			double[] coords = new double[3];
			if (doSubPixelLocalization) {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogpeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogpeak.getDoublePosition(i) * calibration[i];
			}
			Spot spot = new Spot(coords);
			spot.putFeature(Spot.QUALITY, Double.valueOf(-dogpeak.getValue().get()));
			spot.putFeature(Spot.RADIUS, radius);
			spots.add(spot);
		}
		
		// Prune overlapping spots
		spots = TMUtils.suppressSpots(spots, Spot.QUALITY);
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		
		return true;
	}
}
