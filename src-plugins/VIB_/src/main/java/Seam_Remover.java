import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

/*

  This plugin implements the algorithm described in

 @inproceedings{1276390,
 author = {Shai Avidan and Ariel Shamir},
 title = {Seam carving for content-aware image resizing},
 booktitle = {SIGGRAPH '07: ACM SIGGRAPH 2007 papers},
 year = {2007},
 pages = {10},
 location = {San Diego, California},
 doi = {http://doi.acm.org/10.1145/1275808.1276390},
 publisher = {ACM Press},
 address = {New York, NY, USA},
 }

*/

public class Seam_Remover implements PlugInFilter {
	protected ImagePlus image;
	protected int w, h;
	protected int[] pixels;
	protected ImageEnergy energy;

	public void run(ImageProcessor ip) {
		String[] labels = {
			"Remove one Horizontal Seam",
			"Remove one Vertical Seam",
			"Mark one Horizontal Seam",
			"Mark one Vertical Seam",
			"Resize by removing Seams"
		};
		GenericDialog gd = new GenericDialog("Seam Remover");
		gd.addChoice("mode", labels, labels[4]);
		gd.addNumericField("width", image.getWidth(), 0);
		gd.addNumericField("height", image.getHeight(), 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int mode = gd.getNextChoiceIndex();
		int width = (int)gd.getNextNumber();
		int height = (int)gd.getNextNumber();

		w = image.getWidth();
		h = image.getHeight();
		pixels = (int[])image.getProcessor().getPixels();
		energy = new ColorDerivativeEnergy(w, h, pixels);

		if (mode == 4) {
			ImageProcessor n = resize(energy, width, height);
			new ImagePlus("resized " + image.getTitle(), n).show();
			return;
		}

		boolean visualize = mode == 2 || mode == 3;
		boolean vertical = mode == 1 || mode == 3;

		int[] seam = vertical ?
			energy.getVerticalSeam() : energy.getHorizontalSeam();

		if (visualize) {
			image.setRoi(energy.seam2roi(seam, vertical));
			image.updateAndDraw();
		} else {
			ImageProcessor n = energy.removeSeam(seam,
					vertical).getProcessor();
			new ImagePlus("removed seam", n).show();
		}
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_RGB;
	}

	public ImageProcessor resize(ImageEnergy energy, int w, int h) {
		if (w > energy.w || h > energy.h)
			throw new RuntimeException("Cannot enlarge image");
		int total = energy.w - w + energy.h - h;
		int count = 0;
		while (w < energy.w || h < energy.h) {
			if (energy.w - w < energy.h - h) {
				int[] seam = energy.getHorizontalSeam();
				energy = energy.removeSeam(seam, false);
			} else {
				int[] seam = energy.getVerticalSeam();
				energy = energy.removeSeam(seam, true);
			}
			IJ.showProgress(++count, total);
		}
		return energy.getProcessor();
	}

	private abstract static class ImageEnergy {
		int w, h;

		public ImageEnergy(int w, int h) {
			this.w = w;
			this.h = h;
		}

		public abstract float get(int x, int y);

		public int[] getVerticalSeam() {
if (false) {
float[] f = new float[w * h];
for (int y = 0; y < h; y++)
	for (int x = 0; x < w; x++)
		f[x + y * w] = get(x, y);
new ImagePlus("energy", new ij.process.FloatProcessor(w, h, f, null)).show();
}
			int[] seams = new int[w * h];
			float[] currentLine, previousLine;
			currentLine = new float[w];
			previousLine = new float[w];

			for (int x = 0; x < w; x++) {
				seams[x] = x;
				previousLine[x] = (float)Math.abs(get(x, 0));
			}

			for (int y = 1; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int offset = x + y * w;
					currentLine[x] = previousLine[x];
					seams[offset] = x;
					if (x > 0 && currentLine[x] >
							previousLine[x - 1]) {
						currentLine[x] = previousLine[x - 1];
						seams[offset] = x - 1;
					}
					if (x < w - 1 && currentLine[x] >
							previousLine[x + 1]) {
						currentLine[x] = previousLine[x + 1];
						seams[offset] = x + 1;
					}
					currentLine[x] +=
						(float)Math.abs(get(x, y));
				}

				float[] dummy = previousLine;
				previousLine = currentLine;
				currentLine = dummy;
			}

			int best = 0;
			for (int x = 1; x < w; x++)
				if (previousLine[x] < previousLine[best])
					best = x;

			int[] result = new int[h];
			int x = best;
			for (int y = h - 1; y >= 0; y--) {
				result[y] = x;
				x = seams[x + y * w];
			}
			return result;
		}

		public int[] getHorizontalSeam() {
			return new FlippedImageEnergy(this).getVerticalSeam();
		}

		public abstract ImageProcessor getProcessor();

		public abstract ImageEnergy
			removeSeam(int[] seam, boolean vertical);

		public ImageProcessor removeVerticalSeam() {
			return removeSeam(getVerticalSeam(),
					true).getProcessor();
		}

		public ImageProcessor removeHorizontalSeam() {
			return removeSeam(getHorizontalSeam(),
					false).getProcessor();
		}

		public Roi seam2roi(int[] seam, boolean vertical) {
			int[] other = new int[vertical ? h : w];
			for (int i = 0; i < other.length; i++)
				other[i] = i;
			return new PolygonRoi(vertical ? seam : other,
				vertical ? other : seam,
				seam.length, PolygonRoi.FREELINE);
		}
	}

