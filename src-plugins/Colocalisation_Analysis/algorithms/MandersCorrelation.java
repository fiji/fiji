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
		MandersResults results = calculateMandersCorrelation(cursor);

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
	}

	/**
	 * Calculates Manders' split M1 and M2 values without a threshold
	 *
	 * @param cursor A TwinCursor that walks over two images
	 * @return Both Manders' M1 and M2 values
	 */
	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor) {
		return calculateMandersCorrelation(cursor, null, null, ThresholdMode.None);
	}

	public MandersResults calculateMandersCorrelation(TwinCursor<T> cursor,
			T thresholdCh1, T thresholdCh2, ThresholdMode tMode) {
		double m1Numerator = 0.0;
		double m2Numerator = 0.0;
		double sumCh1 = 0.0;
		double sumCh2 = 0.0;

		// iterate over images
		if (tMode == ThresholdMode.None) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();
				// if ch2 is non-zero, increase ch1 numerator
				if (Math.abs(ch2) > 0.00001) {
					m1Numerator += ch1;
				}
				// if ch1 is non-zero, increase ch2 numerator
				if (Math.abs(ch1) > 0.00001) {
					m2Numerator += ch2;
				}
				sumCh1 += ch1;
				sumCh2 += ch2;
			}
		} else if (tMode == ThresholdMode.Below) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();
				if (Math.abs(ch2) > 0.00001 &&
						type1.compareTo(thresholdCh1) <= 0) {
					m1Numerator += ch1;
				}
				if (Math.abs(ch1) > 0.00001 &&
						type2.compareTo(thresholdCh2) <= 0) {
					m2Numerator += ch2;
				}
				sumCh1 += ch1;
				sumCh2 += ch2;
			}
		} else if (tMode == ThresholdMode.Above) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();
				if (Math.abs(ch2) > 0.00001 &&
						type1.compareTo(thresholdCh1) >= 0) {
					m1Numerator += ch1;
				}
				if (Math.abs(ch1) > 0.00001 &&
						type2.compareTo(thresholdCh2) >= 0) {
					m2Numerator += ch2;
				}
				sumCh1 += ch1;
				sumCh2 += ch2;
			}
		} else {
			throw new UnsupportedOperationException();
		}

		MandersResults results = new MandersResults();
		// calculate the results
		results.m1 = m1Numerator / sumCh1;
		results.m2 = m2Numerator / sumCh2;

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
}
