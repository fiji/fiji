import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/*
 * This plugin calculates a classification based on the histogram of the image,
 * by generalizing the IsoData algorithm to more than two classes.
 *
 * (C) 2009 Johannes Schindelin, released under GPL v2
 */
public class IsoData_Classifier implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G | DOES_16;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("How many classes?");
		gd.addNumericField("number_of_classes", 3, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int n = (int)gd.getNextNumber();
		int[] histogram = ip.getHistogram();
		if (n < 2 || n > histogram.length) {
			IJ.error("Need between 2 and " + histogram.length
					+ " classes!");
			return;
		}

		int[] classes = isoData(histogram, n);
		mapPixels(classes, ip.getPixels());
	}

	int[] isoData(int[] histogram, int n) {
		int[] result = new int[histogram.length];

		int total = 0;
		for (int i = 0; i < histogram.length; i++)
			total += histogram[i] * i;

		int left = 0;
		for (int j = 0, i = 0; j < n; j++) {
			int i2 = i, previousLeft = left, count = 0;
			while (i2 < histogram.length &&
					left * n / total < j + 1) {
				left += histogram[i2] * i2;
				count += histogram[i2++];
			}
			int v = count > 0 ?
				(left - previousLeft) / count : (i2 + i) / 2;
			while (i < i2)
				result[i++] = v;
		}

		return result;
	}

	void mapPixels(int[] classes, Object pixels) {
		if (pixels instanceof byte[])
			mapPixels(classes, (byte[])pixels);
		else
			mapPixels(classes, (short[])pixels);
	}

	void mapPixels(int[] classes, byte[] pixels) {
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = (byte)classes[pixels[i] & 0xff];
	}

	void mapPixels(int[] classes, short[] pixels) {
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = (short)classes[pixels[i] & 0xffff];
	}
}
