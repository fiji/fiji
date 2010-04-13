package plugin;

import ij.ImagePlus;

import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Toolbar;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import ij.plugin.MacroInstaller;
import ij.plugin.PlugIn;

import ij.plugin.filter.ThresholdToSelection;

import java.util.TreeMap;

import util.FibonacciHeapDouble;

public class Lasso {
	public final static int BLOW = 0;
	public final static int LASSO = 1;
	public final static int MIN_LASSO = 2;
	public final static int MAX_TOOL = 2;

	public final static String[] modeTitles = {
		"Blow tool", "Lasso tool", "Lasso minimum tool"
	};

	private int mode = BLOW;
	private Difference difference;
	private double[] dijkstra;
	private int[] previous;
	private ImagePlus imp;

	public final int w, h;
	private Roi originalRoi;

	/**
	 * Multiply the spatial distance with this factor before adding to
	 * the color distance.
	 */
	private double ratioSpaceColor = 1;

	/** Create an uninitialized Lasso.
	 *  To initialize it, call initDijkstra(x,y,IJ.shiftKeyDown()); */
	public Lasso(ImagePlus imp, int mode) {
		this.imp = imp;
		this.mode = mode;
		this.w = imp.getWidth();
		this.h = imp.getHeight();
	}

	public Lasso(ImagePlus imp) {
		this(imp, BLOW);
	}

	/** Create and initialize a Lasso at point x,y. */
	public Lasso(ImagePlus imp, int mode, int x, int y, boolean shiftKeyDown) {
		this(imp, mode);
		initDijkstra(x, y, shiftKeyDown);
	}

	public void setRatioSpaceColor(double ratioSpaceColor) {
		this.ratioSpaceColor = ratioSpaceColor;
	}

	public void optionDialog() {
		GenericDialog gd = new GenericDialog("Lasso Tool Options");
		gd.addChoice("mode", Lasso.modeTitles, Lasso.modeTitles[mode]);
		gd.addNumericField("ratio space/color", ratioSpaceColor, 2);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		mode = gd.getNextChoiceIndex();
		ratioSpaceColor = gd.getNextNumber();
	}

