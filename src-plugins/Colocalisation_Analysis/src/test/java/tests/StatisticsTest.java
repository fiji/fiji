package tests;

import static org.junit.Assert.assertTrue;
import gadgets.Statistics;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * This class contains JUnit 4 test cases for the implementation
 * of different statistics methods.
 *
 * @author Tom Kazimiers
 */
public class StatisticsTest {
	/**
	 * Test the error function. Test values have been taken from:
	 * http://en.wikipedia.org/wiki/Error_function
	 */
	@Test
	public void erfTest() {
		// erf(0) = 0
		double erf = Statistics.erf(0.0);
		assertTrue( Math.abs(erf) < 0.0001);
		// erf(0.5) = 0.5204999
		erf = Statistics.erf(0.5);
		assertTrue( Math.abs(erf - 0.5204999) < 0.0001);
		// erf(1.0) = 0.8427008
		erf = Statistics.erf(1.0);
		assertTrue( Math.abs(erf - 0.8427008) < 0.0001);
		// erf(-0.5) = -0.5204999
		erf = Statistics.erf(-0.5);
		assertTrue( Math.abs(erf + 0.5204999) < 0.0001);
		// erf(-1.0) = -0.8427008
		erf = Statistics.erf(-1.0);
		assertTrue( Math.abs(erf + 0.8427008) < 0.0001);
	}

	/**
	 * Test the cumulative distribution function (phi) for the
	 * standard normal distribution.
	 */
	@Test
	public void phiTest() {
		// phi(0) = 0.5
		double phi = Statistics.phi(0.0);
		assertTrue( Math.abs(phi - 0.5) < 0.0001 );
		// phi(-1) = 0.158655
		phi = Statistics.phi(-1.0);
		assertTrue( Math.abs(phi - 0.158655) < 0.0001 );
		// phi(0.5) = 0.691462
		phi = Statistics.phi(0.5);
		assertTrue( Math.abs(phi - 0.691462) < 0.0001 );
		// phi(1.960) = 0.975002
		phi = Statistics.phi(1.960);
		assertTrue( Math.abs(phi - 0.975002) < 0.0001 );
	}

	/**
	 * Test the cumulative distribution function (phi) for the
	 * normal distribution (based on mean and standard derivation).
	 */
	@Test
	public void phiDifferentDistributionTest() {
		// phi(0.5, 0, 1) =  0.691462
		double phi = Statistics.phi(0.5, 0.0, 1.0);
		assertTrue( Math.abs(phi - 0.691462) < 0.0001 );
		// phi(0.5, 20, 12) =  0.052081
		phi = Statistics.phi(0.5, 20, 12);
		assertTrue( Math.abs(phi - 0.052081) < 0.0001 );
		// phi(-1, 42, 33) =  0.096282
		phi = Statistics.phi(-1, 42, 33);
		assertTrue( Math.abs(phi - 0.096282) < 0.0001 );
	}

	/**
	 * Tests the calculation of the standard deviation of a list
	 * of values.
	 */
	@Test
	public void stdDeviationTest() {
		/* the standard deviation of the list
		 * [1, 3, 4, 6, 9, 19] is 6.48074069
		 */
		List<Double> values = Arrays.asList( new Double[] {1.0, 3.0, 4.0, 6.0, 9.0, 19.0} );
		double sd = Statistics.stdDeviation(values);
		assertTrue( Math.abs( sd - 6.48074069 ) < 0.0001);
	}
}