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
public class MandersCorrelation<T extends RealType<T>> extends Algorithm {

	/**
	 * A result containter for Manders' calculations.
	 */
	public static class MandersResults {
		public double m1;
		public double m2;
	}

	@Override
	public void execute(DataContainer container)
			throws MissingPreconditionException {
		// get the two images for the calculation of Manders' values
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// calculate Mander's values
		MandersResults results = calculateMandersCorrelation(img1, img2);

		// save the result in the container
		container.add( new Result.SimpleValueResult("Manders' M1", results.m1));
		container.add( new Result.SimpleValueResult("Manders' M2", results.m2));
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
}
