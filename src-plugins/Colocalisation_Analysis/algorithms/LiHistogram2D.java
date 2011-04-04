package algorithms;

import gadgets.DataContainer;

import java.util.EnumSet;

import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * Represents the creation of a 2D histogram between two images.
 * Channel 1 is set out in x direction, while channel 2 in y direction.
 * The value calculation is done after Li.
 * @param <T>
 */
public class LiHistogram2D<T extends RealType<T>> extends Histogram2D<T> {
	// On execution these variables hold the images means
	double ch1Mean, ch2Mean;

	// On execution these variables hold the images min and max
	double ch1Min, ch1Max, ch2Min, ch2Max;

	// On execution these variables hold the Li's value min and max and their difference
	double liMin, liMax, liDiff;

	// On execution these variables hold the scaling factors
	double ch1Scaling, ch2Scaling;

	// boolean to test which channel we are using for eg. Li 2D histogram y axis
	boolean useCh1 = true;

	public LiHistogram2D(boolean useCh1) {
		this("Histogram 2D (Li)", useCh1);
	}

	public LiHistogram2D(String title, boolean useCh1) {
		this(title, false, useCh1);
	}

	public LiHistogram2D(String title, boolean swapChannels, boolean useCh1) {
		this(title, swapChannels, useCh1, EnumSet.of(DrawingFlags.Plot));
	}

	public LiHistogram2D(String title, boolean swapChannels, boolean useCh1, EnumSet<DrawingFlags> drawingSettings) {
		super(title, swapChannels, drawingSettings);
		this.useCh1 = useCh1;
	}

	public void execute(DataContainer<T> container) throws MissingPreconditionException {
		ch1Mean = swapChannels ? container.getMeanCh2() : container.getMeanCh1();
		ch2Mean = swapChannels ? container.getMeanCh1() : container.getMeanCh2();

		ch1Min = getMinCh1(container);
		ch1Max = getMaxCh1(container);

		ch2Min = getMinCh2(container);
		ch2Max = getMaxCh2(container);

		/* A scaling to the x bins has to be made:
		 * For that to work we need the min and the
		 * max value that could occur.
		 */

		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = getImageCh1(container);
		Image<T> img2 = getImageCh2(container);

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		// give liMin and liMax appropriate starting values at the top and bottom of the range
		liMin = Double.MAX_VALUE;
		liMax = Double.MIN_VALUE;

		// iterate over image
		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			double productOfDifferenceOfMeans = (ch1Mean - ch1) * (ch2Mean - ch2);

			if (productOfDifferenceOfMeans < liMin)
				liMin = productOfDifferenceOfMeans;
			if (productOfDifferenceOfMeans > liMax)
				liMax = productOfDifferenceOfMeans;
		}
		liDiff = Math.abs(liMax - liMin);

		generateHistogramData(container);
	}

	@Override
	protected double getXBinWidth(DataContainer<T> container) {
		return (double) xBins / (double)(liDiff + 1);
	}

	@Override
	protected double getYBinWidth(DataContainer<T> container) {
		double max;
		if (useCh1) {
			max = getMaxCh1(container);
		}
		else {
			max = getMaxCh2(container);
		}
		return (double) yBins / (double)(max + 1);
	}

	@Override
	protected int getXValue(double ch1Val, double ch1BinWidth, double ch2Val, double ch2BinWidth) {
		/* We want the values to be scaled and shifted by and
		 * offset in a way that the smallest (possibly negative)
		 * value is in first bin and highest value in largest bin.
		 */
		return (int)( (( (ch1Mean - ch1Val) * (ch2Mean - ch2Val)) - liMin) * ch1BinWidth);
	}

	@Override
	protected int getYValue(double ch1Val, double ch1BinWidth, double ch2Val, double ch2BinWidth) {
		if (useCh1)
			return (yBins - 1) - (int)(ch1Val * ch2BinWidth);
		else
			return (yBins - 1) - (int)(ch2Val * ch2BinWidth);
	}

	@Override
	protected double getXMin(DataContainer<T> container) {
		return swapChannels ? (useCh1 ? container.getMinCh1(): container.getMinCh2()) : liMin;
	}

	protected double getXMax(DataContainer<T> container) {
		return swapChannels ? (useCh1 ? container.getMaxCh1(): container.getMaxCh2()) : liMax;
	}

	protected double getYMin(DataContainer<T> container) {
		return swapChannels ? liMin : (useCh1 ? container.getMinCh1(): container.getMinCh2());
	}

	protected double getYMax(DataContainer<T> container) {
		return swapChannels ? liMax : (useCh1 ? container.getMaxCh1(): container.getMaxCh2());
	}
}
