/*
 * Two Point Correlation
 *
 * see J. G. Berryman and S. C. Blair,
 * ``Use of digital image analysis to estimate  fluid permeability of
 * porous materials I. Application of two-point correlation functions,''
 * J. Appl. Phys. 60, 1930-1938 (1986)
 *
 * Implementation by J. Schindelin, all rights reserved.
 */
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Two_Point_Correlation implements PlugInFilter {
	ImagePlus image;
	byte[] pixels;
	float[] convolved;
	int w,h;

	final int get(int i, int j) {
		if (i < 0 || j < 0 || i >= w || j >= h)
			return 0;
		return pixels[i + w * j] & 0xff;
	}

	final float getConvolved(float x, float y) {
		int i = (int)Math.floor(x), j = (int)Math.floor(y);
		if (i < -w + 1 || i >= w - 1 || j < -h + 1 || j >= h - 1)
			return 0;
		float dx = x - i, dy = y - j;
		float v00 = convolved[w - 1 + i + (h - 1 + j) * 2 * w];
		float v10 = convolved[w + i + (h - 1 + j) * 2 * w];
		float v01 = convolved[w - 1 + i + (h + j) * 2 * w];
		float v11 = convolved[w + i + (h + j) * 2 * w];

		float w00 = Float.MIN_VALUE + (1 - dx) * (1 - dx) + (1 - dy) * (1 - dy);
		float w10 = Float.MIN_VALUE + dx * dx + (1 - dy) * (1 - dy);
		float w01 = Float.MIN_VALUE + (1 - dx) * (1 - dx) + dy * dy;
		float w11 = Float.MIN_VALUE + dx * dx + dy * dy;

		return (v00 * w00 + v10 * w10 + v01 * w01 + v11 * w11) / (w00 + w10 + w01 + w11);
	}

	static int max(int i, int j) {
		return i > j ? i : j;
	}

	static int min(int i, int j) {
		return i < j ? i : j;
	}

	void getConvolvedNaive() {
		for (int y = -h + 1; y < h; y++)
			for (int x = -w + 1; x < w; x++) {
				int total = 0;
				int index = w - 1 + x + (h - 1 + y) * 2 * w;
				for (int i = min(0, -x); x + i < w
						&& i < w; i++)
					for (int j = min(0, -y); y + j < h
							&& j < h; j++) {
						convolved[index] += get(i, j)
							* get(x + i, y + j);
						total++;
					}
				convolved[index] /= (float)total * 255.0f * 255.0f;
				IJ.showProgress(index + 0, 4 * w * (h - 1));
			}
	}

	void getConvolved() {
		int n, nroot;
		for (nroot = 2; nroot < w || nroot < h; nroot *= 2);
		nroot *= 2;
		n = nroot * nroot;
		double[][] data = new double[n][2];
		double[][] data2 = new double[n][2];
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				int index = i + w * j;
				data[index + w * j][0] = pixels[index];
				data2[w - i - 1 + 2 * w * (h - j - 1)][0] =
					pixels[index];
			}
		IJ.showProgress(0.0);
		double[][] inv = FFT.fft(data);
		IJ.showProgress(0.3);
		double[][] inv2 = FFT.fft(data2);
		IJ.showProgress(0.6);
		double[][] iconv = FFT.multiply(inv, inv2);
		IJ.showProgress(0.7);
		double[][] conv = FFT.ifft(iconv);
		IJ.showProgress(0.9);
		for (int i = 0; i < convolved.length; i++)
			convolved[i] = (float)conv[i][0] / 255.0f / 255.0f;
		IJ.showProgress(1.0);
	}

	public void run(ImageProcessor ip) {
		w = image.getWidth();
		h = image.getHeight();

		GenericDialog gd = new GenericDialog("Two Point Correlation");
		gd.addNumericField("min_radius", 0, 1);
		gd.addNumericField("max_radius",  Math.sqrt(w * w + h * h), 1);
		gd.addNumericField("radius_step", 0.3, 1);
		gd.addCheckbox("invert", false);
		gd.addCheckbox("normalize", false);
		gd.addCheckbox("naive computation (slow)", false);
		gd.addCheckbox("show convolved image", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		double min_radius = gd.getNextNumber();
		double max_radius = gd.getNextNumber();
		double radius_step = gd.getNextNumber();
		boolean invert = gd.getNextBoolean();
		boolean normalize = gd.getNextBoolean();
		boolean naive = gd.getNextBoolean();
		boolean show = gd.getNextBoolean();
		pixels = (byte[])image.getProcessor().getPixels();

		if (invert)
			for (int i = 0; i < pixels.length; i++)
				pixels[i] = (byte)(255 - (pixels[i] & 0xff));

		convolved = new float[4 * w * h];
		if (naive)
			getConvolvedNaive();
		else
			getConvolved();

		if (show)
			new ImagePlus("convolved",
					new FloatProcessor(2 * w, 2 * h,
						convolved, null)).show();

		int n = (int)((max_radius - min_radius) / radius_step) + 1;
		float[] r = new float[n], v = new float[n];
		float max_value = -Float.MAX_VALUE, min_value = -max_value;
		for (int i = 0; i < n; i++) {
			float r2 = (float)(min_radius
					+ i * (max_radius - min_radius) / n);
			int div = (int)(1 + 2 * Math.PI * r2);
			float sum = 0;
			for (int angle = 0; angle < div; angle++) {
				float a = (float)(2 * Math.PI * angle / div);
				sum += getConvolved((float)(r2 * Math.cos(a)),
						(float)(r2 * Math.sin(a)));
			}
			r[i] = r2;
			v[i] = sum / div;
			if (max_value < v[i])
				max_value = v[i];
			else if (min_value > v[i])
				min_value = v[i];
		}

		if (normalize) {
			float factor = getConvolved(0, 0);
			for (int i = 0; i < r.length; i++)
				v[i] /= factor;
			min_value /= factor;
			max_value /= factor;
		}

		PlotWindow plot = new PlotWindow("Two Point Correlation",
				"radius", "S2(radius)", r, v);
		plot.setLimits(0, max_radius, min_value, max_value);
		plot.draw();
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		// TODO: handle 16-bit and 32-bit
		return DOES_8G | NO_CHANGES;
	}
}

