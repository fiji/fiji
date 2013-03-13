package fiji.plugin.trackmate.features.spot;

//import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.KURTOSIS;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MAX_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MEAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MEDIAN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.MIN_INTENSITY;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.STANDARD_DEVIATION;
import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.TOTAL_INTENSITY;

import java.util.Iterator;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.SpotNeighborhood;
//import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.SKEWNESS;
//import static fiji.plugin.trackmate.features.spot.SpotIntensityAnalyzerFactory.VARIANCE;

public class SpotIntensityAnalyzer<T extends RealType<T>> extends IndependentSpotFeatureAnalyzer<T> {

	public SpotIntensityAnalyzer(ImgPlus<T> img, Iterator<Spot> spots) {
		super(img, spots);
	}

	/*
	 * PUBLIC METHODS
	 */
	

	/**
	 * Compute descriptive statistics items for this spot. Implementation follows
	 * {@link http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance}.
	 */
	@Override
	public final void process(Spot spot) {

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
//		double kurtosis = (n*M4) / (M2*M2) - 3;
//		double skewness = Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0);
		
		spot.putFeature(MEDIAN_INTENSITY, median);
		spot.putFeature(MIN_INTENSITY, min);
		spot.putFeature(MAX_INTENSITY, max);
		spot.putFeature(MEAN_INTENSITY, mean);
//		spot.putFeature(VARIANCE, variance);
		spot.putFeature(STANDARD_DEVIATION, Math.sqrt(variance));
		spot.putFeature(TOTAL_INTENSITY, sum);
//		spot.putFeature(KURTOSIS, kurtosis);
//		spot.putFeature(SKEWNESS, skewness);
	}
}