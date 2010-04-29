import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Iterator;

import math3d.FastMatrixN;
import math3d.Point3d;

/*
 * This plugin takes a binary stack as input, where some slices are
 * labeled (i.e. contain white regions), and some are not. It constructs
 * an implicit function (using radial functions as basis) and interpolates
 * the unlabeled slices.
 *
 * It is based on the paper
 * Shape Transformation Using Variational Implicit Functions,
 * Greg Turk and James F. O'Brien
 * See http://www.cc.gatech.edu/~turk/my_papers/schange.pdf
 */

public class Implicit_Interpolate_Binary implements PlugInFilter {
	private double[]lut;
	ImagePlus image;
	public ImplicitSamples[] samples;
	public ImplicitFunction implicitFunction;

	public void run(ImageProcessor ip) {
		ImageStack stack = image.getStack();
		int sliceCount = stack.getSize();
		if (sliceCount < 3) {
			IJ.error("Too few slices to interpolate!");
			return;
		}

		GenericDialog gd = new GenericDialog("Interpolate");
		gd.addNumericField("step", 30, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int step = (int)gd.getNextNumber();

		IJ.showStatus("getting samples");
		samples = new ImplicitSamples[sliceCount];
		for (int z = 0; z < sliceCount; z++) {
			ip = stack.getProcessor(z + 1);
			samples[z] = ImplicitSamples.from((ByteProcessor)ip, z);
		}

		IJ.showStatus("calculating weights");
		getImplicitFunction(step);

		IJ.showStatus("interpolating");
		for (int z = 0; z < sliceCount; z++) {
			if (samples[z] == null)
				evaluate((ByteProcessor)stack.getProcessor(z + 1), z);
			IJ.showProgress(z + 1, sliceCount);
		}
	}

	public void getImplicitFunction(int step) {
		/* build matrix */
		ArrayList ones = new ArrayList();
		ArrayList zeroes = new ArrayList();
		int n = 0;
		for (int z = 0; z < samples.length; z++) {
			if (samples[z] == null)
				continue;
			Iterator iter = samples[z].ones.iterator();
			while (iter.hasNext())
				if (((n++) % step) == 0)
					ones.add(iter.next());
				else
					iter.next();
			iter = samples[z].zeroes.iterator();
			while (iter.hasNext())
				if (((n++) % step) == 0)
					zeroes.add(iter.next());
				else
					iter.next();
		}

		int onesSize = ones.size();
		int zeroesSize = zeroes.size();
		int rank = onesSize + zeroesSize + 4;
		double[][] matrix = new double[rank][rank];
		for (int i = 0; i < rank - 4; i++) {
			Point3d p1 = (Point3d)(i < onesSize ?
					ones.get(i) : zeroes.get(i - onesSize));
			for (int j = i + 1; j < rank - 4; j++) {
				Point3d p2 = (Point3d)(j < onesSize ?
						ones.get(j) :
						zeroes.get(j - onesSize));
				matrix[i][j] = matrix[j][i] = phi(p1, p2);
			}
		}
		for (int i = 0; i < onesSize + zeroesSize; i++) {
			Point3d p = (Point3d)(i < onesSize ?
					ones.get(i) : zeroes.get(i - onesSize));
			matrix[i][rank - 4] = matrix[rank - 4][i] = 1;
			matrix[i][rank - 3] = matrix[rank - 3][i] = p.x;
			matrix[i][rank - 2] = matrix[rank - 2][i] = p.y;
			matrix[i][rank - 1] = matrix[rank - 1][i] = p.z;
		}
		// TODO: use LU decomposition (faster)
		IJ.showStatus("inverting " + rank + " x " + rank + " matrix");
		FastMatrixN.invert(matrix, true);
		implicitFunction = new ImplicitFunction(matrix, ones, zeroes);
	}

	public void evaluate(ByteProcessor ip, int z) {
		int  w = ip.getWidth();
		int h = ip.getHeight();
		byte[] p = (byte[])ip.getPixels();
		for (int j = 0; j < h; j++)
			for (int i = 0; i < w; i++) {
				double v0 = implicitFunction.evaluate(i, j, z);
				p[i + j * w] = (byte)(v0 > 0 ? 255 : 0);
			}		
	}

	public static class ImplicitSamples {
		/*
		 * these arrays contain Point3d's:
		 * "ones" contains those where the implicit function is 1,
		 * and "zeroes", well, you get the concept.
		 */
		ArrayList ones, zeroes;
		ImplicitSamples() {
			ones = new ArrayList();
			zeroes = new ArrayList();
		}

		public static ImplicitSamples from(ByteProcessor ip, int z) {
			ImplicitSamples data = new ImplicitSamples();
			data.initFromSlice(ip, z);
			if (data.ones.size() == 0 && data.zeroes.size() == 0)
				return null;
			return data;
		}

		public void initFromSlice(ByteProcessor ip, int z) {
			int w = ip.getWidth();
			int h = ip.getHeight();
			byte[] p = (byte[])ip.getPixels();
			for (int j = 1; j < h; j++)
				for (int i = 1; i < w; i++) {
					byte b0 = p[i - 1 + (j - 1) * w];
					byte b1 = p[i - 1 + j * w];
					byte b2 = p[i + (j - 1) * w];
					byte b3 = p[i + j * w];

					if (b0 != 0 && b1 == 0 && b2 == 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.6, j - 0.6, z));
						ones.add(new Point3d(i - 0.9, j - 0.9, z));
					} else if (b0 == 0 && b1 != 0 && b2 == 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.6, j - 0.4, z));
						ones.add(new Point3d(i - 0.9, j - 0.1, z));
					} else if (b0 == 0 && b1 == 0 && b2 != 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.4, j - 0.6, z));
						ones.add(new Point3d(i - 0.1, j - 0.9, z));
					} else if (b0 == 0 && b1 == 0 && b2 == 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.4, j - 0.4, z));
						ones.add(new Point3d(i - 0.1, j - 0.1, z));
					} else if (b0 != 0 && b1 != 0 && b2 == 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.7, j - 0.5, z));
						ones.add(new Point3d(i - 1, j - 0.5, z));
					} else if (b0 != 0 && b1 == 0 && b2 != 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.5, j - 0.7, z));
						ones.add(new Point3d(i - 0.5, j - 1, z));
					} else if (b0 == 0 && b1 != 0 && b2 == 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.5, j - 0.3, z));
						ones.add(new Point3d(i - 0.5, j, z));
					} else if (b0 == 0 && b1 == 0 && b2 != 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.3, j - 0.5, z));
						ones.add(new Point3d(i, j - 0.5, z));
					} else if (b0 != 0 && b1 == 0 && b2 == 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.7, j - 0.3, z));
						zeroes.add(new Point3d(i - 0.3, j - 0.7, z));
						ones.add(new Point3d(i - 0.5, j - 0.5, z));
					} else if (b0 == 0 && b1 != 0 && b2 != 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.7, j - 0.7, z));
						zeroes.add(new Point3d(i - 0.3, j - 0.3, z));
						ones.add(new Point3d(i - 0.5, j - 0.5, z));
					} else if (b0 == 0 && b1 != 0 && b2 != 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.7, j - 0.7, z));
						ones.add(new Point3d(i - 0.1, j - 0.1, z));
					} else if (b0 != 0 && b1 == 0 && b2 != 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.7, j - 0.3, z));
						ones.add(new Point3d(i - 0.1, j - 0.9, z));
					} else if (b0 != 0 && b1 != 0 && b2 == 0 && b3 != 0) {
						zeroes.add(new Point3d(i - 0.3, j - 0.7, z));
						ones.add(new Point3d(i - 0.9, j - 0.1, z));
					} else if (b0 != 0 && b1 != 0 && b2 != 0 && b3 == 0) {
						zeroes.add(new Point3d(i - 0.3, j - 0.3, z));
						ones.add(new Point3d(i - 0.9, j - 0.9, z));
					}
				}
		}
	}

	final public double phi(Point3d p, Point3d q) {
		return phi(p.x, p.y, p.z, q);
	}

	final public double phi(double x, double y, double z,
			Point3d q) {
		double squared = (x - q.x) * (x - q.x)
			+ (y - q.y) * (y - q.y) + (z - q.z) * (z - q.z);
		return phi(squared);
	}
	
	final public static double phi(double squared){
		return squared > 0 ? squared * Math.log(Math.sqrt(squared)) : 0;
	}
	
	final public double phiLUT(double squared){
		int w = image.getWidth(), h = image.getHeight(), z = image.getStackSize();
		if(lut == null){
			lut = new double[(w*w + h*h + z*z)];
			for(int i=0;i<lut.length;i++)
				lut[i] = -1.0;
		}
		/*
		 * round squared to the neighbouring 0.1.
		 * test if key is contained in lut
		 * if not: calculate the value of key and store it in lut
		 * return a linear interpolation between 
		 */
		int key_l = (int)Math.floor(squared);
		int key_u = key_l + 1;

		
		if(lut[key_l] < 0){
			lut[key_l] = phi(key_l);
		} 
		if(lut[key_u] < 0){
			lut[key_u] = phi(key_u);
		} 
		
		double value_l = lut[key_l];
		double value_u = lut[key_u];
		
		double ret = Math.abs(squared-key_l) * Math.abs(value_u-value_l)/(double)(key_u-key_l) + value_l;
		
		return ret;
		
	}

	public class ImplicitFunction {
		Point3d[] points;
		double[] weights;
		double p0, pX, pY, pZ;

		ImplicitFunction(double[][] matrix,
				ArrayList ones, ArrayList zeroes) {
			points = new Point3d[matrix.length - 4];
			weights = new double[points.length];
			int onesSize = ones.size();
			for (int i = 0; i < points.length; i++) {
				points[i] = (Point3d)(i < onesSize ?
						ones.get(i) :
						zeroes.get(i - onesSize));
				for (int j = 0; j < onesSize; j++)
					weights[i] += matrix[i][j];
			}
			for (int j = 0; j < onesSize; j++) {
				p0 += matrix[matrix.length - 4][j];
				pX += matrix[matrix.length - 3][j];
				pY += matrix[matrix.length - 2][j];
				pZ += matrix[matrix.length - 1][j];
			}
		}

		public double evaluate(Point3d p) {
			return evaluate(p.x, p.y, p.z);
		}

		public double evaluate(double x, double y, double z) {
			double result = p0 + pX * x + pY * y + pZ * z;
			for (int i = 0; i < points.length; i++) {
				result += phi(x, y, z, points[i]) * weights[i];
			}
			return result;
		}
	}

	public int setup(String arg, ImagePlus imp) {
		image = imp;
		return DOES_8G;
	}
}

