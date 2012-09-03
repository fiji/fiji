package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;


public class DownsampleLogDetectorFactory<T extends RealType<T> & NativeType<T>> extends LogDetectorFactory<T> {

	/*
	 * CONSTANTS
	 */
	
	/** A string key identifying this factory. */ 
	public static final String DETECTOR_KEY = "DOWNSAMPLE_LOG_DETECTOR";
	/** The pretty name of the target detector. */
	public static final String NAME =  "Downsample LoG detector";
	/** An html information text. */
	public static final String INFO_TEXT = "<html>" +
			"This detector is basically identical to the LoG detector, except <br>" +
			"that images are downsampled before filtering, giving it a good <br>" +
			"kick in speed, particularly for large spot sizes. It is the fastest for <br>" +
			"large spot sizes (>&nbsp;~20 pixels), at the cost of precision in localization. " +
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
		final int downsamplingFactor = (Integer) settings.get(KEY_DOWNSAMPLE_FACTOR);
		return new DownsampleLogDetector<T>(imgT, radius, threshold, downsamplingFactor);
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
