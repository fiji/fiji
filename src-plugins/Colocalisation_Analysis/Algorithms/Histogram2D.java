import ij.IJ;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.ShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.container.array.ArrayContainerFactory;

/**
 * Represents the creation of a 2D histogram between two images.
 * Channel 1 is set out in x direction, while channel 2 in y direction.
 * @param <T>
 */
public class Histogram2D<T extends RealType<T>> extends Algorithm {

	// The width of the scatter-plot
	protected final int width = 256;
	// The height of the scatter-plot
	protected final int height = 256;

	public void execute(DataContainer container) throws MissingPreconditionException {
		Result ch1MaxResult = container.get(DataContainer.DataTags.MaxCh1, Result.SimpleValueResult.class);
		Result ch2MaxResult = container.get(DataContainer.DataTags.MaxCh2, Result.SimpleValueResult.class);

		// check if maximum values have already been calculated
		boolean ch1MaxCalculated = ch1MaxResult != null;
		boolean ch2MaxCalculated = ch2MaxResult != null;

		if (!ch1MaxCalculated) {
			IJ.log("[Calculate Scatter-plot] The maximum value of channel 1 is not calculated already.");
			throw new MissingPreconditionException("Max value of channel 1 is not present in the DataContainer object.");
		}

		if (!ch2MaxCalculated) {
			IJ.log("[Calculate Scatter-plot] The maximum value of channel 2 is not calculated already.");
			throw new MissingPreconditionException("Max of channel 2 is not present in the DataContainer object.");
		}

		// Cast the results into SimpleValueResult type
		double ch1Max = ((Result.SimpleValueResult)ch1MaxResult).getValue();
		double ch2Max = ((Result.SimpleValueResult)ch2MaxResult).getValue();

		double ch1Scaling = (double) (width - 1) / (double)ch1Max;
		double ch2Scaling = (double) (height - 1) / (double)ch2Max;

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		// create a ImageFactory<Type<T>> put the scatter-plot in
		final ImageFactory<ShortType> scatterFactory =
			new ImageFactory<ShortType>(new ShortType(), new ArrayContainerFactory());
		Image<ShortType> plotImage = scatterFactory.createImage(new int[] {width, height}, "2D Histogram / Scatterplot");

		// create access cursors
		final LocalizableByDimCursor<ShortType> histogram2DCursor =
			plotImage.createLocalizableByDimCursor();

		// iterate over image
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			/*
			 * Scale values for both channels to fit in the range.
			 * Moreover mirror the y value on the x axis.
			 */
			int scaledXvalue = (int)(ch1 * ch1Scaling);
			int scaledYvalue = 255 - (int)(ch2 * ch2Scaling);
			// set position of input/output cursor
			histogram2DCursor.setPosition( new int[] {scaledXvalue, scaledYvalue});
			// get current value at position and increment it
			int count = histogram2DCursor.getType().getInteger();
			count++;

			// write out new value if in range
			if (count < 65535) {
				histogram2DCursor.getType().set((short)count);
			}
		}

		Result result = new Result.Histogram2DResult("2D Histogram", plotImage, "Channel 1", "Channel 2");
		container.add(result);
	}
}
