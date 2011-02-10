package fiji;

import ij.IJ;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import java.io.File;

public class Compile_and_Run implements PlugIn {
	protected static String directory, fileName;

	public void run(String arg) {
		if (arg == null || arg.equals("")) {
			if (directory == null)
				directory = IJ.getDirectory("plugins");
			OpenDialog od = new OpenDialog("Compile and Run Plugin...", directory, fileName);
			if (od.getFileName() == null)
				return;
			directory = od.getDirectory();
			fileName = od.getFileName();
			arg = directory + File.separator + fileName;
		}
		IJ.runPlugIn("fiji.scripting.java.Refresh_Javas", arg);
	}
}
