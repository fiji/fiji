package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.gauss.DifferenceOfGaussianRealNI;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class DogSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "DogSegmenter: ";
	
	private boolean doSubPixelLocalization = false;
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public DogSegmenter(SegmenterSettings segmenterSettings) {
		super(segmenterSettings);
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
		this.doSubPixelLocalization = ((DogSegmenterSettings) segmenterSettings).doSubPixelLocalization;
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
		// first we need an image factory for FloatType
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), img.getContainerFactory() );
		
		// and the out of bounds strategies for both types
		final OutOfBoundsStrategyFactory<FloatType> oobs2 = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		
		float sigma1, sigma2, minPeakValue;
		sigma1 = (float) (2 / (1+Math.sqrt(2)) *  radius); // / Math.sqrt(img.getNumDimensions())); // in physical unit
		sigma2 = (float) (Math.sqrt(2) * sigma1);
		minPeakValue = settings.threshold;
		
		final DifferenceOfGaussianRealNI<T, FloatType> dog = new DifferenceOfGaussianRealNI<T, FloatType>(intermediateImage, imageFactory, oobs2, sigma1, sigma2, minPeakValue, 1.0, calibration);
		
		// Keep laplace image if needed
		if (doSubPixelLocalization)
			dog.setKeepDoGImage(true);
		
		// Execute
		if ( !dog.checkInput() || !dog.process() )	{
			errorMessage = baseErrorMessage + dog.getErrorMessage();
			return false;
		}
				
		// Get all peaks
		List<DifferenceOfGaussianPeak<FloatType>> list = dog.getPeaks();
		
		// Prune non-relevant peaks
		List<DifferenceOfGaussianPeak<FloatType>> pruned_list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : list) {
			if (dogpeak.getPeakType() != DifferenceOfGaussian.SpecialPoint.MAX)
				continue;
			pruned_list.add(dogpeak);
		}

		// Deal with sub-pixel localization if required
		if (doSubPixelLocalization) {
			Image<FloatType> laplacian = dog.getDoGImage();
			SubpixelLocalization<FloatType> locator = new SubpixelLocalization<FloatType>(laplacian , pruned_list);
			if ( !locator.checkInput() || !locator.process() )	{
				errorMessage = baseErrorMessage + locator.getErrorMessage();
				return false;
			}
			pruned_list = locator.getDoGPeaks();
		}

		// Create spots
		spots.clear();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : pruned_list) {
			float[] coords = new float[3];
			if (doSubPixelLocalization) {
				for (int i = 0; i < img.getNumDimensions(); i++) 
					coords[i] = dogpeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.getNumDimensions(); i++) 
					coords[i] = dogpeak.getPosition(i) * calibration[i];
			}
			Spot spot = new SpotImp(coords);
			spot.putFeature(SpotFeature.QUALITY, -dogpeak.getValue().get());
			spots.add(spot);
		}
		return true;
	}
}
