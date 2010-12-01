package tests;

import static org.junit.Assert.assertTrue;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursorFactory;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Test;

import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;

/**
 * This class contains JUnit 4 test cases for the Pearson's correlation
 * implementation.
 *
 * @author Dan White & Tom Kazimiers
 */
public class PearsonsCorrelationTest extends ColocalisationTest {

	/**
	 * Tests if the fast implementation of Pearson's correlation with two
	 * zero correlated images produce a Pearson's R value of about zero.
	 */
	@Test
	public void fastPearsonsZeroCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinValueRangeCursor<FloatType> cursor = TwinValueRangeCursorFactory
				.generateAlwaysTrueCursor(zeroCorrelationImageCh1, zeroCorrelationImageCh2);
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation.fastPearsons(cursor);
		// check Pearsons R is close to zero
		assertTrue(pearsonsR > -0.05 && pearsonsR < 0.05 );
	}

	/**
	 * Tests if the fast implementation of Pearson's correlation with two
	 * positive correlated images produce a Pearson's R value of about 0.75.
	 */
	@Test
	public void fastPearsonsPositiveCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinValueRangeCursor<FloatType> cursor = TwinValueRangeCursorFactory
				.generateAlwaysTrueCursor(positiveCorrelationImageCh1, positiveCorrelationImageCh2);
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation.fastPearsons(cursor);
		// check Pearsons R is close to 0.75
		assertTrue(pearsonsR > 0.745 && pearsonsR < 0.755 );
	}

	/**
	 * Tests if the classic implementation of Pearson's correlation with two
	 * zero correlated images produce a Pearson's R value of about zero.
	 */
	@Test
	public void classicPearsonsZeroCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinValueRangeCursor<FloatType> cursor = TwinValueRangeCursorFactory
				.generateAlwaysTrueCursor(zeroCorrelationImageCh1, zeroCorrelationImageCh2);
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation
			.classicPearsons(cursor, zeroCorrelationImageCh1Mean, zeroCorrelationImageCh2Mean);
		// check Pearsons R is close to zero
		assertTrue(pearsonsR > -0.05 && pearsonsR < 0.05 );
	}

	/**
	 * Tests if the classic implementation of Pearson's correlation with two
	 * positive correlated images produce a Pearson's R value of about 0.75.
	 */
	@Test
	public void classicPearsonsPositiveCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinValueRangeCursor<FloatType> cursor = TwinValueRangeCursorFactory
				.generateAlwaysTrueCursor(positiveCorrelationImageCh1, positiveCorrelationImageCh2);
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation
			.classicPearsons(cursor, positiveCorrelationImageCh1Mean, positiveCorrelationImageCh2Mean);
		// check Pearsons R is close to 0.75
		assertTrue(pearsonsR > 0.745 && pearsonsR < 0.755 );
	}
}