class FFT {
	/*
	 * Thankfully borrowed from
	 * http://www.cs.princeton.edu/introcs/97data/FFT.java
	 * compute the FFT of x[], x.length must be a power of 2!
	 */
	public static double[][] fft(double[][] x) {
		int n = x.length;

		if (n == 1)
			return new double[][] { x[0] };

		// Cooley-Tukey FFT
		if ((n & 1) != 0)
			throw new RuntimeException("n is not a power of 2");

		// fft of even terms
		double[][] even = new double[n/2][];
		for (int k = 0; k < n/2; k++)
			even[k] = x[2*k];
		double[][] q = fft(even);

		// fft of odd terms
		double[][] odd  = even;  // reuse the array
		for (int k = 0; k < n/2; k++)
			odd[k] = x[2*k + 1];
		double[][] r = fft(odd);

		// combine
		double[][] y = new double[n][2];
		for (int k = 0; k < n/2; k++) {
			double kth = -2 * k * Math.PI / n;
			double c = Math.cos(kth), s = Math.sin(kth);
			double kr0 = c * r[k][0] - s * r[k][1];
			double kr1 = c * r[k][1] + s * r[k][0];
			y[k][0] = q[k][0] + kr0;
			y[k][1] = q[k][1] + kr1;
			y[k + n/2][0] = q[k][0] - kr0;
			y[k + n/2][1] = q[k][1] - kr1;
		}
		return y;
	}

	public static double[][] conjugate(double[][] x) {
		double[][] result = new double[x.length][2];
		for (int i = 0; i < x.length; i++) {
			result[i][0] = x[i][0];
			result[i][1] = -x[i][1];
		}
		return result;
	}

	public static double[][] divide(double[][] x, double factor) {
		double[][] result = new double[x.length][2];
		for (int i = 0; i < x.length; i++) {
			result[i][0] = x[i][0] / factor;
			result[i][1] = x[i][1] / factor;
		}
		return result;
	}

	// compute the inverse FFT of x[]
	public static double[][] ifft(double[][] x) {
		return divide(conjugate(fft(conjugate(x))), x.length);
	}

	public static void print(double[][] values) {
		for (int i = 0; i < values.length; i++)
			System.out.print(" " + values[i][0] + ";"
					+ values[i][1]);
		System.out.println("");
	}

	public static double[][] multiply(double[][] x, double[][] y) {
		double[][] r = new double[x.length][2];
		for (int i = 0; i < x.length; i++) {
			r[i][0] = x[i][0] * y[i][0] - x[i][1] * y[i][1];
			r[i][1] = x[i][1] * y[i][0] + x[i][0] * y[i][1];
		}
		return r;
	}

	public static void main(String[] args) {
		double[][] values = {{1, 2}, {3, 4}, {5, 6}, {0, 1},
			{0, 0}, {0, 0}, {0, 0}, {0, 0}};
		double[][] f = {{0, 0}, {0, 0}, {0, 0}, {0, 0},
			{0, 1}, {5, 6}, {3, 4}, {1, 2}};
		double[][] f2 = {{1, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0},
			{0, 0}, {0, 0}, {0, 0}};
		double[][] inv = fft(values);
		double[][] finv = fft(f);
		double[][] invinv = ifft(inv);
		double[][] imul = multiply(inv, finv);
		double[][] mul = ifft(imul);
		print(values);
		print(inv);
		print(invinv);
		print(finv);
		print(imul);
		print(mul);
	}
}

