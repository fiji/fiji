import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

import java.awt.geom.GeneralPath;

import java.util.ArrayList;

public class Visual_Grep implements PlugInFilter {
	ImagePlus imp;
	float minDistance;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		int[] ids = WindowManager.getIDList();
		String[] idList = new String[ids.length];
		for (int i = 0; i < ids.length; i++)
			idList[i] = WindowManager.getImage(ids[i]).getTitle();

		int level = 0, w = ip.getWidth();
		while (w > 200) {
			w /= 2;
			level++;
		}

		GenericDialog gd = new GenericDialog("Visual Grep");
		gd.addChoice("needle", idList, idList[0]);
		gd.addNumericField("tolerance", 5000, 0);
		gd.addNumericField("pyramidLevel", level, 0);
		gd.addCheckbox("testDistance", false);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int needleIndex = ids[gd.getNextChoiceIndex()];
		ImagePlus needle = WindowManager.getImage(needleIndex);
		int tolerance = (int)gd.getNextNumber();
		level = (int)gd.getNextNumber();
		boolean testDistance = gd.getNextBoolean();

		if (testDistance) {
			testDistance(ip, needle.getProcessor(), level);
			return;
		}

		minDistance = Float.MAX_VALUE;
		ArrayList points = getPoints(ip, needle.getProcessor(),
				tolerance, level, level, null);
		if (points.size() == 0) {
			IJ.error("No region found! Minimal tolerance needed: "
				+ minDistance);
			return;
		}
		Roi roi = getRoi(points,
				needle.getWidth(), needle.getHeight());
		imp.setRoi(roi);
		imp.updateAndDraw();
	}

	void testDistance(ImageProcessor haystack, ImageProcessor needle,
			int level) {
		if (level > 0) {
			int factor = 1 << level;
			haystack =
				haystack.resize(haystack.getWidth() / factor,
						haystack.getHeight() / factor);
			needle =
				needle.resize(needle.getWidth() / factor,
						needle.getHeight() / factor);
		}
		int[] haystackPixels = (int[])haystack.getPixels();
		int[] needlePixels = (int[])needle.getPixels();
		int haystackW = haystack.getWidth();
		int haystackH = haystack.getHeight();
		int needleW = needle.getWidth();
		int needleH = needle.getHeight();
		int w = haystackW - needleW;
		int h = haystackH - needleH;
		float[] pixels = new float[w * h];
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++)
				pixels[i + w * j] = distance(haystackPixels,
						i, j, haystackW, needlePixels,
						needleW, needleH);
			IJ.showProgress(j + 1, h);
		}
		FloatProcessor fp = new FloatProcessor(w, h, pixels, null);
		new ImagePlus("distance", fp).show();
	}

	ArrayList getPoints(ImageProcessor haystack, ImageProcessor needle,
			int tolerance, int level, int totalLevel,
			ArrayList initial) {
		int w = haystack.getWidth(), h = haystack.getHeight();
		int needleW = needle.getWidth(), needleH = needle.getHeight();
		ArrayList points = new ArrayList();
		int[] pixels = (int[])haystack.getPixels();
		int[] needlePixels = (int[])needle.getPixels();

		if (level > 0) {
			ArrayList scaledInitial = null;
			if (initial != null) {
				scaledInitial = new ArrayList();
				for (int i = 0; i < initial.size(); i++) {
					Point p = (Point)initial.get(i);
					Point p2 = new Point(p.x / 2, p.y / 2,
						p.diff);
					scaledInitial.add(p2);
				}
			}
			ImageProcessor scaledHaystack =
				haystack.resize(w / 2, h / 2);
			ImageProcessor scaledNeedle =
				needle.resize(needleW / 2, needleH / 2);
			initial = getPoints(scaledHaystack, scaledNeedle,
					tolerance, level - 1, totalLevel,
					scaledInitial);

			for (int i = 0; i < initial.size(); i++) {
				Point p = (Point)initial.get(i);
				p.x *= 2;
				p.y *= 2;
				int xo = 2, yo = 2;
				if (p.x + xo + needleW > w)
					xo = 1;
				if (p.y + yo + needleH > h)
					yo = 1;
				getPoints(points, pixels, w,
						needlePixels, needleW, needleH,
						p.x, p.y, p.x + xo, p.y + yo,
						tolerance, false);
			}
		} else
			// exhaustive
			getPoints(points, pixels, w,
					needlePixels, needleW, needleH,
					0, 0, w - needleW, h - needleH,
					tolerance, true);

		IJ.showProgress(level + 1, totalLevel);

		return points;
	}

	// returns a list of matches in the boundingBox (x1,y1,x2,y2)

	void getPoints(ArrayList points, int[] pixels, int row,
			int[] needle, int needleW, int needleH,
			int x1, int y1, int x2, int y2,
			int tolerance, boolean showProgress) {
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				float d = distance(pixels, x, y, row, needle,
						needleW, needleH);
				if (d < tolerance)
					addPoint(points, needleW, needleH,
							new Point(x, y, d));
			}
			if (showProgress)
				IJ.showProgress(y - y1 + 1, y2 - y1);
		}
	}

	void addPoint(ArrayList points, int needleW, int needleH, Point p) {
		/*
		 * If there is an overlapping, worse
		 * match, replace it.
		 */
		for (int i = 0; i < points.size(); i++) {
			Point p2 = (Point)points.get(i);
			if (p2.overlaps(p, needleW, needleH)) {
				if (p2.diff > p.diff)
					p2.replaceWith(p);
				return;
			}
		}
		points.add(p);
	}

	float distance(int[] haystack, int x, int y, int row,
			int[] needle, int needleW, int needleH) {
		long diff = 0;
		for (int j = 0; j < needleH; j++)
			for (int i = 0; i < needleW; i++) {
				int v1 = haystack[x + i + row * (y + j)];
				int v2 = needle[i + needleW * j];
				int r = ((v1 >> 16) & 0xff)
					- ((v2 >> 16) & 0xff);
				int g = ((v1 >> 8) & 0xff)
					- ((v2 >> 8) & 0xff);
				int b = (v1 & 0xff) - (v2 & 0xff);
				diff += r * r + g * g + b * b;
			}
		float result = diff / (float)(needleW * needleH);
		if (minDistance > result)
			minDistance = result;
		return result;
	}

	Roi getRoi(ArrayList points, int w, int h) {
		if (points.size() < 0)
			return null;

		if (points.size() == 1) {
			Point p = (Point)points.get(0);
			return new Roi(p.x, p.y, w, h);
		}

		GeneralPath gp = new GeneralPath();
		for (int i = 0; i < points.size(); i++) {
			Point p = (Point)points.get(i);
			gp.moveTo(p.x, p.y);
			gp.lineTo(p.x + w, p.y);
			gp.lineTo(p.x + w, p.y + h);
			gp.lineTo(p.x, p.y + h);
			gp.closePath();
		}

		return new ShapeRoi(gp);
	}

	private class Point {
		int x, y;
		float diff;
		Point(int x, int y, float diff) {
			this.x = x;
			this.y = y;
			this.diff = diff;
		}

		boolean overlaps(Point other, int w, int h) {
			return x + w > other.x && other.x + w > x &&
				y + h > other.y && other.y + h > y;
		}

		void replaceWith(Point other) {
			x = other.x;
			y = other.y;
			diff = other.diff;
		}
	}
}
