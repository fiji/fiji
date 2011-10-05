package algorithms;

import gadgets.DataContainer;
import gadgets.MaskFactory;
import gadgets.ThresholdMode;
import mpicbg.imglib.algorithm.math.ImageStatistics;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import results.ResultHandler;

/**
 * A class that represents the mean calculation of the two source
 * images in the data container.
 *
 * @param <T>
 */
public class PearsonsCorrelation<T extends RealType<T>> extends Algorithm<T> {

	// Identifiers for choosing which implementation to use
	public enum Implementation {Classic, Fast};
	// The member variable to store the implementation of the Pearson's Coefficient calculation used.
	Implementation theImplementation = Implementation.Fast;
	// resulting Pearsons value without thresholds
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
		Image<BitType> mask = container.getMask();

		// get the thresholds of the images
		AutoThresholdRegression<T> autoThreshold = container.getAutoThreshold();
		if (autoThreshold == null ) {
			throw new MissingPreconditionException("Pearsons calculation need AutoThresholdRegression to be run before it.");
		}
		T threshold1 = 	autoThreshold.getCh1MaxThreshold();
		T threshold2 = 	autoThreshold.getCh2MaxThreshold();
		if (threshold1 == null || threshold2 == null ) {
			throw new MissingPreconditionException("Pearsons calculation needs valid (not null) thresholds.");
		}

		/* Create cursors to walk over the images. First go over the
		 * images without a mask. */
		TwinCursor<T> cursor = new TwinCursor<T>(
				img1.createLocalizableByDimCursor(), img2.createLocalizableByDimCursor(),
				mask.createLocalizableCursor());

		MissingPreconditionException error = null;
		if (theImplementation == Implementation.Classic) {
			// get the means from the DataContainer
			double ch1Mean = container.getMeanCh1();
			double ch2Mean = container.getMeanCh2();

			try {
				cursor.reset();
				pearsonsCorrelationValue = classicPearsons(cursor,
						ch1Mean, ch2Mean);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValue = Double.NaN;
				error = e;
			}

			try {
				cursor.reset();
				pearsonsCorrelationValueBelowThr = classicPearsons(cursor,
						ch1Mean, ch2Mean, threshold1, threshold2, ThresholdMode.Below);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValueBelowThr = Double.NaN;
				error = e;
			}

			try {
				cursor.reset();
				pearsonsCorrelationValueAboveThr = classicPearsons(cursor,
						ch1Mean, ch2Mean, threshold1, threshold2, ThresholdMode.Above);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValueAboveThr = Double.NaN;
				error = e;
			}
		}
		else if (theImplementation == Implementation.Fast) {
			try {
				cursor.reset();
				pearsonsCorrelationValue = fastPearsons(cursor);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValue = Double.NaN;
				error = e;
			}

			try {
				cursor.reset();
				pearsonsCorrelationValueBelowThr = fastPearsons(cursor,
						threshold1, threshold2, ThresholdMode.Below);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValueBelowThr = Double.NaN;
				error = e;
			}

			try {
				cursor.reset();
				pearsonsCorrelationValueAboveThr = fastPearsons(cursor,
						threshold1, threshold2, ThresholdMode.Above);
			} catch (MissingPreconditionException e) {
				// probably a numerical error occurred
				pearsonsCorrelationValueAboveThr = Double.NaN;
				error = e;
			}
		}

		cursor.close();

