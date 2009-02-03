// Save the active image as .ico file
package io;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.io.SaveDialog;

import ij.plugin.PlugIn;

import java.awt.Image;

import java.awt.image.BufferedImage;

import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.image4j.codec.ico.ICOEncoder;

public class ICO_Writer implements PlugIn {

	public void run (String arg) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.showStatus("No image is open");
			return;
		}

		// TODO: support saving more than one image

		String path = arg;
		if (path == null || path.length() < 1) {
			String name = image.getTitle();
			SaveDialog sd = new SaveDialog("Save as ICO",
					name, ".ico");
			String directory = sd.getDirectory();
			if (directory == null)
				return;

			if (!directory.endsWith("/"))
				directory += "/";
			name = sd.getFileName();
			path = directory + name;
		}

		try {
			FileOutputStream out = new FileOutputStream(path);
			ICOEncoder.write((BufferedImage)image.getImage(), out);
			out.close();
		} catch (IOException e) {
			IJ.error("Failed to write " + path + ": " + e);
		}
	}
}
