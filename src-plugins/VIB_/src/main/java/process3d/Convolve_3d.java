/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

public class Convolve_3d extends Convolve3d implements PlugInFilter {
	protected ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | DOES_16 | DOES_32;
	}

	public void run(ImageProcessor ip) {
		GenericDialogPlus gd = new GenericDialogPlus("Convolve (3D)");
		gd.addImageChoice("kernel:", null);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus kernelImage = gd.getNextImage();
		if (kernelImage.getType() != ImagePlus.GRAY32) {
			IJ.error("Need a 32-bit image!");
			return;
		}

		int w = kernelImage.getWidth();
		int h = kernelImage.getHeight();
		int d = kernelImage.getStackSize();
		float[][][] kernel = new float[w][h][d];
		for (int k = 0; k < d; k++) {
			float[] pixels = (float[])kernelImage.getStack().getProcessor(k + 1).getPixels();
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++)
					kernel[i][j][k] = pixels[i + w * j];
		}

		convolve(image, kernel).show();
	}
}