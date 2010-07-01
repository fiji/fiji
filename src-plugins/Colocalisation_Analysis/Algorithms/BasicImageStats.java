import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

public class BasicImageStats {
	/**
	 * Calculates the mean of an image.
	 *
	 * @param img The image to calculate the mean of
	 * @return The mean of the image passed
	 */
	public static <T extends RealType<T>> double getImageMean(Image<T> img) {
		  double sum = 0;
		  Cursor<T> cursor = img.createCursor();
		  while (cursor.hasNext()) {
			  cursor.fwd();
			  T type = cursor.getType();
			  sum += type.getRealDouble();
		  }
		  cursor.close();
		  return sum / img.getNumPixels();
	  }

	/**
	 * Calculates the min of an image.
	 *
	 * @param img The image to calculate the min of
	 * @return The min of the image passed
	 */
	public static <T extends RealType<T>> double getImageMin(Image<T> img) {
		  double min = img.createType().getMaxValue();
		  Cursor<T> cursor = img.createCursor();
		  while (cursor.hasNext()) {
			  cursor.fwd();
			  T type = cursor.getType();
			  double currValue = type.getRealDouble();
			  if (currValue < min)
				  min = currValue;
		  }
		  cursor.close();
		  return min;
	  }

	/**
	 * Calculates the max of an image.
	 *
	 * @param img The image to calculate the max of
	 * @return The max of the image passed
	 */
	public static <T extends RealType<T>> double getImageMax(Image<T> img) {
		  double max = img.createType().getMinValue();
		  Cursor<T> cursor = img.createCursor();
		  while (cursor.hasNext()) {
			  cursor.fwd();
			  T type = cursor.getType();
			  double currValue = type.getRealDouble();
			  if (currValue > max)
				  max = currValue;
		  }
		  cursor.close();
		  return max;
	  }
}