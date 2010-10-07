package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;

import mpicbg.imglib.algorithm.gauss.DifferenceOfGaussian;
import mpicbg.imglib.algorithm.gauss.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.gauss.DifferenceOfGaussianRealNI;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;

public class DogSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "DogSegmenter: ";
	
	
	
	/*
	 * CONSTRUCTOR
	 */
	
	public DogSegmenter(SegmenterSettings segmenterSettings) {
		super(segmenterSettings);
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	
	

	/*
	 * METHODS
	 */

	@Override
	public boolean process() {
		
		float radius = settings.expectedRadius;
		// first we need an image factory for FloatType
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), img.getContainerFactory() );
		
		// and the out of bounds strategies for both types
		final OutOfBoundsStrategyFactory<FloatType> oobs2 = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		
		float sigma1, sigma2, minPeakValue;
		sigma1 = (float) (2 / (1+Math.sqrt(2)) *  radius); // / Math.sqrt(img.getNumDimensions())); // in physical unit
		sigma2 = (float) (Math.sqrt(2) * sigma1);
		minPeakValue = settings.threshold;
		
		final DifferenceOfGaussianRealNI<T, FloatType> dog = new DifferenceOfGaussianRealNI<T, FloatType>(img, imageFactory, oobs2, sigma1, sigma2, minPeakValue, 1.0, calibration);
		// execute
		if ( !dog.checkInput() || !dog.process() )
		{
			errorMessage = baseErrorMessage + dog.getErrorMessage();
			return false;
		}
				
		// get all peaks
		final ArrayList<DifferenceOfGaussianPeak<FloatType>> list = dog.getPeaks();

		// Create spots
		spots.clear();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : list) {
			
			if (dogpeak.getPeakType() != DifferenceOfGaussian.SpecialPoint.MAX)
				continue;	
			
			float[] coords = new float[3];
			for (int i = 0; i < img.getNumDimensions(); i++) 
				coords[i] = dogpeak.getPosition(i) * calibration[i];
			Spot spot = new SpotImp(coords);
			spot.putFeature(Feature.QUALITY, -dogpeak.getValue().get());
			spots.add(spot);
		}
		return true;
	}
}
