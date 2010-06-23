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
public class CalculateMeans<T extends RealType<T>> extends Algorithm {

	public void execute(DataContainer container) throws MissingPreconditionException {
		// check if means have already been calculated
		boolean ch1MeanCalculated = container.get(DataContainer.DataTags.MeanCh1) != null;
		boolean ch2MeanCalculated = container.get(DataContainer.DataTags.MeanCh2) != null;

		if (ch1MeanCalculated) {
			IJ.log("[Calculate Means] The mean of channel 1 seems to be calculated already.");
		} else {
			double mean = getImageMean(container.getSourceImage1());
			Result result= new Result.SimpleValueResult("Mean of channel 1", mean);
			container.add(result, DataContainer.DataTags.MeanCh1);
		}

		if (ch2MeanCalculated) {
			IJ.log("[Calculate Means] The mean of channel 2 seems to be calculated already.");
		} else {
			double mean = getImageMean(container.getSourceImage2());
			Result result= new Result.SimpleValueResult("Mean of channel 2", mean);
			container.add(result, DataContainer.DataTags.MeanCh2);
		}
	}

	/**
	 * Calculates the mean of an image.
	 *
	 * @param img The image to calculate the mean of
	 * @return The mean of the image passed
	 */
	protected double getImageMean(Image<T> img) {
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
}
