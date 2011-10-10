package algorithms;

import gadgets.DataContainer;
import gadgets.ThresholdMode;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import results.ResultHandler;

/**
 * This algorithm calculates Manders et al.'s two two correlation
 * values M1 and M2. Those coefficients are independent from signal
 * intensities, but are directly proportional to the amount of
 * flourescence in the co-localized objects in each component of the
 * image. See "Manders, Verbeek, Aten - Measurement of co-localization
 * of objects in dual-colour confocal images".
 *
 * @param <T>
 */
public class MandersCorrelation<T extends RealType<T>> extends Algorithm<T> {
	// Manders M1 and M2 value
	double mandersM1, mandersM2;
	// thresholded Manders M1 and M2 values
	double mandersThresholdedM1, mandersThresholdedM2;

	/**
	 * A result container for Manders' calculations.
	 */
	public static class MandersResults {
		public double m1;
		public double m2;
	}

	public MandersCorrelation() {
		super("Manders correlation");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the two images for the calculation of Manders' values
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();
		Image<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.createLocalizableByDimCursor(),
				img2.createLocalizableByDimCursor(), mask.createLocalizableCursor());

		// calculate Mander's values without threshold
		MandersResults results = calculateMandersCorrelation(cursor, img1.createType());

		// save the results
		mandersM1 = results.m1;
		mandersM2 = results.m2;

		// calculate the thresholded values, if possible
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null ) {
			// calculate Mander's values
			cursor.reset();
			results = calculateMandersCorrelation(cursor, autoThreshold.getCh1MaxThreshold(),
					autoThreshold.getCh2MaxThreshold(), ThresholdMode.Above);

			// save the results
			mandersThresholdedM1 = results.m1;
			mandersThresholdedM2 = results.m2;
		}
		cursor.close();
	}

	/**
	 * Calculates Manders' split M1 and M2 values without a threshold
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @param type A type instance, its value is not relevant
	 * @return Both Manders' M1 and M2 values
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor, T type) {
		return calculateMandersCorrelation(cursor, type, type, ThresholdMode.None);
	}

	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor,
			final T thresholdCh1, final T thresholdCh2, ThresholdMode tMode) {
		MandersAccumulator acc;
		// create a zero-values variable to compare to later on
		final T zero = thresholdCh1.createVariable();
		zero.setZero();

		// iterate over images
		if (tMode == ThresholdMode.None) {
			acc = new MandersAccumulator(cursor) {
				final boolean accecptCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0);
				}
				final boolean accecptCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0);
				}
			};
		} else if (tMode == ThresholdMode.Below) {
			acc = new MandersAccumulator(cursor) {
				final boolean accecptCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) <= 0);
				}
				final boolean accecptCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) <= 0);
				}
			};
		} else if (tMode == ThresholdMode.Above) {
			acc = new MandersAccumulator(cursor) {
				final boolean accecptCh1(T type1, T type2) {
					return (type2.compareTo(zero) > 0) &&
						(type1.compareTo(thresholdCh1) >= 0);
				}
				final boolean accecptCh2(T type1, T type2) {
					return (type1.compareTo(zero) > 0) &&
						(type2.compareTo(thresholdCh2) >= 0);
				}
			};
		} else {
			throw new UnsupportedOperationException();
		}

		MandersResults results = new MandersResults();
		// calculate the results
		results.m1 = acc.condSumCh1 / acc.sumCh1;
		results.m2 = acc.condSumCh2 / acc.sumCh2;

		return results;
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue( "Manders M1 (no threshold)", mandersM1 );
		handler.handleValue( "Manders M2 (no threshold)", mandersM2 );
		handler.handleValue( "Manders M1 (threshold)", mandersThresholdedM1 );
		handler.handleValue( "Manders M2 (threshold)", mandersThresholdedM2 );
	}

	/**
	 * A class similar to the Accumulator class, but more specific
	 * to the Manders calculations.
	 */
	protected abstract class MandersAccumulator {
		double sumCh1, sumCh2, condSumCh1, condSumCh2;

		public MandersAccumulator(TwinCursor<T> cursor) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();
				if (accecptCh1(type1, type2))
					condSumCh1 += ch1;
				if (accecptCh2(type1, type2))
					condSumCh2 += ch2;
				sumCh1 += ch1;
				sumCh2 += ch2;
			}
		}
		abstract boolean accecptCh1(T type1, T type2);
		abstract boolean accecptCh2(T type1, T type2);
	}
}
