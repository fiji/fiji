package tests;

import static org.junit.Assert.assertTrue;
import gadgets.DataContainer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;

import org.junit.Test;

import algorithms.AutoThresholdRegression;
import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;

/**
 * This class contains JUnit 4 test cases for the Costes
 * statistical significance test.
 *
 * @author Dan White & Tom Kazimiers
 */
public class CostesSignificanceTest extends ColocalisationTest {

	/**
	 * This test checks the Costes statistical significance test implementation
	 * by artificially disturbing known colocalisation. It simulates what Costes
	 * describes in figure 3 of section "Simulated data". An image representing
	 * colocalised data is generated. This is put onto two random Perlin noise
	 * images. A smoothing step is applied after the combination. With the two
	 * resulting images the Costes calculation and shuffling is done. These steps
	 * are done multiple times, every time with an increasing percentage of
	 * colocalised data in the images. As stated by Costes, colocalisation data
	 * percentages above three percent should be detected (P value > 0.95. This
	 * is the assertion of this test and checked with every iteration. Percentages
	 * to test are calculated as percentage = 10^x. Five iterations are done,
	 * increasing "x" in steps of 0.5, starting at 0. The test uses circles with
	 * a diameter of 7 as objects (similar to Costes' paper, he uses 7x7 squares).
	 */
	@Test
	public void backgroundNoiseTest() throws MissingPreconditionException {
		final int width = 512;
		final int height = 512;
		final double z = 2.178;
		final double scale = 0.1;
		final int psf = 3;
		final int objectSize = 7;
		final double[] sigma = new double[] {3.0,3.0};

		for (double exp=0; exp < 2.5; exp=exp+0.5) {
			double colocPercentage = Math.pow(10, exp);
			RandomAccessibleInterval<FloatType> ch1 = TestImageAccessor.producePerlinNoiseImage(
				new FloatType(), width, height, z, scale);
			RandomAccessibleInterval<FloatType> ch2 = TestImageAccessor.producePerlinNoiseImage(
				new FloatType(), width, height, z, scale);
			/* calculate the number of colocalised pixels, based on the percentage and the
			 * space one noise point will take (here 9, because we use 3x3 dots)
			 */
			int nrColocPixels = (int) ( ( (width * height / 100.0) * colocPercentage ) / (objectSize * objectSize) );
			// create non-smoothed coloc image. add it to the noise images and smooth them
			RandomAccessibleInterval<FloatType> colocImg = TestImageAccessor.produceNoiseImage(
				width, height, objectSize, nrColocPixels);
			TestImageAccessor.combineImages(ch1, colocImg);
			ch1 = TestImageAccessor.gaussianSmooth(ch1, sigma);
			TestImageAccessor.combineImages(ch2, colocImg);
			ch2 = TestImageAccessor.gaussianSmooth(ch2, sigma);

			DataContainer<FloatType> container
				= new DataContainer<FloatType>(ch1, ch2, 1, 1, "Channel 1", "Channel 2");

			PearsonsCorrelation<FloatType> pc
				= new PearsonsCorrelation<FloatType>(PearsonsCorrelation.Implementation.Fast);
			AutoThresholdRegression<FloatType> atr
				= new AutoThresholdRegression<FloatType>(pc);
			container.setAutoThreshold(atr);
			atr.execute(container);
			try {
				pc.execute(container);
			}
			catch (MissingPreconditionException e) {
				/* this can happen for random noise data in seldom cases,
				 * but we are not after this here. The cases that are
				 * important for Costes work well, but are again sanity
				 * checked here.
				 */
				if (pc.getPearsonsCorrelationValue() == Double.NaN)
					throw e;
			}

			algorithms.CostesSignificanceTest<FloatType> costes
				= new algorithms.CostesSignificanceTest<FloatType>(pc, psf, 10, false);
			costes.execute(container);

			// check if we can expect a high P
			if (colocPercentage > 3.0) {
				double pVal = costes.getCostesPValue();
				assertTrue("Costes P value was " + pVal, pVal > 0.95);
			}
		}
	}
}