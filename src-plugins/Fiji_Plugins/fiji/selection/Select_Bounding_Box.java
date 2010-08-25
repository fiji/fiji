package fiji.selection;

import ij.IJ;

import ij.gui.Toolbar;

import ij.ImagePlus;
import ij.ImageStack;

import ij.plugin.filter.PlugInFilter;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;

import java.util.Arrays;

public class Select_Bounding_Box implements PlugInFilter {
	enum Mode { SELECTION, AUTOCROP, AUTOAUTOCROP };
	Mode mode = Mode.SELECTION;

	ImagePlus image;

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		if ("autocrop".equals(arg))
			mode = Mode.AUTOCROP;
		else if ("autoautocrop".equals(arg))
			mode = Mode.AUTOAUTOCROP;
		return DOES_ALL | DOES_STACKS | SUPPORTS_MASKING | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		double background;
		if (mode == Mode.AUTOAUTOCROP)
			background = guessBackground(ip);
		else if (ip instanceof ColorProcessor) {
			Color color = Toolbar.getBackgroundColor();
			background = (color.getRed() << 16) |
				(color.getGreen() << 8) | color.getBlue();
		}
		else {
			background =
				ip.getBestIndex(Toolbar.getBackgroundColor());
			if (!(ip instanceof ByteProcessor))
				background = ip.getMin() + background *
					(ip.getMax() - ip.getMin()) / 255.0;
		}

		Rectangle rect = getBoundingBox(ip, ip.getRoi(), background);
		switch (mode) {
			case SELECTION:
				image.setRoi(rect);
				break;
			case AUTOCROP: case AUTOAUTOCROP:
				crop(image, rect);
				break;
		}
	}

	public double guessBackground(ImageProcessor ip) {
		Rectangle rect = ip.getRoi();
		if (rect == null)
			rect = new Rectangle(0, 0,
					ip.getWidth(), ip.getHeight());

		// get the border's values
		double[] values =
			new double[(rect.width + rect.height - 2) * 2];
		for (int i = 0; i < rect.width; i++) {
			values[i] = ip.getf(rect.x + i, rect.y + 0);
			values[i + rect.width] =
				ip.getf(rect.x + i, rect.y + rect.height - 1);
		}
		for (int i = 1; i < rect.height - 1; i++) {
			values[i + 2 * rect.width - 1] =
				ip.getf(rect.x + 0, rect.y + i);
			values[i + 2 * rect.width - 1 + rect.height - 2] =
				ip.getf(rect.x + rect.width - 1, rect.y + i);
		}
		if (ip instanceof ColorProcessor)
			for (int i = 0; i < values.length; i++)
				values[i] = ((int)values[i]) & 0xffffff;

		// return the most frequent value
		Arrays.sort(values);
		int best = 0, bestCount = 1, currentCount = 1;
		for (int i = 1; i < values.length; i++)
			if (values[i] != values[i - 1])
				currentCount = 1;
			else if (++currentCount > bestCount) {
				best = i;
				bestCount = currentCount;
			}
		return values[best];
	}

	public static Rectangle getBoundingBox(ImageProcessor ip, Rectangle rect, double background) {
		if (rect == null)
			rect = new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
		else {
			rect = (Rectangle)rect.clone();
			// abuse width/height for maxX/maxY
			rect.width += rect.x;
			rect.height += rect.y;
		}

		Cropper cropper = (ip instanceof ColorProcessor) ?
			new CropperRGB(ip, background) :
			new CropperDefault(ip, background);

		cropper.findMinY(rect);
		cropper.findMaxY(rect);
		cropper.findMinX(rect);
		cropper.findMaxX(rect);

		// make it the proper width/height again
		rect.width -= rect.x;
		rect.height -= rect.y;
		return rect;
	}

	abstract static class Cropper {
		ImageProcessor ip;
		double background;

		Cropper(ImageProcessor ip, double background) {
			this.ip = ip;
			this.background = background;
		}

		abstract boolean isBackground(int x, int y);

		final void findMinY(Rectangle rect) {
			for (int y = rect.y; y < rect.height; y++)
				for (int x = rect.x; x < rect.width; x++)
					if (!isBackground(x, y)) {
						rect.y = y;
						return;
					}
		}

		final void findMaxY(Rectangle rect) {
			for (int y = rect.height - 1; y >= rect.y; y--)
				for (int x = rect.x; x < rect.width; x++)
					if (!isBackground(x, y)) {
						rect.height = y + 1;
						return;
					}
		}

		final void findMinX(Rectangle rect) {
			for (int x = rect.x; x < rect.width; x++)
				for (int y = rect.y; y < rect.height; y++)
					if (!isBackground(x, y)) {
						rect.x = x;
						return;
					}
		}


		final void findMaxX(Rectangle rect) {
			for (int x = rect.width - 1; x >= rect.x; x--)
				for (int y = rect.y; y < rect.height; y++)
					if (!isBackground(x, y)) {
						rect.width = x + 1;
						return;
					}
		}
	}

	final static class CropperDefault extends Cropper {
		CropperDefault(ImageProcessor ip, double background) {
			super(ip, background);
		}

		final boolean isBackground(int x, int y) {
			return ip.getf(x, y) == background;
		}
	}

	final static class CropperRGB extends Cropper {
		int[] pixels;
		int w, backgroundRGB;
		CropperRGB(ImageProcessor ip, double background) {
			super(ip, background);
			pixels = (int[])ip.getPixels();
			w = ip.getWidth();
			backgroundRGB = ((int)background) & 0xffffff;
		}

		final boolean isBackground(int x, int y) {
			return (pixels[x + w * y] & 0xffffff) == backgroundRGB;
		}
	}

	public static ImageProcessor crop(ImageProcessor ip, Rectangle rect) {
		ip.setRoi(rect);
		return ip.crop();
	}

	public static void crop(ImagePlus image, Rectangle rect) {
		if (image.getWidth() == rect.width && image.getHeight() == rect.height)
			return;
		image.changes = true;
		if (image.getStackSize() == 1) {
			image.setProcessor(image.getTitle(),
					crop(image.getProcessor(), rect));
			return;
		}
		ImageStack stack = new ImageStack(rect.width, rect.height);
		ImageStack orig = image.getStack();
		for (int i = 1; i < orig.getSize(); i++)
			stack.addSlice("", crop(orig.getProcessor(i), rect));
		image.setStack(image.getTitle(), stack);
	}
}
