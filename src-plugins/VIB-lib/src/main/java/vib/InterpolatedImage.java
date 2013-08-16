/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import math3d.Point3d;

public class InterpolatedImage {
	public ImagePlus image;
	public int w,h,d;
	private byte[][] pixels;
	private float[][] pixelsFloat;
	private short[][] pixelsShort;
	private int[][] pixelsInt;
	public Interpolate interpol;
	int type;

	public InterpolatedImage(ImagePlus image) {
		this.image = image;
		ImageStack stack = image.getStack();
		d = stack.getSize();
		h = stack.getHeight();
		w = stack.getWidth();
		type = image.getType();

		if (type == ImagePlus.GRAY8 ||
				type == ImagePlus.COLOR_256) {
			pixels = new byte[d][];
			for (int i = 0; i < d; i++)
				pixels[i] = (byte[])stack.getPixels(i+1);

			if (type == ImagePlus.GRAY8 &&
					!image.getProcessor().isColorLut())
				interpol = new AverageByte();
			else
				interpol = new NearestNeighbourByte();
		} else if (type == ImagePlus.GRAY32) {
			pixelsFloat = new float[d][];
			for (int i = 0; i < d; i++)
				pixelsFloat[i] = (float[])stack.getPixels(i+1);

			interpol = new AverageFloat();
		} else if (type == ImagePlus.GRAY16) {
			pixelsShort = new short[d][];
			for (int i = 0; i < d; i++)
				pixelsShort[i] = (short[])stack.getPixels(i+1);

			interpol = new AverageShort();
		} else if (type == ImagePlus.COLOR_RGB) {
			pixelsInt = new int[d][];
			for (int i = 0; i < d; i++)
				pixelsInt[i] = (int[])stack.getPixels(i+1);

			interpol = new AverageInt();
		} else {
			throw new RuntimeException("Image type not supported");
		}
	}

	public int getWidth() { return w; }
	public int getHeight() { return h; }
	public int getDepth() { return d; }

	protected InterpolatedImage() {
	}

	public ImagePlus getImage() {
		return image;
	}

	public interface Interpolate {
		double get(double x, double y, double z);
	}

	Point3d getCenter() {
		Calibration calib = image.getCalibration();
		return new Point3d(
				calib.xOrigin + w * calib.pixelWidth / 2.0,
				calib.yOrigin + h * calib.pixelHeight / 2.0,
				calib.zOrigin + d * calib.pixelDepth / 2.0);
	}

	Point3d getCenterOfGravity() {
		return getCenterOfGravity(0, 0, 0, w, h, d);
	}

	Point3d getCenterOfGravity(int x0, int y0, int z0,
			int x1, int y1, int z1) {
		Calibration calib = image.getCalibration();
		long x, y, z, total;
		double xD, yD, zD, totalD;

		x = y = z = total = 0;
		xD = yD = zD = totalD = 0;
		for (int k = z0; k < z1; k++)
			for (int j = y0; j < y1; j++)
				for (int i = x0; i < x1; i++) {
					if (type==ImagePlus.GRAY32) {
						double val=getNoInterpolFloat(i,j,k);
						xD += i * val;
						yD += j * val;
						zD += k * val;
						totalD += val;
					} else if(type == ImagePlus.COLOR_RGB) {
						int val = getNoInterpolInt(i, j, k);
						int sum = (val & 0xff0000) >> 16 +
								(val & 0xff00) >> 8 +
								(val & 0xff);
						x += i * sum;
						y += j * sum;
						z += k * sum;
						total += sum;
					} else {
						int val = -1;
						if (type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256)
							val = getNoInterpol(i, j, k);
						else if (type==ImagePlus.GRAY16)
							val = getNoInterpolShort(i, j, k);
						x += i * val;
						y += j * val;
						z += k * val;
						total += val;
					}
				}
		/* If there are no non-zero values in the region at
		   all, make the centre of gravity the midpoint of the
		   region. */
		if (type==ImagePlus.GRAY32) {
			if( totalD == 0 ) {
				xD = (x0 + x1) / 2;
				yD = (y0 + y1) / 2;
				zD = (z0 + z1) / 2;
				totalD = 1;
			}
			return new Point3d(
				calib.xOrigin + calib.pixelWidth * xD / totalD,
				calib.yOrigin + calib.pixelHeight * yD / totalD,
				calib.zOrigin + calib.pixelDepth * zD / totalD);				
		} else {
			if( total == 0 ) {
				x = (x0 + x1) / 2;
				y = (y0 + y1) / 2;
				z = (z0 + z1) / 2;
				total = 1;
			}
			return new Point3d(
				calib.xOrigin + calib.pixelWidth * x / total,
				calib.yOrigin + calib.pixelHeight * y / total,
				calib.zOrigin + calib.pixelDepth * z / total);
		}
	}

