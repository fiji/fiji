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

		//put the result into the DataContainer
		Result result = new Result.SimpleValueResult("Pearson's Correlation (Classic)", pearsonDenominator / pearsonNumerator);
		container.add(result);
	}

	public void fastPearsons(DataContainer container) {
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

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

		double pearsons1 = sumProduct1_2 - (sum1*sum2/N);
		double pearsons2 = sum1squared - (sum1*sum1/N);
		double pearsons3 = sum2squared - (sum2*sum2/N);
		double pearsonsR = pearsons1/(Math.sqrt(pearsons2*pearsons3));

		//put the result into the DataContainer
		Result result = new Result.SimpleValueResult("Pearson's Correlation (Fast)", pearsonsR);
		container.add(result);
	}
}
