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
		// TODO explicitly name generic type args for method call???
		//Result ch1MeanResult = container.<Result.SimpleValueResult>get(DataContainer.DataTags.MeanCh1, Result.SimpleValueResult.class);

		Result ch1MeanResult = container.get(DataContainer.DataTags.MeanCh1, Result.SimpleValueResult.class);
		Result ch2MeanResult = container.get(DataContainer.DataTags.MeanCh2, Result.SimpleValueResult.class);

		// check if means have already been calculated
		boolean ch1MeanCalculated = ch1MeanResult != null;
		boolean ch2MeanCalculated = ch2MeanResult != null;

		if (!ch1MeanCalculated) {
			IJ.log("[Calculate Means] The mean of channel 1 is not calculated already.");
			throw new MissingPreconditionException("Mean of channel 1 is not present in the DataContainer object.");
		}

		if (!ch2MeanCalculated) {
			IJ.log("[Calculate Means] The mean of channel 2 is not calculated already.");
			throw new MissingPreconditionException("Mean of channel 2 is not present in the DataContainer object.");
		}

		// Cast the results into SimpleValueResult type
		double ch1Mean = ((Result.SimpleValueResult)ch1MeanResult).getValue();
		double ch2Mean = ((Result.SimpleValueResult)ch2MeanResult).getValue();

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

	}
}
