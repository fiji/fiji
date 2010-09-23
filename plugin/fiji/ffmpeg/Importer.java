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
		IO io = null;
		try {
			io = new IO();
			setStack(path, io.readMovie(path).getStack());
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
