package fiji.statistics;

import ij.gui.Roi;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;

public class RoiStatistics {
	protected int count;
	protected float cumulative;
	protected final Accumulator accumulator;

	public RoiStatistics(Roi roi) {
		Rectangle bounds = roi.getBounds();
		ImageProcessor mask = roi.getMask();
		if (mask == null)
			accumulator = new RectangleAccumulator(bounds);
		else
			accumulator = new GenericAccumulator(bounds.x, bounds.y, (ByteProcessor)mask);
	}

	public void init(ImageProcessor ip) {
		Accessor accessor = null;
		if (ip instanceof ByteProcessor)
			accessor = new ByteAccessor((ByteProcessor)ip);
		else if (ip instanceof ShortProcessor)
			accessor = new ShortAccessor((ShortProcessor)ip);
		else if (ip instanceof FloatProcessor)
			accessor = new FloatAccessor((FloatProcessor)ip);
		else if (ip instanceof ColorProcessor)
			accessor = new RGBAccessor((ColorProcessor)ip);

		accumulator.accumulate(accessor);
	}

	protected interface Accumulator {
		void accumulate(Accessor accessor);
	}

	protected class RectangleAccumulator implements Accumulator {
		protected int x0, y0, x1, y1;

		public RectangleAccumulator(Rectangle rect) {
			x0 = rect.x;
			y0 = rect.y;
			x1 = x0 + rect.width;
			y1 = y0 + rect.height;
			count = (x1 - x0) * (y1 - y0);
		}

		public final void accumulate(final Accessor accessor) {
			cumulative = 0;
			for (int y = y0; y < y1; y++)
				for (int x = x0; x < x1; x++)
					cumulative += accessor.getf(x, y);
		}
	}

	protected class GenericAccumulator implements Accumulator {
		protected int x, y, width, height;
		protected byte[] pixels;

		public GenericAccumulator(int x, int y, ByteProcessor mask) {
			this.x = x;
			this.y = y;
			width = mask.getWidth();
			height = mask.getHeight();

			pixels = (byte[])mask.getPixels();
		}

		public final void accumulate(final Accessor accessor) {
			cumulative = 0;
			count = 0;
			int width = Math.min(this.width, accessor.getWidth() - x);
			int height = Math.min(this.height, accessor.getHeight() - y);
			for (int j = Math.max(0, -y); j < height; j++)
				for (int i = Math.max(0, -x); i < width; i++)
					if (pixels[i + width * j] != 0) {
						cumulative += accessor.getf(x + i, y + j);
						count++;
					}
		}
	}

	protected interface Accessor {
		int getWidth();
		int getHeight();
		float getf(int x, int y);
	}

	protected static class ByteAccessor implements Accessor {
		protected int w, h;
		protected byte[] pixels;

		public ByteAccessor(ByteProcessor ip) {
			w = ip.getWidth();
			h = ip.getHeight();
			pixels = (byte[])ip.getPixels();
		}

		public final int getWidth() {
			return w;
		}

		public final int getHeight() {
			return h;
		}

		public final float getf(int x, int y) {
			return pixels[x + w * y] & 0xff;
		}
	}

	protected static class ShortAccessor implements Accessor {
		protected int w, h;
		protected short[] pixels;

		public ShortAccessor(ShortProcessor ip) {
			w = ip.getWidth();
			h = ip.getHeight();
			pixels = (short[])ip.getPixels();
		}

		public final int getWidth() {
			return w;
		}

		public final int getHeight() {
			return h;
		}

		public final float getf(int x, int y) {
			return pixels[x + w * y] & 0xffff;
		}
	}

	protected static class FloatAccessor implements Accessor {
		protected int w, h;
		protected float[] pixels;

		public FloatAccessor(FloatProcessor ip) {
			w = ip.getWidth();
			h = ip.getHeight();
			pixels = (float[])ip.getPixels();
		}

		public final int getWidth() {
			return w;
		}

		public final int getHeight() {
			return h;
		}

		public final float getf(int x, int y) {
			return pixels[x + w * y];
		}
	}

	/* Legacy, therefore not tuned for performance */
	protected static class RGBAccessor implements Accessor {
		protected ColorProcessor ip;

		public RGBAccessor(ColorProcessor ip) {
			this.ip = ip;
		}

		public final int getWidth() {
			return ip.getWidth();
		}

		public final int getHeight() {
			return ip.getHeight();
		}

		public final float getf(int x, int y) {
			return ip.getf(x, y);
		}
	}

	public int getCount() {
		return count;
	}

	public float getCumulative() {
		return cumulative;
	}

	public float getAverage() {
		return count == 0 ? 0 : cumulative / count;
	}

	public float getAverage(ImageProcessor ip) {
		init(ip);
		return getAverage();
	}
}