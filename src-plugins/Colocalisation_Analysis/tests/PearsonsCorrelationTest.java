package tests;

import static org.junit.Assert.assertTrue;
import gadgets.MaskFactory;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import mpicbg.imglib.cursor.special.TwinValueRangeCursorFactory;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
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
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.createLocalizableByDimCursor(),
				zeroCorrelationImageCh2.createLocalizableByDimCursor(),
				zeroCorrelationAlwaysTrueMask.createLocalizableCursor());
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
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.createLocalizableByDimCursor(),
				positiveCorrelationImageCh2.createLocalizableByDimCursor(),
				positiveCorrelationAlwaysTrueMask.createLocalizableCursor());
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
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.createLocalizableByDimCursor(),
				zeroCorrelationImageCh2.createLocalizableByDimCursor(),
				zeroCorrelationAlwaysTrueMask.createLocalizableCursor());
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
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.createLocalizableByDimCursor(),
				positiveCorrelationImageCh2.createLocalizableByDimCursor(),
				positiveCorrelationAlwaysTrueMask.createLocalizableCursor());
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation
			.classicPearsons(cursor, positiveCorrelationImageCh1Mean, positiveCorrelationImageCh2Mean);
		// check Pearsons R is close to 0.75
		assertTrue(pearsonsR > 0.745 && pearsonsR < 0.755 );
	}

	/**
	 * Tests Pearson's correlation stays close to zero for image pairs with the same mean and spread
	 * of randomized pixel values around that mean.
	 */
	@Test
	public void differentMeansTest() throws MissingPreconditionException {
		final double initialMean = 0.2;
		final double spread = 0.1;
		final double[] sigma = new double[] {3.0, 3.0};

		Image<BitType> mask = MaskFactory.createMask(new int[] {512, 512}, true);

		for (double mean = initialMean; mean < 1; mean += spread) {
			Image<FloatType> ch1 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma);
			Image<FloatType> ch2 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma);

			// create a twin value range cursor that iterates over all pixels of the input data
			TwinCursor<FloatType> cursor = new TwinCursor<FloatType>(ch1.createLocalizableByDimCursor(),
					ch2.createLocalizableByDimCursor(), mask.createLocalizableCursor());
			double resultFast = PearsonsCorrelation.fastPearsons(cursor);
			assertTrue(Math.abs(resultFast) < 0.1);

			/* This test will throw Missing PreconsitionException, as the means are the same
			 * which causes a numerical problem in the classic implementation of Pearson's
			 * double resultClassic = PearsonsCorrelation.classicPearsons(cursor, mean, mean);
			 * assertTrue(Math.abs(resultClassic) < 0.1);
			 */
		}
	}
}
