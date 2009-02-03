package io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;

import java.util.List;

import net.sf.image4j.codec.ico.ICODecoder;

public class ICO_Reader extends ImagePlus implements PlugIn {

	/** Expects path as argument, or will ask for it and then open the image.*/
	public void run(final String arg) {
		File file = null;
		if (arg != null && arg.length() > 0)
			file = new File(arg);
		else {
			OpenDialog od =
				new OpenDialog("Choose .ico file", null);
			String directory = od.getDirectory();
			if (null == directory)
				return;
			file = new File(directory + "/" + od.getFileName());
		}

		try {
			List<BufferedImage> image = ICODecoder.read(file);
			if (image.size() < 1)
				return;

			setTitle(file.getName());
			setImage(image.get(0));

			for (int i = 1; i < image.size(); i++)
				new ImagePlus(file.getName() + "-" + (i + 1),
					image.get(i)).show();
		} catch (IOException e) {
			IJ.error("Error reading file " + file.getAbsolutePath()
				+ ": " + e);
		}
	}
}
