import ij.IJ;
import mpicbg.imglib.image.ImageFactory;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.container.array.ArrayContainerFactory;

/**
 * Represents the creation of a 2D histogram between two images.
 * Channel 1 is set out in x direction, while channel 2 in y direction.
 * @param <T>
 */
public class Histogram2D<T extends RealType<T>> extends Algorithm {

	// The width of the scatter-plot
	protected final int xBins = 256;
	// The height of the scatter-plot
	protected final int yBins = 256;
	// The name of the result 2D histogram to pass elsewhere
	protected String title;


	public Histogram2D(){
		this("2D Histogram");
	}

	public Histogram2D(String title){
		this.title = title;
	}

	public void execute(DataContainer container) throws MissingPreconditionException {

		double ch1Max = container.getMaxCh1();
		double ch2Max = container.getMaxCh2();

		double ch1Scaling = (double) xBins / (double)(ch1Max + 1);
		double ch2Scaling = (double) yBins / (double)(ch2Max + 1);

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		// create a ImageFactory<Type<T>> put the scatter-plot in
		final ImageFactory<FloatType> scatterFactory =
			new ImageFactory<FloatType>(new FloatType(), new ArrayContainerFactory());
		Image<FloatType> plotImage = scatterFactory.createImage(new int[] {xBins, yBins}, "2D Histogram / Scatterplot");

		// create access cursors
		final LocalizableByDimCursor<FloatType> histogram2DCursor =
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
			int scaledYvalue = (yBins - 1) - (int)(ch2 * ch2Scaling);
			// set position of input/output cursor
			histogram2DCursor.setPosition( new int[] {scaledXvalue, scaledYvalue});
			// get current value at position and increment it
			float count = histogram2DCursor.getType().getRealFloat();
			count++;

			histogram2DCursor.getType().set(count);
		}

		Result result = new Result.Histogram2DResult(title, plotImage, "Channel 1", "Channel 2");
		container.add(result);
	}
}
