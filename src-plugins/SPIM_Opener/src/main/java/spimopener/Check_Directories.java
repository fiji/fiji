package spimopener;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;

public class Check_Directories implements PlugIn {

	@Override
	public void run(String arg) {
		GenericDialogPlus gd = new GenericDialogPlus("Check file hierarchy");
		gd.addMessage("Check if the file structure under 'Original' is identical to\n" +
				"the file structure under 'To check'");
		gd.addDirectoryField("Original", "");
		gd.addDirectoryField("To check", "");
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		File f1 = new File(gd.getNextString());
		File f2 = new File(gd.getNextString());

		checkDirectories(f1, f2);
		IJ.log("done");
	}

	void checkDirectories(File org, File toCheck) {
		File[] files = org.listFiles();
		if(files == null)
			return;
		for(File f : files) {
			File newf = new File(toCheck, f.getName());
			if(!newf.exists()) {
				IJ.log(newf.getAbsolutePath() + " does not exist (" + f.getAbsolutePath() + ")");
				return;
			}
			if(f.isDirectory() != newf.isDirectory()) {
				IJ.log(newf.getAbsolutePath() + " is dir? " + newf.isDirectory() + " <-> " + f.getAbsolutePath() + " is dir? " + f.isDirectory());
				return;
			}
			if(f.isDirectory()) {
				checkDirectories(f, newf);
			}
		}
	}
}
