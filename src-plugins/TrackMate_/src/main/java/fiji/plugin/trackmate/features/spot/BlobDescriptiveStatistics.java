package fiji.plugin.trackmate.features.spot;

import java.util.ArrayList;
import java.util.HashMap;

import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;

public class BlobDescriptiveStatistics<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	/*
	 * CONSTANTS
	 */
	
	public static final String NAME = "Spot descriptive statistics";
	
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
	@Override
	public void process(Spot spot) {

		// Prepare neighborhood
		SpotNeighborhood<T> neighborhood = new SpotNeighborhood<T>(spot, img);
		final int npixels = (int) neighborhood.size();

		// For variance, kurtosis and skewness 
		double sum = 0;
		
		double mean = 0;
	    double M2 = 0;
	    double M3 = 0;
	    double M4 = 0;
	    double delta, delta_n, delta_n2;
	    double term1;
	    int n1;
		
	    // Others
		double val;
		final double[] pixel_values = new double[npixels];
		int n = 0;
		
		for ( T pixel : neighborhood ) {
			
			val = pixel.getRealDouble();
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
		double median = pixel_values[npixels/2];
		double min = pixel_values[0];
		double max = pixel_values[npixels-1];
		mean = sum / npixels;
		double variance = M2 / (npixels-1);
		double kurtosis = (n*M4) / (M2*M2) - 3;
		double skewness = Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0);
		
		spot.putFeature(MEDIAN_INTENSITY, median);
		spot.putFeature(MIN_INTENSITY, min);
		spot.putFeature(MAX_INTENSITY, max);
		spot.putFeature(MEAN_INTENSITY, mean);
		spot.putFeature(VARIANCE, variance);
		spot.putFeature(STANDARD_DEVIATION, Math.sqrt(variance));
		spot.putFeature(TOTAL_INTENSITY, sum);
		spot.putFeature(KURTOSIS, kurtosis);
		spot.putFeature(SKEWNESS, skewness);
	}
}