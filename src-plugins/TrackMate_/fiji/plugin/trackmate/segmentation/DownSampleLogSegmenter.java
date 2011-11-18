package fiji.plugin.trackmate.segmentation;

import static fiji.plugin.trackmate.Spot.POSITION_X;
import static fiji.plugin.trackmate.Spot.POSITION_Y;
import static fiji.plugin.trackmate.Spot.POSITION_Z;
import mpicbg.imglib.algorithm.gauss.DownSample;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;

public class DownSampleLogSegmenter <T extends RealType<T> > extends AbstractSpotSegmenter<T> {

	private final static String BASE_ERROR_MESSAGE = "DownSampleLogSegmenter: ";
	private DownSampleLogSegmenterSettings settings;

	/*
	 * CONSTRUCTORS
	 */

	public DownSampleLogSegmenter() {
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * PUBLIC METHODS
	 */

	public SpotSegmenter<T> createNewSegmenter() {
		return new DownSampleLogSegmenter<T>();
	};

	@Override
	public void setTarget(Image<T> image, float[] calibration,	SegmenterSettings settings) {
		super.setTarget(image, calibration, settings);
		this.settings = (DownSampleLogSegmenterSettings) settings;
	}

	@Override
	public boolean checkInput() {
		return super.checkInput();
	}

	@Override
	public SegmenterSettings createDefaultSettings() {
		return new DownSampleLogSegmenterSettings();
	}


	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean process() {

		// 1. Downsample the image
		float downSamplingFactor = settings.downSamplingFactor;
		DownSample<T> downsampler = new DownSample<T>(img, 1 / downSamplingFactor );
		if (!downsampler.checkInput() || !downsampler.process()) {
			errorMessage = BASE_ERROR_MESSAGE + downsampler.getErrorMessage();
			return false;
		}
		Image<T> downsampled = downsampler.getResult();

		// 2. Segment downsampled image

		// 2.1. Create settings object
		LogSegmenterSettings logSettings = new LogSegmenterSettings();
		logSettings.expectedRadius = settings.expectedRadius / downSamplingFactor;
		logSettings.threshold = settings.threshold;
		logSettings.doSubPixelLocalization = false;
		logSettings.useMedianFilter = settings.useMedianFilter;

		// 2.2 Instantiate segmenter
		LogSegmenter<T> segmenter = new LogSegmenter<T>();
		segmenter.setTarget(downsampled, calibration, logSettings);

		// 2.3 Execute segmentation
		if (!segmenter.checkInput() || !segmenter.process()) {
			errorMessage = BASE_ERROR_MESSAGE + segmenter.getErrorMessage();
			return false;
		}
		spots = segmenter.getResult();

		// Rescale spots

		if (img.getDimension(2) > 1) {
			for(Spot spot : spots) {
				float x = spot.getFeature(POSITION_X);
				spot.putFeature(POSITION_X, x * downSamplingFactor);
				float y = spot.getFeature(POSITION_Y);
				spot.putFeature(POSITION_Y, y * downSamplingFactor);
				float z = spot.getFeature(POSITION_Z);
				spot.putFeature(POSITION_Z, z * downSamplingFactor);
			} 
		} else {
			for(Spot spot : spots) {
				float x = spot.getFeature(POSITION_X);
				spot.putFeature(POSITION_X, x * downSamplingFactor);
				float y = spot.getFeature(POSITION_Y);
				spot.putFeature(POSITION_Y, y * downSamplingFactor);
			}
		}

		return true;
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This segmenter is basically identical to the LoG segmenter, except <br>" +
				"that images are downsampled before filtering, giving it a small <br>" +
				"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
				"large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Downsampled LoG segmenter";
	}

}
