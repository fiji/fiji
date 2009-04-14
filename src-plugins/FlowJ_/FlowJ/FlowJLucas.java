 package FlowJ;
import ij.*;
import ij.process.*;
import volume.*;
import bijnum.*;

/**
 * This is a class that implements the Lucas and Kanade optical flow algorithm.
 * Implementation based on convolutions, with local field properties,
 * gradient computation with different kernels including scalable Gaussian derivatives.<br>
 * <pre>
 * References:
 *      Lucas and Kanade, Proc IJNA 1981
 *      Barron, Fleet, Beauchemin, 1994, IJCV 12, 1, 43-77
 *      Abramoff et al, 2002, IEEE TMI.
 * </pre>
 *
 * Copyright (c) 1999-2002, Michael Abramoff. All rights reserved.
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
public class FlowJLucas
{
	  public final boolean          debug = false;
	  public final boolean          displayon = false;   // sets display of various intermediate images.
          /** Types of derivative computation. */
	  protected static final int      GAUSSIANDERIV = 0;
	  protected static final int      CENTRALDIFF = 1;
	  protected static final int      SOBEL2D = 2;
	  protected static final int      SOBEL3D = 3;
	  protected static final int	  SUBTRACT = 4;
	  public static String [] sderiv =    { "Gaussian deriv", "4 Point CD", "Sobel 2D", "Sobel 3D", "2 images diff" };

	  // LK: eigenvalue threshold tau
          protected float        tau;
	  // spatial and temporal prefilter kernels (separated).
          protected Kernel1D      sKernel, tKernel;
	  protected Kernel        gradientKernel;
	  // Gaussian derivative spatial kernel
          protected Kernel1D      sgradientKernel;
	  // Gaussian derivative temporal kernel
          protected Kernel1D      tgradientKernel;
	  // Regularization kernel for the Omega local field.
          protected Kernel      	G;
	  protected float         density;
          // The derivatives in x, y and z direction.
	  protected VolumeFloat   dx, dy, dt;
          /** Types of regularization (smoothness constraint) computation. */
	  protected final int     REG_GAUSSIAN_1D = 0;
	  protected final int     REG_GAUSSIAN = 1;
	  protected final int     REG_AVERAGE = 2;
	  public static String [] sregul = {  "Gaussian 1D", "Gaussian", "Average 1D" };
	  protected int           support;
	  protected boolean 	        includeNormals;


          /**
           * Return the number of frames needed for specific sigmat and gradient type.
           * @param sigmat temporal sigma of the kernel.
           * @param gradientType the type of gradient, one of
           * GAUSSIANDERIV, CENTRALDIFF, SOBEL2D, SOBEL3D, SUBTRACT
           * @return number of frames needed.
           */
          public static int getSupport(float sigmat, int gradientType)
          {
                int support = 0;
                Kernel gradientKernel, tKernel, tgradientKernel;
                switch (gradientType)
                {
                        case CENTRALDIFF:
                                gradientKernel = new CentralDiff();
                                tKernel = new Gaussian(sigmat);
                                support = tKernel.support()+gradientKernel.support()-1;
                                break;
                        case SOBEL2D:
                                gradientKernel = new Sobel();
                                tKernel = new Gaussian(sigmat);
                                support = tKernel.support()+gradientKernel.support()-1;
                                break;
                        case SOBEL3D:
                                gradientKernel = new Sobel3D();
                                tKernel = new Gaussian(sigmat);
                                support = tKernel.support()+gradientKernel.support()-1;
                                break;
                        case GAUSSIANDERIV:
                                tgradientKernel = new GaussianDerivative(sigmat);
                                support = tgradientKernel.support();
                                break;
                        case SUBTRACT:		// subtraction of two images. No kernels.
                                support = 2;
                                break;
                }
                return support;
          }
          /**
           * Return the first frame for which you can compute the flow.
           * @param is an ImageStack containing the images.
           * @param sigmat temporal sigma of the kernel.
           * @param gradientType the type of gradient, one of
           * GAUSSIANDERIV, CENTRALDIFF, SOBEL2D, SOBEL3D, SUBTRACT
           * @return: index of first frame (from 1).
           */
          public static int firstFrame(ImageStack is, float sigmat, int gradientType)
          {
                return getSupport(sigmat, gradientType)/2 + 1;
          }
          /**
           * Return the last frame for which you can compute the flow.
           * @param is an ImageStack containing the images.
           * @param sigmat temporal sigma of the kernel.
           * @param gradientType the type of gradient, one of
           * GAUSSIANDERIV, CENTRALDIFF, SOBEL2D, SOBEL3D, SUBTRACT
           * @return: index of last frame (from 1).
           */
          public static int lastFrame(ImageStack is, float sigmat, int gradientType)
          {
                return is.getSize()-getSupport(sigmat, gradientType)/2-1;
          }
          /**
          * Do filtering.
          */
	  public void filterAll(ImageStack is, int center, float sigmat, float sigmas, int gradientType)
	  throws FlowJException
	  {
                  VolumeFloat   v = null;
                  // Create the kernel for gradient computation.
                  // You need it now to know the width of the support (to check number of images)
                  support = 0;
                  switch (gradientType)
                  {
                                case CENTRALDIFF:   gradientKernel = new CentralDiff();
                                        tKernel = new Gaussian(sigmat);
                                        sKernel = new Gaussian(sigmas);
                                        support = tKernel.support()+gradientKernel.support()-1;
                                        break;
                                case SOBEL2D:       gradientKernel = new Sobel();
                                        tKernel = new Gaussian(sigmat);
                                        sKernel = new Gaussian(sigmas);
                                        support = tKernel.support()+gradientKernel.support()-1;
                                        break;
                                case SOBEL3D:       gradientKernel = new Sobel3D();
                                        tKernel = new Gaussian(sigmat);
                                        sKernel = new Gaussian(sigmas);
                                        support = tKernel.support()+gradientKernel.support()-1;
                                        break;
                                case GAUSSIANDERIV: // create kernels for temporal and spatial derivative
                                        tgradientKernel = new GaussianDerivative(sigmat);
                                        sgradientKernel = new GaussianDerivative(sigmas);
                                        // create filter kernels for the remaining 2 directions
                                        tKernel = new Gaussian(sigmat);
                                        sKernel = new Gaussian(sigmas);
                                        support = tgradientKernel.support();
                                        break;
                                case SUBTRACT:		// subtraction of two images. No kernels.
                                        sKernel = new Gaussian(sigmas);
                                        sgradientKernel = new GaussianDerivative(sigmas);
                                        support = 2;
                                        break;
                  }

                  // Check enough frames in sequence for filtering.
                  if (is.getSize() < support)
                  {
                        throw new FlowJException("Need at least " +support+" slices in stack.");
                  }
                  if ((support > 2) && (center-1 < support/2 || center-1 > (is.getSize()-support/2-1)))
                  {
                        throw new FlowJException("Please select the frame  > "
                                          + support/2 + " and < " + (is.getSize()+1-support/2));
                  }

                  // Now compute the derivatives.
                  dx = new VolumeFloat(is.getWidth(), is.getHeight(), 1);
                  dy = new VolumeFloat(is.getWidth(), is.getHeight(), 1);
                  dt = new VolumeFloat(is.getWidth(), is.getHeight(), 1);

                  switch (gradientType)
                  {
                                case CENTRALDIFF:
                                case SOBEL2D:
                                case SOBEL3D:
                                        // you need to have as many images in the intermediary volume as
                                        // the gradient kernel support.
                                        v = new VolumeFloat(is.getWidth(), is.getHeight(), gradientKernel.support());
                                        // Filter temporally if necessary and load intermediary volume
                                        if (tKernel instanceof Kernel)
                                                v.convolvet(is, center, tKernel);
                                        else
                                                v.load(is, center - gradientKernel.support() / 2);
                                        // Filter spatially if necessary.
                                        if (sKernel instanceof Kernel)
                                                v.convolvexy(sKernel);
                                        IJ.showStatus("Computing "+gradientKernel.toString()+" derivatives...");
                                        if (gradientKernel instanceof Kernel1D)
                                        {
                                                dx.convolvex(v, (Kernel1D) gradientKernel);
                                                dy.convolvey(v, (Kernel1D) gradientKernel);
                                                dt.convolvez(v, (Kernel1D) gradientKernel);
                                        }
                                        else if (gradientKernel instanceof Kernel2D)
                                        {
                                                dx.convolvex(v, (Kernel2D) gradientKernel);
                                                dy.convolvey(v, (Kernel2D) gradientKernel);
                                                dt.convolvez(v, (Kernel2D) gradientKernel);
                                        }
                                        else if (gradientKernel instanceof Kernel3D)
                                        {
                                                dx.convolvex(v, (Kernel3D) gradientKernel);
                                                dy.convolvey(v, (Kernel3D) gradientKernel);
                                                dt.convolvez(v, (Kernel3D) gradientKernel);
                                        }
                                        break;
                                case GAUSSIANDERIV:
                                        IJ.showStatus("Computing Gaussian derivatives...");
                                        // Load and convolve with appropriate temporal kernel on the fly.
                                        dx.convolvet(is, center, tKernel);
                                        dy.convolvet(is, center, tKernel);
                                        dt.convolvet(is, center, tgradientKernel);
                                        // convolve in the remaining directions with the spatial kernels.
                                        // Create a buffer volume.
                                        VolumeFloat temp = new VolumeFloat(is.getWidth(), is.getHeight(), 1);
                                        temp.convolvex(dx, (Kernel1D) sgradientKernel);
                                        dx.convolvey(temp, (Kernel1D) sKernel);
                                        // convolve in the remaining directions for y derivatives.
                                        temp.convolvex(dy, (Kernel1D) sKernel);
                                        dy.convolvey(temp, (Kernel1D) sgradientKernel);
                                        // convolve in the remaining directions for t derivatives.
                                        temp.convolvex(dt, (Kernel1D) sKernel);
                                        dt.convolvey(temp, (Kernel1D) sKernel);
                                        break;
                                case SUBTRACT:
                                        v = new VolumeFloat(is.getWidth(), is.getHeight(), 2);
                                        // Load intermediary volume
                                        v.load(is, Math.max(0, center-1));
                                        // Compute spatial gradients (GaussianDerivative s)
                                        if (sKernel instanceof Kernel)
                                                v.convolvexy(sKernel);
                                        // Compute Gaussian derivatives for x and y.
                                        dx.convolvex(v, sgradientKernel);
                                        dy.convolvey(v, sgradientKernel);
                                        // Compute the difference image for t.
                                        dt.convolvez(v);
                                        (new ImagePlus("dx", dx.getImageStack())).show();
                                        (new ImagePlus("dy", dy.getImageStack())).show();
                                        (new ImagePlus("dt", dt.getImageStack())).show();
                                        break;
                  }
        }
        /**
         * Compute full flow field from the first order gradients in dx, dy, dt
         * for a weighted local neighborhood omega (weighted by G defined by sigmaw)
         * around every image location.
         * Find the best fit for that location using a LSE using the pseudoinverse.
         * A SVD decreased speed and did not influence errors.
         * All elements with eigenvalues of the pseudoinverse matrix m smaller than threshold
         * tau are rejected.
         * Only if the second eigenvalue of the pseudoinverse is larger than tau,
         * normal flow instead of full flow is computed for that location.
         * @param flow will contain the x,y flows.
         * @param includeNormals a flag whether to include normals or not.
         * @param sigmaw SD of the probability function associated with the Gaussian defining the size of the neighborhood
         * @param tau the eigenvalue threshold
         * @param regularizationMethod is one of REG_GAUSSIAN, REG_GAUSSIAN_1D, 0 defines how the local neighborhood is filtered.
        */
        public void computeFull(FlowJFlow flow, boolean includeNormals, float sigmaw, float tau, int regularizationMethod)
        {
                  this.includeNormals = includeNormals;
                  this.tau = tau;
                  // Precompute the products of the derivatives.
                  VolumeFloat xt = new VolumeFloat(dx);
                  xt.mul(dt);
                  VolumeFloat yt = new VolumeFloat(dy);
                  yt.mul(dt);
                  VolumeFloat xx = new VolumeFloat(dx);
                  xx.mul(dx);
                  VolumeFloat yy = new VolumeFloat(dy);
                  yy.mul(dy);
                  VolumeFloat xy = new VolumeFloat(dx);
                  xy.mul(dy);
                  dx = dy = dt = null;

                  switch (regularizationMethod)
                  {
                                  case REG_GAUSSIAN:
                                                        // slower.
                                                        G = new Gaussian2D(sigmaw);
                                                        // Convolve with a Gaussian to obtain weighted products.
                                                        xt.convolvexy((Kernel2D) G);
                                                        yt.convolvexy((Kernel2D) G);
                                                        xx.convolvexy((Kernel2D) G);
                                                        yy.convolvexy((Kernel2D) G);
                                                        xy.convolvexy((Kernel2D) G);
                                                        break;

                                  case REG_GAUSSIAN_1D:
                                                        // faster, almost same angular error.
                                                        G = new Gaussian(sigmaw);
                                                        // Convolve with a Gaussian to obtain weighted products.
                                                        xt.convolvexy((Kernel1D) G);
                                                        yt.convolvexy((Kernel1D) G);
                                                        xx.convolvexy((Kernel1D) G);
                                                        yy.convolvexy((Kernel1D) G);
                                                        xy.convolvexy((Kernel1D) G);
                                                        break;
                                  default:
                                                        // for comparison purposes: conforms to Barron text.
                                                        G = new Gaussian(5);
                                                        // Convolve with a Gaussian to obtain weighted products.
                                                        xt.convolvexy((Kernel1D) G);
                                                        yt.convolvexy((Kernel1D) G);
                                                        xx.convolvexy((Kernel1D) G);
                                                        yy.convolvexy((Kernel1D) G);
                                                        xy.convolvexy((Kernel1D) G);
                                                        break;
                  }

                  int total=0; int full=0; int normals = 0;
                  flow.v.setEdge(xx.getEdge());
                  for (int y = 0; y < xx.getHeight(); y++)
                  {
                                  IJ.showProgress((float) y/(float) xx.getHeight());
                                  for (int x = 0; x < xx.getWidth(); x++)
                                  {
                                                flow.set(x, y, 0, 0, false);
                                                if (xx.valid(x,y))
                                                {
                                                          float [] b = new float[2];
                                                          float [][] m = new float[2][2];
                                                          m[0][0] = xx.v[0][y][x];
                                                          m[0][1] = xy.v[0][y][x];
                                                          m[1][0] = xy.v[0][y][x];
                                                          m[1][1] = yy.v[0][y][x];
                                                          b[0] = xt.v[0][y][x];
                                                          b[1] = yt.v[0][y][x];
                                                          float [][] mi = null; BIJJacobi j = null;
                                                          try
                                                          {
                                                                mi = BIJmatrix.inverse(m);
                                                                j = new BIJJacobi(m, true);
                                                                //if (debug && ! BIJMatrix.checkinverse(m, mi))
                                                                 //              IJ.write("inverse failure! det = "+BIJMatrix.determinant(m)+" "+x+", "+y);
                                                                 j.compute();
                                                                 // For debugging. Suppplied by Barron & Beauchemin
                                                                 //if (debug)	j.check(m);
                                                                 j.sort();
                                                                 if (j.eigenvalues[0] >= tau && j.eigenvalues[1] >= tau)
                                                                 {
                                                                       // Full velocity if spread of M is small
                                                                       if (BIJmatrix.determinant(m) > 0)
                                                                       {
                                                                                 float [] v = BIJmatrix.mul(mi, b);
                                                                                 flow.set(x, y, -v[0], v[1], true); // y
                                                                                 full++;
                                                                       }
                                                                 }
                                                                 else if (includeNormals && j.eigenvalues[0] > tau && Math.abs(BIJmatrix.determinant(m)) > 0.00000001)
                                                                 {
                                                                       // Normal velocity.
                                                                       float [] v = BIJmatrix.mul(mi, b);
                                                                       // Project v onto that direction
                                                                       float [] ff = new float[2];
                                                                       ff[0] = (v[0]*j.eigenvectors[0][0] + v[1]*j.eigenvectors[1][0])*j.eigenvectors[1][0];
                                                                       ff[1] = (- v[0]*j.eigenvectors[0][0] - v[1]*j.eigenvectors[1][0])*j.eigenvectors[0][0];
                                                                       flow.set(x, y, ff[0], ff[1], true);
                                                                       normals++;
                                                                 }
                                                          } catch (Exception e) { IJ.write("Inverse or Jacobi error "+e); }
                                                           total++;
                                                } // if
                                  }  // for x
                  } // for y
                  density = (float) full/(float) total;
        } // computeFull
        public String toString()
        // Use a format that is compatible with file names.
        {
                String s = "LK ";
                if (sKernel instanceof Kernel)
                              s += "s"+sKernel.toString();
                if (tKernel instanceof Kernel)
                              s += " t"+tKernel.toString();
                s += " w"+G.toString()+" tau"+tau+" ";
                if (gradientKernel instanceof Kernel)
                              s+= gradientKernel.toString();
                else if (tgradientKernel instanceof Kernel)
                              s+= " Gaussian diff ";
                else
                              s+= " diff ";
                s += " ("+IJ.d2s(density*100,2)+"%)"+
                              (includeNormals?" (w. normals) ":"")+"(support="+support+")";
                return s;
        }
}
