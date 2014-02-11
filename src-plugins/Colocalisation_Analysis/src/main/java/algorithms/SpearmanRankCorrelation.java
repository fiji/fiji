package algorithms;

import gadgets.DataContainer;

import java.util.Arrays;
import java.util.Comparator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import results.ResultHandler;

/*
* This code has been heavily adapted from Numerical Recipces: The Art of Scientific Computing.
* 3rd ed., 2007. Other formulations have been gathered from Wolfram's MathWorld:
* http://mathworld.wolfram.com/SpearmanRankCorrelationCoefficient.html
* 
* Adapted from code written by Dan White and Tom Kazimiers
* 
* @author Leonardo Guizzetti
*/


/**
 * This algorithm calculates Spearman's rank correlation coefficient (Spearman's rho)
 *
 * @param <T>
 */
public class SpearmanRankCorrelation<T extends RealType< T >> extends Algorithm<T> {
	// the resulting Spearman rho value
	static double rhoValue;
	static double tStatisticSpearman;
	static int dfSpearman;

	// create two paired arrays: one with raw pixel values and one for the corresponding ranks
	static double[][] data;
	static double[] ch1raw;
	static double[] ch2raw;
	static double[] ch1ranks;
	static double[] ch2ranks;
	
	public SpearmanRankCorrelation() {
		super("Spearman's Rank Corelation calculation");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {

		// get the 2 images for the calculation of Spearman's rho
		RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());
		// calculate Spearman's rho value
		rhoValue = calculateSpearmanRank(cursor);
	}

	/**
	 * Calculates Spearman's Rank Correlation Coefficient (Spearman's rho) for
	 * two images.
	 *
	 * @param cursor A TwinCursor that iterates over two images
	 * @return Spearman's rank correlation coefficient (rho) value
	 */
	public static <T extends RealType<T>> double calculateSpearmanRank(TwinCursor<T> cursor) {
		
		// Step 0: Count the pixels first.
		int n = 0;
		while (cursor.hasNext()) {
			n++;
			cursor.fwd();
		}
		cursor.reset();
		
		data = new double[n][2];
		
		for (int i = 0; i < n; i++) {
			cursor.fwd();
			T type1 = cursor.getFirst();
			T type2 = cursor.getSecond();
			data[i][0] = type1.getRealDouble();
			data[i][1] = type2.getRealDouble();
		}
		
		return calculateSpearmanRank(data);
	}

