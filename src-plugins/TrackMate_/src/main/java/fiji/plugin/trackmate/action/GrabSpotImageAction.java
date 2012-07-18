package fiji.plugin.trackmate.action;

import java.util.List;

import javax.swing.ImageIcon;

import net.imglib2.img.ImgPlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.HyperSliceImgPlus;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.features.spot.SpotIconGrabber;
import fiji.plugin.trackmate.gui.DisplayerPanel;

public class GrabSpotImageAction<T extends RealType<T> & NativeType<T>> extends AbstractTMAction<T> {

	private static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/photo_add.png"));
	
	public GrabSpotImageAction() {
		this.icon = ICON;
	}
	
	@Override
	public void execute(TrackMate_<T> plugin) {
		TrackMateModel<T> model = plugin.getModel();
		logger.log("Grabbing spot images.\n");
		Logger oldLogger = model.getLogger();
		model.setLogger(logger);
		Settings<T> settings = model.getSettings();
		final int targetChannel = settings.segmentationChannel - 1; // TODO: maybe be more flexible about that
		
		SpotCollection allSpots = model.getFilteredSpots();
		for (int frame : allSpots.keySet()) {
			List<Spot> spots = allSpots.get(frame);
			ImgPlus<T> img = HyperSliceImgPlus.fixTimeAxis( HyperSliceImgPlus.fixChannelAxis(settings.img, targetChannel), frame ); 
			SpotIconGrabber<T> grabber = new SpotIconGrabber<T>();
			grabber.setTarget(img);
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
