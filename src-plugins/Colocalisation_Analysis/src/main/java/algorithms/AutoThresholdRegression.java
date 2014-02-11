package algorithms;

import gadgets.DataContainer;
import gadgets.ThresholdMode;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * A class implementing the automatic finding of a threshold
 * used for Person colocalisation calculation.
 */
public class AutoThresholdRegression<T extends RealType< T >> extends Algorithm<T> {
	/* the threshold for y-intercept to y-max to
	 *  raise a warning about it being to high.
	 */
	final double warnYInterceptToYMaxRatioThreshold = 0.01;
	// the slope and and intercept of the regression line
	double autoThresholdSlope = 0.0, autoThresholdIntercept = 0.0;
	/* The thresholds for both image channels. Pixels below a lower
	 * threshold do NOT include the threshold and pixels above an upper
	 * one will NOT either. Pixels "in between (and including)" thresholds
	 * do include the threshold values.
	 */
	T ch1MinThreshold, ch1MaxThreshold, ch2MinThreshold, ch2MaxThreshold;
	// additional information
	double bToYMaxRatio = 0.0;
	//This is the Pearson's correlation we will use for further calculations
	PearsonsCorrelation<T> pearsonsCorrellation;

	public AutoThresholdRegression(PearsonsCorrelation<T> pc) {
		super("auto threshold regression");
		pearsonsCorrellation = pc;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images for the calculation of Pearson's
		final RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		final RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		double ch1Mean = container.getMeanCh1();
		double ch2Mean = container.getMeanCh2();

		double combinedMean = ch1Mean + ch2Mean;

		// get the cursors for iterating through pixels in images
		TwinCursor<T> cursor = new TwinCursor<T>(
				img1.randomAccess(), img2.randomAccess(),
				Views.iterable(mask).localizingCursor());

		// variables for summing up the
		double ch1MeanDiffSum = 0.0, ch2MeanDiffSum = 0.0, combinedMeanDiffSum = 0.0;
		double combinedSum = 0.0;
		int N = 0, NZero = 0;

		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getFirst();
			double ch1 = type1.getRealDouble();
			T type2 = cursor.getSecond();
			double ch2 = type2.getRealDouble();

			combinedSum = ch1 + ch2;

			// TODO: Shouldn't the whole calculation take only pixels
			// into account that are combined above zero? And not just
			// the denominator (like it is done now)?

			// calculate the numerators for the variances
			ch1MeanDiffSum += (ch1 - ch1Mean) * (ch1 - ch1Mean);
			ch2MeanDiffSum += (ch2 - ch2Mean) * (ch2 - ch2Mean);
			combinedMeanDiffSum += (combinedSum - combinedMean) * (combinedSum - combinedMean);

			// count only pixels that are above zero
			if ( (ch1 + ch2) > 0.00001)
				NZero++;

			N++;
		}

		double ch1Variance = ch1MeanDiffSum / (N - 1);
		double ch2Variance = ch2MeanDiffSum / (N - 1);
		double combinedVariance = combinedMeanDiffSum / (N - 1.0);

		//http://mathworld.wolfram.com/Covariance.html
		//?2 = X2?(X)2
		// = E[X2]?(E[X])2
		//var (x+y) = var(x)+var(y)+2(covar(x,y));
		//2(covar(x,y)) = var(x+y) - var(x)-var(y);

		double ch1ch2Covariance = 0.5*(combinedVariance - (ch1Variance + ch2Variance));

		// calculate regression parameters
		double denom = 2*ch1ch2Covariance;
		double num = ch2Variance - ch1Variance
			+ Math.sqrt( (ch2Variance - ch1Variance) * (ch2Variance - ch1Variance)
					+ (4 * ch1ch2Covariance *ch1ch2Covariance) );

		double m = num/denom;
		double b = ch2Mean - m*ch1Mean ;

		// initialize some variables relevant for regression
		// indicates whether the threshold has been found or not
		boolean thresholdFound = false;
		// the maximum number of iterations to look for the threshold
		final int maxIterations = 100;
		// the current iteration
		int iteration = 0;
		// the initial thresholds
		double threshold1 = (container.getMaxCh1() + container.getMinCh1()) * 0.5;
		double threshold2 = container.getMaxCh1();

		// Min threshold not yet implemented
		double ch1ThreshMax = container.getMaxCh1();
		double ch2ThreshMax = container.getMaxCh2();

		// define some image type specific threshold variables
		T thresholdCh1 = Util.getTypeFromRandomAccess(img1).createVariable();
		T thresholdCh2 = Util.getTypeFromRandomAccess(img2).createVariable();
		// reset the previously created cursor
		cursor.reset();

