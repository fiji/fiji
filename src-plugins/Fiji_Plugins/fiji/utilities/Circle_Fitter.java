package fiji.utilities;

import ij.ImagePlus;

import ij.gui.GenericDialog;
import ij.gui.OvalRoi;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

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
