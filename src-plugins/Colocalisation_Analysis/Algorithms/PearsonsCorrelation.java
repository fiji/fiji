import ij.IJ;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;

/**
 * A class that represents the mean calculation of the two source
 * images in the data container.
 *
 * @param <T>
 */
public class PearsonsCorrelation<T extends RealType<T>> extends Algorithm<T> {

	// Identifiers for choosing which implementation to use
	enum Implementation {Classic, Fast};
	// The member variable to store the implementation of the Pearson's Coefficient calculation used.
	Implementation theImplementation = Implementation.Fast;
	// resulting Pearsing value without thresholds
	double pearsonsCorrelationValue;
	// resulting Pearsons value below threshold
	double pearsonsCorrelationValueBelowThr;
	// resulting Pearsons value above threshold
	double pearsonsCorrelationValueAboveThr;

	/**
	 * Creates a new Pearson's Correlation and allows us to define
	 * which implementation of the calculation to use.
	 * @param theImplementation The implementation of Pearson's Coefficient calculation to use.
	 */
	public PearsonsCorrelation(Implementation implementation) {
		super();
		this.theImplementation = implementation;
	}

	/**
	 * Creates a new Pearson's Correlation with default (fast) implementation parameter.
	 */
	public PearsonsCorrelation() {
		this(Implementation.Fast);
	}

	public void execute(DataContainer<T> container) throws MissingPreconditionException {
		if (theImplementation == Implementation.Classic)
			pearsonsCorrelationValue = classicPearsons(container);
		else {
			fastPearsons(container);
		}
	}

	public double classicPearsons(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the means from the DataContainer
		double ch1Mean = container.getMeanCh1();
		double ch2Mean = container.getMeanCh2();

		// Do the Classic version of the Pearson's Correlation as per Manders/Costes articles.

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();
		double pearsonDenominator = 0;
		double ch1diffSquaredSum = 0;
		double ch2diffSquaredSum = 0;
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1diff = type1.getRealDouble() - ch1Mean;
			T type2 = cursor2.getType();
			double ch2diff = type2.getRealDouble() - ch2Mean;
			pearsonDenominator += ch1diff*ch2diff;
			ch1diffSquaredSum += (ch1diff*ch1diff);
			ch2diffSquaredSum += (ch2diff*ch2diff);
		}
		double pearsonNumerator = Math.sqrt(ch1diffSquaredSum * ch2diffSquaredSum);

		// close the cursors
		cursor1.close();
		cursor2.close();

		double pearsonsR = pearsonDenominator / pearsonNumerator;

		checkForSanity(pearsonsR);

