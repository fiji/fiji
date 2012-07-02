import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

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
		if (image.getStackSize() > 1)
			gd.addCheckbox("use_stack_histogram", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int n = (int)gd.getNextNumber();
		boolean useStackHistogram = image.getStackSize() > 1 ?
			gd.getNextBoolean() : false;

		ImageStack stack = image.getStack();
		int[] classes = null;
		if (useStackHistogram && stack.getSize() > 1) {
			int[] histogram = stack.getProcessor(1).getHistogram();
			for (int i = 2; i <= stack.getSize(); i++) {
				int[] h = stack.getProcessor(i).getHistogram();
				for (int j = 0; j < h.length; j++)
					histogram[j] += h[j];
			}
			classes = isoData(histogram, n);
		}

		for (int i = 1; i <= stack.getSize(); i++)
			mapPixels(classes, stack.getProcessor(i), n);
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

	void mapPixels(int[] classes, ImageProcessor ip, int n) {
		if (classes == null)
			classes = isoData(ip.getHistogram(), n);
		mapPixels(classes, ip.getPixels());
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
