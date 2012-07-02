// Save the active image as .icns file
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

import iconsupport.icns.IcnsCodec;
import iconsupport.icns.IconSuite;

public class Icns_Writer implements PlugIn {

	public void run (String arg) {
		ImagePlus image = WindowManager.getCurrentImage();
		if (image == null) {
			IJ.showStatus("No image is open");
			return;
		}

		// TODO: support saving more than one image

		int w = image.getWidth(), h = image.getHeight();
		IconSuite icons = new IconSuite();
		if (w == 16 && h == 16)
			icons.setSmallIcon((BufferedImage)image.getImage());
		else if (w == 32 && h == 32)
			icons.setLargeIcon((BufferedImage)image.getImage());
		else if (w == 48 && h == 48)
			icons.setHugeIcon((BufferedImage)image.getImage());
		else if (w == 128 && h == 128)
			icons.setThumbnailIcon((BufferedImage)image.getImage());
		else {
			IJ.error("Invalid dimensions: " + w + "x" + h +
					"\nMust be one of 16x16, 32x32, " +
					"48x48 or 128x128");
			return;
		}

		String path = arg;
		if (path == null || path.length() < 1) {
			String name = image.getTitle();
			SaveDialog sd = new SaveDialog("Save as Icns",
					name, ".icns");
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
			IcnsCodec codec = new IcnsCodec();
			codec.encode(icons, out);
			out.close();
		} catch (IOException e) {
			IJ.error("Failed to write " + path + ": " + e);
		}
	}
}
