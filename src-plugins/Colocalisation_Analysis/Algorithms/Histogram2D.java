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
	// Swap or not swap ch1 and ch2
	boolean swapChannels = false;
	// member variables for labeling
	String ch1Label = "Channel 1";
	String ch2Label = "Channel 2";

	public Histogram2D(){
		this("2D Histogram");
	}

	public Histogram2D(String title){
		this(title, false);
	}

	public Histogram2D(String title, boolean swapChannels){
		this.title = title;
		this.swapChannels = swapChannels;
	}

	public void execute(DataContainer container) throws MissingPreconditionException {

		double ch1Max = swapChannels ? container.getMaxCh2() : container.getMaxCh1();
		double ch2Max = swapChannels ? container.getMaxCh1() : container.getMaxCh2();

		double ch1BinWidth = (double) xBins / (double)(ch1Max + 1);
		double ch2BinWidth = (double) yBins / (double)(ch2Max + 1);

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = swapChannels ? container.getSourceImage2() : container.getSourceImage1();
		Image<T> img2 = swapChannels ? container.getSourceImage1() : container.getSourceImage2();

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
			int scaledXvalue = (int)(ch1 * ch1BinWidth);
			int scaledYvalue = (yBins - 1) - (int)(ch2 * ch2BinWidth);
			// set position of input/output cursor
			histogram2DCursor.setPosition( new int[] {scaledXvalue, scaledYvalue});
			// get current value at position and increment it
			float count = histogram2DCursor.getType().getRealFloat();
			count++;

			histogram2DCursor.getType().set(count);
		}

		String label1 = swapChannels ? ch2Label : ch1Label;
		String label2 = swapChannels ? ch1Label : ch2Label;

		Result result = new Result.Histogram2DResult(title, plotImage, ch1BinWidth, ch2BinWidth, label1, label2);
		container.add(result);
	}
}
