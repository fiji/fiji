package fiji.util;

import ij.ImagePlus;

import ij.gui.GenericDialog;
import ij.gui.OvalRoi;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

/*
 * Fit a circle selection to an image.
 *
 * The implementation largely follows
 *
 *  http://www.dtcenter.org/met/users/docs/write_ups/circle_fit.pdf
 *
 * (It is summarized below.)
 *
 * The major contributions of this plugin is that it does not take a list of
 * points, but uses all pixel locations, weighted by intensity.
 *
 * The circle fitting works like this: given a set of points
 * (x_i, y_i), i = 1, .., N try to fit a circle with radius r and center
 * (x_c, y_c) using the least squares method.
 *
 * The function to minimize is
 *
 *  S = \sum g_i^2, where g_i = (x_i - x)^2 + (y_i - y)^2 - r^2
 *
 * (It must be the square of g_i so that no summand is negative.)
 *
 * Without loss of generality, it can be assumed that
 *
 *  \sum x_i = \sum y_i = 0
 *
 * (If that is not the case, just subtract the means of {x_i} and {y_i} resp.
 * and add them to x_c and y_c resp.)
 *
 * The derivative of S with regard to r^2 must be 0:
 *
 *  d/dr^2 \sum g_i^2 = \sum 2 * g_i * -1
 *
 * and therefore
 *
 *  \sum g_i = 0
 *
 * Expanded, with uu = \sum x_i * x_i, uv = \sum x_i * y_i and u, v, uu, uuu,
 * uuv, uvv, vvv defined analogously, this yields
 *
 *  uu - 2 * u * x_c + N * x_c^2 + vv - 2 * v * y_c + N * y_c^2 - N * r^2 = 0
 *
 * With u = v = 0, as per the earlier assumption, it follows that
 *
 *  uu + vv + N * (x_c^2 + y_c^2 - r^2) = 0 (eq. 1)
 *
 * The derivative of S with regard to x_c must be 0:
 *
 *  d/dx_c \sum g_i^2 = \sum 2 * g_i * (2 * (x_i - x_c) * (-1))
 *  = -4 * \sum g_i * x_i + 4 * x_c * \sum g_i
 *
 * and therefore
 *
 *  \sum g_i * x_i = 0 (as \sum g_i must be 0 already)
 *
 * Expanded, with uu and friends defined as before, this yields
 *
 *  uuu - 2 * uu * x_c + u * x_c^2
 *  + uvv - 2 * uv * y_c + v * y_c^2 + u * r^2 = 0
 *
 * Like before, u = v = 0, therefore
 *
 *  2 * uu * x_c + 2 * uv * y_c = uuu + uvv (eq. 2)
 *
 * For y_c, it is
 *
 *  2 * uv * x_c + 2 * vv * y_c = uuv + vvv (eq. 3)
 *
 * Equations 2 and 3 determine x_c and y_c, and equation 1 gives us the radius.
 */
public class Circle_Fitter implements PlugInFilter {
	ImagePlus image;
	ImageProcessor ip;
	float threshold;
	int w, h;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		this.ip = ip;
		w = ip.getWidth();
		h = ip.getHeight();
		threshold = getDefaultThreshold();

		GenericDialog gd = new GenericDialog("Fit Circle");
		gd.addNumericField("threshold", threshold, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		threshold = (float)gd.getNextNumber();

		// calculate mean centroid
		float x, y, total;
		x = y = total = 0;
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				float value = getValue(i, j);
				x += i * value;
				y += j * value;
				total += value;
			}

		x /= total;
		y /= total;

		// calculate the rest
		float uu, uv, vv, uuu, uuv, uvv, vvv;
		uu = uv = vv = uuu = uuv = uvv = vvv = 0;
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				float value = getValue(i, j);
				float u = i - x;
				float v = j - y;
				uu += u * u * value;
				uv += u * v * value;
				vv += v * v * value;
				uuu += u * u * u * value;
				uuv += u * u * v * value;
				uvv += u * v * v * value;
				vvv += v * v * v * value;
			}

		// calculate center & radius
		float f = 0.5f / (uu * vv - uv * uv);
		float centerU = (vv * (uuu + uvv) - uv * (uuv + vvv)) * f;
		float centerV = (-uv * (uuu + uvv) + uu * (uuv + vvv)) * f;
		float radius = (float)Math.sqrt(centerU * centerU
				+ centerV * centerV + (uu + vv) / total);

		int x0 = (int)Math.max(0, x + centerU - radius);
		int y0 = (int)Math.max(0, y + centerV - radius);
		int x1 = (int)Math.min(w, x + centerU + radius);
		int y1 = (int)Math.min(h, y + centerV + radius);
		image.setRoi(new OvalRoi(x0, y0, x1 - x0, y1 - y0));
	}

	float getValue(int i, int j) {
		return Math.max(0, ip.getf(i, j) - threshold);
	}

	/*
	 * The default threshold is determined like this: as a circle is a
	 * linear structure, we want the _square root_ of the total number of
	 * pixels to contribute.
	 *
	 * Therefore, we split the histogram into background and foreground
	 * where the number of pixels in the foreground part is \sqrt(w * h)
	 * and the split point is the desired threshold.
	 */
	float getDefaultThreshold() {
		int w = ip.getWidth(), h = ip.getHeight();
		float min = (float)ip.getMin(), max = (float)ip.getMax();
		float[] histogram = new float[256];

		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++)
				histogram[(int)((ip.getf(i, j) - min)
					* 255.99f / (max - min))]++;
		int total = 0, i, want = (int)Math.sqrt(w * h);
		for (i = 255; i > 0 && total < want; i--)
			total += histogram[i];
		return min + i * (max - min) / 255.99f;
	}
}