	/* as getCenterOfGravity(), but count only the pixels with this value */
	Point3d getCenterOfGravity(int value) {
		if (!(type==ImagePlus.GRAY8||type==ImagePlus.COLOR_256))
			throw new RuntimeException("InterpolatedImage.getCenterOfGravity(int) only makes sense with 8 bit images. (Probably.)");
		Calibration calib = image.getCalibration();
		long x, y, z, total;

		x = y = z = total = 0;
		for (int k = 0; k < d; k++)
			for (int j = 0; j < h; j++)
				for (int i = 0; i < w; i++) {	
					int val = getNoInterpol(i, j, k);
					if (val != value)
						continue;
					x += i;
					y += j;
					z += k;
					total++;
				}
		return new Point3d(
				calib.xOrigin + calib.pixelWidth * x / total,
				calib.yOrigin + calib.pixelHeight * y / total,
				calib.zOrigin + calib.pixelDepth * z / total);
	}

	class AverageByte implements Interpolate {
		final public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpol(x1, y1, z1),
			v001 = getNoInterpol(x1, y1, z1 + 1),
			v010 = getNoInterpol(x1, y1 + 1, z1),
			v011 = getNoInterpol(x1, y1 + 1, z1 + 1),
			v100 = getNoInterpol(x1 + 1, y1, z1),
			v101 = getNoInterpol(x1 + 1, y1, z1 + 1),
			v110 = getNoInterpol(x1 + 1, y1 + 1, z1),
			v111 = getNoInterpol(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR * (yR * (zR * v000 + (1 - zR) * v001)
				+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
				+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
				+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	/*
	 * This weights the values of the 8 ligands by the inverted distance
	 * and picks the one with the maximum
	 */
	class MaxLikelihoodByte implements Interpolate {
		int[] value = new int[8];
		double[] histo = new double[256];
		double xF, yF, zF;

		public MaxLikelihoodByte(double pixelWidth, double pixelHeight,
				double pixelDepth) {
			xF = pixelWidth;
			yF = pixelHeight;
			zF = pixelDepth;
		}

		double xR, yR, zR;
		final double eps = 1e-10;
		final double factor(int dx, int dy, int dz) {
			double x = (dx == 0 ? xR : 1 - xR);
			double y = (dy == 0 ? yR : 1 - yR);
			double z = (dz == 0 ? zR : 1 - zR);
			return 1.0 / (eps + x * x + y * y + z * z);
		}
			
		final public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			xR = x1 + 1 - x;
			yR = y1 + 1 - y;
			zR = z1 + 1 - z;

			for (int i = 0; i < 2; i++)
				for (int j = 0; j < 2; j++)
					for (int k = 0; k < 2; k++) {
						int l = i + 2 * (j + 2 * k);
						value[l] = getNoInterpol(x1 + i,
								y1 + j, z1 + k);
						histo[value[l]]++;
						/*histo[value[l]] += factor(i,
								j, k);*/
					}

			int winner = value[0];

			for (int i = 1; i < 8; i++)
				//if (histo[value[i]] >= histo[winner])
				if (value[i] >= winner)
					winner = value[i];

			for (int i = 0; i < 8; i++)
				histo[value[i]] = 0;

			return winner;
		}
	}

	public class NearestNeighbourByte implements Interpolate {
		final public double get(double x, double y, double z) {
			return getInt(x, y, z);
		}

		final public int getInt(double x, double y, double z) {
			double x1 = Math.round(x);
			double y1 = Math.round(y);
			double z1 = Math.round(z);
			return getNoInterpol((int)x1, (int)y1, (int)z1);
		}
	}

	final public int getNoCheck(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixels[z][x + w * y] & 0xff;
	}

	final public int getNoInterpol(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheck(x, y, z);
	}

	final public byte getNearestByte(double x, double y, double z) {
		int i = (int)Math.round(x);
		if (i < 0 || i >= w)
			return 0;
		int j = (int)Math.round(y);
		if (j < 0 || j >= h)
			return 0;
		int k = (int)Math.round(z);
		if (k < 0 || k >= d)
			return 0;
		return pixels[k][i + w * j];
	}

	public void set(int x, int y, int z, int value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixels[z][x + w * y] = (byte)value;
	}

	public class Iterator implements java.util.Iterator {
		// these are the coordinates
		public int i, j, k;

		boolean showProgress = false;

		int x0, x1, y0, y1, z0, z1, xd, zd;

		public Iterator(boolean showProgress, int x0, int y0, int z0,
				int x1, int y1, int z1) {
			this.showProgress = showProgress;
			this.x0 = x0; this.y0 = y0; this.z0 = z0;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
			xd = x1 - x0; zd = z1 - z0;
			i = x0 - 1; j = y0; k = z0;
		}

		public boolean hasNext() {
			return i + 1 < x1 || j + 1 < y1 || k + 1 < z1;
		}

		public Object next() {
			if (++i >= x1) {
				i = x0;
				if (++j >= y1) {
					j = y0;
					if (++k >= z1)
						return null;
					if (showProgress)
						IJ.showProgress(k - z0 + 1, zd);
				}
			}
			return this;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator iterator() {
		return iterator(false);
	}

	public Iterator iterator(boolean showProgress) {
		return iterator(showProgress, 0, 0, 0, w, h, d);
	}

	public Iterator iterator(boolean showProgress,
			int x0, int y0, int z0, int x1, int y1, int z1) {
		return new Iterator(showProgress, x0, y0, z0, x1, y1, z1);
	}

	// this is quick'n dirty
	public void drawLine(int x1, int y1, int z1, int x2, int y2, int z2,
			int value) {
		int c1 = Math.abs(x1 - x2);
		int c2 = Math.abs(y1 - y2);
		int c3 = Math.abs(z1 - z2);
		if (c2 > c1)
			c1 = c2;
		if (c3 > c1)
			c1 = c3;
		if (c1 == 0) {
			set(x1, y1, z1, value);
			return;
		}
		for (int i = 0; i <= c1; i++)
			set(x1 + i * (x2 - x1) / c1, y1 + i * (y2 - y1) / c1,
					z1 + i * (z2 - z1) / c1, value);
	}

	/* float */
	class AverageFloat implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpolFloat(x1, y1, z1),
			v001 = getNoInterpolFloat(x1, y1, z1 + 1),
			v010 = getNoInterpolFloat(x1, y1 + 1, z1),
			v011 = getNoInterpolFloat(x1, y1 + 1, z1 + 1),
			v100 = getNoInterpolFloat(x1 + 1, y1, z1),
			v101 = getNoInterpolFloat(x1 + 1, y1, z1 + 1),
			v110 = getNoInterpolFloat(x1 + 1, y1 + 1, z1),
			v111 = getNoInterpolFloat(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR * (yR * (zR * v000 + (1 - zR) * v001)
				+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
				+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
				+ (1 - yR) * (zR * v110 + (1 - zR) * v111));

			return ret;
		}
	}

	public float getNoCheckFloat(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsFloat[z][x + w * y];
	}

	public float getNoInterpolFloat(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckFloat(x, y, z);
	}

	public void setFloat(int x, int y, int z, float value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsFloat[z][x + w * y] = value;
	}

	/* int */
	class AverageInt implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			int v000 = getNoInterpolInt(x1, y1, z1),
				v001 = getNoInterpolInt(x1, y1, z1 + 1),
				v010 = getNoInterpolInt(x1, y1 + 1, z1),
				v011 = getNoInterpolInt(x1, y1 + 1, z1 + 1),
				v100 = getNoInterpolInt(x1 + 1, y1, z1),
				v101 = getNoInterpolInt(x1 + 1, y1, z1 + 1),
				v110 = getNoInterpolInt(x1 + 1, y1 + 1, z1),
				v111 = getNoInterpolInt(x1 + 1, y1 + 1, z1 + 1);

			int red = (int)Math.round(xR * (yR * (zR * r(v000) + (1 - zR) * r(v001))
				+ (1 - yR) * (zR * r(v010) + (1 - zR) * r(v011)))
				+ (1 - xR) * (yR * (zR * r(v100) + (1 - zR) * r(v101))
				+ (1 - yR) * (zR * r(v110) + (1 - zR) * r(v111))));

			int green = (int)Math.round(xR * (yR * (zR * g(v000) + (1 - zR) * g(v001))
				+ (1 - yR) * (zR * g(v010) + (1 - zR) * g(v011)))
				+ (1 - xR) * (yR * (zR * g(v100) + (1 - zR) * g(v101))
				+ (1 - yR) * (zR * g(v110) + (1 - zR) * g(v111))));

			int blue = (int)Math.round(xR * (yR * (zR * b(v000) + (1 - zR) * b(v001))
				+ (1 - yR) * (zR * b(v010) + (1 - zR) * b(v011)))
				+ (1 - xR) * (yR * (zR * b(v100) + (1 - zR) * b(v101))
				+ (1 - yR) * (zR * b(v110) + (1 - zR) * b(v111))));
			
			return (red << 16) + (green << 8) + blue;
		}

		private double r(int v) {
			return (double)((v & 0xff0000) >> 16);
		}

		private double g(int v) {
			return (double)((v & 0xff00) >> 8);
		}

		private double b(int v) {
			return (double)(v & 0xff);
		}
	}
	
