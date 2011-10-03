package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mpicbg.imglib.cursor.special.DiscCursor;
import mpicbg.imglib.cursor.special.DomainCursor;
import mpicbg.imglib.cursor.special.SphereCursor;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.util.Util;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;

public class BlobDescriptiveStatistics extends IndependentSpotFeatureAnalyzer {

	/*
	 * CONSTANTS
	 */
	
	public static final String	MEAN_INTENSITY 	= "MEAN_INTENSITY";
	public static final String	MEDIAN_INTENSITY = "MEDIAN_INTENSITY";
	public static final String	MIN_INTENSITY = "MIN_INTENSITY";
	public static final String	MAX_INTENSITY = "MAX_INTENSITY";
	public static final String	TOTAL_INTENSITY = "TOTAL_INTENSITY";
	public static final String	STANDARD_DEVIATION = "STANDARD_DEVIATION";
	public static final String	VARIANCE = "VARIANCE";
	public static final String	KURTOSIS = "KURTOSIS";
	public static final String	SKEWNESS = "SKEWNESS";
	
	public static final ArrayList<String> 			FEATURES = new ArrayList<String>(9);
	public static final HashMap<String, String> 	FEATURE_NAMES = new HashMap<String, String>(9);
	public static final HashMap<String, String> 	FEATURE_SHORT_NAMES = new HashMap<String, String>(9);
	public static final HashMap<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(9);
	static {
		FEATURES.add(MEAN_INTENSITY);
		FEATURES.add(MEDIAN_INTENSITY);
		FEATURES.add(MIN_INTENSITY);
		FEATURES.add(MAX_INTENSITY);
		FEATURES.add(TOTAL_INTENSITY);
		FEATURES.add(STANDARD_DEVIATION);
		FEATURES.add(VARIANCE);
		FEATURES.add(KURTOSIS);
		FEATURES.add(SKEWNESS);

		FEATURE_NAMES.put(MEAN_INTENSITY, "Mean intensity");
		FEATURE_NAMES.put(MEDIAN_INTENSITY, "Median intensity");
		FEATURE_NAMES.put(MIN_INTENSITY, "Minimal intensity");
		FEATURE_NAMES.put(MAX_INTENSITY, "Maximal intensity");
		FEATURE_NAMES.put(TOTAL_INTENSITY, "Total intensity");
		FEATURE_NAMES.put(STANDARD_DEVIATION, "Standard deviation");
		FEATURE_NAMES.put(VARIANCE, "Variance");
		FEATURE_NAMES.put(KURTOSIS, "Kurtosis");
		FEATURE_NAMES.put(SKEWNESS, "Skewness");
		
		FEATURE_SHORT_NAMES.put(MEAN_INTENSITY, "Mean");
		FEATURE_SHORT_NAMES.put(MEDIAN_INTENSITY, "Median");
		FEATURE_SHORT_NAMES.put(MIN_INTENSITY, "Min");
		FEATURE_SHORT_NAMES.put(MAX_INTENSITY, "Max");
		FEATURE_SHORT_NAMES.put(TOTAL_INTENSITY, "Total int.");
		FEATURE_SHORT_NAMES.put(STANDARD_DEVIATION, "Stdev.");
		FEATURE_SHORT_NAMES.put(VARIANCE, "Var.");
		FEATURE_SHORT_NAMES.put(KURTOSIS, "Kurtosis");
		FEATURE_SHORT_NAMES.put(SKEWNESS, "Skewness");
		
		FEATURE_DIMENSIONS.put(MEAN_INTENSITY, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(MEDIAN_INTENSITY, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(MIN_INTENSITY, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(MAX_INTENSITY, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(TOTAL_INTENSITY, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(STANDARD_DEVIATION, Dimension.INTENSITY);
		FEATURE_DIMENSIONS.put(VARIANCE, Dimension.INTENSITY_SQUARED);
		FEATURE_DIMENSIONS.put(KURTOSIS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(SKEWNESS, Dimension.NONE);
	}
	
	
	/*
	 * PUBLIC METHODS
	 */
	

	/**
	 * Compute descriptive statistics items for this spot. Implementation follows
	 * {@link http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void process(Spot spot) {
		final DomainCursor<? extends RealType<?>> cursor;
		final float[] coords;
		final float radius = spot.getFeature(Spot.RADIUS);
		if (img.getNumDimensions() == 3) {
			cursor = new SphereCursor(img, new float[3], radius, calibration);
			coords = new float[3];
		} else { 
			cursor = new DiscCursor(img, new float[2], radius, calibration);
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
		
		spot.putFeature(MEDIAN_INTENSITY, median);
		spot.putFeature(MIN_INTENSITY, min);
		spot.putFeature(MAX_INTENSITY, max);
		spot.putFeature(MEAN_INTENSITY, mean);
		spot.putFeature(VARIANCE, variance);
		spot.putFeature(STANDARD_DEVIATION, (float) Math.sqrt(variance));
		spot.putFeature(TOTAL_INTENSITY, sum);
		spot.putFeature(KURTOSIS, kurtosis);
		spot.putFeature(SKEWNESS, skewness);
	}

	

	@Override
	public Collection<String> getFeatures() {
		return FEATURES;
	}
	
	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}

	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}

	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	}
}