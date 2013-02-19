package fiji.color;

import ij.ImagePlus;

import ij.plugin.filter.PlugInFilter;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
* Convert all reds to magentas (to help red-green blind viewers)
*/
public class Convert_Red_To_Magenta implements PlugInFilter {
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
		return DOES_RGB;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (typically, plugins use
	 * the ImagePlus set above instead).
	 */
	public void run(ImageProcessor ip) {
		process((ColorProcessor)ip);
		image.updateAndDraw();
	}

	public static void process(ColorProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		int[] pixels = (int[])ip.getPixels();
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				int value = pixels[i + j * w];
				int red = (value >> 16) & 0xff;
				int green = (value >> 8) & 0xff;
				int blue = value & 0xff;
				if (false && blue > 16)
					continue;
				pixels[i + j * w] = (red << 16) | (green << 8) | red;
			}
	}
}