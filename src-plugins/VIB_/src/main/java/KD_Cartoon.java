import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.plugin.filter.PlugInFilter;

public class KD_Cartoon implements PlugInFilter {

	private ImagePlus image;

	public int setup(String arg, ImagePlus img){
		this.image = img;
		return DOES_ALL | NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Despeckle");
		gd.addNumericField("ratio color/space",
				256.0 / (ip.getWidth() * ip.getHeight()), 3);
		gd.addNumericField("number of classes", 20, 0);
		gd.addNumericField("iterations (at most)", 50, 0);
		gd.showDialog();
		if(gd.wasCanceled())
			return;

		double ratio = gd.getNextNumber();
		int n = (int)gd.getNextNumber();
		int iter = (int)gd.getNextNumber();

		KD kd = new KD(ip, ratio, n, iter);
		ImageProcessor result = kd.getResult();
		new ImagePlus("KD Cartoon of " + image.getTitle(), result)
			.show();
	}

	private static class Difference {
		ImageProcessor ip;
		double ratio;
		int count;
		double x2, y2;

		public Difference(ImageProcessor ip,
				double space_color_ratio) {
			this.ip = ip;
			ratio = space_color_ratio;
		}

		public void init() {
			x2 = y2 = 0;
			count = 0;
		}

		public void add(int x, int y) {
			x2 += x;
			y2 += y;
			count++;
		}

		public void finish() {
			x2 /= count;
			y2 /= count;
		}

		public double getDiff(int x, int y) {
			double x1 = x - x2;
			double y1 = y - y2;

			return ratio * (x1 * x1 + y1 * y1);
		}
	}

	private static class ColorDifference extends Difference {
		public ColorDifference(ImageProcessor ip,
				double space_color_ratio) {
			super(ip, space_color_ratio);
		}

		double r, g, b;
		private void decompose(int c) {
			r = (c >> 16) & 0xff;
			g = (c >> 8) & 0xff;
			b = c & 0xff;
		}

		double r2, g2, b2;

		public void init() {
			r2 = g2 = b2 = 0;
			super.init();
		}

		public void add(int x, int y) {
			int c = ip.get(x, y);
			decompose(c);
			r2 += r;
			g2 += g;
			b2 += b;
			super.add(x, y);
		}

		public void finish() {
			if (count < 1)
				return;
			r2 /= count;
			g2 /= count;
			b2 /= count;
			super.finish();
		}

		public double getDiff(int x, int y) {
			int c = ip.get(x, y);
			decompose(c);

			r -= r2;
			g -= g2;
			b -= b2;
			double x1 = x - x2;
			double y1 = y - y2;

			return super.getDiff(x, y) +
				r * r + g * g + b * b;
		}

		public int getMean() {
			return ((int)r2) << 16 | ((int)g2) << 8 | ((int) b2);
		}
	}

	private static class GrayDifference extends Difference {
		public GrayDifference(ImageProcessor ip,
				double space_color_ratio) {
			super(ip, space_color_ratio);
		}

		float mean;

		public void init() {
			mean = 0;
			super.init();
		}

		public void add(int x, int y) {
			mean += ip.getf(x, y);
			super.add(x, y);
		}

		public void finish() {
			if (count < 1)
				return;
			mean /= count;
			super.finish();
		}

		public double getDiff(int x, int y) {
			float c = mean - ip.getf(x, y);

			return super.getDiff(x, y) + c * c;
		}

		public float getMean() { return (float)mean; }
	}

	private static class KD {
		ImageProcessor ip;
		boolean isColor;
		double ratio;
		int w, h, n, iterations;
		int[] c;
		Difference[] diff;

		public KD(ImageProcessor ip, double ratio, int n, int iter) {
			this.ip = ip;
			isColor = ip.isColorLut() ||
				(ip instanceof ColorProcessor);
			this.ratio = ratio;
			w = ip.getWidth();
			h = ip.getHeight();
			c = new int[w * h];
			for (int i = 0; i < w * h; i++)
				c[i] = (int)(Math.random() * n);
			this.n = n;
			iterations = iter;
		}

		public int iterate() {
			diff = new Difference[n];

			for (int i = 0; i < n; i++) {
				diff[i] = isColor ?
					(Difference)new ColorDifference(ip,
						ratio) :
					new GrayDifference(ip, ratio);
				diff[i].init();
			}

			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++)
					diff[c[x + w * y]].add(x, y);

			for (int i = 0; i < n; i++)
				diff[i].finish();

			int count = 0;
			for (int y = 0; y < h; y++)
				for (int x = 0; x < w; x++) {
					int best = 0;
					double d = diff[0].getDiff(x, y);
					for (int i = 1; i < n; i++) {
						double d2 =
							diff[i].getDiff(x, y);
						if (d > d2) {
							best = i;
							d = d2;
						}
					}
					if (c[x + w * y] != best) {
						count++;
						c[x + w * y] = best;
					}
				}

			return count;
		}

		public ImageProcessor getResult() {
			for (int i = 0; i < iterations; i++) {
				int count = iterate();
				IJ.showStatus("adjusted pixels: " + count +
						" (" + (i + 1) + "/" +
						iterations + ")");
				if (count == 0)
					break;
			}
			return getImage();
		}

		private ImageProcessor getImage() {
			if (isColor) {
				int[] colors = new int[n];
				for (int i = 0; i < n; i++)
					colors[i] = ((ColorDifference)diff[i])
						.getMean();
				int[] pixels = new int[w * h];
				for (int i = 0; i < w * h; i++)
					pixels[i] = colors[c[i]];
				return new ColorProcessor(w, h, pixels);
			} else {
				float[] colors = new float[n];
				for (int i = 0; i < n; i++)
					colors[i] = ((GrayDifference)diff[i])
						.getMean();
				float[] pixels = new float[w * h];
				for (int i = 0; i < w * h; i++)
					pixels[i] = colors[c[i]];
				return new FloatProcessor(w, h, pixels, null);
			}
		}
	}
}
