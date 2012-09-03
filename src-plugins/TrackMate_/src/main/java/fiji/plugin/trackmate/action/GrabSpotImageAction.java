package fiji.plugin.trackmate.action;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;

import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imglib2.img.ImagePlusAdapter;
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

	public static final ImageIcon ICON = new ImageIcon(DisplayerPanel.class.getResource("images/photo_add.png"));
	public static final String NAME = "Grab spot images";
	public static final String INFO_TEXT = "<html>" +
			"Grab all spot images from the current image. " +
			"This can be useful to update the image field of spots loaded from a file." +
			"</html>";
	
	
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
		int targetChannel = 0;
		if (settings != null && settings.detectorSettings != null) {
			// Try to extract it from detector settings target channel
			Map<String, Object> ds = settings.detectorSettings;
			Object obj = ds.get(KEY_TARGET_CHANNEL);
			if (null != obj && obj instanceof Integer) {
				targetChannel = ((Integer) obj) - 1;
			}
		} // TODO: maybe be more flexible about that
		final ImgPlus<T> source = ImagePlusAdapter.wrapImgPlus(settings.imp);
		final ImgPlus<T> imgC = HyperSliceImgPlus.fixChannelAxis(source, targetChannel);
		
		SpotCollection allSpots = model.getFilteredSpots();
		for (int frame : allSpots.keySet()) {
			List<Spot> spots = allSpots.get(frame);
			ImgPlus<T> img = HyperSliceImgPlus.fixTimeAxis( imgC , frame ); 
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
		return INFO_TEXT;
	}
	
	@Override
	public String toString() {
		return NAME;
	}

}
