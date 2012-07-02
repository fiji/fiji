// MathUtils.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * Handy utility functions which have some Mathematical relavance.
 *
 * @author Matthew Goode
 * @author Alexei Drummond
 *
 * @version $Id: MathUtils.java,v 1.1 2006/06/01 15:27:32 gene099 Exp $
 */


public class MathUtils {

	public MathUtils() {}

	/**
	 * A random number generator that is initialized with the clock when this
	 * class is loaded into the JVM. Use this for all random numbers.
	 * @note This method or getting random numbers in not thread-safe. Since
	 * MersenneTwisterFast is currently (as of 9/01) not synchronized using
	 * this function may cause concurrency issues. Use the static get methods of the
	 * MersenneTwisterFast class for access to a single instance of the class, that
	 * has synchronization.
	 */
	public static MersenneTwisterFast random = new MersenneTwisterFast();

	/**
	 * @return a new double array where all the values sum to 1.
	 * Relative ratios are preserved.
	 */
	public static final double[] getNormalized(double[] array) {
		double[] newArray = new double[array.length];
		double total = getTotal(array);
		for(int i = 0 ; i < array.length ; i++) {
			newArray[i] = array[i]/total;
		}
		return newArray;
	}

	/**
	 * @param end the index of the element after the last one to be included
	 * @return the total of a the values in a range of an array
	 */
	public static final double getTotal(double[] array, int start, int end) {
		double total = 0.0;
		for(int i = start ; i < array.length ; i++) {
			total+=array[i];
		}
		return total;
	}

	/**
	 * @return the total of the values in an array
	 */
	public static final double getTotal(double[] array) {
		return getTotal(array,0, array.length);

	}
	/**
	 * @return a set of valid, but randomly generated, arguments for a particular MultivariateFunction
	 */
	public static final double[] getRandomArguments(MultivariateFunction mf) {
		double[] values = new double[mf.getNumArguments()];
		for(int i = 0; i < values.length ; i++) {
			double min = mf.getLowerBound(i);
			double max = mf.getUpperBound(i);
			values[i] = (max-min)*MersenneTwisterFast.getNextDouble()+min;
		}
		return values;
	}
}
