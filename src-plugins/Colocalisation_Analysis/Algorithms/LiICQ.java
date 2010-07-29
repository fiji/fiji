import weka.gui.experiment.ResultsPanel;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * This algorithm calculates Li et al.'s ICQ (intensity
 * correlation quotient).
 *
 * @param <T>
 */
public class LiICQ<T extends RealType<T>> extends Algorithm {

	@Override
	public void execute(DataContainer container)
			throws MissingPreconditionException {
		double mean1 = container.getMeanCh1();
		double mean2 = container.getMeanCh2();

		// get the 2 images for the calculation of Li's ICQ
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// calculate ICQ value
		double icqValue = calculateLisICQ(img1, mean1, img2, mean2);

		// save the result in the container
		container.add( new Result.SimpleValueResult("Li's ICQ", icqValue));
	}

	/**
	 * Calculates Li et al.'s intensity correlation quotient (ICQ) for
	 * two images.
	 *
	 * @param img1 The first image
	 * @param mean1 The first images mean
	 * @param img2 The second image
	 * @param mean2 The second images mean
	 * @return Li et al.'s ICQ value
	 */
	public double calculateLisICQ(Image<T> img1, double mean1, Image<T> img2, double mean2) {
		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		/* variables to count the positive and negative results
		 * of Li's product of the difference of means.
		 */
		long numPositiveProducts = 0;
		long numNegariveProducts = 0;
		// iterate over image
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			double productOfDifferenceOfMeans = (mean1 - ch1) * (mean2 - ch2);

			// check for positive and negative values
			if (productOfDifferenceOfMeans < 0.0 )
				++numNegariveProducts;
			else
				++numPositiveProducts;
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		/* calculate Li's ICQ value by dividing the amount of "positive pixels" to the
		 * total number of pixels. Then shift it in the -0.5,0.5 range.
		 */
		return ( (double) numPositiveProducts / (double) (numNegariveProducts + numPositiveProducts) ) - 0.5;
	}
}