		return pearsonsR;
	}

	public void fastPearsons(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		pearsonsCorrelationValue = fastPearsons(img1, img2);

		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold != null ) {
			pearsonsCorrelationValueBelowThr = fastPearsons(img1, img2,
					autoThreshold.getCh1MaxThreshold().getRealDouble(),
					autoThreshold.getCh2MaxThreshold().getRealDouble(), false);
			pearsonsCorrelationValueAboveThr = fastPearsons(img1, img2,
					autoThreshold.getCh1MaxThreshold().getRealDouble(),
					autoThreshold.getCh2MaxThreshold().getRealDouble(), true);
		}
	}

	public static <T extends RealType<T>> double fastPearsons(Image<T> img1, Image<T> img2)
			throws MissingPreconditionException {
		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();
		double sum1 = 0.0, sum2 = 0.0, sumProduct1_2 = 0.0, sum1squared= 0.0, sum2squared = 0.0;
		int N = 0;

		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			sum1 += ch1;
			sum2 += ch2;
			sumProduct1_2 += (ch1 * ch2);
			sum1squared += (ch1 * ch1);
			sum2squared += (ch2 * ch2);
			N++;
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		// for faster computation, have the inverse of N available
		double invN = 1.0 / N;

		double pearsons1 = sumProduct1_2 - (sum1 * sum2 * invN);
		double pearsons2 = sum1squared - (sum1 * sum1 * invN);
		double pearsons3 = sum2squared - (sum2 * sum2 * invN);
		double pearsonsR = pearsons1 / (Math.sqrt(pearsons2 * pearsons3));

		checkForSanity(pearsonsR);

		return pearsonsR;
	}

	/**
	 * Calculates Person's R value by using a fast implementation of the
	 * algorithm. This method allows the specification of upper limits for
	 * the pixel values for each channel. One can also define if one wants
	 * Pearson's correlation value above or below the threshold.
	 *
	 * @param <T> The image base type
	 * @param img1 Channel one
	 * @param img2 Channel two
	 * @param ch1ThreshMax Upper limit of channel one
	 * @param ch2ThreshMax Upper limit of channel two
	 * @param aboveThr use pixels above (true) or below (false) threshold
	 * @return Person's R value
	 */
	public static <T extends RealType<T>> double fastPearsons(Image<T> img1, Image<T> img2,
			double ch1ThreshMax, double ch2ThreshMax, boolean aboveThr)
				throws MissingPreconditionException {
		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();
		double sum1 = 0.0, sum2 = 0.0, sumProduct1_2 = 0.0, sum1squared= 0.0, sum2squared = 0.0;
		// the total amount of pixels that have been taken into consideration
		int N = 0;

		if (aboveThr) {
			while (cursor1.hasNext() && cursor2.hasNext()) {
				cursor1.fwd();
				cursor2.fwd();
				T type1 = cursor1.getType();
				double ch1 = type1.getRealDouble();
				T type2 = cursor2.getType();
				double ch2 = type2.getRealDouble();
				// is either channel one or channel two within the limits?
				if ( (ch1 > ch1ThreshMax) || (ch2 > ch2ThreshMax)) {
					sum1 += ch1;
					sumProduct1_2 += (ch1 * ch2);
					sum1squared += (ch1 * ch1);
					sum2squared += (ch2 *ch2);
					sum2 += ch2;
					N++;
				}
			}
		} else {
			while (cursor1.hasNext() && cursor2.hasNext()) {
				cursor1.fwd();
				cursor2.fwd();
				T type1 = cursor1.getType();
				double ch1 = type1.getRealDouble();
				T type2 = cursor2.getType();
				double ch2 = type2.getRealDouble();
				// is either channel one or channel two within the limits?
				if ( (ch1 < ch1ThreshMax) || (ch2 < ch2ThreshMax)) {
					sum1 += ch1;
					sumProduct1_2 += (ch1 * ch2);
					sum1squared += (ch1 * ch1);
					sum2squared += (ch2 *ch2);
					sum2 += ch2;
					N++;
				}
			}
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		// for faster computation, have the inverse of N available
		double invN = 1.0 / N;

		double pearsons1 = sumProduct1_2 - (sum1 * sum2 * invN);
		double pearsons2 = sum1squared - (sum1 * sum1 * invN);
		double pearsons3 = sum2squared - (sum2 * sum2 * invN);
		double pearsonsR = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		checkForSanity(pearsonsR);

		return pearsonsR;
	}

	/**
	 * Calculates Person's R value by using a fast implementation of the
	 * algorithm. This method allows the specification of a TwinValueRangeCursor.
	 * With such a cursor one for instance can combine different thresholding
	 * conditions for each channel. The cursor is not closed in here.
	 *
	 * @param <T> The image base type
	 * @param img1 Channel one
	 * @param img2 Channel two
	 * @param cursor The cursor that defines the walk over both images.
	 * @return Person's R value
	 */
	public static <T extends RealType<T>> double fastPearsons(TwinValueRangeCursor<T> cursor)
			throws MissingPreconditionException {
		double sum1 = 0.0, sum2 = 0.0, sumProduct1_2 = 0.0, sum1squared= 0.0, sum2squared = 0.0;
		// the total amount of pixels that have been taken into consideration
		int N = 0;

		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getChannel1Type();
			double ch1 = type1.getRealDouble();
			T type2 = cursor.getChannel2Type();
			double ch2 = type2.getRealDouble();

			sum1 += ch1;
			sumProduct1_2 += (ch1 * ch2);
			sum1squared += (ch1 * ch1);
			sum2squared += (ch2 *ch2);
			sum2 += ch2;
			N++;
		}

		// for faster computation, have the inverse of N available
		double invN = 1.0 / N;

		double pearsons1 = sumProduct1_2 - (sum1 * sum2 * invN);
		double pearsons2 = sum1squared - (sum1 * sum1 * invN);
		double pearsons3 = sum2squared - (sum2 * sum2 * invN);
		double pearsonsR = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		checkForSanity(pearsonsR);

		return pearsonsR;
	}

	/**
	 * Does a sanity check for calculated Pearsons values. Wrong
	 * values can happen for fast and classic implementation.
	 *
	 * @param val The value to check.
	 */
	private static void checkForSanity(double value) throws MissingPreconditionException {
		if ( Double.isNaN(value) || Double.isInfinite(value)) {
			/* For the _fast_ implementation this could happen:
			 *   Infinity could happen if only the numerator is 0, i.e.:
			 *     sum1squared == sum1 * sum1 * invN
			 *   and
			 *     sum2squared == sum2 * sum2 * invN
			 *   If the denominator is also zero, one will get NaN, i.e:
			 *     sumProduct1_2 == sum1 * sum2 * invN
			 *
			 * For the classic implementation it could happen, too:
			 *   Infinity happens if one channels sum of value-mean-differences
			 *   is zero. If it is negative for one image you will get NaN.
			 *   Additionally, if is zero for both channels at once you
			 *   could get NaN. NaN
			 */
			throw new MissingPreconditionException("A numerical problem occured: the input data is unsuitable for this algorithm. Possibly too few pixels.");
		}
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleValue("Pearson's R value (no threshold)", pearsonsCorrelationValue, 2);
		handler.handleValue("Pearson's R value (below threshold)", pearsonsCorrelationValueBelowThr, 2);
		handler.handleValue("Pearson's R value (above threshold)", pearsonsCorrelationValueAboveThr, 2);
	}

	public double getPearsonsCorrelationValue() {
		return pearsonsCorrelationValue;
	}

	public double getPearsonsCorrelationBelowThreshold() {
		return pearsonsCorrelationValueBelowThr;
	}

	public double getPearsonsCorrelationAboveThreshold() {
		return pearsonsCorrelationValueAboveThr;
	}
}
