package fiji.plugin.trackmate.features.spot;

import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.util.Util;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;

public class BlobDescriptiveStatistics <T extends RealType<T>> extends IndependentSpotFeatureAnalyzer {

	/*
	 * FIELDS
	 */
	
	/** The original image that is analyzed. */
	private Image<T> img;
	private float[] calibration;
	
	/*
	 * CONSTRUCTORS
	 */
	
	public BlobDescriptiveStatistics(Image<T> originalImage, float[] calibration) {
		this.img = originalImage;
		this.calibration = calibration;
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public SpotFeature getFeature() {
		return SpotFeature.MEAN_INTENSITY;
	}

	/**
	 * Compute descriptive statistics items for this spot. Implementation follows
	 * {@link http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance}.
	 */
	@Override
	public void process(Spot spot) {
		final DomainCursor<T> cursor;
		final float[] coords;
		final float radius = spot.getFeature(SpotFeature.RADIUS);
		if (img.getNumDimensions() == 3) {
			cursor = new SphereCursor<T>(img, new float[3], radius, calibration);
			coords = new float[3];
		} else { 
			cursor = new DiscCursor<T>(img, new float[2], radius, calibration);
			coords = new float[2];
		}
		final int npixels = cursor.getNPixels();

		// For variance, kurtosis and skewness 
		float sum = 0;
		
		float mean = 0;
	    float M2 = 0;
	    float M3 = 0;
	    float M4 = 0;
	    float delta, delta_n, delta_n2;
	    float term1;
	    int n1;
		
	    // Others
		float val;
		final float[] pixel_values = new float[npixels];
		int n = 0;
		
		for (int i = 0; i < coords.length; i++)
			coords[i] = spot.getFeature(Spot.POSITION_FEATURES[i]);
		cursor.moveCenterToCoordinates(coords);
		
		while (cursor.hasNext()) {
			cursor.next();
			val = cursor.getType().getRealFloat();
			// For median, min and max
			pixel_values[n] = val;
			// For variance and mean
			sum += val;
			
			// For kurtosis
			n1 = n;
			n++;
			delta = val - mean;
			delta_n = delta / n;
			delta_n2 = delta_n * delta_n;
			term1 = delta * delta_n * n1;
			mean = mean + delta_n;
			M4 = M4 + term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
	        M3 = M3 + term1 * delta_n * (n - 2) - 3 * delta_n * M2;
	        M2 = M2 + term1;
		}
	
		Util.quicksort(pixel_values, 0, npixels-1);
		float median = pixel_values[npixels/2];
		float min = pixel_values[0];
		float max = pixel_values[npixels-1];
		mean = sum / npixels;
		float variance = M2 / (npixels-1);
		float kurtosis = (n*M4) / (M2*M2) - 3;
		float skewness = (float) ( Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0) );
		
		spot.putFeature(SpotFeature.MEDIAN_INTENSITY, median);
		spot.putFeature(SpotFeature.MIN_INTENSITY, min);
		spot.putFeature(SpotFeature.MAX_INTENSITY, max);
		spot.putFeature(SpotFeature.MEAN_INTENSITY, mean);
		spot.putFeature(SpotFeature.VARIANCE, variance);
		spot.putFeature(SpotFeature.STANDARD_DEVIATION, (float) Math.sqrt(variance));
		spot.putFeature(SpotFeature.TOTAL_INTENSITY, sum);
		spot.putFeature(SpotFeature.KURTOSIS, kurtosis);
		spot.putFeature(SpotFeature.SKEWNESS, skewness);
	}
}