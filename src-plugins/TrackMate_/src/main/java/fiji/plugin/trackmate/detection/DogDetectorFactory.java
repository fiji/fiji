package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;

public class DogDetectorFactory<T extends RealType<T> & NativeType<T>> extends LogDetectorFactory<T> {


	/*
	 * CONSTANTS
	 */
	
	/** A string key identifying this factory. */ 
	public static final String DETECTOR_KEY = "DOG_DETECTOR";
	/** The pretty name of the target detector. */
	public static final String NAME = "DoG detector";
	/** An html information text. */
	public static final String INFO_TEXT = "<html>" +
			"This segmenter is based on an approximation of the LoG operator <br> " +
			"by differences of gaussian (DoG). Computations are made in direct space. <br>" +
			"It is the quickest for small spot sizes (< ~5 pixels). " +
			"<p> " +
			"Spots found too close are suppressed. This segmenter can do sub-pixel <br>" +
			"localization of spots using a quadratic fitting scheme. It is based on <br>" +
			"the scale-space framework made by Stephan Preibisch for ImgLib. " +
			"</html>";	

	/*
	 * METHODS
	 */

	@Override
	public SpotDetector<T> getDetector(final int frame) {
		final int targetChannel = (Integer) settings.get(KEY_TARGET_CHANNEL) - 1; // parameter is 1-based
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(img, targetChannel);
		final ImgPlus<T> imgT = HyperSliceImgPlus.fixTimeAxis(imgC, frame);
		final double radius = (Double) settings.get(KEY_RADIUS);
		final double threshold = (Double) settings.get(KEY_THRESHOLD);
		final boolean doMedian = (Boolean) settings.get(KEY_DO_MEDIAN_FILTERING);
		final boolean doSubpixel = (Boolean) settings.get(KEY_DO_SUBPIXEL_LOCALIZATION);
		return new DogDetector<T>(imgT, radius, threshold, doSubpixel, doMedian);
	}
	
	@Override
	public String getKey() {
		return DETECTOR_KEY;
	}
	
	@Override
	public String toString() {
		return NAME;
	}
}
