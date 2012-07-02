package fiji.geom;

import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.GenericDialog;

import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.PlugInFilter;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/**
 * Calculate the shape index as defined in
 * J Koenderink and A van Doorn, “Surface shape and
 * curvature scales,” Image Vision Comput, vol. 10, no. 8,
 * pp. 557–565, 1992
 */
public class Shape_Index_Map implements PlugInFilter {
	protected ImagePlus image;
	protected static GaussianBlur gaussianBlur;

	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
	 * @param image is the currently opened image
	 */
	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | DOES_16 | DOES_32;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (we use the ImagePlus set above instead).
	 */
	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Shape index map");
		gd.addNumericField("Gaussian_blur_radius (0 = off)", 0, 0);
		gd.showDialog();
		if (!gd.wasCanceled())
			getShapeIndexMap(image, gd.getNextNumber()).show();
	}

	public static ImagePlus getShapeIndexMap(ImagePlus image, double gaussianBlurRadius) {
		ImageStack stack = image.getStack();
		ImageStack result = new ImageStack(image.getWidth(), image.getHeight());
		for (int i = 1; i <= stack.getSize(); i++) {
			ImageProcessor ip = stack.getProcessor(i);
			if (gaussianBlurRadius > 0) {
				FloatProcessor fp = (FloatProcessor)
					(image.getType() != ImagePlus.GRAY32 ? ip.convertToFloat() : ip.duplicate());
				if (gaussianBlur == null)
					gaussianBlur = new GaussianBlur();
				gaussianBlur.blurFloat(fp, gaussianBlurRadius, gaussianBlurRadius, 0.02);
				ip = fp;
			}
			result.addSlice("", getShapeIndex(ip));
		}
		return new ImagePlus("Shape index of " + image.getTitle(), result);
	}

	/**
	 * The formula is:
	 *
	 *                                 dnx_x + dny_y
	 * s = 2 / PI * arctan ---------------------------------------
	 *                     sqrt((dnx_x - dny_y)^2 + 4 dny_x dnx_y)
	 *
	 * where _x and _y are the x and y components of the
	 * partial derivatives of the normal vector of the surface
	 * defined by the intensities of the image.
	 *
	 * n_x and n_y are the negative partial derivatives of the
	 * intensity, approximated by simple differences.
	 */
	public static ImageProcessor getShapeIndex(ImageProcessor ip) {
		ImageProcessor dx = deriveX(ip);
		ImageProcessor dy = deriveY(ip);
		ImageProcessor dxx = deriveX(dx);
		ImageProcessor dxy = deriveY(dx);
		ImageProcessor dyx = deriveX(dy);
		ImageProcessor dyy = deriveY(dy);

		float factor = 2 / (float)Math.PI;
		int w = ip.getWidth(), h = ip.getHeight();
		FloatProcessor fp = new FloatProcessor(w, h);
		for (int i = 0; i < w; i++)
			for (int j = 0; j < h; j++) {
				float dnx_x = -dxx.getf(i, j);
				float dnx_y = -dxy.getf(i, j);
				float dny_x = -dyx.getf(i, j);
				float dny_y = -dyy.getf(i, j);
				double D = Math.sqrt((dnx_x - dny_y) * (dnx_x - dny_y) + 4 * dnx_y * dny_x);
				float s = factor * (float)Math.atan((dnx_x + dny_y) / D);
				fp.setf(i, j, Float.isNaN(s) ? 0 : s);
			}
		return fp;
	}

	public static ImageProcessor deriveX(ImageProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		FloatProcessor fp = new FloatProcessor(w, h);
		for (int j = 0; j < h; j++) {
			float previous = 0;
			for (int i = 0; i < w; i++) {
				float current = ip.getf(i, j);
				fp.setf(i, j, current - previous);
				previous = current;
			}
		}
		return fp;
	}

	public static ImageProcessor deriveY(ImageProcessor ip) {
		int w = ip.getWidth(), h = ip.getHeight();
		FloatProcessor fp = new FloatProcessor(w, h);
		for (int i = 0; i < w; i++) {
			float previous = 0;
			for (int j = 0; j < h; j++) {
				float current = ip.getf(i, j);
				fp.setf(i, j, current - previous);
				previous = current;
			}
		}
		return fp;
	}
}
