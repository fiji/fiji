package fiji.ffmpeg;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.io.OpenDialog;

import ij.plugin.PlugIn;

import java.io.File;
import java.io.IOException;

public class Importer extends ImagePlus implements PlugIn {
	/** Takes path as argument, or asks for it and then open the image.*/
	public void run(final String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose movie file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		GenericDialog gd = new GenericDialog("Import options");
		gd.addCheckbox("Use_virtual_stack", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		boolean useVirtualStack = gd.getNextBoolean();

		String path = file.getAbsolutePath();
		IO io = null;
		try {
			io = new IO(new IJProgress());
			setStack(path, io.readMovie(path, useVirtualStack).getStack());
			if (arg.equals(""))
				show();
		} catch (IOException e) {
			if (io != null)
				io.free();
			IJ.error("Could not read " + path + ": " + e);
		} catch (OutOfMemoryError e) {
			if (io != null)
				io.free();
			IJ.error("Ran out of memory while reading " + path);
		}
	}
}
