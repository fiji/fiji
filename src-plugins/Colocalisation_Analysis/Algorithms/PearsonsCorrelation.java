import ij.IJ;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursorFactory;

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
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// get the thresholds of the images
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold == null ) {
			throw new MissingPreconditionException("Pearsons calculation need AutoThresholdRegression to be run before it.");
		}
		T threshold1 = 	autoThreshold.getCh1MaxThreshold();
		T threshold2 = 	autoThreshold.getCh2MaxThreshold();

		// create cursors to walk over the images
		TwinValueRangeCursor<T> alwaysTrueCursor
			= TwinValueRangeCursorFactory.generateAlwaysTrueCursor(img1, img2);
		TwinValueRangeCursor<T> belowThresholdCursor
			= TwinValueRangeCursorFactory.generateBelowThresholdCursor(img1, img2, threshold1, threshold2);
		TwinValueRangeCursor<T> aboveThresholdCursor
			= TwinValueRangeCursorFactory.generateAboveThresholdCursor(img1, img2, threshold1, threshold2);

		if (theImplementation == Implementation.Classic) {
			// get the means from the DataContainer
			double ch1Mean = container.getMeanCh1();
			double ch2Mean = container.getMeanCh2();

			pearsonsCorrelationValue = classicPearsons(alwaysTrueCursor, ch1Mean, ch2Mean);
			pearsonsCorrelationValueBelowThr = classicPearsons(belowThresholdCursor, ch1Mean, ch2Mean);
			pearsonsCorrelationValueAboveThr = classicPearsons(aboveThresholdCursor, ch1Mean, ch2Mean);
		}
		else if (theImplementation == Implementation.Fast) {
			pearsonsCorrelationValue = fastPearsons(alwaysTrueCursor);
			pearsonsCorrelationValueBelowThr = fastPearsons(belowThresholdCursor);
			pearsonsCorrelationValueAboveThr = fastPearsons(aboveThresholdCursor);
		}
	}

	/**
	 * Calculates Pearson's R value without any constraint in values, thus it uses no thresholds.
	 * If additional data like the images mean is needed, it is calculated.
	 *
	 * @param <S> The images base type.
	 * @param img1 The first image to walk over.
	 * @param img2 The second image to walk over.
	 * @return Pearson's R value.
	 * @throws MissingPreconditionException
	 */
	public <S extends RealType<S>> double calculatePearsons(Image<S> img1, Image<S> img2)
			throws MissingPreconditionException {
		// create cursors to walk over the images
		TwinValueRangeCursor<S> alwaysTrueCursor
			= TwinValueRangeCursorFactory.generateAlwaysTrueCursor(img1, img2);

		if (theImplementation == Implementation.Classic) {
			/* since we need the means and apparently don't have them
			 * calculate them.
			 */
			double mean1 = ImageStatistics.getImageMean(img1);
			double mean2 = ImageStatistics.getImageMean(img2);
			// do the actual calculation
			return classicPearsons(alwaysTrueCursor, mean1, mean2);
		} else {
			return fastPearsons(alwaysTrueCursor);
		}
	}

	/**
	 * Calculates Pearson's R value with the possibility to constraint in values.
	 * This could be useful of one wants to apply thresholds. You need to provide
	 * the images means, albeit not used by all implementations.
	 *
	 * @param <S> The images base type.
	 * @param cursor The cursor to walk over both images.
	 * @return Pearson's R value.
	 * @throws MissingPreconditionException
	 */
	public <S extends RealType<S>> double calculatePearsons(TwinValueRangeCursor<S> cursor, double mean1, double mean2)
			throws MissingPreconditionException {
		if (theImplementation == Implementation.Classic) {
			// do the actual calculation
			return classicPearsons(cursor, mean1, mean2);
		} else {
			return fastPearsons(cursor);
		}
	}

	/**
	 * Calculates Person's R value by using a Classic implementation of the
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
	public static <T extends RealType<T>> double classicPearsons(TwinValueRangeCursor<T> cursor, double meanCh1, double meanCh2)
			throws MissingPreconditionException {
		double pearsonDenominator = 0;
		double ch1diffSquaredSum = 0;
		double ch2diffSquaredSum = 0;
		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getChannel1Type();
			double ch1diff = type1.getRealDouble() - meanCh1;
			T type2 = cursor.getChannel2Type();
			double ch2diff = type2.getRealDouble() - meanCh2;
			pearsonDenominator += ch1diff*ch2diff;
			ch1diffSquaredSum += (ch1diff*ch1diff);
			ch2diffSquaredSum += (ch2diff*ch2diff);
		}
		double pearsonNumerator = Math.sqrt(ch1diffSquaredSum * ch2diffSquaredSum);

		double pearsonsR = pearsonDenominator / pearsonNumerator;

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
