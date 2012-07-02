package tests;

import static org.junit.Assert.assertTrue;
import net.imglib2.TwinCursor;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;

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
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				Views.iterable(positiveCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, positiveCorrelationImageCh1Mean,
					positiveCorrelationImageCh2Mean);
		assertTrue(icq > 0.34 && icq < 0.35);
	}

	/**
	 * Checks Li's ICQ value for zero correlated images. The ICQ value
	 * should be about zero.
	 */
	@Test
	public void liZeroCorrTest() {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				Views.iterable(zeroCorrelationAlwaysTrueMask).localizingCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, zeroCorrelationImageCh1Mean,
					zeroCorrelationImageCh2Mean);
		assertTrue(Math.abs(icq) < 0.01);
	}
}
