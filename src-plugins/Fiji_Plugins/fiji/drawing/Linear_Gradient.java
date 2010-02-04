package fiji.drawing;

import ij.IJ;
import ij.gui.Line;

import ij.ImagePlus;

import ij.gui.Roi;
import ij.gui.Toolbar;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.awt.Rectangle;

public class Linear_Gradient implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (roi == null || roi.getType() != roi.LINE) {
			IJ.error("Need a linear selection");
			return;
		}
		Line line = (Line)roi;
		double length = line.getLength();
		if (length == 0) {
			IJ.error("Line too short");
			return;
		}

		int from = Toolbar.getBackgroundColor().getRGB();
		int to = Toolbar.getForegroundColor().getRGB();

		int w = ip.getWidth(), h = ip.getHeight();
		int[] pixels = (int[])ip.getPixels();
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				double scalar = (i - line.x1d) * (line.x2d - line.x1d)
					+ (j - line.y1d) * (line.y2d - line.y1d);
				pixels[i + j * w] = getColor(from, to, 	scalar / length / length);
			}
		image.updateAndDraw();
	}

	int getByte(int from, int to, double factor, int shift) {
		from = (from >> shift) & 0xff;
		to = (to >> shift) & 0xff;
		int value = (int)Math.round(from + factor * (to - from));
		return Math.min(255, Math.max(0, value)) << shift;
	}

	int getColor(int from, int to, double factor) {
		return getByte(from, to, factor, 0) |
			getByte(from, to, factor, 8) |
			getByte(from, to, factor, 16);
	}
}