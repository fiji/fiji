package mpicbg.imglib.cursor.special;

import mpicbg.imglib.cursor.special.meta.AboveThresholdPredicate;
import mpicbg.imglib.cursor.special.meta.BelowThresholdPredicate;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.Type;

/**
 * The TwinValueRangeCursorFactory provides some convenience methods
 * to create new TwinValueRangeCursors.
 *
 * @author Dan White & Tom Kazimiers
 */
public class TwinValueRangeCursorFactory
{
	/**
	 * Factory that generates a twin value cursor with no constraints in value or range.
	 *
	 * @param <T> The type of the images to work on. Needs to be a Comparable.
	 * @param img1 The first image to iterate over.
	 * @param img2 The second image to iterate over.
	 * @return A TwinValueRangeCursor that walks over all values of the images.
	 */
	public static < T extends Type<T> & Comparable<T> > TwinValueRangeCursor<T> generateAlwaysTrueCursor(Image<T> img1, Image<T> img2) {
		return new TwinValueRangeCursor< T > (img1.createCursor(), img2.createCursor());
	}

	/**
	 * Factory to generate a twin value cursor using an above threshold predicate. The
	 * generated cursor will walk only over values above that threshold (threshold not
	 * included).
	 *
	 * @param <T> The type of the images to work on. Needs to be a Comparable.
	 * @param img1 The first image to iterate over.
	 * @param img2 The second image to iterate over.
	 * @param threshold1 The threshold for image one.
	 * @param threshold2 The threshold for image two.
	 * @return A TwinValueRangeCursor that walks over values above the thresholds.
	 */
	public static < T extends Type<T> & Comparable< T > > TwinValueRangeCursor<T> generateAboveThresholdCursor(Image<T> img1, Image<T> img2,
			T threshold1, T threshold2) {
		AboveThresholdPredicate< T > predicate1 = new AboveThresholdPredicate< T >(threshold1);
		AboveThresholdPredicate< T > predicate2 = new AboveThresholdPredicate< T >(threshold2);
		return new TwinValueRangeCursor< T > (img1.createCursor(), img2.createCursor(), predicate1, predicate2);
	}

	/**
	 * Factory to generate a twin value cursor using a below threshold predicate. The
	 * generated cursor will walk only over values below that threshold (threshold not
	 * included).
	 *
	 * @param <T> The type of the images to work on. Needs to be a Comparable.
	 * @param img1 The first image to iterate over.
	 * @param img2 The second image to iterate over.
	 * @param threshold1 The threshold for image one.
	 * @param threshold2 The threshold for image two.
	 * @return A TwinValueRangeCursor that walks over values below the thresholds.
	 */
	public static < T extends Type<T> & Comparable< T > > TwinValueRangeCursor<T> generateBelowThresholdCursor(Image<T> img1, Image<T> img2,
			T threshold1, T threshold2) {
		BelowThresholdPredicate< T > predicate1 = new BelowThresholdPredicate< T >(threshold1);
		BelowThresholdPredicate< T > predicate2 = new BelowThresholdPredicate< T >(threshold2);
		return new TwinValueRangeCursor< T > (img1.createCursor(), img2.createCursor(), predicate1, predicate2);
	}
}
