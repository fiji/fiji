package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiSaver;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.io.IOUtils;

public class SaveDescriptor extends SomeDialogDescriptor {

	public static final String DESCRIPTOR = "SavingPanel";
	private final TrackMate trackmate;
	
	public SaveDescriptor(LogPanel logPanel, TrackMate trackmate) {
		super(logPanel);
		this.trackmate = trackmate;
	}
	

	@Override
	public void displayingPanel() {
		try {

			Logger logger = logPanel.getLogger();
			logger.log("Saving data...\n", Logger.BLUE_COLOR);
			if (null == file ) {
//				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				File folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
				try {
					file = new File(folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}
			
			// If we are to save tracks, we better ensures that track and edge features are there, even if we have to enforce it
			if (trackmate.getModel().getTrackModel().getNTracks() > 0) {
				trackmate.computeEdgeFeatures(true);
				trackmate.computeTrackFeatures(true);
			}

			GuiSaver saver = new GuiSaver(wizard);
			File tmpFile = IOUtils.askForFile(file, wizard, logger);
			if (null == tmpFile) {
				wizard.setNextButtonEnabled(true);
				return;
			}
			file = tmpFile;
			saver.writeFile(file);

		} finally {
			wizard.setNextButtonEnabled(true);
		}
	}

}
