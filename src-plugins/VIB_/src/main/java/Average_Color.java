import color.CIELAB;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

import java.awt.Rectangle;
import java.util.ArrayList;

public class Average_Color implements PlugInFilter {
	protected ImagePlus image;
	protected int w, h;
	protected int[] pixels;

	public void test() {
		GenericDialog gd = new GenericDialog("L*");
		gd.addNumericField("L*", 75, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		float l = (float)gd.getNextNumber();

		float[] lab = new float[3];
		float[] rgb = new float[3];
		int[] rgbi = new int[3];

		w = h = 128 * 2 + 1;
		pixels = new int[w * h];
		for (int a = -128; a <= 128; a++)
			for (int b = -128; b <=  128; b++) {
				lab[0] = l;
				lab[1] = a;
				lab[2] = b;
				CIELAB.CIELAB2sRGB(lab, rgb);
				rgbi[0] = CIELAB.unnorm(rgb[0]);
				rgbi[1] = CIELAB.unnorm(rgb[1]);
				rgbi[2] = CIELAB.unnorm(rgb[2]);
				pixels[a + 128 + (b + 128) * w] =
					(rgbi[0] << 16) | (rgbi[1] << 8) |
					rgbi[2];
			}

		ColorProcessor p = new ColorProcessor(w, h, pixels);
		new ImagePlus("Colors", p).show();
	}

	public void run(ImageProcessor ip) {
		Roi roi = image.getRoi();
		if (roi == null) {
			IJ.error("Need a ROI");
			return;
		}
		boolean haveShapeRoi = (roi instanceof ShapeRoi);

		GenericDialog gd = new GenericDialog("Average Color");
		gd.addCheckbox("CIELab averaging", true);
		if (haveShapeRoi)
			gd.addCheckbox("Split roi", true);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		boolean cielab = gd.getNextBoolean();
		boolean splitRoi = haveShapeRoi ? gd.getNextBoolean() : false;

		w = image.getWidth();
		h = image.getHeight();
		pixels = (int[])image.getProcessor().getPixels();

		if (splitRoi) {
			ShapeRoi shape = (ShapeRoi)roi;
			Roi[] rois = shape.getRois();
			for (int i = 0; i < rois.length; i++)
				averageColorInRoi(rois[i], cielab);
		} else
			averageColorInRoi(roi, cielab);
		image.updateAndDraw();
	}

	final float[] getAverageColor(Roi roi, boolean cielab) {
		Rectangle r = roi.getBounds();
		float[] rgb = new float[3];
		float[] lab = new float[3];
		float[] cumul = new float[3];
		int count = 0;

		/* first get the cumulated values in the given color space */
		for (int y = r.y; y < r.y + r.height; y++)
			for (int x = r.x; x < r.x + r.width; x++) {
				if (!roi.contains(x, y))
					continue;
				int v = pixels[x + w * y];
				rgb[0] = CIELAB.norm((v >> 16) & 0xff);
				rgb[1] = CIELAB.norm((v >> 8) & 0xff);
				rgb[2] = CIELAB.norm(v & 0xff);
				if (cielab) {
					CIELAB.sRGB2CIELAB(rgb, lab);
					cumul[0] += lab[0];
					cumul[1] += lab[1];
					cumul[2] += lab[2];
				} else {
					cumul[0] += rgb[0];
					cumul[1] += rgb[1];
					cumul[2] += rgb[2];
				}
				count++;
			}

		/* then make the average... */
		cumul[0] /= count;
		cumul[1] /= count;
		cumul[2] /= count;

		return cumul;
	}

	private void averageColorInRoi(Roi roi, boolean cielab) {
		Rectangle r = roi.getBounds();
		float[] cumul = getAverageColor(roi, cielab);
		float[] rgb = new float[3];
		int[] rgbi = new int[3];

		if (cielab) {
			CIELAB.CIELAB2sRGB(cumul, rgb);
			rgbi[0] = CIELAB.unnorm(rgb[0]);
			rgbi[1] = CIELAB.unnorm(rgb[1]);
			rgbi[2] = CIELAB.unnorm(rgb[2]);
		} else {
			rgbi[0] = CIELAB.unnorm(cumul[0]);
			rgbi[1] = CIELAB.unnorm(cumul[1]);
			rgbi[2] = CIELAB.unnorm(cumul[2]);
		}

		int v = (rgbi[0] << 16) | (rgbi[1] << 8) | rgbi[2];

		/* and now substitute that */
		for (int y = r.y; y < r.y + r.height; y++)
			for (int x = r.x; x < r.x + r.width; x++)
				if (roi.contains(x, y))
					pixels[x + w * y] = v;
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}

	public static String getAverageCIELAB() {
		Average_Color t = new Average_Color();
		try {
			t.image = WindowManager.getCurrentImage();
			t.pixels = (int[])t.image.getProcessor().getPixels();
			t.w = t.image.getWidth();
			t.h = t.image.getHeight();
			Roi roi = t.image.getRoi();
			if (roi == null)
				roi = new Roi(0, 0, t.w, t.h);
			float[] result = t.getAverageColor(roi, true);
			return "" + result[0] + " " + result[1] + " " + result[2];
		} catch (Exception e) {
			return "";
		}

	}

	public static String setColorCIELAB(String L, String a, String b) {
		try {
			float[] lab = new float[3];
			lab[0] = Float.parseFloat(L);
			lab[1] = Float.parseFloat(a);
			lab[2] = Float.parseFloat(b);
			float[] rgb = new float[3];
			CIELAB.CIELAB2sRGB(lab, rgb);
			int red = CIELAB.unnorm(rgb[0]);
			int green = CIELAB.unnorm(rgb[1]);
			int blue = CIELAB.unnorm(rgb[2]);
			IJ.setForegroundColor(red, green, blue);
			return "" + red + " " + green + " " + blue;
		} catch (Exception e) {
			return "";
		}
	}
}
