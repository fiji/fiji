package algorithms;

import gadgets.DataContainer;
import ij.IJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import results.ResultHandler;

/**
 * This class implements some basic checks for the input image
 * data. For instance is the percentage of zero-zero pixels
 * checked or how many pixels are saturated.
 */
public class InputCheck<T extends RealType< T >> extends Algorithm<T> {
	/* the maximum allowed ratio between zero-zero and
	 * normal pixels
	 */
	protected final double maxZeroZeroRatio = 0.1f;
	/* the maximum allowed ratio between saturated and
	 * normal pixels within a channel
	 */
	protected final double maxSaturatedRatio = 0.1f;
	// the zero-zero pixel ratio
	double zeroZeroPixelRatio;
	// the saturated pixel ratio of channel 1
	double saturatedRatioCh1;
	// the saturated pixel ratio of channel 2
	double saturatedRatioCh2;

	public InputCheck() {
		super("input data check");
	}

	@Override
	public void execute(DataContainer<T> container)
			throws MissingPreconditionException {
		// get the 2 images and the mask
		final RandomAccessibleInterval<T> img1 = container.getSourceImage1();
		final RandomAccessibleInterval<T> img2 = container.getSourceImage2();
		final RandomAccessibleInterval<BitType> mask = container.getMask();

		// get the cursors for iterating through pixels in images
		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).cursor());

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

		while (cursor.hasNext()) {
			cursor.fwd();
			double ch1 = cursor.getChannel1().getRealDouble();
			double ch2 = cursor.getChannel2().getRealDouble();

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

		// calculate results
		double zeroZeroRatio = (double)Nzero / (double)N;
		// for channel wise ratios we have to use half of the total pixel amount
		double ch1SaturatedRatio = (double)NsaturatedCh1 / ( (double)N *0.5);
		double ch2SaturatedRatio = (double)NsaturatedCh2 / ( (double)N * 0.5);

		/* save results
		 * Percentage results need to be multiplied by 100
		 */
		zeroZeroPixelRatio = zeroZeroRatio * 100.0;
		saturatedRatioCh1 = ch1SaturatedRatio * 100.0;
		saturatedRatioCh2 = ch2SaturatedRatio * 100.0;

		// add warnings if values are not in tolerance range
		if ( Math.abs(zeroZeroRatio) > maxZeroZeroRatio ) {

			addWarning("zero-zero ratio too high",
				"The ratio between zero-zero pixels and other pixels is larger "
				+ IJ.d2s(zeroZeroRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("saturated ch1 ratio too high",
				"The ratio between saturated pixels and other pixels in channel one is larger "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
		if ( Math.abs(ch1SaturatedRatio) > maxSaturatedRatio ) {
			addWarning("saturated ch2 ratio too high",
				"The ratio between saturated pixels and other pixels in channel two is larger "
				+ IJ.d2s(maxSaturatedRatio, 2) + ". Maybe you should use a ROI.");
		}
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("% zero-zero pixels", zeroZeroPixelRatio, 2);
		handler.handleValue("% saturated ch1 pixels", saturatedRatioCh1, 2);
		handler.handleValue("% saturated ch2 pixels", saturatedRatioCh2, 2);
	}
}