	private static class FlippedImageEnergy extends ImageEnergy {
		private ImageEnergy orig;

		public FlippedImageEnergy(ImageEnergy orig) {
			super(orig.h, orig.w);
			this.orig = orig;
		}

		public float get(int x, int y) {
			return orig.get(y, x);
		}

		public ImageEnergy removeSeam(int[] seam, boolean vert) {
			return orig.removeSeam(seam, !vert);
		}

		public ImageProcessor getProcessor() {
			throw new RuntimeException("Cannot flip arbitrary "
				+ "ImageProcessor");
		}
	}

	private class ColorDerivativeEnergy extends ImageEnergy {
		int[] pixels;

		public ColorDerivativeEnergy(int w, int h, int[] pixels) {
			super(w, h);
			this.pixels = pixels;
		}

		int getDiff(int x1, int y1, int x2, int y2) {
			int v1 = pixels[x1 + y1 * w];
			int v2 = pixels[x2 + y2 * w];
			int r = ((v1 >> 16) & 0xff) - ((v2 >> 16) & 0xff);
			int g = ((v1 >> 8) & 0xff) - ((v2 >> 8) & 0xff);
			int b = (v1 & 0xff) - (v2 & 0xff);
			return r + g + b;
		}

		public float get(int x, int y) {
			return (x == 0 ? 2 * getDiff(x, y, x + 1, y) :
				(x == w - 1 ? 2 * getDiff(x - 1, y, x, y) :
				 getDiff(x - 1, y, x + 1, y))) +
				(y == 0 ? 2 * getDiff(x, y, x, y + 1) :
				 (y == h - 1 ? 2 * getDiff(x, y - 1, x, y) :
				  getDiff(x, y - 1, x, y + 1)));
		}

		public ImageEnergy removeSeam(int[] seam, boolean vert) {
			if (vert) {
				int[] p = new int[(w - 1) * h];
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < seam[y]; x++)
						p[x + y * (w - 1)] =
							pixels[x + y * w];
					for (int x = seam[y] + 1; x < w; x++)
						p[x - 1 + y * (w - 1)] =
							pixels[x + y * w];
				}
				return new ColorDerivativeEnergy(w - 1, h, p);
			} else {
				int[] p = new int[w * (h - 1)];
				for (int x = 0; x < w; x++) {
					for (int y = 0; y < seam[x]; y++)
						p[x + y * w] =
							pixels[x + y * w];
					for (int y = seam[x] + 1; y < h; y++)
						p[x + (y - 1) * w] =
							pixels[x + y * w];
				}
				return new ColorDerivativeEnergy(w, h - 1, p);
			}
		}

		public ImageProcessor getProcessor() {
			return new ColorProcessor(w, h, pixels);
		}
	}
}
