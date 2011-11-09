package fiji.plugin.trackmate.visualization.trackscheme;

import mpicbg.imglib.image.Image;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotImageUpdater {
	
	private final SpotIconGrabber grabber;
	private final TrackMateModel model;
	private final float[] calibration;
	private Integer previousFrame;

	public SpotImageUpdater(final TrackMateModel model) {
		this.model = model;
		this.grabber = new SpotIconGrabber();
		this.previousFrame = -1;
		this.calibration = model.getSettings().getCalibration();
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
			Image img = TMUtils.getSingleFrameAsImage(settings.imp, frame, settings);
			grabber.setTarget(img, calibration);
			previousFrame = frame;
		}
		grabber.process(spot);			
	}

}
