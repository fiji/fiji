package fiji.plugin.trackmate.gui;

import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.io.IOUtils;

public class SaveDescriptor extends SomeDialogDescriptor {

	public static final String DESCRIPTOR = "SavingPanel";
	
	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public void displayingPanel() {
		try {

			logger.log("Saving data...\n", Logger.BLUE_COLOR);
			if (null == file ) {
//				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				File folder = new File(plugin.getModel().getSettings().imp.getOriginalFileInfo().directory);
				try {
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}
			
			// If we are to save tracks, we better ensures that track and edge features are there, even if we have to enforce it
			if (plugin.getModel().getTrackModel().getNTracks() > 0) {
				plugin.computeEdgeFeatures(true);
				plugin.computeTrackFeatures(true);
			}

			GuiSaver saver = new GuiSaver(wizard);
			File tmpFile = IOUtils.askForFile(file, wizard, logger);
			if (null == tmpFile) {
				wizard.setNextButtonEnabled(true);
				return;
			}
			file = tmpFile;
			saver.writeFile(file);

		}	finally {
			wizard.setNextButtonEnabled(true);
		}
	}

}
