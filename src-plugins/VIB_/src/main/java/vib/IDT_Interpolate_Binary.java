package vib;

import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/*
 * This plugin takes a binary stack as input, where some slices are
 * labeled (i.e. contain white regions), and some are not. The unlabaled
 * regions are interpolated by weighting the signed integer distance
 * transformed labeled slices.
 */

public class IDT_Interpolate_Binary extends BinaryInterpolator
		implements PlugInFilter {
	ImagePlus image;

	public void run(ImageProcessor ip) {
		ImageStack stack = image.getStack();
		run(stack);
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}
}

