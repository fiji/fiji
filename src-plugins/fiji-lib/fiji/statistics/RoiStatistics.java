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
	protected float cumulative, cumulativeX, cumulativeY, min, max;
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
		accumulate(getAccessor(ip));
	}

	protected Accessor getAccessor(ImageProcessor ip) {
		if (ip instanceof ByteProcessor)
			return new ByteAccessor((ByteProcessor)ip);
		else if (ip instanceof ShortProcessor)
			return new ShortAccessor((ShortProcessor)ip);
		else if (ip instanceof FloatProcessor)
			return new FloatAccessor((FloatProcessor)ip);
		else if (ip instanceof ColorProcessor)
			return new RGBAccessor((ColorProcessor)ip);
		throw new RuntimeException("No accessor available for " + ip);
	}

	public interface PixelHandler {
		void handle(int x, int y, float value);
	}

	public void iterate(final ImageProcessor ip, final PixelHandler handler) {
		accumulator.iterate(getAccessor(ip), handler);
	}

	protected void accumulate(final Accessor accessor) {
		count = 0;
		cumulative = cumulativeX = cumulativeY = 0;
		min = Float.MAX_VALUE;
		max = -Float.MAX_VALUE;
		accumulator.iterate(accessor, new PixelHandler() {
			public final void handle(int x, int y, float value) {
				cumulative += value;
				cumulativeX += x * value;
				cumulativeY += y * value;
				count++;
				if (min > value)
					min = value;
				if (max < value)
					max = value;
			}
		});
	}

	protected interface Accumulator {
		void iterate(Accessor accessor, PixelHandler handler);
	}

	protected class RectangleAccumulator implements Accumulator {
		protected int x0, y0, x1, y1;

		public RectangleAccumulator(Rectangle rect) {
			x0 = rect.x;
			y0 = rect.y;
			x1 = x0 + rect.width;
			y1 = y0 + rect.height;
		}

		@Override
		public final void iterate(final Accessor accessor, final PixelHandler handler) {
			for (int y = y0; y < y1; y++)
				for (int x = x0; x < x1; x++)
					handler.handle(x, y, accessor.getf(x, y));
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

		@Override
		public final void iterate(final Accessor accessor, final PixelHandler handler) {
			int width = Math.min(this.width, accessor.getWidth() - x);
			int height = Math.min(this.height, accessor.getHeight() - y);
			for (int j = Math.max(0, -y); j < height; j++)
				for (int i = Math.max(0, -x); i < width; i++)
					if (pixels[i + width * j] != 0)
						handler.handle(x + i, y + i, accessor.getf(x + i, y + j));
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

	public float getCentroidX() {
		return cumulative == 0 ? 0 : cumulativeX / cumulative;
	}

	public float getCentroidY() {
		return cumulative == 0 ? 0 : cumulativeY / cumulative;
	}

	public float getAverage(ImageProcessor ip) {
		init(ip);
		return getAverage();
	}

	public float getMin() {
		return min;
	}

	public float getMax() {
		return max;
	}
}