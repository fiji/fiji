package vib;

import amira.AmiraParameters;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import vib.app.module.AverageBrain;

public class AverageBrain_ implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Transform Parameters");
		gd.addStringField("files", "");
		gd.addStringField("matrices", "");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		String[] fileNames = gd.getNextString().split(",");
		FastMatrix[] matrices = FastMatrix.parseMatrices(
				gd.getNextString());
		new AverageBrain().doit(image, fileNames, matrices);
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_8C;
	}
}

