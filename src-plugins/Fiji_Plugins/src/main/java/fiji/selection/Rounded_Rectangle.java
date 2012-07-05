package fiji.selection;

import ij.IJ;
import ij.ImagePlus;

import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.plugin.filter.PlugInFilter;

import ij.process.ImageProcessor;

import java.awt.Rectangle;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

public class Rounded_Rectangle implements PlugInFilter {
	ImagePlus image;
	protected GeneralPath gp;
	protected int x, y;
	protected static float kappa = (float)(4 * (Math.sqrt(2) - 1) / 3);

	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (roi == null || roi.getType() != roi.RECTANGLE) {
			IJ.error("Need a rectangular selection!");
			return;
		}

		Rectangle rect = roi.getBounds();
		int min = Math.min(rect.width, rect.height);
		GenericDialog gd = new GenericDialog("Make rectangle rounded");
		gd.addSlider("radius", 0, min / 2, Math.min(5, min / 2));
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int radius = (int)gd.getNextNumber();
		gp = new GeneralPath();
		start(rect.x + radius, rect.y);
		quarterCircle(radius, -90);
		straight(0, rect.height - 2 * radius);
		quarterCircle(radius, -180);
		straight(rect.width - 2 * radius, 0);
		quarterCircle(radius, -270);
		straight(0, 2 * radius - rect.height);
		quarterCircle(radius, 0);
		gp.closePath();
		image.setRoi(new ShapeRoi(gp));
	}

	protected void start(int x, int y) {
		this.x = x; this.y = y;
		gp.moveTo(this.x, this.y);
	}

	protected void straight(int x, int y) {
		this.x += x; this.y += y;
		gp.lineTo(this.x, this.y);
	}

	protected void quarterCircle(int radius, int startAngle) {
		float dx = (float)Math.cos(Math.PI * startAngle / 180) * radius;
		float dy = (float)Math.sin(Math.PI * startAngle / 180) * radius;
		float x1 = x + (1 - kappa) * dy;
		float y1 = y + (1 - kappa) * -dx;
		x += dy - dx; y += -dx - dy;
		float x2 = x + (1 - kappa) * dx;
		float y2 = y + (1 - kappa) * dy;
		gp.curveTo(x1, y1, x2, y2, x, y);
	}

	void debug(GeneralPath gp) {
		PathIterator iter = gp.getPathIterator(new AffineTransform());
		double[] coords = new double[6];
		while (!iter.isDone()) {
			int type = iter.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_CLOSE:
				System.err.println("close");
				break;
			case PathIterator.SEG_CUBICTO:
				System.err.println("cubic ("
					+ coords[0] + ", " + coords[1] + ") ("
					+ coords[2] + ", " + coords[3] + ") ("
					+ coords[4] + ", " + coords[5] + ")");
				break;
			case PathIterator.SEG_QUADTO:
				System.err.println("quad ("
					+ coords[0] + ", " + coords[1] + ") ("
					+ coords[2] + ", " + coords[3] + ")");
				break;
			case PathIterator.SEG_LINETO:
			case PathIterator.SEG_MOVETO:
				System.err.println((type == iter.SEG_LINETO ?
							"line" : "move")
					+ "to (" + coords[0] + ", "
					+ coords[1] + ")");
				break;
			default:
				System.err.println("unknown type: " + type);
			}
			iter.next();
		}
	}
}
