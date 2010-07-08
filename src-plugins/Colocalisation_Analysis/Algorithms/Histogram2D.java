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
	protected int xBins = 256;
	// The height of the scatter-plot
	protected int yBins = 256;
	// The name of the result 2D histogram to pass elsewhere
	protected String title;
	// Swap or not swap ch1 and ch2
	protected boolean swapChannels = false;
	// member variables for labeling
	protected String ch1Label = "Channel 1";
	protected String ch2Label = "Channel 2";

	public Histogram2D(){
		this("2D Histogram");
	}

	public Histogram2D(String title){
		this(title, false);
	}

	public Histogram2D(String title, boolean swapChannels){
		this.title = title;
		this.swapChannels = swapChannels;

		if (swapChannels) {
			int xBins = this.xBins;
			this.xBins = this.yBins;
			this.yBins = xBins;
		}
	}

	/**
	 * Gets the maximum of channel one. Takes channel
	 * swapping into consideration and will return max
	 * of channel two if swapped.
	 *
	 * @return The maximum of what is seen as channel one.
	 */
	protected double getMaxCh1(DataContainer container) {
		return swapChannels ? container.getMaxCh2() : container.getMaxCh1();
	}

	/**
	 * Gets the maximum of channel two. Takes channel
	 * swapping into consideration and will return max
	 * of channel one if swapped.
	 *
	 * @return The maximum of what is seen as channel two.
	 */
	protected double getMaxCh2(DataContainer container) {
		return swapChannels ? container.getMaxCh1() : container.getMaxCh2();
	}

	/**
	 * Gets the image of channel one. Takes channel
	 * swapping into consideration and will return image
	 * of channel two if swapped.
	 *
	 * @return The image of what is seen as channel one.
	 */
	protected Image<T> getImageCh1(DataContainer container) {
		return swapChannels ? container.getSourceImage2() : container.getSourceImage1();
	}

	/**
	 * Gets the image of channel two. Takes channel
	 * swapping into consideration and will return image
	 * of channel one if swapped.
	 *
	 * @return The image of what is seen as channel two.
	 */
	protected Image<T> getImageCh2(DataContainer container) {
		return swapChannels ? container.getSourceImage1() : container.getSourceImage2();
	}

	/**
	 * Gets the label of channel one. Takes channel
	 * swapping into consideration and will return label
	 * of channel two if swapped.
	 *
	 * @return The label of what is seen as channel one.
	 */
	protected String getLabelCh1() {
		return swapChannels ? ch2Label : ch1Label;
	}

	/**
	 * Gets the label of channel two. Takes channel
	 * swapping into consideration and will return label
	 * of channel one if swapped.
	 *
	 * @return The label of what is seen as channel two.
	 */
	protected String getLabelCh2() {
		return swapChannels ? ch1Label : ch2Label;
	}

	public void execute(DataContainer container) throws MissingPreconditionException {

		double ch1Max = getMaxCh1(container);
		double ch2Max = getMaxCh2(container);

		double ch1BinWidth = (double) xBins / (double)(ch1Max + 1);
		double ch2BinWidth = (double) yBins / (double)(ch2Max + 1);

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = getImageCh1(container);
		Image<T> img2 = getImageCh2(container);

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
			int scaledXvalue = getXValue(ch1, ch1BinWidth, ch2, ch2BinWidth);
			int scaledYvalue = getYValue(ch1, ch1BinWidth, ch2, ch2BinWidth);
			// set position of input/output cursor
			histogram2DCursor.setPosition( new int[] {scaledXvalue, scaledYvalue});
			// get current value at position and increment it
			float count = histogram2DCursor.getType().getRealFloat();
			count++;

			histogram2DCursor.getType().set(count);
		}

		String label1 = getLabelCh1();
		String label2 = getLabelCh2();

		Result result = new Result.Histogram2DResult(title, plotImage, ch1BinWidth, ch2BinWidth, label1, label2);
		container.add(result);
	}

	/**
	 * Calculates the locations x value.
	 * @param ch1Val The intensity of channel one
	 * @param ch1BinWidt The bin width for channel one
	 * @return The x value of the data point location
	 */
	protected int getXValue(double ch1Val, int ch1BinWidth, double ch2Val, int ch2BinWidth) {
		return (int)(ch1Val * ch1BinWidth);
	}

	/**
	 * Calculates the locations y value.
	 * @param ch2Val The intensity of channel one
	 * @param ch2BinWidt The bin width for channel one
	 * @return The x value of the data point location
	 */
	protected int getYValue(double ch1Val, int ch1BinWidth, double ch2Val, int ch2BinWidth) {
		return (yBins - 1) - (int)(ch2Val * ch2BinWidth);
	}
}
