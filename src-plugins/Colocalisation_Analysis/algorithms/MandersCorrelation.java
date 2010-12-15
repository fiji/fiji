package algorithms;

import results.ResultHandler;
import gadgets.DataContainer;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.meta.Predicate;
import imglib.mpicbg.imglib.cursor.special.meta.AboveThresholdPredicate;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

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
	 * A result containter for Manders' calculations.
	 */
	public static class MandersResults {
		public double m1;
		public double m2;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the two images for the calculation of Manders' values
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// calculate Mander's values
		MandersResults results = calculateMandersCorrelation(img1, img2);

		// save the results
		mandersM1 = results.m1;
		mandersM2 = results.m2;

		// calculate the thresholded values, if possible
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null ) {
			Predicate<T> img1Predicate =
				new AboveThresholdPredicate<T>( autoThreshold.getCh1MaxThreshold() );
			Predicate<T> img2Predicate =
				new AboveThresholdPredicate<T>( autoThreshold.getCh2MaxThreshold() );
			TwinValueRangeCursor<T> cursor =
				new TwinValueRangeCursor<T>(img1.createCursor(), img2.createCursor(), img1Predicate, img2Predicate);
			// calculate Mander's values
			results = calculateMandersCorrelation(cursor, container.getIntegralCh1(), container.getIntegralCh2() );

			// save the results
			mandersThresholdedM1 = results.m1;
			mandersThresholdedM2 = results.m2;
		}
	}

	/**
	 * Calculates Manders' split M1 and M2 values.
	 *
	 * @param img1 The first image
	 * @param img2 The second image
	 * @return Both Manders' M1 and M2 values
	 */
	public MandersResults calculateMandersCorrelation(Image<T> img1, Image<T> img2) {
		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		double m1Nominator = 0;
		double m2Nominator = 0;
		double sumCh1 = 0;
		double sumCh2 = 0;

		// iterate over images
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			// if ch2 is non-zero, increase ch1 nominator
			if (Math.abs(ch2) > 0.00001) {
				m1Nominator += ch1;
			}

			// if ch1 is non-zero, increase ch2 nominator
			if (Math.abs(ch1) > 0.00001) {
				m2Nominator += ch2;
			}

			sumCh1 += ch1;
			sumCh2 += ch2;
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		MandersResults results = new MandersResults();
		// calculate the results
		results.m1 = m1Nominator / sumCh1;
		results.m2 = m2Nominator / sumCh2;

		return results;
	}


	/**
	 * Calculates Manders' split M1 and M2 values from image
	 * values above certain thresholds.
	 *
	 * @param cursor The cursor to walk over the images.
	 * @param ch1Total Channel ones total sum of image values.
	 * @param ch2Total Channel twos total sum of image values.
	 * @return Both Manders M1 and M2 coefficients
	 */
	public MandersResults calculateMandersCorrelation(TwinValueRangeCursor<T> cursor,
			double ch1Total, double ch2Total) {

		double m1Nominator = 0;
		double m2Nominator = 0;

		// iterate over images
		while ( cursor.hasNext() ) {
			cursor.fwd();
			T type1 = cursor.getChannel1Type();
			double ch1 = type1.getRealDouble();
			T type2 = cursor.getChannel2Type();
			double ch2 = type2.getRealDouble();

			// if ch2 is non-zero, increase ch1 nominator
			if (Math.abs(ch2) > 0.00001) {
				m1Nominator += ch1;
			}

			// if ch1 is non-zero, increase ch2 nominator
			if (Math.abs(ch1) > 0.00001) {
				m2Nominator += ch2;
			}
		}

		// close the cursor
		cursor.close();

		MandersResults results = new MandersResults();
		// calculate the results
		results.m1 = m1Nominator / ch1Total;
		results.m2 = m2Nominator / ch2Total;

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
