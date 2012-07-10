package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.outofbounds.OutOfBoundsStrategyValueFactory;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.util.TMUtils;

public class DogSegmenter<T extends RealType<T>> extends AbstractSpotSegmenter<T> {

	/*
	 * FIELDS
	 */
	
	public final static String BASE_ERROR_MESSAGE = "DogSegmenter: ";
	
	private LogSegmenterSettings settings;
	
	/*
	 * CONSTRUCTOR
	 */
	
	public DogSegmenter() {
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}
	

	/*
	 * METHODS
	 */
	
	public SpotSegmenter<T> createNewSegmenter() {
		return new DogSegmenter<T>();
	};

	@Override
	public SegmenterSettings createDefaultSettings() {
		return new LogSegmenterSettings();
	}
	
	@Override
	public void setTarget(Img<T> image, float[] calibration, SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);
		this.settings = (LogSegmenterSettings) settings;
	}

	@Override
	public boolean process() {
		
		// Deal with median filter:
		Img<T> intermediateImg = applyMedianFilter(img);;
		if (settings.useMedianFilter) {
			intermediateImg = applyMedianFilter(intermediateImg);
			if (null == intermediateImg) {
				return false;
			}
		}
		
		float radius = settings.expectedRadius;
		// First we need an image factory for FloatType
		final ImgFactory<FloatType> imageFactory =img.factory().imgFactory(new FloatType());
		
		// And the out of bounds strategies for both types. It needs to be a value-oobs, with a constant
		// value of 0; otherwise, we will miss maxima on the border of the image.
		final OutOfBoundsConstantValueFactory<FloatType, Img<FloatType>> oobs2 = new OutOfBoundsConstantValueFactory<FloatType, Img<FloatType>>(new FloatType(0f));
		
		float sigma1, sigma2, minPeakValue;
		sigma1 = (float) (2 / (1+Math.sqrt(2)) *  radius); // / Math.sqrt(img.getNumDimensions())); // in physical unit
		sigma2 = (float) (Math.sqrt(2) * sigma1);
		minPeakValue = 0; // settings.threshold;
		
		final DifferenceOfGaussianRealNI<T, FloatType> dog = new DifferenceOfGaussianRealNI<T, FloatType>(intermediateImg, imageFactory, oobs2, sigma1, sigma2, minPeakValue, 1.0, calibration);
		/* The DogSegmenter class will be called in a multi-threaded way, so the DifferenceOfGaussianRealNI
		 * does not need to be multi-threaded. On top of that, reports from users on win32 platform 
		 * indicate that multi-threading generates some silent problems, with some frames (first ones
		 * being not present in the final SpotColleciton. 
		 * On 64-bit platforms, I could see that keeping the DogRNI multi-threaded translated by a 
		 * speedup of about 10%, which I sacrifice without hesitation if i can make the plugin more stable. */
		dog.setNumThreads(1);
		
		// Keep laplace image if needed
		if (settings.doSubPixelLocalization)
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
			if (cursor.get().getRealFloat() < settings.threshold)
				continue;
			
			pruned_list.add(dogpeak);
		}
		
		// Deal with sub-pixel localization if required
		if (settings.doSubPixelLocalization && pruned_list.size() > 0) {
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
			float[] coords = new float[3];
			if (settings.doSubPixelLocalization) {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogpeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.numDimensions(); i++) 
					coords[i] = dogpeak.getPosition(i) * calibration[i];
			}
			Spot spot = new SpotImp(coords);
			spot.putFeature(Spot.QUALITY, -dogpeak.getValue().get());
			spot.putFeature(Spot.RADIUS, settings.expectedRadius);
			spots.add(spot);
		}
		
		// Prune overlapping spots
		spots = TMUtils.suppressSpots(spots, Spot.QUALITY);
		
		return true;
	}
	
	@Override
	public String toString() {
		return "DoG segmenter";
	}
	
	@Override
	public String getInfoText() {
		return "<html>" +
				"This segmenter is based on an approximation of the LoG operator <br> " +
				"by differences of gaussian (DoG). Computations are made in direct space. <br>" +
				"It is the quickest for small spot sizes (< ~5 pixels). " +
				"<p> " +
				"Spots found too close are suppressed. This segmenter can do sub-pixel <br>" +
				"localization of spots using a quadratic fitting scheme. It is based on <br>" +
				"the scale-space framework made by Stephan Preibisch for ImgLib. " +
				"</html>";	
	}

}
