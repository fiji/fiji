import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;

import ij.plugin.PlugIn;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Iterator;

/** Select two images with a Line ROI in each, and rotate/translate/scale one
 * to the other.
 * Stacks are not explicitly supported, but a macro can easily use this plugin
 * for the purpose by iterating over all slices.
 */
public class Align_Image implements PlugIn {

	private boolean isSupported(int type) {
		switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32:
				return true;
		}
		return false;
	}

	public void run(String arg) {

		// Find all images that have a LineRoi in them
		int[] ids = WindowManager.getIDList();
		if (null == ids) return; // no images open
		ArrayList all = new ArrayList();
		for (int i=0; i<ids.length; i++) {
			ImagePlus imp = WindowManager.getImage(ids[i]);
			Roi roi = imp.getRoi();
			int type = imp.getType();
			if (null != roi && roi instanceof Line && isSupported(imp.getType()))
				all.add(imp);
		}
		if (all.size() < 2) {
			IJ.showMessage("Need 2 images with a line roi in each.\n" +
				       "Images must be 8, 16 or 32-bit.");
			return;
		}

		// create choice arrays
		String[] titles = new String[all.size()];
		int k=0;
		for (Iterator it = all.iterator(); it.hasNext(); )
			titles[k++] = ((ImagePlus)it.next()).getTitle();

		GenericDialog gd = new GenericDialog("Align Images");
		String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice("source", titles, current.equals(titles[0]) ?
				titles[1] : titles[0]);
		gd.addChoice("target", titles, current);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus source = WindowManager.getImage(ids[gd.getNextChoiceIndex()]);
		Line line1 = (Line)source.getRoi();

		ImagePlus target = WindowManager.getImage(ids[gd.getNextChoiceIndex()]);
		Line line2 = (Line)target.getRoi();

		ImageProcessor result = align(source.getProcessor(), line1, target.getProcessor(), line2);
		ImagePlus imp = new ImagePlus(source.getTitle() + " aligned to " + target.getTitle(), result);
		imp.setCalibration(source.getCalibration());
		imp.setRoi(line2);
		imp.show();
	}

	/**
	 * Align an image to another image given line selections in each.
	 */
	public static ImageProcessor align(ImageProcessor source, Line line1, ImageProcessor target, Line line2) {
		int w = target.getWidth(), h = target.getHeight();
		ImageProcessor result = new FloatProcessor(w, h);
		float[] pixels = (float[])result.getPixels();

		Interpolator inter = new BilinearInterpolator(source);

		/* the linear mapping to map line1 onto line2 */
		float a00, a01, a02, a10, a11, a12;
		float dx1 = line1.x2 - line1.x1;
		float dy1 = line1.y2 - line1.y1;
		float dx2 = line2.x2 - line2.x1;
		float dy2 = line2.y2 - line2.y1;
		float det = dx2 * dx2 + dy2 * dy2;
		a00 = (dx2 * dx1 + dy2 * dy1) / det;
		a10 = (dx2 * dy1 - dy2 * dx1) / det;
		a01 = -a10;
		a11 = a00;
		a02 = line1.x1 - a00 * line2.x1 - a01 * line2.y1;
		a12 = line1.y1 - a10 * line2.x1 - a11 * line2.y1;

		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				float x = i * a00 + j * a01 + a02;
				float y = i * a10 + j * a11 + a12;
				pixels[i + j * w] = inter.get(x, y);
			}
			IJ.showProgress(j + 1, h);
		}

		result.setMinAndMax(source.getMin(), source.getMax());
		return result;
	}

	protected static abstract class Interpolator {
		ImageProcessor ip;
		int w, h;

		public Interpolator(ImageProcessor ip) {
			this.ip = ip;
			w = ip.getWidth();
			h = ip.getHeight();
		}

		public abstract float get(float x, float y);
	}

	protected static class BilinearInterpolator extends Interpolator {
		public BilinearInterpolator(ImageProcessor ip) {
			super(ip);
		}

		public float get(float x, float y) {
			int i = (int)x;
			int j = (int)y;
			float fx = x - i;
			float fy = y - j;
			float v00 = ip.getPixelValue(i, j);
			float v01 = ip.getPixelValue(i + 1, j);
			float v10 = ip.getPixelValue(i, j + 1);
			float v11 = ip.getPixelValue(i + 1, j + 1);
			return (1 - fx) * (1 - fy) * v00 + fx * (1 - fy) * v01
				+ (1 - fx) * fy * v10 + fx * fy * v11;
		}
	}
}