import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This plugin implements the algorithms from the paper
 * Image Deformation Using Moving Least Squares, Schaefer S. and McPhail T.
 * and Warrent J.
 * (http://faculty.cs.tamu.edu/schaefer/research/mls.pdf)
 */
public class Moving_Least_Squares implements PlugIn {
	ImagePlus image;

	final public static int AFFINE = 0;
	final public static int SIMILARITY = 1;
	final public static int RIGID = 2;

	private void need2Images() {
		IJ.showMessage("Need 2 images with a point roi in each,\nwith equal number of points in each roi.");
	}

	public void run(String arg) {

		// Find all images that have a PointRoi in them
		int[] ids = WindowManager.getIDList();
		if (null == ids) {
			need2Images();
			return;
		}
		ArrayList all = new ArrayList();
		for (int i=0; i<ids.length; i++) {
			ImagePlus imp = WindowManager.getImage(ids[i]);
			Roi roi = imp.getRoi();
			if (null != roi && roi instanceof PointRoi)
				all.add(imp);
		}
		if (all.size() < 2) {
			need2Images();
			return;
		}

		// create choice arrays
		String[] titles = new String[all.size()];
		int i=0;
		for (Iterator it = all.iterator(); it.hasNext(); )
			titles[i++] = ((ImagePlus)it.next()).getTitle();

		String[] methods = {"Affine", "Similarity", "Rigid"};
		GenericDialog gd = new GenericDialog("Align Images");
		String current = WindowManager.getCurrentImage().getTitle();
		gd.addChoice("source image", titles, current.equals(titles[0]) ? titles[1] : titles[0]);
		gd.addChoice("target image", titles, current);
		gd.addChoice("method", methods, methods[2]);
		gd.addNumericField("alpha", 1.0, 3);
		gd.addNumericField("gridSize", 0.0, 3);
		gd.addCheckbox("forward", true);
		gd.addCheckbox("merged result", true);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		ImagePlus source = (ImagePlus)all.get(gd.getNextChoiceIndex());
		ImagePlus target = (ImagePlus)all.get(gd.getNextChoiceIndex());
		Method method = getMethod(gd.getNextChoiceIndex());
		method.alpha = (float)gd.getNextNumber();
		float gridSize = (float)gd.getNextNumber();
		boolean useForward = gd.getNextBoolean();
		boolean merge = gd.getNextBoolean();


		PointRoi points1 = (PointRoi)source.getRoi();
		PointRoi points2 = (PointRoi)target.getRoi();

		if (points1.getNCoordinates() != points2.getNCoordinates()) {
			IJ.showMessage("Unequal number of points!");
			return;
		}

		// ready
		ImageProcessor ip = process(source, points1, target, points2, method, gridSize, useForward, merge);
		if (null != ip) new ImagePlus("warped" + (useForward ? " forward" : ""), ip).show();
	}

	static public ImageProcessor process(ImagePlus source, PointRoi points1, ImagePlus target, PointRoi points2, Method method, float gridSize, boolean useForward, boolean merge) {
		if (points1.getNCoordinates() != points2.getNCoordinates()) {
			IJ.log("Unequal number of points!");
			return null;
		}

		Interpolator inter = source.getType() == ImagePlus.COLOR_RGB ?
			(Interpolator)new ColorInterpolator(source.getProcessor()) :
			(Interpolator)new BilinearInterpolator(source.getProcessor());

		// store the result in a new ImageProcessor
		ImageProcessor ip = target.getProcessor().duplicate();
		if (!merge) {
			// make the target image pixels be blank
			ip.setValue(0);
			ip.setRoi(0, 0, ip.getWidth(), ip.getHeight());
			ip.fill();
		}

		if (useForward) {
			if (gridSize < 1)
				gridSize = 10;
			method.setCoordinates(points1, points2);
			method.warpImageForward(inter, (int)gridSize, ip);
		} else {
			method.setCoordinates(points2, points1);
			int w = ip.getWidth(), h = ip.getHeight();
			method.warpImage(inter, w, h, ip.getPixels());
			if (gridSize > 0)
				method.drawGrid(w, h, gridSize, ip.getPixels());
		}

		return ip;
	}

	public static Method getMethod(int method) {
		switch (method) {
			case AFFINE:
				return new Affine();
			case SIMILARITY:
				return new Similarity();
			default:
				return new Rigid();
		}
	}

	static abstract class Method {
		// an alpha of 1 corresponds to Euclidean distance
		public float alpha = 1.0f;

		int n;
		// the centroids
		float pCX, pCY, qCX, qCY;
		float[] pX, pY, qX, qY;

		public void setCoordinates(int[] x1, int[] y1,
				int[] x2, int[] y2, int n) {
			setCoordinates(x1, y1, 0, 0, x2, y2, 0, 0, n);
		}

		public void setCoordinates(PointRoi points1, PointRoi points2) {
			Rectangle r1 = points1.getBounds();
			Rectangle r2 = points2.getBounds();
			setCoordinates(points1.getXCoordinates(),
				points1.getYCoordinates(), r1.x, r1.y,
				points2.getXCoordinates(),
				points2.getYCoordinates(), r2.x, r2.y,
				points1.getNCoordinates());
		}

		public void setCoordinates(int[] x1, int[] y1, int oX1, int oY1,
				int[] x2, int[] y2, int oX2, int oY2, int n) {
			this.n = n;
			pX = new float[n];
			pY = new float[n];
			qX = new float[n];
			qY = new float[n];
			for (int i = 0; i < n; i++) {
				pX[i] = x1[i] + oX1;
				pY[i] = y1[i] + oY1;
				qX[i] = x2[i] + oX2;
				qY[i] = y2[i] + oY2;
			}
		}

		public void setCoordinates(float[] x1, float[] y1,
				float[] x2, float[] y2, int n) {
			this.n = n;
			pX = x1;
			pY = y1;
			qX = x2;
			qY = y2;
		}

		// both x, y and px, py are supposed to be absolute
		public float w(float x, float y, float px, float py) {
			x -= px;
			y -= py;
			if (x == 0 && y == 0)
				return 1e10f;
			x = 1 / (x * x + y * y);
			if (alpha == 1)
				return x;
			return (float)Math.exp(Math.log(x) * alpha);
		}

		float m11, m12, m21, m22;

		// x, y is supposed to be absolute
		public void calculateCentroids(float x, float y) {
			pCX = pCY = qCX = qCY = 0;
			float total = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				total += w;
				pCX += w * pX[i];
				pCY += w * pY[i];
				qCX += w * qX[i];
				qCY += w * qY[i];
			}
			pCX /= total;
			pCY /= total;
			qCX /= total;
			qCY /= total;
		}

		public abstract void calculateM(float x, float y);

		// resultX, resultY is absolute
		float resultX, resultY;

		// x, y is supposed to be absolute
		public void calculate(float x, float y) {
			calculateCentroids(x, y);
			calculateM(x, y);
			resultX = qCX + m11 * (x - pCX)
				+ m12 * (y - pCY);
			resultY = qCY + m21 * (x - pCX)
				+ m22 * (y - pCY);
		}

		public void warpImage(Interpolator inter,
				int w, int h, Object pixels) {
			if (pixels instanceof byte[])
				warpImage(inter, w, h, (byte[])pixels);
			else if (pixels instanceof short[])
				warpImage(inter, w, h, (short[])pixels);
			else if (pixels instanceof float[])
				warpImage(inter, w, h, (float[])pixels);
			else if (pixels instanceof int[])
				warpImage(inter, w, h, (int[])pixels);
			else
				IJ.error("Unknown pixel type");
		}

		public void warpImage(Interpolator inter,
				int w, int h, float[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] =
						//qCX;
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, int[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] = (int)
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, short[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] = (short)
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void warpImage(Interpolator inter,
				int w, int h, byte[] pixels) {
			for (int j = 0; j < h; j++) {
				for (int i = 0; i < w; i++) {
					calculate(i, j);
					pixels[i + w * j] = (byte)
						inter.get(resultX, resultY);
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, Object pixels) {
			if (pixels instanceof byte[])
				drawGrid(w, h, step, (byte[])pixels);
			else if (pixels instanceof short[])
				drawGrid(w, h, step, (short[])pixels);
			else if (pixels instanceof float[])
				drawGrid(w, h, step, (float[])pixels);
			else
				IJ.error("Unknown pixel type");
		}

		boolean gridCondition(int i, int j, float step) {
			float x0, y0, x1, y1, x2, y2, x3, y3;
			x0 = (float)Math.floor(resultX / step);
			y0 = (float)Math.floor(resultY / step);
			calculate(i, j - 0.5f);
			x1 = (float)Math.floor(resultX / step);
			y1 = (float)Math.floor(resultY / step);
			calculate(i, j + 0.5f);
			x2 = (float)Math.floor(resultX / step);
			y2 = (float)Math.floor(resultY / step);
			calculate(i + 0.5f, j);
			x3 = (float)Math.floor(resultX / step);
			y3 = (float)Math.floor(resultY / step);
			return x0 != x1 || x0 != x2 || x0 != x3 || y0 != y1 ||
				y0 != y2 || y0 != y3;
		}

		public void drawGrid(int w, int h, float step, float[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, short[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}

		public void drawGrid(int w, int h, float step, byte[] pixels) {
			for (int j = 0; j < h; j++) {
					calculate(-0.5f, j - 0.5f);
				for (int i = 0; i < w; i++) {
					if (gridCondition(i, j, step))
						pixels[i + w * j] = 0;
				}
				IJ.showProgress(j + 1, h);
			}
		}

		/**
		 * Transform the given quadrilateral by iterating through
		 * the integer coordinates of the target, and bilinearly
		 * interpolating in the source.
		 * This is implemented quick 'n dirty.
		 */
		void forwardQuadrilateral(Interpolator source,
				float[] sX, float[] sY,
				ImageProcessor target,
				int w, int h,
				float[] tX, float[] tY) {
			float minY, maxY;
			minY = maxY = tY[0];
			for (int i = 1; i < 4; i++)
				if (minY > tY[i])
					minY = tY[i];
				else if (maxY < tY[i])
					maxY = tY[i];
			int startY = minY < 0 ? 0 : (int)Math.floor(minY);
			int stopY = maxY > h ? h : (int)Math.ceil(maxY);
			for (int y = startY; y < stopY; y++) {
				// find intersecting edges
				float minX = w, maxX = 0;
				for (int i = 0; i < 4; i++) {
					int i1 = i == 3 ? 0 : i + 1;
					float y1 = (float)Math.round(tY[i]);
					float y2 = (float)Math.round(tY[i1]);
					float rY = (float)Math.round(y);
					if ((rY - y1) * (rY - y2) > 0)
						continue;
					if (y1 != y2) {
						float x = tX[i] + (y - tY[i]) *
							(tX[i1] - tX[i]) /
							(tY[i1] - tY[i]);
						if (minX > x)
							minX = x;
						if (maxX < x)
							maxX = x;
						continue;
					}
					if (tX[i] < tX[i1]) {
						if (minX > tX[i])
							minX = tX[i];
						if (maxX < tX[i1])
							maxX = tX[i1];
					} else {
						if (minX > tX[i1])
							minX = tX[i1];
						if (maxX < tX[i])
							maxX = tX[i];
					}
				}
				// now interpolate
				int startX = minX < 0 ? 0
					: (int)Math.floor(minX);
				int stopX = maxX > w ? w
					: (int)Math.ceil(maxX);
				for (int x = startX; x < stopX; x++) {
					float a, b, c, d, e, f, g, s;
					a = tX[0] - x;
					b = tX[1] - tX[0];
					c = tX[3] - tX[0];
					d = tX[2] - tX[3] - tX[1] + tX[0];
					e = tY[0] - y;
					f = tY[1] - tY[0];
					g = tY[3] - tY[0];
					s = tY[2] - tY[3] - tY[1] + tY[0];
					float p, q, r, dx, dy;
					p = b * s - d * f;
					q = b * g + a * s - d * e - c * f;
					r = a * g - c * e;
					float D = q * q - 4 * p * r;
					if (p == 0 || D < 0) {
						if (b == 0) {
							dx = 0;
							dy = -e / g;
						} else {
							dx = -a / c;
							dy = 0;
						}
					} else {
						dx = (-q + (float)Math.sqrt(D))
							/ 2 / p;
						dy = -(a + b * dx)
							/ (c + d * dx);
					}

					float x0 = (1 - dx) * (1 - dy) * sX[0]
						+ dx * (1 - dy) * sX[1]
						+ (1 - dx) * dy * sX[3]
						+ dx * dy * sX[2];
					float y0 = (1 - dx) * (1 - dy) * sY[0]
						+ dx * (1 - dy) * sY[1]
						+ (1 - dx) * dy * sY[3]
						+ dx * dy * sY[2];
					float value = source.get(x0, y0);
					target.setf(x, y, value);
				}
			}
		}

		// this is quick 'n dirty
		public void warpImageForward(Interpolator source,
				int gridSize, ImageProcessor target) {
			boolean drawGrid = false;
			int w = target.getWidth(), h = target.getHeight();
			if (drawGrid)
				target.setValue(0);
			for (int y = 0; y < source.h; y += gridSize) {
				for (int x = 0; x < source.w; x += gridSize) {
					float[] sX = new float[4];
					float[] sY = new float[4];
					float[] tX = new float[4];
					float[] tY = new float[4];
					sX[0] = x; sY[0] = y;
					sX[1] = x + gridSize; sY[1] = y;
					sX[2] = x + gridSize;
					sY[2] = y + gridSize;
					sX[3] = x; sY[3] = y + gridSize;
					for (int i = 0; i < 4; i++) {
						calculate(sX[i], sY[i]);
						tX[i] = resultX;
						tY[i] = resultY;
					}
					forwardQuadrilateral(source, sX, sY,
							target, w, h, tX, tY);
					if (drawGrid) {
						target.drawLine(
							(int)tX[0], (int)tY[0],
							(int)tX[1], (int)tY[1]);
						target.drawLine(
							(int)tX[1], (int)tY[1],
							(int)tX[2], (int)tY[2]);
						target.drawLine(
							(int)tX[2], (int)tY[2],
							(int)tX[3], (int)tY[3]);
						target.drawLine(
							(int)tX[3], (int)tY[3],
							(int)tX[0], (int)tY[0]);
					}
				}
				IJ.showProgress(y + gridSize, source.h);
			}
		}
	}

	static class Affine extends Method {
		public void calculateM(float x, float y) {
			float a11, a12, a22;
			float b11, b12, b21, b22;
			a11 = a12 = a22 = b11 = b12 = b21 = b22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				a11 += w * pXi * pXi;
				a12 += w * pXi * pYi;
				a22 += w * pYi * pYi;
				b11 += w * pXi * qXi;
				b12 += w * pXi * qYi;
				b21 += w * pYi * qXi;
				b22 += w * pYi * qYi;
			}
			float detA = a11 * a22 - a12 * a12;
			m11 = (a22 * b11 - a12 * b21) / detA;
			m12 = (-a12 * b11 + a11 * b21) / detA;
			m21 = (a22 * b12 - a12 * b22) / detA;
			m22 = (-a12 * b12 + a11 * b22) / detA;
		}
	}

	static class Similarity extends Method {
		public void calculateM(float x, float y) {
			float mu = 0;
			m11 = m12 = m21 = m22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				m11 += w * (pXi * qXi + pYi * qYi);
				m12 += w * (pYi * qXi - pXi * qYi);
				mu += w * (pXi * pXi + pYi * pYi);
			}
			m11 /= mu;
			m12 /= mu;
			m21 = -m12;
			m22 = m11;
		}
	}

	static class Rigid extends Method {
		public void calculateM(float x, float y) {
			float mu1 = 0, mu2 = 0;
			m11 = m12 = m21 = m22 = 0;
			for (int i = 0; i < n; i++) {
				float w = w(x, y, pX[i], pY[i]);
				float pXi = pX[i] - pCX, pYi = pY[i] - pCY;
				float qXi = qX[i] - qCX, qYi = qY[i] - qCY;
				m11 += w * (pXi * qXi + pYi * qYi);
				m12 += w * (pYi * qXi - pXi * qYi);
			}
			float mu = (float)Math.sqrt(m11 * m11 + m12 * m12);
			m11 /= mu;
			m12 /= mu;
			m21 = -m12;
			m22 = m11;
		}
	}

	static abstract class Interpolator {
		ImageProcessor ip;
		int w, h;

		public Interpolator(ImageProcessor ip) {
			this.ip = ip;
			w = ip.getWidth();
			h = ip.getHeight();
		}

		public abstract float get(float x, float y);
	}

	static class BilinearInterpolator extends Interpolator {
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

	static class ColorInterpolator extends Interpolator {
		ColorProcessor cp;
		public ColorInterpolator(ImageProcessor ip) {
			super(ip);
			cp = (ColorProcessor)ip;
		}

		public float get(float x, float y) {
			return (float)cp.getInterpolatedRGBPixel(x, y);
		}
	}
}

