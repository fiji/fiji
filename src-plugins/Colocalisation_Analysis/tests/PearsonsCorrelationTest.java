package tests;

import static org.junit.Assert.assertEquals;
import gadgets.MaskFactory;
import net.imglib2.TwinCursor;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.PearsonsCorrelation.Implementation;

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
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				zeroCorrelationAlwaysTrueMask.localizingCursor());
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation.fastPearsons(cursor);
		// check Pearsons R is close to zero
		assertEquals(0.0, pearsonsR, 0.05);
	}

	/**
	 * Tests if the fast implementation of Pearson's correlation with two
	 * positive correlated images produce a Pearson's R value of about 0.75.
	 */
	@Test
	public void fastPearsonsPositiveCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				positiveCorrelationAlwaysTrueMask.localizingCursor());
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation.fastPearsons(cursor);
		// check Pearsons R is close to 0.75
		assertEquals(0.75, pearsonsR, 0.01);
	}

	/**
	 * Tests if the classic implementation of Pearson's correlation with two
	 * zero correlated images produce a Pearson's R value of about zero.
	 */
	@Test
	public void classicPearsonsZeroCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				zeroCorrelationImageCh1.randomAccess(),
				zeroCorrelationImageCh2.randomAccess(),
				zeroCorrelationAlwaysTrueMask.localizingCursor());
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation
			.classicPearsons(cursor, zeroCorrelationImageCh1Mean, zeroCorrelationImageCh2Mean);
		// check Pearsons R is close to zero
		assertEquals(0.0, pearsonsR, 0.05);
	}

	/**
	 * Tests if the classic implementation of Pearson's correlation with two
	 * positive correlated images produce a Pearson's R value of about 0.75.
	 */
	@Test
	public void classicPearsonsPositiveCorrTest() throws MissingPreconditionException {
		// create a twin value range cursor that iterates over all pixels of the input data
		TwinCursor<UnsignedByteType> cursor = new TwinCursor<UnsignedByteType>(
				positiveCorrelationImageCh1.randomAccess(),
				positiveCorrelationImageCh2.randomAccess(),
				positiveCorrelationAlwaysTrueMask.localizingCursor());
		// get the Pearson's value
		double pearsonsR = PearsonsCorrelation
			.classicPearsons(cursor, positiveCorrelationImageCh1Mean, positiveCorrelationImageCh2Mean);
		// check Pearsons R is close to 0.75
		assertEquals(0.75, pearsonsR, 0.01);
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

		Img<BitType> mask = MaskFactory.createMask(new long[] {512, 512}, true);

		for (double mean = initialMean; mean < 1; mean += spread) {
			Img<FloatType> ch1 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma);
			Img<FloatType> ch2 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(),
					512, 512, mean, spread, sigma);

			// create a twin value range cursor that iterates over all pixels of the input data
			TwinCursor<FloatType> cursor = new TwinCursor<FloatType>(ch1.randomAccess(),
					ch2.randomAccess(), mask.localizingCursor());
			double resultFast = PearsonsCorrelation.fastPearsons(cursor);
			assertEquals(0.0, resultFast, 0.1);

			/* This test will throw Missing PreconsitionException, as the means are the same
			 * which causes a numerical problem in the classic implementation of Pearson's
			 * double resultClassic = PearsonsCorrelation.classicPearsons(cursor, mean, mean);
			 * assertTrue(Math.abs(resultClassic) < 0.1);
			 */
		}
	}

	/**
	 * The 1993 paper of Manders et. al about colocalization presents an own
	 * method and testing data for it. For that testing data there are
	 * Pearson colocalization numbers, too, and these get tested in this test.
	 * @throws MissingPreconditionException
	 */
	@Test
	public void mandersPaperImagesTest() throws MissingPreconditionException {
		PearsonsCorrelation<UnsignedByteType> pc =
				new PearsonsCorrelation<UnsignedByteType>(Implementation.Classic);
		double r;

		// test A-A combination
		r = pc.calculatePearsons(mandersA, mandersA, mandersAlwaysTrueMask);
		assertEquals(1.0d, r, 0.01);

		// test A-B combination
		r = pc.calculatePearsons(mandersA, mandersB, mandersAlwaysTrueMask);
		assertEquals(0.72d, r, 0.01);

		// test A-C combination
		r = pc.calculatePearsons(mandersA, mandersC, mandersAlwaysTrueMask);
		assertEquals(0.44d, r, 0.01);

		// test A-D combination
		r = pc.calculatePearsons(mandersA, mandersD, mandersAlwaysTrueMask);
		assertEquals(0.16d, r, 0.01);

		// test A-E combination
		r = pc.calculatePearsons(mandersA, mandersE, mandersAlwaysTrueMask);
		assertEquals(-0.12d, r, 0.01);

		// test A-F combination
		r = pc.calculatePearsons(mandersA, mandersF, mandersAlwaysTrueMask);
		assertEquals(0.22d, r, 0.01);

		// test A-G combination
		r = pc.calculatePearsons(mandersA, mandersG, mandersAlwaysTrueMask);
		assertEquals(0.30d, r, 0.01);

		// test A-H combination
		r = pc.calculatePearsons(mandersA, mandersH, mandersAlwaysTrueMask);
		assertEquals(0.48d, r, 0.01);

		// test A-I combination
		r = pc.calculatePearsons(mandersA, mandersI, mandersAlwaysTrueMask);
		assertEquals(0.23d, r, 0.01);
	}
}
