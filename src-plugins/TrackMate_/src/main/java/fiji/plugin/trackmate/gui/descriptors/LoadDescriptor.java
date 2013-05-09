package fiji.plugin.trackmate.gui.descriptors;

import java.io.File;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GuiReader;
import fiji.plugin.trackmate.gui.LogPanel;

public class LoadDescriptor extends SomeDialogDescriptor {

	public static final String DESCRIPTOR = "LoadingPanel";
	private final TrackMate trackmate;

	public LoadDescriptor(LogPanel logPanel, TrackMate trackmate) {
		super(logPanel);
		this.trackmate = trackmate;
	}


	@Override
	public void displayingPanel() {

		try {

			if (null == file) {
				try {
					File folder = new File(trackmate.getSettings().imp.getOriginalFileInfo().directory);
					file = new File(folder.getPath() + File.separator + trackmate.getSettings().imp.getShortTitle() +".xml");
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
			trackmate = reader.getPlugin();
			setTargetNextID(reader.getTargetDescriptor());

		} finally {
			wizard.setNextButtonEnabled(true);
		}

	}

}
