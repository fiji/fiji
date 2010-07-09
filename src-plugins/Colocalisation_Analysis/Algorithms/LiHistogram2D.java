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

	public LiHistogram2D() {
		this("Histogram 2D (Li)");
	}

	public LiHistogram2D(String title) {
		this(title, false);
	}

	public LiHistogram2D(String title, boolean swapChannels) {
		super(title, swapChannels);
	}

	public void execute(DataContainer container) throws MissingPreconditionException {
		ch1Mean = swapChannels ? container.getMeanCh2() : container.getMeanCh1();
		ch2Mean = swapChannels ? container.getMeanCh1() : container.getMeanCh2();

		ch1Min = getMinCh1(container);
		ch1Max = getMaxCh1(container);

		ch2Min = getMinCh2(container);
		ch2Max = getMaxCh2(container);

		/* A scaling to the x bins has to be made:
		 * For that to work we need the min and the
		 * max value that could occur. That is
		 * min: (mean1 - ch1max) * (mean2 - ch2max)
		 * max: (mean1 - ch1min) * (mean2 - ch2min)
		 */
		liMin = (ch1Mean - ch1Max) * (ch2Mean - ch2Max);
		liMax = (ch1Mean - ch1Min) * (ch2Mean - ch2Min);
		if (liMin > liMax) {
			double min = liMin;
			liMin = liMax;
			liMax = min;
		}
		liDiff = Math.abs(liMax - liMin);

		super.execute(container);
	}

	@Override
	protected double getCh1BinWidth(DataContainer container) {
		return (double) xBins / (double)(liDiff + 1);
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
		return (yBins - 1) - (int)(ch2Val * ch2BinWidth);
	}
}