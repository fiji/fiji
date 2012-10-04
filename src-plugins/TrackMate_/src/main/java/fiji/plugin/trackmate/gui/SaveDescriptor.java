package fiji.plugin.trackmate.gui;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.util.TMUtils;

public class SaveDescriptor <T extends RealType<T> & NativeType<T>> extends SomeDialogDescriptor<T> {

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
			GuiSaver<T> saver = new GuiSaver<T>(wizard);
			File tmpFile = TMUtils.askForFile(file, wizard, logger);
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
