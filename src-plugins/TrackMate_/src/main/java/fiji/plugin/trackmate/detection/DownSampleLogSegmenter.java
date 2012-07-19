package fiji.plugin.trackmate.detection;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.util.TMUtils;

public class DownSampleLogSegmenter <T extends RealType<T>  & NativeType<T>> extends AbstractSpotSegmenter<T> {

	private final static String BASE_ERROR_MESSAGE = "DownSampleLogSegmenter: ";
	private DownSampleLogSegmenterSettings<T> settings;

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
	public void setTarget(final ImgPlus<T> image, final SegmenterSettings<T> settings) {
		super.setTarget(image, settings);
		this.settings = (DownSampleLogSegmenterSettings<T>) settings;
	}

	@Override
	public boolean checkInput() {
		return super.checkInput();
	}

	@Override
	public SegmenterSettings<T> createDefaultSettings() {
		return new DownSampleLogSegmenterSettings<T>();
	}


	/*
	 * ALGORITHM METHODS
	 */

	@Override
	public boolean process() {
		
		// 0. Prepare new dimensions

		int downSamplingFactor = settings.downSamplingFactor;
		long[] dimensions = new long[img.numDimensions()];
		int[] dsarr = new int[img.numDimensions()];
		double[] dwnCalibration = new double[img.numDimensions()];
		double[] calibration = TMUtils.getSpatialCalibration(img);
		for (int i = 0; i < 2; i++) {
			dimensions[i] = img.dimension(i) / downSamplingFactor;
			dsarr[i] = downSamplingFactor;
			dwnCalibration[i] = calibration[i] * downSamplingFactor;
		}
		if (img.numDimensions() > 2) {
			// 3D
			double zratio = calibration[2] / calibration[0]; // Z spacing is how much bigger
			int zdownsampling = (int) (downSamplingFactor / zratio); // temper z downsampling
			zdownsampling = Math.max(1, zdownsampling); // but at least 1
			dimensions[2] = img.dimension(2) / zdownsampling;
			dsarr[2] = zdownsampling;
			dwnCalibration[2] = calibration[2] * zdownsampling;
		}
		
		// 1. Downsample the image

		Img<T> downsampled = img.factory().create(dimensions, img.firstElement().createVariable());
		ImgPlus<T> dsimg = new ImgPlus<T>(downsampled, img);
		dsimg.setCalibration(dwnCalibration);
		
		Cursor<T> dwnCursor = downsampled.localizingCursor();
		RandomAccess<T> srcCursor = img.randomAccess();
		int[] pos = new int[img.numDimensions()];
		
		while (dwnCursor.hasNext()) {
			dwnCursor.fwd();
			dwnCursor.localize(pos);
			
			// Scale up position
			for (int i = 0; i < pos.length; i++) {
				pos[i] = pos[i] * dsarr[i];
			}
			
			// Pass it to source cursor
			srcCursor.setPosition(pos);
			
			// Copy pixel data
			dwnCursor.get().set(srcCursor.get());
		}

		// 2. Segment downsampled image

		// 2.1. Create settings object
		LogSegmenterSettings<T> logSettings = new LogSegmenterSettings<T>();
		logSettings.expectedRadius = settings.expectedRadius; 
		logSettings.threshold = settings.threshold;
		logSettings.doSubPixelLocalization = true;;
		logSettings.useMedianFilter = settings.useMedianFilter;

		// 2.2 Instantiate segmenter
		LogSegmenter<T> segmenter = new LogSegmenter<T>();
		segmenter.setTarget(dsimg, logSettings);

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
