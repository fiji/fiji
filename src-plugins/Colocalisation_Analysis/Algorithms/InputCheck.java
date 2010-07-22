import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

/**
 * This class implements some basic checks for the input image
 * data. For instance is the percentage of zero-zero pixels
 * checked or how many pixels are saturated.
 */
public class InputCheck<T extends RealType<T>> extends Algorithm {
	/* the maximum allowed ratio between zero-zero and
	 * normal pixels
	 */
	protected final double maxZeroZeroRatio = 0.1f;
	/* the maximum allowed ratio between saturated and
	 * normal pixels within a channel
	 */
	protected final double maxSaturatedRatio = 0.1f;

	@Override
	public void execute(DataContainer container)
			throws MissingPreconditionException {
		// get the 2 images for the calculation of Pearson's
		Image<T> img1 = container.getSourceImage1();
		Image<T> img2 = container.getSourceImage2();

		// get the cursors for iterating through pixels in images
		Cursor<T> cursor1 = img1.createCursor();
		Cursor<T> cursor2 = img2.createCursor();

		double ch1Max = container.getMaxCh1();
		double ch2Max = container.getMaxCh2();

		// the total amount of pixels that have been taken into consideration
		int N = 0;
		// the amount of pixels that are zero in both channels
		int Nzero = 0;
		// the amount of ch1 pixels with the maximum ch1 value;
		int NsaturatedCh1 = 0;
		// the amount of ch2 pixels with the maximum ch2 value;
		int NsaturatedCh2 = 0;

		while (cursor1.hasNext() && cursor2.hasNext()) {
			cursor1.fwd();
			cursor2.fwd();
			T type1 = cursor1.getType();
			double ch1 = type1.getRealDouble();
			T type2 = cursor2.getType();
			double ch2 = type2.getRealDouble();

			// is the current pixels combination a zero pixel?
			if (Math.abs(ch1 + ch2) < 0.00001)
				Nzero++;

			// is the current pixel of channel one saturated?
			if (Math.abs(ch1Max - ch1) < 0.00001)
				NsaturatedCh1++;

			// is the current pixel of channel one saturated?
			if (Math.abs(ch2Max - ch2) < 0.00001)
				NsaturatedCh2++;

			N++;
		}

		// close the cursors
		cursor1.close();
		cursor2.close();

		// calculate results
		double zeroZeroRatio = (double)Nzero / (double)N;
		// for channel wise ratios we have to use half of the total pixel amount
		double ch1SaturatedRatio = (double)NsaturatedCh1 / ( (double)N *0.5);
		double ch2SaturatedRatio = (double)NsaturatedCh2 / ( (double)N * 0.5);

		/* add results to data container
		 * Percentage results need to be multiplied by 100 before
		 * they are added as result.
		 */
		container.add( new Result.SimpleValueResult("% zero-zero pixels", zeroZeroRatio * 100.0, 3));
		container.add( new Result.SimpleValueResult("% saturated ch1 pixels", ch1SaturatedRatio * 100.0, 3));
		container.add( new Result.SimpleValueResult("% saturated ch2 pixels", ch2SaturatedRatio * 100.0, 3));

		// add warnings if values are not in tolerance range
		if ( Math.abs(zeroZeroRatio) > maxZeroZeroRatio ) {
			container.add( new Result.WarningResult("zero-zero ratio too high",
					"The ratio between zero-zero pixels and other pixels is larger "
					+ zeroZeroRatio + ". Maybe you should use a ROI.") );
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			container.add( new Result.WarningResult("saturated ch1 ratio too high",
					"The ratio between saturated pixels and other pixels in channel one is larger "
					+ maxSaturatedRatio + ". Maybe you should use a ROI.") );
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			container.add( new Result.WarningResult("saturated ch2 ratio too high",
					"The ratio between saturated pixels and other pixels in channel two is larger "
					+ maxSaturatedRatio + ". Maybe you should use a ROI.") );
		}
	}

}