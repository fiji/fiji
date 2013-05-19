/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.measure.Calibration;

/**
 * Smooth_es an ImagePlus, either uniformly or by Gaussian blur.
 * The user can specify if for Gaussian smoothing, the dimensions of
 * the pixels should be taken into account when calculating the
 * kernel. The user also specifies the radius / std dev.
 */
public class Smooth_ extends Smooth implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {

		GenericDialog gd = new GenericDialog("Smooth_");
		gd.addChoice("Method",
				new String[]{"Uniform", "Gaussian"},
				"Gaussian");
		gd.addNumericField("sigma", 1.0f, 3);
		gd.addCheckbox("Use calibration", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		boolean useGaussian = gd.getNextChoice().equals("Gaussian");
		float sigma = (float)gd.getNextNumber();
		boolean useCalibration = gd.getNextBoolean();

		ImagePlus smoothed = smooth(image,
					useGaussian, sigma, useCalibration);
		smoothed.show();
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}
}
