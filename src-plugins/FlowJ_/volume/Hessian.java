package volume;
import bijnum.*;

/**
 * This class implements computing the Hessian tensor matrix of an image, and computing the determinant
 * (to detect blobs) and largest eigenvalues (to detect edges).
 *
 * Copyright (c) 1999-2004, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * Permission to use, copy, modify and distribute this version of this software or any parts
 * of it and its documentation or any parts of it ("the software"), for any purpose is
 * hereby granted, provided that the above copyright notice and this permission notice
 * appear intact in all copies of the software and that you do not sell the software,
 * or include the software in a commercial package.
 * The release of this software into the public domain does not imply any obligation
 * on the part of the author to release future versions into the public domain.
 * The author is free to make upgraded or improved versions of the software available
 * for a fee or commercially only.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class Hessian
{
	/**
	 * Compute the smallest Hessian eigenvectors of a 2-D image.
	 * @param result a float[] in which the result will be stored.
	 * @param image the image
	 * @param width the width of image in pixels.
	 * @param scale the scale of the Gaussian used to compute the first and second order derivatives.
	 */
	public static void smallest(float [] result, float [] image, int width, double scale)
	{
		/** Compute the kernels. */
		Kernel1D k0 = new GaussianDerivative(scale, 0);
		Kernel1D k1 = new GaussianDerivative(scale, 1);
		Kernel1D k2 = new GaussianDerivative(scale, 2);
                // Compute hxx.
		float [] hxx = Convolver.convolvex(image, width, image.length / width, k2);
                hxx = Convolver.convolvey(hxx, width, image.length / width, k0);
                // Compute hxy.
		float [] hxy = Convolver.convolvex(image, width, image.length / width, k1);
                hxy = Convolver.convolvey(hxy, width, image.length / width, k1);
                // Compute hyy.
		float [] hyy = Convolver.convolvex(image, width, image.length / width, k0);
                hyy = Convolver.convolvey(hyy, width, image.length / width, k2);
		// Compute the eigenimage into result.
		for (int j = 0; j < image.length / width; j++)
		for (int i = 0; i < width; i++)
		{
			final float b = -(hxx[j*width+i] + hyy[j*width+i]);
			final float c = hxx[j*width+i]*hyy[j*width+i] - hxy[j*width+i]*hxy[j*width+i];
			final float h1 = (float)(-0.5*(b + (b < 0 ? -1 : 1)*Math.sqrt(b*b - 4*c)));
			final float h2 = c/h1;
			if (h1 > h2)
	                       result[j*width+i] = h2;
			else
			       result[j*width+i] = h1;
		}
	}
	/**
	 * Compute the largest Hessian eigenvectors of a 2-D image.
	 * @param result a float[] in which the result will be stored.
	 * @param image the image
	 * @param width the width of image in pixels.
	 * @param scale the scale of the Gaussian used to compute the first and second order derivatives.
	 */
	public static void largest(float [] result, float [] image, int width, double scale)
        throws IllegalArgumentException
	{
		/** Compute the kernels. */
		Kernel1D k0 = new GaussianDerivative(scale, 0);
		Kernel1D k1 = new GaussianDerivative(scale, 1);
		Kernel1D k2 = new GaussianDerivative(scale, 2);
                // Compute hxx.
		float [] hxx = Convolver.convolvex(image, width, image.length / width, k2);
                hxx = Convolver.convolvey(hxx, width, image.length / width, k0);
                // Compute hxy.
		float [] hxy = Convolver.convolvex(image, width, image.length / width, k1);
                hxy = Convolver.convolvey(hxy, width, image.length / width, k1);
                // Compute hyy.
		float [] hyy = Convolver.convolvex(image, width, image.length / width, k0);
                hyy = Convolver.convolvey(hyy, width, image.length / width, k2);
		// Compute the eigenimage into result.
		for (int j = 0; j < image.length / width; j++)
		for (int i = 0; i < width; i++)
		{
			final double b = -(hxx[j*width+i] + hyy[j*width+i]);
			final double c = hxx[j*width+i]*hyy[j*width+i] - hxy[j*width+i]*hxy[j*width+i];
			final double d = b*b - 4*c;
			double e = 0;
			if (d > 0)
	                         e = Math.sqrt(d);
			final double h1 = -0.5*(b + (b < 0 ? -1 : 1)*d);
                        final double h2;
                        if (h1 != 0)
			        h2 = c/h1;
                        else
                                h2 = 0;
			if (h1 > h2)
	                       result[j*width+i] = (float) h1;
			else
			       result[j*width+i] = (float) h2;
                        if (Float.isNaN(result[j*width+i]))
                               result[j*width+i] = 0;
                               // throw new IllegalArgumentException("Hessian: NaN");
		}
	}
	/**
	 * Compute the largest Hessian eigenvectors of a 2-D image.
	 * @param image the image
	 * @param width the width of image in pixels.
	 * @param scale the scale of the Gaussian used to compute the first and second order derivatives.
	 * @return result a float[] witht he largest eigenvalues.
	 */
	public static float [] largest(float [] image, int width, double scale)
	{
		float [] result = new float[image.length];
		largest(result, image, width, scale);
		return result;
	}
	/**
	 * Compute the smallest Hessian eigenvectors of a 2-D image.
	 * @param image the image
	 * @param width the width of image in pixels.
	 * @param scale the scale of the Gaussian used to compute the first and second order derivatives.
	 * @return result a float[] witht he largest eigenvalues.
	 */
	public static float [] smallest(float [] image, int width, double scale)
	{
		float [] result = new float[image.length];
		smallest(result, image, width, scale);
		return result;
	}
	/**
	 * Compute the determinant of the Hessian matrix of image.
	 * L1 L2
	 * where
	 * L1 = (dxx + dyy) + SQRT((dxx - dyy)^2 + 4 dxy^2)
	 * L2 = (dxx + dyy) - SQRT((dxx - dyy)^2 + 4 dxy^2)
	 * @param image the image from which the derivatives are computed
	 * @param width of image in pixels.
	 * @param scale the scale at which the derivatives will be computed as the stddev of the Gaussian.
	 * @return L1 L2 as a float.
	 */
	public static float [] det(float [] image, int width, double scale)
	{
		/** Compute the kernels. */
		Kernel1D k0 = new GaussianDerivative(scale, 0);
		Kernel1D k1 = new GaussianDerivative(scale, 1);
		Kernel1D k2 = new GaussianDerivative(scale, 2);
                // Compute hxx.
		float [] hxx = Convolver.convolvex(image, width, image.length / width, k2);
                hxx = Convolver.convolvey(hxx, width, image.length / width, k0);
                // Compute hxy.
		float [] hxy = Convolver.convolvex(image, width, image.length / width, k1);
                hxy = Convolver.convolvey(hxy, width, image.length / width, k1);
                // Compute hyy.
		float [] hyy = Convolver.convolvex(image, width, image.length / width, k0);
                hyy = Convolver.convolvey(hyy, width, image.length / width, k2);
		/** Now compute L1, L2. */
		// Compute hxx + hyy.
		float [] hxxplushyy = BIJmatrix.addElements(hxx, hyy);
		// Compute hxx - hyy.
		float [] hxxminhyy = BIJmatrix.sub(hxx, hyy);
 		// Compute square of hxx-hyy.
		float [] sqrhxxminhyy = BIJmatrix.mulElements(hxxminhyy, hxxminhyy);
 		// Compute 4 hxy hxy.
		float [] hxytimeshxy4 = BIJmatrix.mulElements(hxy, hxy);
                BIJmatrix.mulElements(hxytimeshxy4, hxytimeshxy4, 4);
 		// Compute square root of ((hxx-hyy)^2+ 4 hxy hxy).
		// Don't know whether this implementation is faster  or the one in largest.
		float [] sum = BIJmatrix.addElements(sqrhxxminhyy, hxytimeshxy4);
		float [] sqrt = new float[sum.length];
                BIJmatrix.pow(sqrt, sum, 0.5f);
		float [] l1 = BIJmatrix.addElements(hxxplushyy, sqrt);
		float [] l2 = BIJmatrix.sub(hxxplushyy, sqrt);
		float [] det = BIJmatrix.mulElements(l1, l2);
		return det;
	}
}
