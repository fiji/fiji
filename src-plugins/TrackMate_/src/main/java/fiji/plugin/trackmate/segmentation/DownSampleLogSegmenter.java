package fiji.plugin.trackmate.segmentation;

import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.cursor.LocalizableCursor;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;

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
		
		// 0. Prepare new dimensions

		int downSamplingFactor = settings.downSamplingFactor;
		int[] dimensions = new int[img.getNumDimensions()];
		int[] dsarr = new int[img.getNumDimensions()];
		float[] dwnCalibration = new float[img.getNumDimensions()];
		for (int i = 0; i < 2; i++) {
			dimensions[i] = img.getDimension(i) / downSamplingFactor;
			dsarr[i] = downSamplingFactor;
			dwnCalibration[i] = calibration[i] * downSamplingFactor;
		}
		if (img.getNumDimensions() > 2) {
			// 3D
			float zratio = calibration[2] / calibration[0]; // Z spacing is how much bigger
			int zdownsampling = (int) (downSamplingFactor / zratio); // temper z downsampling
			zdownsampling = Math.max(1, zdownsampling); // but at least 1
			dimensions[2] = img.getDimension(2) / zdownsampling;
			dsarr[2] = zdownsampling;
			dwnCalibration[2] = calibration[2] * zdownsampling;
		}
		
		// 1. Downsample the image

		Image<T> downsampled = img.createNewImage(dimensions);
		LocalizableCursor<T> dwnCursor = downsampled.createLocalizableCursor();
		LocalizableByDimCursor<T> srcCursor = img.createLocalizableByDimCursor();
		int[] pos = dwnCursor.createPositionArray();
		
		while (dwnCursor.hasNext()) {
			dwnCursor.fwd();
			dwnCursor.getPosition(pos);
			
			// Scale up position
			for (int i = 0; i < pos.length; i++) {
				pos[i] = pos[i] * dsarr[i];
			}
			
			// Pass it to source cursor
			srcCursor.setPosition(pos);
			
			// Copy pixel data
			dwnCursor.getType().set(srcCursor.getType());
		}
		dwnCursor.close();
		srcCursor.close();

		// 2. Segment downsampled image

		// 2.1. Create settings object
		LogSegmenterSettings logSettings = new LogSegmenterSettings();
		logSettings.expectedRadius = settings.expectedRadius; 
		logSettings.threshold = settings.threshold;
		logSettings.doSubPixelLocalization = true;;
		logSettings.useMedianFilter = settings.useMedianFilter;

		// 2.2 Instantiate segmenter
		LogSegmenter<T> segmenter = new LogSegmenter<T>();
		segmenter.setTarget(downsampled, dwnCalibration, logSettings);

		// 2.3 Execute segmentation
		if (!segmenter.checkInput() || !segmenter.process()) {
			errorMessage = BASE_ERROR_MESSAGE + segmenter.getErrorMessage();
			return false;
		}
		
		// 3. Benefits
		spots = segmenter.getResult();
		
		return true;
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This segmenter is basically identical to the LoG segmenter, except <br>" +
				"that images are downsampled before filtering, giving it a good <br>" +
				"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
				"large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Downsampled LoG segmenter";
	}

}
