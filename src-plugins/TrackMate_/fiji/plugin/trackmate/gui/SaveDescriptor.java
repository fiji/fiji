package fiji.plugin.trackmate.gui;

import java.io.File;

import fiji.plugin.trackmate.Logger;

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
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			plugin.computeTrackFeatures();
			GuiSaver saver = new GuiSaver(wizard);
			File tmpFile = saver.askForFile(file);
			if (null == tmpFile) {
				wizard.setNextButtonEnabled(true);
				return;
			}
			file = tmpFile;
			saver.writeFile(file, plugin.getModel(), targetID);

		}	finally {
			wizard.setNextButtonEnabled(true);
		}
	}

}
