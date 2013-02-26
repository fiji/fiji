package fiji.plugin.trackmate.gui;

import java.io.File;

public class LoadDescriptor extends SomeDialogDescriptor {

	public static final String DESCRIPTOR = "LoadingPanel";

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public void displayingPanel() {

		try {

			if (null == file) {
				try {
					File folder = new File(plugin.getModel().getSettings().imp.getOriginalFileInfo().directory);
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			GuiReader reader = new GuiReader(wizard);
			File tmpFile = reader.askForFile(file);
			if (null == tmpFile) {
				wizard.setNextButtonEnabled(true);
				return;
			}
			file = tmpFile;
			reader.loadFile(file);
			plugin = reader.getPlugin();
			setTargetNextID(reader.getTargetDescriptor());

		} finally {
			wizard.setNextButtonEnabled(true);
		}

	}

}
