package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Map;

import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotImageUpdater {
	
	private Integer previousFrame;
	private SpotIconGrabber<?> grabber;
	private final Settings settings;

	public SpotImageUpdater(final Settings settings) {
		this.settings = settings;
		this.previousFrame = -1;
	}

	/**
	 * @return the image string of the given spot, based on the raw images contained in the given model.
	 * For performance, the image at target frame is stored for subsequent calls of this method. 
	 * So it is a good idea to group calls to this method for spots that belong to the
	 * same frame.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getImageString(final Spot spot) {

		Integer frame = spot.getFeature(Spot.FRAME).intValue();
		if (null == frame)
			return "";
		if (frame == previousFrame) {
			// Keep the same image than in memory
		} else {
			ImgPlus img = TMUtils.rawWraps(settings.imp);
			int targetChannel = 0;
			if (settings != null && settings.detectorSettings != null) {
				// Try to extract it from detector settings target channel
				Map<String, Object> ds = settings.detectorSettings;
				Object obj = ds.get(KEY_TARGET_CHANNEL);
				if (null != obj && obj instanceof Integer) {
					targetChannel = ((Integer) obj) - 1;
				}
			} // TODO: be more flexible about that
			ImgPlus<?> imgCT = HyperSliceImgPlus.fixTimeAxis( 
					HyperSliceImgPlus.fixChannelAxis(img, targetChannel), 
					frame);
			grabber = new SpotIconGrabber(imgCT);
			previousFrame = frame;
		}
		return grabber.getImageString(spot);			
	}
}