	public ImagePlus getImage() {
		return imp;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public int getMode() { return mode; }

	public void moveLasso(int x, int y) {
		getDijkstra(x, y);
		int[] xPoints = new int[w * h];
		int[] yPoints = new int[w * h];
		int i = 0;
		do {
			if (i >= w * h)
				break;
			xPoints[i] = x;
			yPoints[i] = y;
			i++;
			int j = previous[x + w * y];
			x = j % w;
			y = j / w;
		} while (x != startX || y != startY);
		Roi roi = new PolygonRoi(xPoints, yPoints, i,
				PolygonRoi.FREELINE);
		if (originalRoi != null)
			roi = new ShapeRoi(originalRoi).or(new ShapeRoi(roi));
		imp.setRoi(roi);
		imp.updateAndDraw();
	}

	public void moveBlow(int x, int y) {
		getDijkstra(x, y);
		FloatProcessor fp = new FloatProcessor(w, h, dijkstra);
		fp.setThreshold(Double.MIN_VALUE, dijkstra[x + w * y] + 1,
				ImageProcessor.NO_LUT_UPDATE);
		ImagePlus blowImage = new ImagePlus("blow", fp);
		ThresholdToSelection t2s = new ThresholdToSelection();
		t2s.setup("", blowImage);
		t2s.run(fp);
		Roi roi = blowImage.getRoi();
		if (originalRoi != null)
			roi = new ShapeRoi(originalRoi).or(new ShapeRoi(roi));
		imp.setRoi(roi);
		imp.updateAndDraw();
	}

	Difference getDifference(ImageProcessor ip) {
		if (mode == MIN_LASSO) {
			if (ip instanceof ByteProcessor)
				return new ByteMinValue((byte[])ip.getPixels());
			if (ip instanceof ColorProcessor)
				return new ColorMinValue((int[])ip.getPixels());
			return new MinValue(ip);
		}

		if (ip instanceof ByteProcessor)
			return new ByteDifference((byte[])ip.getPixels());
		if (ip instanceof ColorProcessor)
			return new ColorDifference((int[])ip.getPixels());
		return new Difference(ip);
	}

	private class Difference {
		ImageProcessor ip;

		Difference(ImageProcessor ip) {
			this.ip = ip;
		}

		double difference(int x0, int y0, int x1, int y1) {
			return Math.abs(ip.getPixelValue(x0, y0)
				- ip.getPixelValue(x1, y1));
		}
	}

	private class ByteDifference extends Difference {
		byte[] pixels;

		ByteDifference(byte[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			return Math.abs((pixels[x0 + w * y0] & 0xff)
				- (pixels[x1 + w * y1] & 0xff));
		}
	}

	private class ColorDifference extends Difference {
		int[] pixels;

		ColorDifference(int[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			int v0 = pixels[x0 + w * y0];
			int v1 = pixels[x1 + w * y1];
			int r = ((v1 >> 16) & 0xff) - ((v0 >> 16) & 0xff);
			int g = ((v1 >> 8) & 0xff) - ((v0 >> 8) & 0xff);
			int b = (v1 & 0xff) - (v0 & 0xff);
			return Math.abs(r) + Math.abs(g) + Math.abs(b);
		}
	}

	private class MinValue extends Difference {
		ImageProcessor ip;

		MinValue(ImageProcessor ip) {
			super(null);
			this.ip = ip;
		}

		double difference(int x0, int y0, int x1, int y1) {
			return ip.getPixelValue(x1, y1);
		}
	}

	private class ByteMinValue extends Difference {
		byte[] pixels;

		ByteMinValue(byte[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			return pixels[x1 + w * y1] & 0xff;
		}
	}

	private class ColorMinValue extends Difference {
		int[] pixels;

		ColorMinValue(int[] pixels) {
			super(null);
			this.pixels = pixels;
		}

		final double difference(int x0, int y0, int x1, int y1) {
			int v1 = pixels[x1 + w * y1];
			int r = (v1 >> 16) & 0xff;
			int g = (v1 >> 8) & 0xff;
			int b = v1 & 0xff;
			return r + g + b; // TODO: use correct weighting
		}
	}

	private class PixelCost {
		int x, y;
		double cost;

		public PixelCost(int x, int y, double cost) {
			this.x = x;
			this.y = y;
			this.cost = cost;
		}

		public String toString() {
			return "(" + x + ", " + y + ": " + cost + ")";
		}
	}

	final static int[] stepX = { -1, 0, 1, 1, 1, 0, -1, -1 };
	final static int[] stepY = { -1, -1, -1, 0, 1, 1, 1, 0 };
	final static int[] stepW = { 4, 3, 4, 3, 4, 3, 4, 3 };

	FibonacciHeapDouble queue;
	int startX, startY;

	public void initDijkstra(int x, int y, boolean shiftKeyDown) {
		originalRoi = shiftKeyDown ? imp.getRoi() : null;

		ImageProcessor ip = imp.getProcessor();

		difference = getDifference(ip);
		previous = new int[w * h];
		previous[x + w * y] = x + w * y;
		dijkstra = new double[w * h];
		for (int i = 0; i < w * h; i++)
			dijkstra[i] = Double.MAX_VALUE;

		queue = new FibonacciHeapDouble();
		queue.add(0, new PixelCost(x, y, 0));
		startX = x;
		startY = y;
	}

	private void getDijkstra(int x_, int y_) {
		PixelCost pixel;
		while (queue.compareTo(dijkstra[x_ + w * y_]) < 0
				&& (pixel = (PixelCost)queue.pop()) != null) {
			int x = pixel.x;
			int y = pixel.y;
			double cost = pixel.cost;
			if (dijkstra[x + w * y] <= cost)
				continue;

			dijkstra[x + w * y] = cost;
			for (int i = 0; i < stepW.length; i++) {
				int x2 = x + stepX[i];
				int y2 = y + stepY[i];

				if (x2 < 0 || y2 < 0 || x2 >= w || y2 >= h)
					continue;
				double newC = cost + stepW[i] + (ratioSpaceColor
					 * difference.difference(x, y, x2, y2));
				if (dijkstra[x2 + w * y2] > newC) {
					queue.add(newC, new PixelCost(x2,
								y2, newC));
					previous[x2 + w * y2] = x + w * y;
				}
			}
		}
	}
}

