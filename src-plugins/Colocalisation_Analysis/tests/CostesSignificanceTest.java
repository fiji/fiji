package tests;

import gadgets.DataContainer;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.real.FloatType;

import org.junit.Test;

import algorithms.MissingPreconditionException;
import algorithms.PearsonsCorrelation;
import algorithms.PearsonsCorrelation.Implementation;

public class CostesSignificanceTest extends ColocalisationTest {
	@Test
	public void differentMeansTest() throws MissingPreconditionException {
		final double initialMean = 0.2;
		final double spread = 0.1;
		final double[] sigma = new double[] {3.0, 3.0};

		for (double mean = initialMean; mean < 1; mean += spread) {
			Image<FloatType> ch1 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(), 512, 512, mean, spread, sigma);
			Image<FloatType> ch2 = TestImageAccessor.produceMeanBasedNoiseImage(new FloatType(), 512, 512, mean, spread, sigma);

			// create a new DataContainer for the data, necessary for Costes
			DataContainer<FloatType> container = new DataContainer<FloatType>(ch1, ch2, 1, 1);
			// create a new Pearsons correlation object to be used by Costes
			PearsonsCorrelation<FloatType> pearsons = new PearsonsCorrelation<FloatType>(Implementation.Fast);
			// create a new Costes object based on the previously defined Pearsons calculator
			algorithms.CostesSignificanceTest<FloatType> costes = new algorithms.CostesSignificanceTest<FloatType>(pearsons, 3, 5);

			costes.execute(container);
			System.err.println(costes.getShuffledMean());
		}
	}
}