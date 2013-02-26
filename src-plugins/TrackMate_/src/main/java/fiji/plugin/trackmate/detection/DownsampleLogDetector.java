package fiji.plugin.trackmate.detection;

import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

public class DownsampleLogDetector <T extends RealType<T>  & NativeType<T>> implements SpotDetector<T> {

	private final static String BASE_ERROR_MESSAGE = "DownSampleLogDetector: ";
	
	
	/*
	 * FIELDS
	 */

	/** The image to segment. Will not modified. */
	protected final ImgPlus<T> img;
	protected final double radius;
	protected final double threshold;
	protected final int downsamplingFactor;
	protected String baseErrorMessage;
	protected String errorMessage;
	/** The list of {@link Spot} that will be populated by this detector. */
	protected List<Spot> spots = new ArrayList<Spot>(); // because this implementation is fast to add elements at the end of the list
	/** The processing time in ms. */
	protected long processingTime;

	/*
	 * CONSTRUCTORS
	 */

	public DownsampleLogDetector(final ImgPlus<T> img, final double radius, final double threshold, final int downsamplingFactor) {
		this.img = img;
		this.radius = radius;
		this.threshold = threshold;
		this.downsamplingFactor = downsamplingFactor;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public boolean checkInput() {
		if (null == img) {
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		if (!(img.numDimensions() == 2 || img.numDimensions() == 3)) {
			errorMessage = baseErrorMessage + "Image must be 2D or 3D, got " + img.numDimensions() +"D.";
			return false;
		}
		if (downsamplingFactor < 1) {
			errorMessage = baseErrorMessage + "Downsampling factor must be above 1, was "+downsamplingFactor+".";
			return false;
		}
		return true;
	}


	@Override
	public boolean process() {
		
		long start = System.currentTimeMillis();
		
		// 0. Prepare new dimensions

		long[] dimensions = new long[img.numDimensions()];
		int[] dsarr = new int[img.numDimensions()];
		double[] dwnCalibration = new double[img.numDimensions()];
		double[] calibration = TMUtils.getSpatialCalibration(img);
		for (int i = 0; i < 2; i++) {
			dimensions[i] = img.dimension(i) / downsamplingFactor;
			dsarr[i] = downsamplingFactor;
			dwnCalibration[i] = calibration[i] * downsamplingFactor;
		}
		if (img.numDimensions() > 2) {
			// 3D
			double zratio = calibration[2] / calibration[0]; // Z spacing is how much bigger
			int zdownsampling = (int) (downsamplingFactor / zratio); // temper z downsampling
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

		// 2.1 Instantiate detector
		LogDetector<T> detector = new LogDetector<T>(dsimg, radius, threshold, false, false);

		// 2.2 Execute detection
		if (!detector.checkInput() || !detector.process()) {
			errorMessage = BASE_ERROR_MESSAGE + detector.getErrorMessage();
			return false;
		}
		
		// 3. Benefits
		spots = detector.getResult();
		
		long end = System.currentTimeMillis();
		processingTime = end - start;
		
		return true;
	}

	@Override
	public List<Spot> getResult() {
		return spots;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	}
}