	/**
	 * Calculates Spearman's Rank Correlation Coefficient (Spearman's rho) for
	 * two images.
	 *
	 * @param data A 2D array containing the data to be ranked
	 * @return Spearman's rank correlation coefficient (rho) value
	 */
	public static double calculateSpearmanRank(double[][] data) {
		final int n = data.length;
		ch1raw = new double[n];
		ch2raw = new double[n];
		ch1ranks = new double[n];
		ch2ranks = new double[n];

		/**
		 * Here's the concept. Rank-transform the data, then run 
		 * the Pearson correlation on the transformed data.
		 * 
		 * 1) We will sort the dataset by one column, extract the 
		 *    column values and rank them, and replace the data by 
		 *    the ranks.
		 * 2) Repeat the process now with the remaining column.
		 * 3) Calculate the coefficient from the individual rank
		 *    columns, the t-statistic and the df's of the test.
		 */
		
		// Step 1: Sort the raw data, by column #2 (arbitrary choice).
		Arrays.sort(data, new Comparator<double[]>() {
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[1], row2[1]);
			}
		});
		
		for (int i = 0; i < n; i++) {
			ch2raw[i] = data[i][1];
		}
		
		// Rank the data then replace them into the dataset.
		ch2ranks = rankValues(ch2raw);
		for (int i = 0; i < n; i++) {
			data[i][1] = ch2ranks[i];
		}
		
		// Step 2: Repeat step 1 with the other data column.
		Arrays.sort(data, new Comparator<double[]>() {
			@Override
			public int compare(double[] row1, double[] row2) {
				return Double.compare(row1[0], row2[0]);
			}
		});
		
		for (int i = 0; i < n; i++) {
			ch1raw[i] = data[i][0];
		}
		
		ch1ranks = rankValues(ch1raw);
		for (int i = 0; i < n; i++) {
			data[i][0] = ch1ranks[i];
			ch2ranks[i] = data[i][1];
		}
		
		// Step 3: Compute statistics.
		rhoValue = calculateRho(ch1ranks, ch2ranks);
		tStatisticSpearman = getTStatistic(rhoValue, n);
		dfSpearman = getSpearmanDF(n);
		
		return rhoValue;
	}

	/**
	 * Returns degrees of freedom for Spearman's rank correlation.
	 *
	 * @param n - N (number of data pairs)
	 * @return Spearman's rank degrees of freedom.
	 */
	public static int getSpearmanDF(int n) {
		return n - 2;
	}
	
	/**
	 * Returns associated T-Statistic for Spearman's rank correlation.
	 *
	 * @param rho - Spearman's rho
	 * @param n - N (number of data pairs)
	 * @return Spearman's rank correlation t-statistic
	 */
	public static double getTStatistic(double rho, int n) {
		double rho_squared = rho * rho;
		return rho * Math.sqrt( (n - 2) / (1 - rho_squared) );
	}
	
	/**
	 * Returns sorted rankings for a list of sorted values.
	 *
	 * @param rankedVals - The sorted absolute values
	 * @return ranked sorted list of values
	 */
	public static double[] rankValues(double[] sortedVals) {
		
		int len = sortedVals.length;
		int start = 0;
		int end = 0;
		double[] newranks = new double[len];
		double avg = 0, ranksum = 0;
		boolean ties_found = false;
		
		// first assign ranks, ascending from 1
		for (int i=0; i<len; i++) {
			newranks[i] = i+1;
		}
		
		// check for tied values
		for (int i=0; i<len; i++) {
			start = i;
			end = i;
			
			// Advance values while you haven't exceeded the final value in the ranked data,
			// and until we break a tie in values.
			while ((++end < len) && (sortedVals[start] == sortedVals[end])) {
				ties_found = true;
			}
			
			// Check if we advanced our end position
			if ((end-start != 1) && (ties_found)) {

				// Compute arithmetic average of rank according to Spearman's method:
				// average = sum of ranks / number of ranks
				avg = 0;
				ranksum = 0;
				for (int j=start; j<end; j++) {
					ranksum += newranks[j];
				}
				avg = ranksum / (end-start);
			
				// Assign averages to the tied ranks
				for (int x=start; x<end; x++) {
					newranks[x] = avg;
				}
				
				ties_found = false;			
			}
			
			//reset i
			i=end-1;
		}
		
		return newranks;
	}
	

	/**
	 * Calculates Spearman's rho (Pearson's correlation coefficient on ranked data).
	 *
	 * @param x - One array of rankings (may include ties)
	 * @param y - ditto.
	 * @return Spearman's rho.
	 */
	public static double calculateRho(double[] x, double[] y) {
		// Define some variables.
		double rho;
		int len = x.length; // the lengths should be the same for each array
		double mean_x = 0.0, mean_y = 0.0;
		double sum_x = 0.0, sum_y = 0.0;
		double sd_x = 0.0, sd_y = 0.0, sd_xy = 0.0;
		double ssd_x = 0.0, ssd_y = 0.0;
		double denominator = 0.0;
		int i = 0;
		
		// Calculate the mean of each rank set
		for (i = 0; i < len; i++) {
			sum_x += x[i];
			sum_y += y[i];
		}
		mean_x = sum_x / len;
		mean_y = sum_y / len;
		
		for (i = 0; i < len; i++) {
			// Calculate the Sum of Differences (numerator)
			sd_x = x[i] - mean_x;
			sd_y = y[i] - mean_y;
			sd_xy += sd_x * sd_y;
			
			// Calculate the Sum of Squared Difference
			ssd_x += (x[i] - mean_x) * (x[i] - mean_x);
			ssd_y += (y[i] - mean_y) * (y[i] - mean_y);
		}
		
		/** Calculate rho
		 * We calculate it this way rather than the alternative in the case
		 * (1 - [ (6 sum d^2) / (n(n^2 - 1)) ] which is a simplification when
		 * there are no ties in rank transformed data.
		 */
		denominator = Math.sqrt(ssd_x * ssd_y);
		rho = sd_xy / denominator;
		
		/**
		 * Debug print-outs.
		 * System.out.println("n: " + len);
		 * System.out.println("Sum of ch1: " + sum_x + " Sum of ch2: " + sum_y);
		 * System.out.println("Mean of ch1: " + mean_x + " Mean of ch2: " + mean_y);
		 * System.out.println("Numerator, sd_xy: " + sd_xy);
		 * System.out.println("ssd ch1: " + ssd_x + " ssd ch2: " + ssd_y);
		 * System.out.println("denom: " + denominator);
		 * System.out.println("rho: " + rho);
		 */
		return rho;
	}
	
	
	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Spearman's rank correlation value", rhoValue, 8);
		handler.handleValue("Spearman's correlation t-statistic", tStatisticSpearman, 4);
		handler.handleValue("t-statistic degrees of freedom", dfSpearman);
	}
}
