package fiji.plugin.trackmate.detection;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.util.TMUtils;

public class DownsampleLogDetector <T extends RealType<T>  & NativeType<T>> extends AbstractSpotDetector<T> {

	private final static String BASE_ERROR_MESSAGE = "DownSampleLogDetector: ";
	private DownSampleLogDetectorSettings<T> settings;

	/*
	 * CONSTRUCTORS
	 */

	public DownsampleLogDetector() {
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * PUBLIC METHODS
	 */

	public SpotDetector<T> createNewDetector() {
		return new DownsampleLogDetector<T>();
	};

	@Override
	public void setTarget(final ImgPlus<T> image, final DetectorSettings<T> settings) {
		super.setTarget(image, settings);
		this.settings = (DownSampleLogDetectorSettings<T>) settings;
	}

	@Override
	public boolean checkInput() {
		return super.checkInput();
	}

	@Override
	public DetectorSettings<T> createDefaultSettings() {
		return new DownSampleLogDetectorSettings<T>();
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
		LogDetectorSettings<T> logSettings = new LogDetectorSettings<T>();
		logSettings.expectedRadius = settings.expectedRadius; 
		logSettings.threshold = settings.threshold;
		logSettings.doSubPixelLocalization = true;;
		logSettings.useMedianFilter = settings.useMedianFilter;

		// 2.2 Instantiate detector
		LogDetector<T> detector = new LogDetector<T>();
		detector.setTarget(dsimg, logSettings);

		// 2.3 Execute detection
		if (!detector.checkInput() || !detector.process()) {
			errorMessage = BASE_ERROR_MESSAGE + detector.getErrorMessage();
			return false;
		}
		
		// 3. Benefits
		spots = detector.getResult();
		
		return true;
	}

	@Override
	public String getInfoText() {
		return "<html>" +
				"This detector is basically identical to the LoG detector, except <br>" +
				"that images are downsampled before filtering, giving it a good <br>" +
				"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
				"large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " +
				"</html>";
	}

	@Override
	public String toString() {
		return "Downsampled LoG detector";
	}

}
