package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import mpicbg.imglib.algorithm.gauss.DifferenceOfGaussianRealNI;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussian;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyFactory;
import mpicbg.imglib.outofbounds.OutOfBoundsStrategyMirrorFactory;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
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
	public void setTarget(Image<T> image, float[] calibration, SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);
		this.settings = (LogSegmenterSettings) settings;
	}

	@Override
	public boolean process() {
		
		// Deal with median filter:
		Image<T> intermediateImage = applyMedianFilter(img);;
		if (settings.useMedianFilter) {
			intermediateImage = applyMedianFilter(intermediateImage);
			if (null == intermediateImage) {
				return false;
			}
		}
		
		float radius = settings.expectedRadius;
		// first we need an image factory for FloatType
		final ImageFactory<FloatType> imageFactory = new ImageFactory<FloatType>( new FloatType(), img.getContainerFactory() );
		
		// and the out of bounds strategies for both types
		final OutOfBoundsStrategyFactory<FloatType> oobs2 = new OutOfBoundsStrategyMirrorFactory<FloatType>();
		
		float sigma1, sigma2, minPeakValue;
		sigma1 = (float) (2 / (1+Math.sqrt(2)) *  radius); // / Math.sqrt(img.getNumDimensions())); // in physical unit
		sigma2 = (float) (Math.sqrt(2) * sigma1);
		minPeakValue = 0; // settings.threshold;
		
		final DifferenceOfGaussianRealNI<T, FloatType> dog = new DifferenceOfGaussianRealNI<T, FloatType>(intermediateImage, imageFactory, oobs2, sigma1, sigma2, minPeakValue, 1.0, calibration);
		
		// Keep laplace image if needed
		if (settings.doSubPixelLocalization)
			dog.setKeepDoGImage(true);
		
		// Execute
		if ( !dog.checkInput() || !dog.process() )	{
			errorMessage = baseErrorMessage + dog.getErrorMessage();
			return false;
		}
				
		// Get all peaks
		List<DifferenceOfGaussianPeak<FloatType>> list = dog.getPeaks();
		LocalizableByDimCursor<T> cursor = img.createLocalizableByDimCursor();
		
		// Prune non-relevant peaks
		List<DifferenceOfGaussianPeak<FloatType>> pruned_list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();
		for(DifferenceOfGaussianPeak<FloatType> dogpeak : list) {
			if ( (dogpeak.getPeakType() != DifferenceOfGaussian.SpecialPoint.MAX))
				continue;
			cursor.setPosition(dogpeak);
			if (cursor.getType().getRealFloat() < settings.threshold)
				continue;
			
			pruned_list.add(dogpeak);
		}
		
		// Deal with sub-pixel localization if required
		if (settings.doSubPixelLocalization && pruned_list.size() > 0) {
			Image<FloatType> laplacian = dog.getDoGImage();
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
				for (int i = 0; i < img.getNumDimensions(); i++) 
					coords[i] = dogpeak.getSubPixelPosition(i) * calibration[i];
			} else {
				for (int i = 0; i < img.getNumDimensions(); i++) 
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
