import ij.IJ;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A class that calculates the minimum and maximum intensity value of the two source
 * images in the data container.
 *
 * @param <T>
 */
public class CalculateMinMax<T extends RealType<T>> extends Algorithm {

	public void execute(DataContainer container) throws MissingPreconditionException {
		// check if min and max have already been calculated
		boolean ch1MinCalculated = container.get(DataContainer.DataTags.MinCh1) != null;
		boolean ch1MaxCalculated = container.get(DataContainer.DataTags.MaxCh1) != null;
		boolean ch2MinCalculated = container.get(DataContainer.DataTags.MinCh2) != null;
		boolean ch2MaxCalculated = container.get(DataContainer.DataTags.MaxCh2) != null;

		if (ch1MinCalculated) {
			IJ.log("[Calculate Min] The min of channel 1 seems to be calculated already.");
		} else {
			double min = getImageMin(container.getSourceImage1());
			Result result= new Result.SimpleValueResult("Min of channel 1", min);
			container.add(result, DataContainer.DataTags.MinCh1);
		}

		if (ch1MaxCalculated) {
			IJ.log("[Calculate Max] The max of channel 1 seems to be calculated already.");
		} else {
			double max = getImageMax(container.getSourceImage1());
			Result result= new Result.SimpleValueResult("Max of channel 1", max);
			container.add(result, DataContainer.DataTags.MaxCh1);
		}

		if (ch2MinCalculated) {
			IJ.log("[Calculate Min] The min of channel 2 seems to be calculated already.");
		} else {
			double min = getImageMin(container.getSourceImage2());
			Result result= new Result.SimpleValueResult("Min of channel 2", min);
			container.add(result, DataContainer.DataTags.MinCh2);
		}

		if (ch2MaxCalculated) {
			IJ.log("[Calculate Max] The max of channel 2 seems to be calculated already.");
		} else {
			double max = getImageMax(container.getSourceImage2());
			Result result= new Result.SimpleValueResult("Max of channel 2", max);
			container.add(result, DataContainer.DataTags.MaxCh2);
		}
	}

	/**
	 * Calculates the min of an image.
	 *
	 * @param img The image to calculate the min of
	 * @return The min of the image passed
	 */
	protected double getImageMin(Image<T> img) {
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
	protected double getImageMax(Image<T> img) {
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