	public int getNoCheckInt(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsInt[z][x + w * y];
	}
	
	public int getNoInterpolInt(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckInt(x, y, z);
	}
	
	public void setInt(int x, int y, int z, int value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsInt[z][x + w * y] = value;
	}
	
	/* short */
	class AverageShort implements Interpolate {
		public double get(double x, double y, double z) {
			int x1 = (int)Math.floor(x);
			int y1 = (int)Math.floor(y);
			int z1 = (int)Math.floor(z);
			double xR = x1 + 1 - x;
			double yR = y1 + 1 - y;
			double zR = z1 + 1 - z;

			double v000 = getNoInterpolShort(x1, y1, z1),
				v001 = getNoInterpolShort(x1, y1, z1 + 1),
				v010 = getNoInterpolShort(x1, y1 + 1, z1),
				v011 = getNoInterpolShort(x1, y1 + 1, z1 + 1),
				v100 = getNoInterpolShort(x1 + 1, y1, z1),
				v101 = getNoInterpolShort(x1 + 1, y1, z1 + 1),
				v110 = getNoInterpolShort(x1 + 1, y1 + 1, z1),
				v111 = getNoInterpolShort(x1 + 1, y1 + 1, z1 + 1);

			double ret = xR * (yR * (zR * v000 + (1 - zR) * v001)
				+ (1 - yR) * (zR * v010 + (1 - zR) * v011))
				+ (1 - xR) * (yR * (zR * v100 + (1 - zR) * v101)
				+ (1 - yR) * (zR * v110 + (1 - zR) * v111));
			
			return ret;
		}
	}
	
