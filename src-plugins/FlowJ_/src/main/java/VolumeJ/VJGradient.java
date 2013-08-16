package VolumeJ;
/**
 * This is a class that defines the gradient of a voxel.
 * This means that a gradient can be any derivative of the voxel or its neighborhood.
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
public class VJGradient
{
		// Defines gradient both for scalar and vector (max 3 dimensional) voxel volumes
		private float 				mag0, mag1, mag2;
		// Gradients in x-direction.
		private float 				g0x, g0y, g0z;
		// Gradients in y-direction.
		private float				g1x, g1y, g1z;
		// Gradients in z-direction.
		private float				g2x, g2y, g2z;
		private int					dimensions;
		private static final double defaultEpsilon = 0.00000001;

		public VJGradient(double [] g)
		/* Gradient of scalar volume. */
		{
				this(g[0], g[1], g[2]);
				dimensions = 1;
		}
		/**
                 * Gradient of scalar volume.
                 * @param gx, gy, gz the gradients in x, y, and z-dimensions.
                 */
                public VJGradient(double gx, double gy, double gz)
		{
                        g0x = (float) gx; g0y = (float) gy; g0z = (float) gz;
			mag0 = magnitude(g0x, g0y, g0z); mag1 = 0; mag2 = 0;
			dimensions = 1;
		}
		/**
                 * Gradient of scalar volume.
                 * @param gx, gy, gz the gradients in x, y, and z-dimensions.
                 */
 		public VJGradient(float gx, float gy, float gz)
		{
			g0x = gx; g0y = gy; g0z = gz;
			mag0 = magnitude(g0x, g0y, g0z); mag1 = 0; mag2 = 0;
			dimensions = 1;
		}
		/**
                 *  n-dimensional vector volume gradient.
                 *
                */
		public VJGradient(double [][] g)
		{
                        dimensions = g.length;
			if (dimensions > 3)
			{
                                VJUserInterface.write("error: too many dimensions for gradient.");
                                return;
                        }
			// gradients for first dimension of the vector.
			g0x = (float) g[0][0]; g0y = (float) g[0][1]; g0z = (float) g[0][2];
			// gradients for second dimension of the vector.
			g1x = (float) g[1][0]; g1y = (float) g[1][1]; g1z = (float) g[1][2];
			mag0 = magnitude(g0x, g0y, g0z);
			mag1 = magnitude(g1x, g1y, g1z);
			if (dimensions == 3)
			{
				// three-dimensional vector.
				g2x = (float) g[2][0]; g2y = (float) g[2][1]; g2z = (float) g[2][2];
				mag2 = magnitude(g2x, g2y, g2z);
			}
		}
		public float getx() { return g0x; }
		public float gety() { return g0y; }
		public float getz() { return g0z; }
		public float getmag() { return mag0; }
		public float getx(int i) { switch (i) { case 0: return g0x; case 1: return g1x; default: return g2x; } }
		public float gety(int i) { switch (i) { case 0: return g0y; case 1: return g1y; default: return g2y; } }
		public float getz(int i) { switch (i) { case 0: return g0z; case 1: return g1z; default: return g2z; } }
		public float getmag(int i) { switch (i) { case 0: return mag0; case 1: return mag1; default: return mag2; } }
		// Only valid for vector gradients.
		public float getAverageMag() { if (dimensions == 3) return (mag0 + mag1 + mag2) / 3.0f; else return mag0; }
		public int getDimensions() { return dimensions; }
		public void normalize()
		{
              // Normalize the gradient. (Scalar or first dimension of vector.
			  if (mag0 > defaultEpsilon)
			  {
					  g0x /= mag0;
					  g0y /= mag0;
					  g0z /= mag0;
			  }
			  else { g0x = 0; g0y = 0; g0z = 0; }
			  // Second dim.
			  if (mag1 > defaultEpsilon)
			  {
					  g1x /= mag1;
					  g1y /= mag1;
					  g1z /= mag1;
			  }
			  // Third dim
			  if (mag2 > defaultEpsilon)
			  {
					  g2x /= mag2;
					  g2y /= mag2;
					  g2z /= mag2;
			  }
		}
		private float magnitude(double gx, double gy, double gz)
		{
			  return (float) Math.sqrt(gx*gx+gy*gy+gz*gz);
		}
		public String toString()
		{
				return ""+ij.IJ.d2s(mag0,2)+":"
						+ij.IJ.d2s(g0x,2)+","+ij.IJ.d2s(g0y,2)+","+ij.IJ.d2s(g0z,2);
		}
}
