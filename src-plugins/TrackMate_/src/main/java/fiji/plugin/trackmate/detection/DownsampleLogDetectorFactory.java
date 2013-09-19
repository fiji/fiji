package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;


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
	
	@Override
	public boolean checkInput() {
		StringBuilder errorHolder = new StringBuilder();
		boolean ok = checkInput(settings, errorHolder);
		if (!ok) {
			errorMessage = errorHolder.toString();
		}
		return ok;
	}
	
	/**
	 * Check that the given settings map is suitable for the Downsample LoG detector.
	 * @param settings  the map to test.
	 * @param errorHolder  if not suitable, will contain an error message.
	 * @return  true if the settings map is valid.
	 */
	public static final boolean checkInput(Map<String, Object> settings, StringBuilder errorHolder) {
		boolean ok = true;
		ok = ok & checkParameter(settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_RADIUS, Double.class, errorHolder) ;
		ok = ok & checkParameter(settings, KEY_THRESHOLD, Double.class, errorHolder);
		ok = ok & checkParameter(settings, KEY_DOWNSAMPLE_FACTOR, Integer.class, errorHolder);
		List<String> mandatoryKeys = new ArrayList<String>();
		mandatoryKeys.add(KEY_TARGET_CHANNEL);
		mandatoryKeys.add(KEY_RADIUS);
		mandatoryKeys.add(KEY_THRESHOLD);
		mandatoryKeys.add(KEY_DOWNSAMPLE_FACTOR);
		ok = ok & checkMapKeys(settings, mandatoryKeys, null, errorHolder);
		return ok;	
	}
	
	
}