		// do regression
		while (!thresholdFound && iteration<maxIterations) {
			// round ch1 threshold and compute ch2 threshold
			ch1ThreshMax = Math.round( threshold1 );
			ch2ThreshMax = Math.round( (ch1ThreshMax * m) + b );
			// set the image type specific variables
			thresholdCh1.setReal( ch1ThreshMax );
			thresholdCh2.setReal( ch2ThreshMax );
			// Person's R value
			double currentPersonsR = Double.MAX_VALUE;
			// indicates if we have actually found a real number
			boolean badResult = false;
			try {
				// do persons calculation within the limits
				currentPersonsR = pearsonsCorrellation.calculatePearsons(cursor,
						ch1Mean, ch2Mean, thresholdCh1, thresholdCh2, ThresholdMode.Below);
			} catch (MissingPreconditionException e) {
				/* the exception that could occur is due to numerical
				 * problems within the pearsons calculation.
				 */
				badResult = true;
			}

			/* If the difference between both thresholds is < 1, we consider
			 * that as reasonable close to abort the regression.
			 */
			final double thrDiff = Math.abs(threshold1 - threshold2);
			if (thrDiff < 1.0)
				thresholdFound = true;

			// update working thresholds
			threshold2 = threshold1;
			if (badResult || currentPersonsR < 0) {
				// we went too far, increase by the absolute half
				threshold1 = threshold1 + thrDiff * 0.5;
			} else if (currentPersonsR > 0) {
				// as long as r > 0 we go half the way down
				threshold1 = threshold1 - thrDiff * 0.5;
			}

			// reset the cursor to reuse it
			cursor.reset();

			// increment iteration counter
			iteration++;
		}

		/* Get min and max value of image data type. Since type of image
		 * one and two are the same, we dont't need to distinguish them.
		 */
		T dummyT = Util.getTypeFromRandomAccess(img1).createVariable();
		double minVal = dummyT.getMinValue();
		double maxVal = dummyT.getMaxValue();

		/* Store the new results. The lower thresholds are the types
		 * min value for now. For the max threshold we do a clipping
		 * to make it fit into the image type.
		 */
		ch1MinThreshold = Util.getTypeFromRandomAccess(img1).createVariable();
		ch1MinThreshold.setReal(minVal);

		ch1MaxThreshold = Util.getTypeFromRandomAccess(img1).createVariable();
		if ( minVal > ch1ThreshMax )
			ch1MaxThreshold.setReal( minVal );
		else if ( maxVal < ch1ThreshMax )
			ch1MaxThreshold.setReal( maxVal );
		else
			ch1MaxThreshold.setReal( ch1ThreshMax );

		ch2MinThreshold = Util.getTypeFromRandomAccess(img2).createVariable();
		ch2MinThreshold.setReal(minVal);

		ch2MaxThreshold = Util.getTypeFromRandomAccess(img2).createVariable();
		if ( minVal > ch2ThreshMax )
			ch2MaxThreshold.setReal( minVal );
		else if ( maxVal < ch2ThreshMax )
			ch2MaxThreshold.setReal( maxVal );
		else
			ch2MaxThreshold.setReal( ch2ThreshMax );

		autoThresholdSlope = m;
		autoThresholdIntercept = b;
		bToYMaxRatio = b / container.getMaxCh2();

		// add warnings if values are not in tolerance range
		if ( Math.abs(bToYMaxRatio) > warnYInterceptToYMaxRatioThreshold ) {
			addWarning("y-intercept high",
				"The absolute y-intercept of the auto threshold regression line is high. Maybe you should use a ROI, maybe do a background subtraction in both channels.");
		}

		// add warning if threshold is above the image mean
		if (ch1ThreshMax > ch1Mean) {
			addWarning("Threshold of ch. 1 too high",
					"Too few pixels are taken into account for above-threshold calculations. The threshold is above the channel's mean.");
		}
		if (ch2ThreshMax > ch2Mean) {
			addWarning("Threshold of ch. 2 too high",
					"Too few pixels are taken into account for above-threshold calculations. The threshold is above the channel's mean.");
		}

		// add warnings if values are below lowest pixel value of images
		if ( ch1ThreshMax < container.getMinCh1() || ch2ThreshMax < container.getMinCh2() ) {
			addWarning("thresholds too low",
				"The auto threshold method could not find a positive threshold.");
		}
	}

	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);

		handler.handleValue( "m (slope)", autoThresholdSlope , 2 );
		handler.handleValue( "b (y-intercept)", autoThresholdIntercept, 2 );
		handler.handleValue( "b to y-max ratio", bToYMaxRatio, 2 );
		handler.handleValue( "Ch1 Max Threshold", ch1MaxThreshold.getRealDouble(), 2);
		handler.handleValue( "Ch2 Max Threshold", ch2MaxThreshold.getRealDouble(), 2);
	}

	public double getBToYMaxRatio() {
		return bToYMaxRatio;
	}

	public double getWarnYInterceptToYMaxRatioThreshold() {
		return warnYInterceptToYMaxRatioThreshold;
	}

	public double getAutoThresholdSlope() {
		return autoThresholdSlope;
	}

	public double getAutoThresholdIntercept() {
		return autoThresholdIntercept;
	}

	public T getCh1MinThreshold() {
		return ch1MinThreshold;
	}

	public T getCh1MaxThreshold() {
		return ch1MaxThreshold;
	}

	public T getCh2MinThreshold() {
		return ch2MinThreshold;
	}

	public T getCh2MaxThreshold() {
		return ch2MaxThreshold;
	}
}
