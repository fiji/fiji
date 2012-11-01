package fiji.plugin.trackmate.action;

import java.util.List;

import javax.swing.ImageIcon;

import mpicbg.imglib.image.Image;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.util.TMUtils;

public class GrabSpotImageAction extends AbstractTMAction {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/photo_add.png"));
	
	public GrabSpotImageAction() {
		this.icon = ICON;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void execute(TrackMate_ plugin) {
		TrackMateModel model = plugin.getModel();
		logger.log("Grabbing spot images.\n");
		Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		Settings settings = model.getSettings();
		float[] calibration = settings.getCalibration();
		final int targetChannel = settings.segmentationChannel; // TODO: maybe be more flexible about that
		
		SpotCollection allSpots = model.getFilteredSpots();
		for (int frame : allSpots.keySet()) {
			List<Spot> spots = allSpots.get(frame);
			Image img = TMUtils.getUncroppedSingleFrameAsImage(settings.imp, frame, targetChannel);
			SpotIconGrabber grabber = new SpotIconGrabber();
			grabber.setTarget(img, calibration);
			grabber.process(spots);			
			logger.setProgress((float) (frame + 1) / allSpots.keySet().size());
		}
		model.setLogger(oldLogger);
		logger.log("Done.\n");
		logger.setProgress(0);
	}

	@Override
	public String getInfoText() {
		return "<html>" +
		"Grab all spot images from the current image. " +
		"This can be useful to update the image field of spots loaded from a file." +
		"</html>";
	}
	
	@Override
	public String toString() {
		return "Grab spot images";
	}

}
