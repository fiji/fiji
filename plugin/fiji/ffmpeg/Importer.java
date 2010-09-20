package fiji.ffmpeg;

import ij.IJ;
import ij.ImagePlus;

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

		String path = file.getAbsolutePath();
		try {
			setStack(path, new IO().readMovie(path).getStack());
			if (arg.equals(""))
				show();
		} catch (IOException e) {
			IJ.error("Could not read " + path + ": " + e);
		}
	}
}
