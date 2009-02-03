package io;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij.io.OpenDialog;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import iconsupport.icns.IcnsCodec;
import iconsupport.icns.IconSuite;

public class Icns_Reader extends ImagePlus implements PlugIn {

	/** Expects path as argument, or will ask for it and then open the image.*/
	public void run(final String arg) {
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

		try {
			FileInputStream in = new FileInputStream(file);
			IcnsCodec codec = new IcnsCodec();
			IconSuite icons = codec.decode(in);
			alreadyShown = 0;
			show(file.getName(), icons.getThumbnailIcon());
			show(file.getName(), icons.getHugeIcon());
			show(file.getName(), icons.getLargeIcon());
			show(file.getName(), icons.getSmallIcon());
		} catch (IOException e) {
			IJ.error("Error reading file " + file.getAbsolutePath()
				+ ": " + e);
		}
	}

	private int alreadyShown;

	private void show(String name, BufferedImage image) {
		if (image == null)
			return;

		if (alreadyShown > 0)
			new ImagePlus(name + "-" + (++alreadyShown),
					image).show();
		else {
			setTitle(name);
			setImage(image);
			alreadyShown++;
		}
	}
}