		// if an error occurred, throw it one level up
		if (error != null)
			throw error;
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
		Image<BitType> alwaysTrueMask = MaskFactory.createMask(img1.getDimensions(), true);
		TwinCursor<S> alwaysTrueCursor = new TwinCursor<S>(
				img1.createLocalizableByDimCursor(), img2.createLocalizableByDimCursor(),
				alwaysTrueMask.createLocalizableCursor());

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
	public <S extends RealType<S>> double calculatePearsons(TwinCursor<S> cursor,
			double mean1, double mean2, S thresholdCh1, S thresholdCh2,
			ThresholdMode tMode) throws MissingPreconditionException {
		if (theImplementation == Implementation.Classic) {
			// do the actual calculation
			return classicPearsons(cursor, mean1, mean2,
					thresholdCh1, thresholdCh2, tMode);
		} else {
			return fastPearsons(cursor, thresholdCh1,
					thresholdCh2, tMode);
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
	public static <T extends RealType<T>> double classicPearsons(TwinCursor<T> cursor,
			double meanCh1, double meanCh2) throws MissingPreconditionException {
		return classicPearsons(cursor, meanCh1, meanCh2, null, null, ThresholdMode.None);
	}

	public static <T extends RealType<T>> double classicPearsons(TwinCursor<T> cursor,
			double meanCh1, double meanCh2, T thresholdCh1, T thresholdCh2,
			ThresholdMode tMode) throws MissingPreconditionException {
		double pearsonDenominator = 0;
		double ch1diffSquaredSum = 0;
		double ch2diffSquaredSum = 0;
		int count = 0;

		if (tMode == ThresholdMode.None) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				double ch1diff = type1.getRealDouble() - meanCh1;
				double ch2diff = type2.getRealDouble() - meanCh2;
				pearsonDenominator += ch1diff*ch2diff;
				ch1diffSquaredSum += (ch1diff*ch1diff);
				ch2diffSquaredSum += (ch2diff*ch2diff);
				count++;
			}
		} else if (tMode == ThresholdMode.Below) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				// test if one of the values is below its threshold
				if (type1.compareTo(thresholdCh1) < 0
						|| type2.compareTo(thresholdCh2) < 0) {
					double ch1diff = type1.getRealDouble() - meanCh1;
					double ch2diff = type2.getRealDouble() - meanCh2;
					pearsonDenominator += ch1diff*ch2diff;
					ch1diffSquaredSum += (ch1diff*ch1diff);
					ch2diffSquaredSum += (ch2diff*ch2diff);
					count++;
				}
			}
		} else if (tMode == ThresholdMode.Above) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				// test if one of the values is below its threshold
				if (type1.compareTo(thresholdCh1) > 0
						|| type2.compareTo(thresholdCh2) > 0) {
					double ch1diff = type1.getRealDouble() - meanCh1;
					double ch2diff = type2.getRealDouble() - meanCh2;
					pearsonDenominator += ch1diff*ch2diff;
					ch1diffSquaredSum += (ch1diff*ch1diff);
					ch2diffSquaredSum += (ch2diff*ch2diff);
					count++;
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}
		double pearsonNumerator = Math.sqrt(ch1diffSquaredSum * ch2diffSquaredSum);

		double pearsonsR = pearsonDenominator / pearsonNumerator;

		checkForSanity(pearsonsR, count);

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
	public static <T extends RealType<T>> double fastPearsons(TwinCursor<T> cursor)
			throws MissingPreconditionException {
		return fastPearsons(cursor, null, null, ThresholdMode.None);
	}

	public static <T extends RealType<T>> double fastPearsons(TwinCursor<T> cursor,
			T thresholdCh1, T thresholdCh2, ThresholdMode tMode)
			throws MissingPreconditionException {
		double sum1 = 0.0, sum2 = 0.0, sumProduct1_2 = 0.0, sum1squared= 0.0, sum2squared = 0.0;
		// the total amount of pixels that have been taken into consideration
		int N = 0;

		if (tMode == ThresholdMode.None) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();

				double ch1 = type1.getRealDouble();
				double ch2 = type2.getRealDouble();
				sum1 += ch1;
				sumProduct1_2 += (ch1 * ch2);
				sum1squared += (ch1 * ch1);
				sum2squared += (ch2 *ch2);
				sum2 += ch2;
				N++;
			}
		} else if (tMode == ThresholdMode.Below) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				// test if one of the values is below its threshold
				if (type1.compareTo(thresholdCh1) < 0
						|| type2.compareTo(thresholdCh2) < 0) {
					double ch1 = type1.getRealDouble();
					double ch2 = type2.getRealDouble();
					sum1 += ch1;
					sumProduct1_2 += (ch1 * ch2);
					sum1squared += (ch1 * ch1);
					sum2squared += (ch2 *ch2);
					sum2 += ch2;
					N++;
				}

			}
		} else if (tMode == ThresholdMode.Above) {
			while (cursor.hasNext()) {
				cursor.fwd();
				T type1 = cursor.getChannel1();
				T type2 = cursor.getChannel2();
				// test if one of the values is below its threshold
				if (type1.compareTo(thresholdCh1) > 0
						|| type2.compareTo(thresholdCh2) > 0) {
					double ch1 = type1.getRealDouble();
					double ch2 = type2.getRealDouble();
					sum1 += ch1;
					sumProduct1_2 += (ch1 * ch2);
					sum1squared += (ch1 * ch1);
					sum2squared += (ch2 *ch2);
					sum2 += ch2;
					N++;
				}
			}
		} else {
			throw new UnsupportedOperationException();
		}

		// for faster computation, have the inverse of N available
		double invN = 1.0 / N;

		double pearsons1 = sumProduct1_2 - (sum1 * sum2 * invN);
		double pearsons2 = sum1squared - (sum1 * sum1 * invN);
		double pearsons3 = sum2squared - (sum2 * sum2 * invN);
		double pearsonsR = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		checkForSanity(pearsonsR, N);

		return pearsonsR;
	}

	/**
	 * Does a sanity check for calculated Pearsons values. Wrong
	 * values can happen for fast and classic implementation.
	 *
	 * @param val The value to check.
	 */
	private static void checkForSanity(double value, int iterations) throws MissingPreconditionException {
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
			throw new MissingPreconditionException("A numerical problem occured: the input data is unsuitable for this algorithm. Possibly too few pixels (in range were: " + iterations + ").");
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
