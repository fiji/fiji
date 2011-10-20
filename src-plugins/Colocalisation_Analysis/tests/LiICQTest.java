package tests;

import static org.junit.Assert.assertTrue;

import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;

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
				positiveCorrelationImageCh1.createLocalizableByDimCursor(),
				positiveCorrelationImageCh2.createLocalizableByDimCursor(),
				positiveCorrelationAlwaysTrueMask.createLocalizableCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, positiveCorrelationImageCh1Mean,
					positiveCorrelationImageCh2Mean);
		assertTrue(icq > 0.34 && icq < 0.35);
		cursor.close();
	}

	/**
	 * Checks Li's ICQ value for zero correlated images.
	 */
	@Test
	public void liZeroCorrTest() {
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.createLocalizableByDimCursor(),
				zeroCorrelationImageCh2.createLocalizableByDimCursor(),
				zeroCorrelationAlwaysTrueMask.createLocalizableCursor());
		// calculate Li's ICQ value
		double icq = LiICQ.calculateLisICQ(cursor, zeroCorrelationImageCh1Mean,
					zeroCorrelationImageCh2Mean);
		assertTrue(icq > -0.01 && icq < 0.01);
		cursor.close();
	}
}