	public short getNoCheckShort(int x, int y, int z) {
		/* no check; we know exactly that it is inside */
		return pixelsShort[z][x + w * y];
	}
	
	public short getNoInterpolShort(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return 0;
		return getNoCheckShort(x, y, z);
	}
	
	public void setShort(int x, int y, int z, short value) {
		if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= d)
			return;
		pixelsShort[z][x + w * y] = value;
	}
	
	public InterpolatedImage cloneDimensionsOnly() {
		return cloneDimensionsOnly(image, type);
	}

	public static InterpolatedImage cloneDimensionsOnly(ImagePlus ip,
			int type) {
		InterpolatedImage result = new InterpolatedImage();
		result.w = ip.getWidth();
		result.h = ip.getHeight();
		result.d = ip.getStack().getSize();

		switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				result.pixels = new byte[result.d][];
				break;
			case ImagePlus.GRAY32:
				result.pixelsFloat = new float[result.d][];
				break;
			case ImagePlus.GRAY16:
				result.pixelsShort = new short[result.d][];
				break;
			case ImagePlus.COLOR_RGB:
				result.pixelsInt = new int[result.d][];
				break;
			default:
				throw new RuntimeException("Image type not supported");
		}

		ImageStack stack = new ImageStack(result.w, result.h, null);
		for (int i = 0; i < result.d; i++)
			switch (type) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				result.pixels[i] =
					new byte[result.w * result.h];
				stack.addSlice("", result.pixels[i]);
				break;
			case ImagePlus.GRAY32:
				result.pixelsFloat[i] =
					new float[result.w * result.h];
				stack.addSlice("", result.pixelsFloat[i]);
				break;
			case ImagePlus.GRAY16:
				result.pixelsShort[i] =
					new short[result.w * result.h];
				stack.addSlice("", result.pixelsShort[i]);
				break;
			case ImagePlus.COLOR_RGB:
				result.pixelsInt[i] =
					new int[result.w * result.h];
				stack.addSlice("", result.pixelsInt[i]);
				break;
			}

		result.image = new ImagePlus("", stack);
		result.image.setCalibration(ip.getCalibration());
		return result;
	}

	public InterpolatedImage cloneImage() {
		InterpolatedImage res = cloneDimensionsOnly();
		for (int k = 0; k < d; k++)
			switch (type) {
				case ImagePlus.GRAY8:
				case ImagePlus.COLOR_256:
					System.arraycopy(pixels[k], 0,
							res.pixels[k],
							0, w * h);
					break;
				case ImagePlus.GRAY32:
					System.arraycopy(pixelsFloat[k], 0,
							res.pixelsFloat[k],
							0, w * h);
					break;
				case ImagePlus.GRAY16:
					System.arraycopy(pixelsShort[k], 0,
							 res.pixelsShort[k],
							 0, w * h);
					break;
				case ImagePlus.COLOR_RGB:
					System.arraycopy(pixelsInt[k], 0,
							 res.pixelsInt[k],
							 0, w * h);
					break;
			}
		return res;
	}
}

