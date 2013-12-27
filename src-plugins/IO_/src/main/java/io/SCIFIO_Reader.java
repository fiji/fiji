package io;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import io.scif.img.IO;

import java.io.File;

import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.ImgPlus;

/**
 * Opens images using SCIFIO.
 * 
 * @author Johannes Schindelin
 */
public class SCIFIO_Reader extends ImagePlus implements PlugIn {

	@Override
	public void run(String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose .icns file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		@SuppressWarnings("rawtypes")
		ImgPlus img = IO.open(file.getPath());

		if (img == null) {
			IJ.error("Could not open " + file);
		}
		else {
			final String title = file.getName();
			setTitle(title);
			@SuppressWarnings("unchecked")
			final ImagePlus imp = ImageJFunctions.wrap(img, title);
			setStack(imp.getImageStack());

			if (arg.equals("")) show();
		}
	}

}
