package fiji.selection;

import ij.IJ;
import ij.process.ByteProcessor;

import ij.gui.Toolbar;

import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.awt.Rectangle;

public class Select_Bounding_Box implements PlugInFilter {
	enum Mode { SELECTION, AUTOCROP };
	Mode mode = Mode.SELECTION;

	ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		if ("autocrop".equals(arg))
			mode = Mode.AUTOCROP;
		return DOES_ALL | DOES_STACKS | SUPPORTS_MASKING | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		Rectangle rect = ip.getRoi();
		if (rect == null)
			rect = new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
		else {
			rect = (Rectangle)rect.clone();
			// abuse width/height for maxX/maxY
			rect.width += rect.x;
			rect.height += rect.y;
		}


		double background = ip.getBestIndex(Toolbar.getBackgroundColor());
		if (!(ip instanceof ByteProcessor))
			background = ip.getMin() + (ip.getMax() - ip.getMin()) * background / 255.0;
		findMinY(ip, rect, background);
		findMaxY(ip, rect, background);
		findMinX(ip, rect, background);
		findMaxX(ip, rect, background);

		// make it the proper width/height again
		rect.width -= rect.x;
		rect.height -= rect.y;
		switch (mode) {
			case SELECTION: image.setRoi(rect); break;
			case AUTOCROP: crop(image, rect); break;
		}
	}

	void findMinY(ImageProcessor ip, Rectangle rect, double background) {
		for (int y = rect.y; y < rect.height; y++)
			for (int x = rect.x; x < rect.width; x++)
				if (ip.getf(x, y) != background) {
					rect.y = y;
					return;
				}
	}

	void findMaxY(ImageProcessor ip, Rectangle rect, double background) {
		for (int y = rect.height - 1; y >= rect.y; y--)
			for (int x = rect.x; x < rect.width; x++)
				if (ip.getf(x, y) != background) {
					rect.height = y + 1;
					return;
				}
	}

	void findMinX(ImageProcessor ip, Rectangle rect, double background) {
		for (int x = rect.x; x < rect.width; x++)
			for (int y = rect.y; y < rect.height; y++)
				if (ip.getf(x, y) != background) {
					rect.x = x;
					return;
				}
	}


	void findMaxX(ImageProcessor ip, Rectangle rect, double background) {
		for (int x = rect.width - 1; x >= rect.x; x--)
			for (int y = rect.y; y < rect.height; y++)
				if (ip.getf(x, y) != background) {
					rect.width = x + 1;
					return;
				}
	}

	public static ImageProcessor crop(ImageProcessor ip, Rectangle rect) {
		ip.setRoi(rect);
		return ip.crop();
	}

	public static void crop(ImagePlus image, Rectangle rect) {
		if (image.getStackSize() == 1) {
			image.setProcessor(image.getTitle(), crop(image.getProcessor(), rect));
			return;
		}
		ImageStack stack = new ImageStack(rect.width, rect.height);
		ImageStack orig = image.getStack();
		for (int i = 1; i < orig.getSize(); i++)
			stack.addSlice("", crop(orig.getProcessor(i), rect));
		image.setStack(image.getTitle(), stack);
	}
}
