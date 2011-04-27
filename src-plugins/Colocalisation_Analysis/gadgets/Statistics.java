package gadgets;

import java.util.List;

/**
 * This class provides some basic statistics methods.
 *
 * @author Tom Kazimiers
 *
 */
public class Statistics {

	// have the inverted square root of two ready to use
	static final double invSqrtTwo = 1.0 / Math.sqrt(2);

	/**
	 * Calculates an estimate of the upper tail cumulative normal distribution
	 * (which is simply the complementary error function with linear scalings
	 * of x and y axis).
	 *
	 * Fractional error in math formula less than 1.2 * 10 ^ -7.
	 * although subject to catastrophic cancellation when z in very close to 0
	 *
	 * Code from (thanks to Bob Dougherty):
	 * w
	 *
	 * Original algorithm from Section 6.2 of Numerical Recipes
	 */
	public static double erf(double z) {
		double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

		// use Horner's method
		double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
						    t * ( 1.00002368 +
						    t * ( 0.37409196 +
						    t * ( 0.09678418 +
						    t * (-0.18628806 +
						    t * ( 0.27886807 +
						    t * (-1.13520398 +
						    t * ( 1.48851587 +
						    t * (-0.82215223 +
						    t * ( 0.17087277))))))))));
		if (z >= 0)
			return  ans;
		else
			return -ans;
	}

	/**
	 * Calculates phi, which is the area of the Gaussian standard
	 * distribution from minus infinity to the query value in units
	 * of standard derivation.
	 * The formula is:
	 *
	 *          1 + erf( z / sqrt(2) )
	 * Phi(z) = ----------------------
	 *                   2
	 * @param z The point of interest
	 * @return phi
	 */
	public static double phi(double z) {
		return 0.5 * (1.0 + erf( z * invSqrtTwo ) );
	}

	/**
	 * Calculates phi, but with a Gaussian distribution defined by
	 * its mean and its standard derivation. This is a quantile.
	 *
	 *                      1 + erf( (z - mean) / (sqrt(2) * stdDev) )
	 * Phi(z,mean,stdDev) = ------------------------------------------
	 *                                          2	 *
	 * @param z The point of interest
	 * @param mean The mean of the distribution
	 * @param sd The standard derivation of the distribution
	 * @return phi
	 */
	public static double phi(double z, double mean, double sd) {
		return phi( (z - mean) / sd);
	}

	/**
	 * Calculates the standard deviation of a vist of values.
	 *
	 * @param values The list of values.
	 * @return The standard deviation.
	 */
	public static double stdDeviation(List<Double> values) {
		int count = values.size();
		// calculate mean
		double sum = 0;
		for( Double val : values ) {
			sum += val;
		}
		double mean = sum / count;

		// calculate deviates
		sum = 0;
		for( Double val : values ) {
			double diff = val - mean;
			double sqDiff = diff * diff;
			sum += sqDiff;
		}
		double stdDeviation = Math.sqrt( sum / (count - 1) );

		return stdDeviation;
	}
}
