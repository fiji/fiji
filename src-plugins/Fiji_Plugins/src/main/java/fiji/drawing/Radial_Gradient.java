package fiji.drawing;

import ij.ImagePlus;

import ij.gui.Roi;
import ij.gui.Toolbar;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.awt.Rectangle;

public class Radial_Gradient implements PlugInFilter {
	ImagePlus image;

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		int in = Toolbar.getForegroundColor().getRGB();
		int out = Toolbar.getBackgroundColor().getRGB();
		int w = ip.getWidth();
		int[] pixels = (int[])ip.getPixels();
		Roi roi = image.getRoi();
		if (roi == null) {
			Gradient g = new Gradient(w, ip.getHeight(), in, out);
			while (g.fwd())
				pixels[g.x + g.y * w] = g.color;
		}
		else {
			Rectangle rect = roi.getBounds();
			Gradient g = new Gradient(rect.width, rect.height,
				in, out);
			while (g.fwd()) {
				int x = rect.x + g.x;
				int y = rect.y + g.y;
				if (roi.contains(x, y))
					pixels[x + y * w] = g.color;
			}
		}
	}

	class Gradient {
		int x, y, color;
		protected int w, h, halfW, halfH, max, yMinusHalfH2;
		protected int in, out;

		Gradient(int width, int height, int inColor, int outColor) {
			w = width;
			h = height;
			in = inColor;
			out = outColor;

			x = -1;
			y = 0;
			halfW = (w + 1) / 2;
			halfH = (h + 1) / 2;
			max = halfW * halfW + halfH * halfH;
			yMinusHalfH2 = (y - halfH) * (y - halfH);
		}

		protected int getColor(int distance, int max) {
			int result = 0;
			for (int i = 0xff; i != 0xff000000; i <<= 8)
				result |= i & (int)((in & i) +
						((out & i) - (in & i))
						* (long)distance / max);
			return result;
		}

		boolean fwd() {
			if (++x >= w) {
				x = 0;
				if (++y >= h)
					return false;
				yMinusHalfH2 = (y - halfH) * (y - halfH);
			}
			color = getColor((x - halfW) * (x - halfW)
					+ yMinusHalfH2, max);
			return true;
		}
	}
}
