package fiji.ffmpeg;

/*
 * Simple movie writer for ImageJ using ffmpeg-java; based on Libavformat API
 * example from FFMPEG
 *
 * Based on the example "output_example.c" provided with FFMPEG.  LGPL version
 * (no SWSCALE) by Uwe Mannl.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;

import java.io.IOException;

public class Exporter implements PlugIn {
	@Override
	public void run(String arg) {
		IO io;
		try {
			io = new IO(new IJProgress());
		} catch (IOException e) {
			IJ.error("This plugin needs ffmpeg to be installed!");
			return;
		}

		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.error("No image is open");
			return;
		}

		if (image.getType() == ImagePlus.GRAY32) {
			IJ.error("Need a color, 8-bit or 16-bit image");
			return;
		}

		String name = IJ.getImage().getTitle();
		SaveDialog sd = new SaveDialog("Export via FFMPEG",
				name, ".mpg");
		name = sd.getFileName();
		String directory = sd.getDirectory();
		String path = directory+name;

		GenericDialog gd = new GenericDialog("FFMPEG Exporter");
		gd.addNumericField("Framerate", 25, 0);
		gd.addNumericField("Bitrate", 400000, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int frameRate = (int)gd.getNextNumber();
		int bitRate = (int)gd.getNextNumber();

		try {
			io.writeMovie(image, path, frameRate, bitRate);
			IJ.showStatus("Saved " + path + ".");
		} catch (OutOfMemoryError e) {
			io.free();
			IJ.error("Could not write " + path + ": " + e);
		} catch (IOException e) {
			io.free();
			IJ.error("Could not write " + path + ": " + e);
		}
	}
}
