package video2;

import java.awt.Polygon;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.Toolbar;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.PolygonRoi;
import ij.gui.GenericDialog;

import ij.process.ImageProcessor;

import ij.plugin.filter.PlugInFilter;

public class Draw_Roi implements PlugInFilter {

	private ImagePlus image;

	public static final int DEF_PIX_PER_SLICE = 3;

	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		int current = image.getCurrentSlice();

		GenericDialog gd = new GenericDialog("Draw Roi");
		gd.addNumericField("Slice", current, 0);
		gd.addNumericField("Pixels per slice", DEF_PIX_PER_SLICE, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		int slice = (int)gd.getNextNumber();
		int speed = (int)gd.getNextNumber();

		drawRois(image, image.getRoi(), slice, speed);
	}

	public static void drawRois(ImagePlus image,
					Roi roi, int slice, int speed) {
		if(roi == null) {
			IJ.error("Selection required");
			return;
		}
		Roi[] rois = null;
		if(roi.getType() == Roi.COMPOSITE)
			rois = ((ShapeRoi)roi).getRois();
		else
			rois = new Roi[] {roi};

		int c = 0;
		for(int i = 0; i < rois.length; i++)
			c += drawRoi(image, rois[i], slice, speed);
	}

	public static int drawRoi(ImagePlus image,
				Roi roi, int slice, int speed) {

		if(roi == null)
			return 0;
		Polygon p = null;
		switch(roi.getType()) {
			case Roi.LINE:
				Line l = (Line)roi;
				p = new Polygon();
				p.addPoint(l.x1, l.y1);
				p.addPoint(l.x2, l.y2);
				break;
			case Roi.RECTANGLE:
			case Roi.OVAL:
			case Roi.POLYGON:
			case Roi.FREEROI:
			case Roi.TRACED_ROI:
				p = roi.getPolygon();
				p.addPoint(p.xpoints[0], p.ypoints[0]);
				break;
			case Roi.POLYLINE:
			case Roi.FREELINE:
			case Roi.ANGLE:
				p = roi.getPolygon();
				break;
			default: throw new IllegalArgumentException(
					"Roi type not supported");
		}

		return drawRoi(image, p, slice, speed);
	}

	public static int drawRoi(ImagePlus image,
				Polygon p, int slice, int speed) {

		ImageStack stack = image.getStack();
		int linewidth = Line.getWidth();
		int color = Toolbar.getForegroundColor().getRGB();

		int n = p.npoints;
		int[] x = p.xpoints;
		int[] y = p.ypoints;

		int x_last = x[0], y_last = y[0];

		ImageProcessor ip = stack.getProcessor(slice);

		LineIterator li = new LineIterator();
		int c = -1;
		int slicesInserted = 0;
		for(int z = 0; z < n - 1; z++) {
			li.init(x[z], y[z], x[z+1], y[z+1]);
			while(li.next() != null) {
				c++;
				ip.setValue(color);
				ip.setLineWidth(linewidth);
				ip.moveTo(x_last, y_last);
				x_last = (int)li.x;
				y_last = (int)li.y;
				ip.lineTo(x_last, y_last);
				if(speed < 1 || c % speed != 0)
					continue;
				stack.addSlice("", ip, slice + slicesInserted);
				slicesInserted++;
			}
		}
		// maybe the last one was not added (in case c % speed was 0)
		if(speed < 1 || c % speed != 0) {
			stack.addSlice("", ip, slice + slicesInserted);
			slicesInserted++;
		}
		return slicesInserted;
	}

	private static class LineIterator {

		int x1, y1;
		int x2, y2;
		int dx, dy;
		boolean finished;
		double x, y, dx_dt, dy_dt;

		public LineIterator() {}

		public LineIterator(int x1, int y1, int x2, int y2) {
			init(x1, y1, x2, y2);
		}

		public void init(int x1, int y1, int x2, int y2) {
			this.x1 = x1; this.x2 = x2;
			this.y1 = y1; this.y2 = y2;
			this.x = x1;
			this.y = y1;

			dx = x2 - x1;
			dy = y2 - y1;

			int dt = Math.abs(dx) > Math.abs(dy) ? dx : dy;
			dt = Math.abs(dt);
			if(dt == 0)
				dt = 1;

			dx_dt = (double)dx/dt;
			dy_dt = (double)dy/dt;

			dx = Math.abs(dx);
			dy = Math.abs(dy);

			finished = false;
		}

		public LineIterator next() {
			if(finished)
				return null;
			x += dx_dt;
			y += dy_dt;
			finished = Math.abs((int)x - x1) >= dx &&
				Math.abs((int)y - y1) >= dy;
			if(finished) {
				x = x2;
				y = y2;
			}
			return this;
		}
	}
}
