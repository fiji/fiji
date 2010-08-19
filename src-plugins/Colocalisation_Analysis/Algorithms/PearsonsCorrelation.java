import ij.IJ;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A class that represents the mean calculation of the two source
 * images in the data container.
 *
 * @param <T>
 */
public class PearsonsCorrelation<T extends RealType<T>> extends Algorithm {

	// Identifiers for choosing which implementation to use
	enum Implementation {Classic, Fast};
	// The member variable to store the implementation of the Pearson's Coefficient calculation used.
	Implementation theImplementation = Implementation.Fast;
	// resulting Pearsing value
	double pearsonsCorrelationValue;

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

	public void execute(DataContainer container) throws MissingPreconditionException {
		if (theImplementation == Implementation.Classic)
			classicPearsons(container);
		else fastPearsons(container);
	}

	public void classicPearsons(DataContainer container) throws MissingPreconditionException {
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

		//put the result into the DataContainer
		pearsonsCorrelationValue = pearsonDenominator / pearsonNumerator;
	}

	public void fastPearsons(DataContainer container) {
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		pearsonsCorrelationValue = fastPearsons(img1, img2);
	}

	public static <T extends RealType<T>> double fastPearsons(Image<T> img1, Image<T> img2) {
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
			sumProduct1_2 += (ch1 * ch2);
			sum1squared += (ch1 * ch1);
			sum2squared += (ch2 *ch2);
			sum2 += ch2;
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

		return pearsonsR;
	}

	/**
	 * Calculates Person's R value by using a fast implementation of the
	 * algorithm. This method allows the specification of upper limits for
	 * the pixel values for each channel. One can also define if pixels
	 * that are zero in both channels should be discarded or not.
	 *
	 * @param <T> The image base type
	 * @param img1 Channel one
	 * @param img2 Channel two
	 * @param ch1ThreshMax Upper limit of channel one
	 * @param ch2ThreshMax Upper limit of channel two
	 * @param discardZeroPixels defines if zero pixels should be discarded
	 * @return Person's R value
	 */
	public static <T extends RealType<T>> double fastPearsons(Image<T> img1, Image<T> img2,
			double ch1ThreshMax, double ch2ThreshMax, boolean discardZeroPixels) {
		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();
		double sum1 = 0.0, sum2 = 0.0, sumProduct1_2 = 0.0, sum1squared= 0.0, sum2squared = 0.0;
		// the total amount of pixels that have been taken into consideration
		int N = 0;
		// the amount of pixels that are zero in both channels
		int Nzero = 0;
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();
			// is either channel one or channel two within the limits?
			if ( (ch1 < ch1ThreshMax) || (ch2 < ch2ThreshMax)) {
				// is the current pixels combination a zero pixel?
				if (Math.abs(ch1 + ch2) < 0.00001)
					Nzero++;

				sum1 += ch1;
				sumProduct1_2 += (ch1 * ch2);
				sum1squared += (ch1 * ch1);
				sum2squared += (ch2 *ch2);
				sum2 += ch2;
				N++;
			}
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		// if told to do so, discard the zero pixels
		if (discardZeroPixels)
			N = N - Nzero;

		// for faster computation, have the inverse of N available
		double invN = 1.0 / N;

		double pearsons1 = sumProduct1_2 - (sum1 * sum2 * invN);
		double pearsons2 = sum1squared - (sum1 * sum1 * invN);
		double pearsons3 = sum2squared - (sum2 * sum2 * invN);
		double pearsonsR = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		return pearsonsR;
	}

	public double getPearsonsCorrelationValue() {
		return pearsonsCorrelationValue;
	}
}
