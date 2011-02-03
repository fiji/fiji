import ij.ImagePlus;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/**
 * This is a template for a plugin that requires one image to
 * be opened, and takes it as parameter.
 */
public class Bare_PlugInFilter implements PlugInFilter {
	protected ImagePlus image;

	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
	 * @param image is the currently opened image
	 */
	public int setup(String arg, ImagePlus image) {
		this.image = image;
		/*
		 * The current return value accepts all gray-scale
		 * images (if you access the pixels with ip.getf(x, y)
		 * anyway, that works quite well.
		 *
		 * It could also be DOES_ALL; you can add "| NO_CHANGES"
		 * to indicate that the current image will not be
		 * changed by this plugin.
		 *
		 * Beware of DOES_STACKS: this will call the run()
		 * method with all slices of the current image
		 * (channels, z-slices and frames, all). Most likely
		 * not what you want.
		 */
		return DOES_8G | DOES_16 | DOES_32;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (typically, plugins use
	 * the ImagePlus set above instead).
	 */
	public void run(ImageProcessor ip) {
	}
}