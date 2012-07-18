package fiji.plugin.trackmate.visualization.trackscheme;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;

public class SpotImageUpdater <T extends RealType<T> & NativeType<T>> {
	
	private final SpotIconGrabber<T> grabber;
	private final TrackMateModel<T> model;
	private Integer previousFrame;

	public SpotImageUpdater(final TrackMateModel<T> model) {
		this.model = model;
		this.grabber = new SpotIconGrabber<T>();
		this.previousFrame = -1;
	}

	/**
	 * Update the image string of the given spot, based on the raw images contained in the given model.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void update(final Spot spot) {

		Integer frame = model.getSpots().getFrame(spot);
		if (null == frame)
			return;
		if (frame == previousFrame) {
			// Keep the same image than in memory
		} else {
			Settings settings = model.getSettings();
			int targetChannel = settings.segmentationChannel - 1; // TODO: be more flexible about that
			ImgPlus<T> img = HyperSliceImgPlus.fixTimeAxis( 
					HyperSliceImgPlus.fixChannelAxis(settings.img, targetChannel), 
					frame);
			grabber.setTarget(img);
			previousFrame = frame;
		}
		grabber.process(spot);			
	}

}
