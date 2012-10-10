package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.Map;

import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;

public class SpotImageUpdater <T extends RealType<T> & NativeType<T>> {
	
	private final TrackMateModel<T> model;
	private Integer previousFrame;
	private SpotIconGrabber<T> grabber;

	public SpotImageUpdater(final TrackMateModel<T> model) {
		this.model = model;
		this.previousFrame = -1;
	}

	/**
	 * Update the image string of the given spot, based on the raw images contained in the given model.
	 */
	public void update(final Spot spot) {

		Integer frame = spot.getFeature(Spot.FRAME).intValue();
		if (null == frame)
			return;
		if (frame == previousFrame) {
			// Keep the same image than in memory
		} else {
			Settings<T> settings = model.getSettings();
			ImgPlus<T> img = ImagePlusAdapter.wrapImgPlus(settings.imp);
			int targetChannel = 0;
			if (settings != null && settings.detectorSettings != null) {
				// Try to extract it from detector settings target channel
				Map<String, Object> ds = settings.detectorSettings;
				Object obj = ds.get(KEY_TARGET_CHANNEL);
				if (null != obj && obj instanceof Integer) {
					targetChannel = ((Integer) obj) - 1;
				}
			} // TODO: be more flexible about that
			ImgPlus<T> imgCT = HyperSliceImgPlus.fixTimeAxis( 
					HyperSliceImgPlus.fixChannelAxis(img, targetChannel), 
					frame);
			grabber = new SpotIconGrabber<T>(imgCT, null);
			previousFrame = frame;
		}
		grabber.process(spot);			
	}

}
