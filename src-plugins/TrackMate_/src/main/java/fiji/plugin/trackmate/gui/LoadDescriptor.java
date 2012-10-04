package fiji.plugin.trackmate.gui;

import java.io.File;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class LoadDescriptor <T extends RealType<T> & NativeType<T>> extends SomeDialogDescriptor<T> {


	public static final String DESCRIPTOR = "LoadingPanel";

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public void displayingPanel() {

		try {

			if (null == file) {
				File folder = new File(System.getProperty("user.dir")).getParentFile().getParentFile();
				try {
					file = new File(folder.getPath() + File.separator + plugin.getModel().getSettings().imp.getShortTitle() +".xml");
				} catch (NullPointerException npe) {
					file = new File(folder.getPath() + File.separator + "TrackMateData.xml");
				}
			}

			GuiReader<T> reader = new GuiReader<T>(wizard);
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
