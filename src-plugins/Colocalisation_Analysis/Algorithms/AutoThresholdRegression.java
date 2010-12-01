package algorithms;

import results.ResultHandler;
import gadgets.DataContainer;
import imglib.mpicbg.imglib.cursor.special.TwinValueRangeCursor;
import imglib.mpicbg.imglib.cursor.special.meta.BelowThresholdPredicate;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A class implementing the automatic finding of a threshold
 * used for Person colocalisation calculation.
 */
public class AutoThresholdRegression<T extends RealType<T>> extends Algorithm<T> {
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
	PearsonsCorrelation pearsonsCorrellation;

	public AutoThresholdRegression(PearsonsCorrelation pc){
		pearsonsCorrellation = pc;
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		double ch1Mean = container.getMeanCh1();
		double ch2Mean = container.getMeanCh2();

		double combinedMean = ch1Mean + ch2Mean;

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		// variables for summing up the
		double ch1MeanDiffSum = 0.0, ch2MeanDiffSum = 0.0, combinedMeanDiffSum = 0.0;
		double combinedSum = 0.0;
		int N = 0, NZero = 0;

		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			combinedSum = ch1 + ch2;

			// TODO: Shouldn't the whole calculation take only pixels
			// into account that are combined above zero? And not just
			// the denominator (like it is done now)?

			// calculate the nominators for the variances
			ch1MeanDiffSum += (ch1 - ch1Mean) * (ch1 - ch1Mean);
			ch2MeanDiffSum += (ch2 - ch2Mean) * (ch2 - ch2Mean);
			combinedMeanDiffSum += (combinedSum - combinedMean) * (combinedSum - combinedMean);

			// count only pixels that are above zero
			if ( (ch1 + ch2) > 0.00001)
				NZero++;

			N++;
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

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
		final int maxIterations = 30;
		// the current iteration
		int iteration = 0;
		// the current maximum threshold
		double maxThreshold = container.getMaxCh1();
		// last Person's R value
		double lastPersonsR;
		/* current Person's R value
		 * Since we want it to get as small as possible
		 * we initialize it with a maximum double value.
		 */
		double currentPersonsR = Double.MAX_VALUE;

		double ch1ThreshMin = 0;
		double ch1ThreshMax = container.getMaxCh1();
		double ch2ThreshMin = 0;
		double ch2ThreshMax = container.getMaxCh2();

		// the best found threshold for channel one
		double ch1BestThreshold = ch1ThreshMax;
		/* The related best Person's R value.
		 * Since we want it to get as small as possible
		 * we initialize it with a maximum double value.
		 */
		double bestPersonsR = Double.MAX_VALUE;
		/* A tolerance for deciding if the threshold has
		 * been found. If Person's R value is between
		 * -tolerance and tolerance, it is considered
		 * small enough.
		 */
		final double tolerance = 0.01;
		// define some image type specific threshold variables
		T threshold1 = img1.createType();
		T threshold2 = img2.createType();
		/* have a thresholded TwinValueRangeCursor of which
		 * predicates thresholds can be altered.
		 */
		BelowThresholdPredicate<T> predicate1 = new BelowThresholdPredicate<T>(threshold1);
		BelowThresholdPredicate<T> predicate2 = new BelowThresholdPredicate<T>(threshold2);
		TwinValueRangeCursor<T> cursor = new TwinValueRangeCursor<T>(
				img1.createCursor(), img2.createCursor(), predicate1, predicate2);

		// do regression
		while (!thresholdFound && iteration<maxIterations) {
			// calculate both thresholds
			ch1ThreshMax = Math.round( maxThreshold );
			ch2ThreshMax = Math.round( (ch1ThreshMax * m) + b );
			// set the image type specific variables
			threshold1.setReal( ch1ThreshMax );
			threshold2.setReal( ch2ThreshMax );
			// set the thresholds of the predicates
			predicate1.setThreshold( threshold1 );
			predicate2.setThreshold( threshold2 );
			// indicates if we have actually found a real number
			boolean badResult = false;

			// backup last Person's R value
			lastPersonsR = currentPersonsR;
			try {
				// do persons calculation within the limits
				currentPersonsR = pearsonsCorrellation.calculatePearsons(cursor, ch1Mean, ch2Mean);
				// indicates if we have actually found a real number
				badResult = Double.isNaN(currentPersonsR);

				//check to see if we're getting closer to zero for r
				if ( (bestPersonsR * bestPersonsR) > (currentPersonsR * currentPersonsR) ) {
					ch1BestThreshold = ch1ThreshMax;
					bestPersonsR = currentPersonsR;
				}

				/* If our r is close to our level of tolerance,
				 * then set threshold has been found.
				 */
				if ( (currentPersonsR < tolerance) && (currentPersonsR > -tolerance) )
					thresholdFound = true;

				// if we've reached ch1 = 1 then we've exhausted our possibilities
				if (Math.round(ch1ThreshMax) == 0)
					thresholdFound = true;
			} catch (MissingPreconditionException e) {
				/* the exception that could occur is due to numerical
				 * problems within the pearsons calculation.
				 */
				badResult = true;
			}

			// change threshold maximum
			if (badResult || currentPersonsR < 0.0) {
				/* If a bad result was calculated or the Person's R value
				 * is negative, increase the maximum threshold.
				 */
				maxThreshold = maxThreshold * 1.5;
			} else {
				/* If current Person's R value is above the previous one,
				 * decrease the threshold, otherwise increase it.
				 */
				if ( currentPersonsR >= lastPersonsR )
					maxThreshold = maxThreshold * 0.5;
				else
					maxThreshold = maxThreshold * 1.5;
			}

			// reset the cursor to reuse it
			cursor.reset();

			// increment iteration counter
			iteration++;
		}

		// close the TwinValueRangeCursor, we don't need it anymore
		cursor.close();

		// remember the best threshold values
		ch1ThreshMax = Math.round( ch1BestThreshold );
		ch2ThreshMax = Math.round( (ch1BestThreshold * m) + b );

		/* Get min and max value of image data type. Since type of image
		 * one and two are the same, we dont't need to distinguish them.
		 */
		T dummyT = img1.createType();
		double minVal = dummyT.getMinValue();
		double maxVal = dummyT.getMaxValue();

		/* Store the new results. The lower thresholds are the types
		 * min value for now. For the max threshold we do a clipping
		 * to make it fit into the image type.
		 */
		ch1MinThreshold = img1.createType();
		ch1MinThreshold.setReal(minVal);

		ch1MaxThreshold = img1.createType();
		if ( minVal > ch1ThreshMax )
			ch1MaxThreshold.setReal( minVal );
		else if ( maxVal < ch1ThreshMax )
			ch1MaxThreshold.setReal( maxVal );
		else
			ch1MaxThreshold.setReal( ch1ThreshMax );

		ch2MinThreshold = img2.createType();
		ch2MinThreshold.setReal(minVal);

		ch2MaxThreshold = img2.createType();
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
				"The y-intercept of the auto threshold regression line is high. Maybe you should use a ROI.");
		}

		// add warnings if values are below lowest pixel value of images
		if ( ch1ThreshMax < container.getMinCh1() || ch2ThreshMax < container.getMinCh2() ) {
			addWarning("thresholds too low",
				"The auto threshold method could not find a positive threshold.");
		}
	}

	public void processResults(ResultHandler handler) {
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
