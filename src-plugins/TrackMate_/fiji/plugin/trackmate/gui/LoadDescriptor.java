package fiji.plugin.trackmate.gui;

import java.io.File;

import fiji.plugin.trackmate.TrackMateModel;

public class LoadDescriptor extends SomeDialogDescriptor {


	public static final String DESCRIPTOR = "LoadingPanel";
	
	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public void displayingPanel() {

		new Thread("TrackMate loading thread") {
			@Override
			public void run() {
				try {
					// New model to feed
					TrackMateModel newModel = new TrackMateModel();
					newModel.setLogger(logger);

					if (null == file) {
						File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
						try {
							file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
						} catch (NullPointerException npe) {
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
					setTargetNextID(reader.getTargetDescriptor());

				} finally {
					wizard.setNextButtonEnabled(true);
				}
			}
		}.start();
	}

}
