import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * A class implementing the automatic finding of a threshold
 * used for Person colocalisation calculation.
 */
public class AutoThresholdRegression<T extends RealType<T>> extends Algorithm {
	protected boolean discardZeroPixels = false;
	/* the threshold for y-intercept to y-max to
	 *  raise a warning about it being to high.
	 */
	final double warnYInterceptToYMaxRatioThreshold = 0.01;

	@Override
	public void execute(DataContainer container)
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

		if (discardZeroPixels)
			N = N - NZero;

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

		// do regression
		while (!thresholdFound && iteration<maxIterations) {
			ch1ThreshMax = Math.round(maxThreshold);
			ch2ThreshMax = Math.round( (ch1ThreshMax * m) + b );

			// backup last Person's R value
			lastPersonsR = currentPersonsR;
			// do persons calculation within the limits
			currentPersonsR = PearsonsCorrelation.fastPearsons(img1, img2, ch1ThreshMax, ch2ThreshMax, discardZeroPixels);

			// indicates if we have actually found a real number
			boolean badResult = Double.isNaN(currentPersonsR);

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

			// increment iteration counter
			iteration++;
		}

		// remember the best threshold values
		ch1ThreshMax = Math.round( ch1BestThreshold );
		ch2ThreshMax = Math.round( (ch1BestThreshold * m) + b );

		// tell the data container about the new results
		container.setCh1MaxThreshold(ch1ThreshMax);
		container.setCh2MaxThreshold(ch2ThreshMax);
		container.setAutoThresholdSlope(m);
		container.setAutoThresholdIntercept(b);
		container.add( new Result.SimpleValueResult("m (slope)", m));
		container.add( new Result.SimpleValueResult("b (y-intercept)", b));

		double bToYMaxRatio = b / container.getMaxCh2();
		container.add( new Result.SimpleValueResult("b to y-max ratio", bToYMaxRatio) );

		// add warnings if values are not in tolerance range
		if ( Math.abs(bToYMaxRatio) > warnYInterceptToYMaxRatioThreshold ) {
			container.add( new Result.WarningResult("y-intercept high",
					"The y-intercept of the auto threshold regression line is high. Maybe you should use a ROI.") );
		}
	}
}
