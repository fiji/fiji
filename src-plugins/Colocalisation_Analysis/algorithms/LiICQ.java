package algorithms;

import gadgets.DataContainer;
import mpicbg.imglib.cursor.special.TwinCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.logic.BitType;
import mpicbg.imglib.type.numeric.RealType;
import results.ResultHandler;

/**
 * This algorithm calculates Li et al.'s ICQ (intensity
 * correlation quotient).
 *
 * @param <T>
 */
public class LiICQ<T extends RealType<T>> extends Algorithm<T> {
	// the resulting ICQ value
	double icqValue;

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		double mean1 = container.getMeanCh1();
		double mean2 = container.getMeanCh2();

		// get the 2 images for the calculation of Li's ICQ
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();
		Image<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.createLocalizableByDimCursor(),
				img2.createLocalizableByDimCursor(), mask.createLocalizableCursor());
		// calculate ICQ value
		icqValue = calculateLisICQ(cursor, mean1, mean2);
		cursor.close();
	}

	/**
	 * Calculates Li et al.'s intensity correlation quotient (ICQ) for
	 * two images.
	 *
	 * @param cursor A TwinCursor that iterates over two images
	 * @param mean1 The first images mean
	 * @param mean2 The second images mean
	 * @return Li et al.'s ICQ value
	 */
	public static <T extends RealType<T>> double calculateLisICQ(TwinCursor<T> cursor, double mean1, double mean2) {
		/* variables to count the positive and negative results
		 * of Li's product of the difference of means.
		 */
		long numPositiveProducts = 0;
		long numNegariveProducts = 0;
		// iterate over image
		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getChannel1();
			T type2 = cursor.getChannel1();
			double ch1 = type1.getRealDouble();
			double ch2 = type2.getRealDouble();

			double productOfDifferenceOfMeans = (mean1 - ch1) * (mean2 - ch2);

			// check for positive and negative values
			if (productOfDifferenceOfMeans < 0.0 )
				++numNegariveProducts;
			else
				++numPositiveProducts;
		}

		/* calculate Li's ICQ value by dividing the amount of "positive pixels" to the
		 * total number of pixels. Then shift it in the -0.5,0.5 range.
		 */
		return ( (double) numPositiveProducts / (double) (numNegariveProducts + numPositiveProducts) ) - 0.5;
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Li's ICQ value", icqValue);
	}
}
