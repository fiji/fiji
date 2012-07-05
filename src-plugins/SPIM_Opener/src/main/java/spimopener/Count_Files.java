package spimopener;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.plugin.PlugIn;

import java.io.File;

public class Count_Files implements PlugIn {
	@Override
	public void run(String arg) {
		GenericDialogPlus gd = new GenericDialogPlus("Count Files");
		gd.addDirectoryField("Directory", "");
		gd.addCheckbox("Count hidden files, too", false);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		String dir = gd.getNextString();
		boolean countHidden = gd.getNextBoolean();

		int n = count(new File(dir), countHidden);

		IJ.showMessage("Found " + n + " files");
	}

	public int count(File dir, boolean countHidden) {
		int count = 0;
		if(dir.isDirectory()) {
			File[] children = dir.listFiles();
			if(children != null) {
				for(File f : children)
					count += count(f, countHidden);
			}
			return count;
		}

		if(!dir.exists())
			return 0;
		if(dir.getName().charAt(0) == '.')
			return 0;
		return 1;
	}
}
