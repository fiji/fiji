package volume;
import ij.*;
import ij.gui.*;

/**
 * This is a class that implements nonlinear diffusion in 2D, 3D and 4D.
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
public class Diffusion3D
{
      protected static int iterations=10;
      protected static double k=2;
      protected static double lambda=1.0;
      // depth of every 3D volume in a 4D hypervolume.
      protected int depth = 23;
      protected boolean scaleSpaceContrast=false;
      protected VolumeFloat v;
      protected HyperVolume hv;

      public boolean params(ImagePlus imp, VolumeFloat v)
      {
            this.v = v;
            GenericDialog gd = new GenericDialog("3D diffusion", imp.getWindow());
            gd.addNumericField("lambda", lambda, 2);
            gd.addNumericField("iterations", (double) iterations, 0);
            gd.addNumericField("k", k, 2);
            gd.showDialog();
            if (gd.wasCanceled())
                return false;
            lambda = gd.getNextNumber();
            iterations = (int) gd.getNextNumber();
            k = gd.getNextNumber();
            return true;
      }
      public void compute3D()
      {
            // create a buffer volume.
            VolumeFloat nv = new VolumeFloat(v.getWidth(), v.getHeight(), v.getDepth());
            nv.setEdge(1);
            v.setEdge(1);
            VolumeFloat t, v1, v2;

            v1 = v;
            v2 = nv;
            IJ.showStatus("3D Nonlinear diffusion (lambda "+lambda+" k "+k+" n "+iterations+")");
            for (int i = 0; i < iterations; i++)
            {
                  IJ.showProgress((double)i/(double) iterations);
                  for (int z = 0; z < v.getDepth(); z++)
                  {
                        for (int y = 0; y < v.getHeight(); y++)
                              for (int x = 0; x < v.getWidth(); x++)
                              {
                                      if (v1.valid(x, y, z))
                                      {
                                              // Compute the gradients.
                                              double n = dN(v1, x, y, z);
                                              double s = dS(v1, x, y, z);
                                              double e = dE(v1, x, y, z);
                                              double w = dW(v1, x, y, z);
                                              double u = dU(v1, x, y, z);
                                              double d = dD(v1, x, y, z);
                                              double cN, cS, cW, cE, cU, cD;
                                              if (scaleSpaceContrast)
                                              {
                                                    // Compute the diffusion coefficients.
                                                    cN = gScaleSpace(Math.abs(n), k);
                                                    cS = gScaleSpace(Math.abs(s), k);
                                                    cE = gScaleSpace(Math.abs(e), k);
                                                    cW = gScaleSpace(Math.abs(w), k);
                                                    cU = gScaleSpace(Math.abs(u), k);
                                                    cD = gScaleSpace(Math.abs(d), k);
                                                }
                                                else
                                        {
                                                    // Compute the diffusion coefficients.
                                                  cN = g(Math.abs(n), k);
                                                  cS = g(Math.abs(s), k);
                                                  cE = g(Math.abs(e), k);
                                                  cW = g(Math.abs(w), k);
                                                  cU = g(Math.abs(u), k);
                                                    cD = g(Math.abs(d), k);
}
                                        // diffuse the voxel.
                                        v2.v[z][y][x] = (float) (v1.v[z][y][x] +
                        lambda * (cN * n + cS * s + cE * e + cW * w + cU * u + cD * d));
                                      }
                                      else
                                          v2.v[z][y][x] = 0;
                              }
                  }
                  // swap temporary volumes.
                  t = v1; v1 = v2; v2 = t;
            }
            if (iterations % 2 != 0)
                  // odd number of iterations. swap the temporary volume back to v.
                  for (int z = 0; z < v.getDepth(); z++)
                        for (int y = 0; y < v.getHeight(); y++)
                              for (int x = 0; x < v.getWidth(); x++)
                                      v.v[z][y][x] = v1.v[z][y][x];
      }
      public boolean params4D(ImagePlus imp)
      {
            GenericDialog gd = new GenericDialog("4D diffusion", imp.getWindow());
            iterations = 100;   // much larger default value.
            gd.addNumericField("lambda", lambda, 2);
            gd.addNumericField("iterations", (double) iterations, 0);
            gd.addNumericField("k", k, 2);
            gd.addNumericField("3D depth", depth, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                return false;
            lambda = gd.getNextNumber();
            iterations = (int) gd.getNextNumber();
            k = gd.getNextNumber();
            depth = (int) gd.getNextNumber();
            hv = new HyperVolume(imp.getStack(), depth);
            return true;
      }
      public void compute4D()
      {
            // create a buffer volume.
            HyperVolume nv = new HyperVolume(hv.getWidth(), hv.getHeight(), hv.getDepth(), hv.getLength());
            nv.setEdge(1);
            hv.setEdge(1);

            HyperVolume v1 = hv;
            HyperVolume v2 = nv;
            IJ.showStatus("4D Nonlinear diffusion (lambda "+lambda+" k "+k+" n "+iterations+")");
            for (int i = 0; i < iterations; i++)
            {
                  IJ.showProgress((double)i/(double) iterations);
                  for (int t = 0; t < hv.getLength(); t++)
                        for (int z = 0; z < hv.getDepth(); z++)
                              for (int y = 0; y < hv.getHeight(); y++)
                                    for (int x = 0; x < hv.getWidth(); x++)
                                    {
					                                  if (v1.valid(x, y, z, t))
                                            {
                                                  // Compute the gradients.
                                                  double n = dN(v1, x, y, z, t);
					                                        double s = dS(v1, x, y, z, t);
					                                        double e = dE(v1, x, y, z, t);
					                                        double w = dW(v1, x, y, z, t);
					                                        double u = dU(v1, x, y, z, t);
					                                        double d = dD(v1, x, y, z, t);
					                                        double b = dB(v1, x, y, z, t);
					                                        double a = dA(v1, x, y, z, t);
                                                  double cN, cS, cW, cE, cU, cD, cB, cA;
                                                  if (scaleSpaceContrast)
                                                  {
                                                        // Compute the diffusion coefficients.
					                                              cN = gScaleSpace(Math.abs(n), k);
					                                              cS = gScaleSpace(Math.abs(s), k);
					                                              cE = gScaleSpace(Math.abs(e), k);
					                                              cW = gScaleSpace(Math.abs(w), k);
					                                              cU = gScaleSpace(Math.abs(u), k);
					                                              cD = gScaleSpace(Math.abs(d), k);
					                                              cB = gScaleSpace(Math.abs(b), k);
					                                              cA = gScaleSpace(Math.abs(a), k);
                                                  }
                                                  else
                                                  {
                                                        // Compute the diffusion coefficients.
					                                              cN = g(Math.abs(n), k);
					                                              cS = g(Math.abs(s), k);
					                                              cE = g(Math.abs(e), k);
					                                              cW = g(Math.abs(w), k);
					                                              cU = g(Math.abs(u), k);
                                                        cD = g(Math.abs(d), k);
                                                        cB = g(Math.abs(b), k);
                                                        cA = g(Math.abs(a), k);
                                                  }
                                                  // diffuse the voxel.
                                                  v2.hv[t][z][y][x] = (float) (v1.hv[t][z][y][x] +
                        lambda * (cN * n + cS * s + cE * e + cW * w + cU * u + cD * d + cB * b + cA * a));
                                              }
                                              else
                                                    v2.hv[t][z][y][x] = 0;
                                      }
                  // swap temporary volumes.
                  HyperVolume vt = v1; v1 = v2; v2 = vt;
            }
            if (iterations % 2 != 0)
                  // odd number of iterations. swap the temporary volume back to v.
                  for (int t = 0; t < hv.getLength(); t++)
                        for (int z = 0; z < hv.getDepth(); z++)
                              for (int y = 0; y < hv.getHeight(); y++)
                                    for (int x = 0; x < hv.getWidth(); x++)
                                          hv.hv[t][z][y][x] = v1.hv[t][z][y][x];
      }
      public void intoStack(ImageStack is)
      {
              if (v instanceof VolumeFloat)
                    v.intoStack(is);
              else if (hv instanceof HyperVolume)
                    hv.intoStack(is);
      }
      protected static double gScaleSpace(double a, double k)
      // The diffusion transfer function with scale space.
      {
              return (Math.exp(- Math.pow(a/k, 2)));
      }
      protected static double g(double grad, double k)
      // The diffusion transfer function.
      {
              return (1.0 / (1.0 + Math.pow(grad/k, 2)));
      }
      protected static double dN(VolumeFloat v, int x, int y, int z)
      // Return northern nearest neighborhood difference.
      {     return (v.v[z][y-1][x] - v.v[z][y][x]);   }
      protected static double dS(VolumeFloat v, int x, int y, int z)
      // Return southern nearest neighborhood difference.
      {     return (v.v[z][y+1][x] - v.v[z][y][x]);   }
      protected static double dE(VolumeFloat v, int x, int y, int z)
      // Return eastern nearest neighborhood difference.
      {     return (v.v[z][y][x+1] - v.v[z][y][x]);   }
      protected static double dW(VolumeFloat v, int x, int y, int z)
      // Return western nearest neighborhood difference.
      {     return (v.v[z][y][x-1] - v.v[z][y][x]);   }
      protected static double dU(VolumeFloat v, int x, int y, int z)
      // Return above nearest neighborhood difference.
      {     return (v.v[z-1][y][x] - v.v[z][y][x]);   }
      protected static double dD(VolumeFloat v, int x, int y, int z)
      // Return below nearest neighborhood difference.
      {     return (v.v[z+1][y][x] - v.v[z][y][x]);   }
      protected static double dN(HyperVolume v, int x, int y, int z, int t)
      // Return northern nearest neighborhood difference.
      {     return (v.hv[t][z][y-1][x] - v.hv[t][z][y][x]);   }
      protected static double dS(HyperVolume v, int x, int y, int z, int t)
      // Return southern nearest neighborhood difference.
      {     return (v.hv[t][z][y+1][x] - v.hv[t][z][y][x]);   }
      protected static double dE(HyperVolume v, int x, int y, int z, int t)
      // Return eastern nearest neighborhood difference.
      {     return (v.hv[t][z][y][x+1] - v.hv[t][z][y][x]);   }
      protected static double dW(HyperVolume v, int x, int y, int z, int t)
      // Return western nearest neighborhood difference.
      {     return (v.hv[t][z][y][x-1] - v.hv[t][z][y][x]);   }
      protected static double dU(HyperVolume v, int x, int y, int z, int t)
      // Return above nearest neighborhood difference.
      {     return (v.hv[t][z-1][y][x] - v.hv[t][z][y][x]);   }
      protected static double dD(HyperVolume v, int x, int y, int z, int t)
      // Return below nearest neighborhood difference.
      {     return (v.hv[t][z+1][y][x] - v.hv[t][z][y][x]);   }
      protected static double dB(HyperVolume v, int x, int y, int z, int t)
      // Return before nearest neighborhood difference.
      {     return (v.hv[t-1][z][y][x] - v.hv[t][z][y][x]);   }
      protected static double dA(HyperVolume v, int x, int y, int z, int t)
      // Return after nearest neighborhood difference.
      {     return (v.hv[t+1][z][y][x] - v.hv[t][z][y][x]);   }
}

