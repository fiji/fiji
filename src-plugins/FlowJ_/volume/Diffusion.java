package volume;
import ij.*;
import ij.process.*;
import ij.gui.*;

/**
 * This is a class that implements nonlinear diffusion in 2D.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
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
public class Diffusion
{
      protected static int iterations=10;
      protected static float k=2;
      protected static float lambda = 1.0f;
      protected ImagePlus imp;
      protected float [] result = null;
      protected boolean scaleSpaceContrast=false;

      public Diffusion(ImagePlus imp)
      {
             this.imp = imp;
      }
      public boolean params()
      {
            GenericDialog gd = new GenericDialog("Nonlinear diffusion", imp.getWindow());
            gd.addNumericField("lambda", lambda, 2);
            gd.addNumericField("iterations", (float) iterations, 0);
            gd.addNumericField("k", k, 2);
            gd.showDialog();
            if (gd.wasCanceled())
                  return false;
            lambda = (float) gd.getNextNumber();
            iterations = (int) gd.getNextNumber();
            k = (float) gd.getNextNumber();
            return true;
      }
      public void compute()
      {
            IJ.showStatus("Nonlinear diffusion (lambda "+lambda+" k "+k+" n "+iterations+")");
	    FloatProcessor fp = (FloatProcessor) imp.getProcessor().convertToFloat();
	    int width = fp.getWidth();
	    float [] source = (float []) fp.getPixels();
	    float [] target = new float[source.length];
	    for (int i = 0; i < iterations; i++)
            {
                  IJ.showProgress((float)i / (float) iterations);
                  for (int y = 1; y < fp.getHeight()-1; y++)
                  for (int x = 1; x < fp.getWidth()-1; x++)
                  {
			// Compute the gradients.
			float n = dN(source, width, x, y);
			float s = dS(source, width, x, y);
			float e = dE(source, width, x, y);
			float w = dW(source, width, x, y);
		        float cN, cS, cW, cE;
			if (scaleSpaceContrast)
			{
				// Compute the diffusion coefficients.
				cN = gScaleSpace(Math.abs(n), k);
				cS = gScaleSpace(Math.abs(s), k);
				cE = gScaleSpace(Math.abs(e), k);
				cW = gScaleSpace(Math.abs(w), k);
			}
		        else
			{
				// Compute the diffusion coefficients.
				cN = g(Math.abs(n), k);
				cS = g(Math.abs(s), k);
				cE = g(Math.abs(e), k);
				cW = g(Math.abs(w), k);
			}
			// diffuse the voxel.
			float value = source[width*y + x];
		        value += lambda * (cN * n + cS * s + cE * e + cW * w );
			target[width*y+x] = value;
		}
		// Switch target and source images.
		float [] t = source;
		source = target;
		target = t;
            }
	    // already swapped at end.
	    result = source;
      }
      protected static float gScaleSpace(float a, float k)
      // The diffusion transfer function with scale space.
      {
              return (float) (Math.exp(- Math.pow(a/k, 2)));
      }
      protected static float g(float grad, float k)
      // The diffusion transfer function.
      {
              return (float) (1.0f / (1.0f + Math.pow(grad/k, 2)));
      }
      protected static float dN(float [] source, int width, int x, int y)
      // Return northern nearest neighborhood difference.
      {
	      float p1 = source[(y-1)*width+x];
	      float p2 = source[(y)*width+x];
	      return p1 - p2;
      }
      protected static float dS(float [] source, int width, int x, int y)
      // Return southern nearest neighborhood difference.
      {
	      float p1 = source[(y+1)*width+x];
	      float p2 = source[(y)*width+x];
	      return p1 - p2;
      }
      protected static float dE(float [] source, int width, int x, int y)
      // Return eastern  nearest neighborhood difference.
      {
	      float p1 = source[(y)*width+(x+1)];
	      float p2= source[(y)*width+(x)];
	      return p1 - p2;
      }
      protected static float dW(float [] source, int width, int x, int y)
      // Return western nearest neighborhood difference.
      {
	      float p1 = source[(y)*width+(x-1)];
	      float p2 = source[(y)*width+(x)];
	      return p1 - p2;
      }
      public ImageProcessor getProcessor()
      {
	        return new FloatProcessor(imp.getProcessor().getWidth(),
		        imp.getProcessor().getHeight(), result, null); }
}

