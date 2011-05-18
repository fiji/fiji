package fiji.denoise;

import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.GenericDialog;

import ij.plugin.filter.PlugInFilter;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * This denoising method is based on total-variation, originally proposed by
 * Rudin, Osher and Fatemi. In this particular case fixed point iteration
 * is utilized.
 *
 * For the included image, a fairly good result is obtained by using a
 * theta value around 12-16. A possible addition would be to analyze the
 * residual with an entropy function and add back areas that have a lower
 * entropy, i.e. there are some correlation between the surrounding pixels.
 *
 * Based on the Matlab code by Philippe Magiera & Carl Löndahl:
 * http://www.mathworks.com/matlabcentral/fileexchange/22410-rof-denoising-algorithm
 */
public class ROF_Denoise implements PlugInFilter {
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
		return DOES_32;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (typically, plugins use
	 * the ImagePlus set above instead).
	 */
	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("ROF Denoise");
		gd.addNumericField("Theta", 25, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		float theta = (float)gd.getNextNumber();

		ImageStack stack = image.getStack();
		for (int slice = 1; slice <= stack.getSize(); slice++)
			denoise((FloatProcessor)stack.getProcessor(slice), theta);
		image.updateAndDraw();
	}

	public static void denoise(FloatProcessor ip, float theta) {
		denoise(ip, theta, 1, 0.25f, 5);
	}

	public static void denoise(FloatProcessor ip, float theta, float g, float dt, int iterations) {
		int w = ip.getWidth();
		int h = ip.getHeight();
		float[] pixels = (float[])ip.getPixels();

		float[] u = new float[w * h];
		float[] p = new float[w * h * 2];
		float[] d = new float[w * h * 2];
		float[] du = new float[w * h * 2];
		float[] div_p = new float[w * h];

		for (int iteration = 0; iteration < iterations; iteration++) {
			for (int i = 0; i < w; i++) {
				for (int j = 1; j < h - 1; j++)
					div_p[i + w * j] = p[i + w * j] - p[i + w * (j - 1)];
				// Handle boundaries
				div_p[i] = p[i];
				div_p[i + w * (h - 1)] = -p[i + w * (h - 1)];
			}

			for (int j = 0; j < h; j++) {
				for (int i = 1; i < w - 1; i++)
					div_p[i + w * j] += p[i + w * (j + h)] - p[i - 1 + w * (j + h)];
				// Handle boundaries
				div_p[w * j] = p[w * (j + h)];
				div_p[w - 1 + w * j] = -p[w - 1 + w * (j + h)];
			}

			// Update u
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++)
					u[i + w * j] = pixels[i + w * j] - theta * div_p[i + w * j];

			// Calculate forward derivatives
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					if (i < w - 1)
						du[i + w * (j + h)] = u[i + 1 + w * j] - u[i + w * j];
					if (j < h - 1)
						du[i + w * j] = u[i + w * (j + 1)] - u[i + w * j];
				}

			// Iterate
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {
					float du1 = du[i + w * j], du2 = du[i + w * (j + h)];
					d[i + w * j] = 1 + dt / theta / g * Math.abs((float)Math.sqrt(du1 * du1 + du2 * du2));
					d[i + w * (j + h)] = 1 + dt / theta / g * Math.abs((float)Math.sqrt(du1 * du1 + du2 * du2));
					p[i + w * j] = (p[i + w * j] - dt / theta * du[i + w * j]) / d[i + w * j];
					p[i + w * (j + h)] = (p[i + w * (j + h)] - dt / theta * du[i + w * (j + h)]) / d[i + w * (j + h)];
				}
		}
		System.arraycopy(u, 0, pixels, 0, w * h);
	}
}