package tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import algorithms.LiICQ;

/**
 * This class contains JUnit 4 test cases for the calculation of Li's
 * ICQ value.
 *
 * @author Dan White & Tom Kazimiers
 */
public class LiICQTest extends ColocalisationTest {

	/**
	 * Checks Li's ICQ value for positive correlated images.
	 */
	@Test
	public void liPositiveCorrTest() {
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(positiveCorrelationImageCh1, positiveCorrelationImageCh1Mean,
					positiveCorrelationImageCh2, positiveCorrelationImageCh2Mean);
		assertTrue(icq > 0.34 && icq < 0.35);
	}

	/**
	 * Checks Li's ICQ value for zero correlated images.
	 */
	@Test
	public void liZeroCorrTest() {
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(zeroCorrelationImageCh1, zeroCorrelationImageCh1Mean,
					zeroCorrelationImageCh2, zeroCorrelationImageCh2Mean);
		assertTrue(icq > -0.01 && icq < 0.01);
	}
}
