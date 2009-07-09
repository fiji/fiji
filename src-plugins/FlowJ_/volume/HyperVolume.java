package volume;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

/**
 * This is a class that implements a float hyper (4D) volumes and operations including
 * convolutions on it. A hypervolume has dimensions width*height*depth*length, with convenient
 * indices of x,y,z,t.
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
public class HyperVolume extends Volume implements java.io.Serializable
{
      public float [][][][]         hv;       // a 4D hypervolume of volumes.
      protected int                 length;   // the 4th dimension of the volume

      /**
       * Creates the null hypervolume.
        */
      public HyperVolume()
      { hv = null; }
      /** Creates a hypervolume with width, height, depth, length */
      public HyperVolume(int width, int height, int depth, int length)
      {
            this.width = width; this.height = height; this.depth = depth; this.length = length;
            hv = new float[length][depth][height][width];
            edge = 0;
      }
      /**
       * Create a new hypervolume from a stack of volumes. No convolution.
       * Each volume has a depth of depth (slices).
       * @param is and ImageJ ImageStack containing the slices.
       * @param depth the depth of each volume in the hypervolume.
      */
      public HyperVolume(ImageStack is, int depth)
      {
			this(is.getWidth(), is.getHeight(), depth, is.getSize() / depth);
			load(is, 0);
      }
      /**
       * Create a new hypervolume from a stack of volumes. No convolution.
       * Each volume has a depth of depth (slices).
       * @param is and ImageJ ImageStack containing the slices.
       * @param depth the depth of each volume in the hypervolume.
      */
      /**
       * Creates a hypervolume depthxn from a stack containing n volumes of depth depth and
       * convolve around center with a filtering kernel on the fly.
       * All n volumes will have depth slices.
       * @param is and ImageJ ImageStack containing the slices.
       * @param depth the depth of each volume in the hypervolume.
       * @param center the index of the slice around which to convolve.
       * @param n the number of volumes in this hypervolume.
       * @param kernel the convolution kernel.
      */
      public HyperVolume(ImageStack is, int depth, int center, int n, Kernel1D kernel)
      {
			this(is.getWidth(), is.getHeight(), depth, n);
			if (kernel instanceof Kernel1D && kernel.support() > 0)
				  loadConvolve(is, center, kernel);
            else
                  load(is, center);
      }
        /**
         * Get the vector value of the hypervolume that is at x,y,z.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @return a Number with the voxel value at x,y,z
         */
        public Object get(int x, int y, int z) { return (Object) hv[z][y][x]; }
        /**
         * Set the vector value of the hypervolume that is at x,y,z to value.
         * @value a Number with the voxel value at x,y,z
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z) { hv[z][y][x] = (float []) value; }
      /**
       * Convert this hypervolume to a stack. It scales automatically.
      */
      public ImageStack toStack()
      {
			ImageStack is = new ImageStack(width, height);
			for (int t = 0; t < length; t++)
					 for (int z = 0; z < depth; z++)
					 {
							// create a float processor.
							FloatProcessor fp = new FloatProcessor(width, height);
							float [] im = (float [])fp.getPixels();
							for (int y= 0; y < height; y++)
							{
								int offset = y * width;
								for (int x = 0; x < width; x++)
									  im[offset + x] = hv[t][z][y][x];
							}
							is.addSlice(""+t+":"+(z+1), fp);
					 }
		  	return is;
	  } // toStack
      /*
			Load a stack into this hypervolume centered at the VOLUME centralVolume in is.
			Then convolve with kernel on the fly.
      */
	  private void loadConvolve(ImageStack is, int centralVolume, Kernel1D kernel)
      {
                  float[][] t1 = new float[height][width];
				  // n volumes around the centrale volume in is.
				  if ((((centralVolume - length/2)-kernel.halfwidth)*depth) < 0 ||
						((centralVolume + length/2 + kernel.halfwidth+1)*depth) > is.getSize())
				  {
						IJ.error("loadConvolve: slice indexOutOfBounds error"
								+(((centralVolume - length/2)-kernel.halfwidth)*depth)+" - "+
								(((centralVolume + length/2)+kernel.halfwidth)*depth+depth));
						return;
				  }
				  for (int t = centralVolume - length/2; t <= centralVolume + length/2; t++)
                  {
                        /*
                              Around centralVolume, convolve over -kernel.halfwidth .. center .. +kernel.halfwidth.
                              For reasons of speed, calculate the contribution of each image
                              image by image instead of pixelwise.
                        */
                        for (int z = 0; z < depth; z++)
                              for (int y = 0; y < height; y++)
		  	                            for (int x = 0; x < width; x++)
												  hv[t-(centralVolume-length/2)][z][y][x] = 0;
						for (int z = 0; z < depth; z++)
								for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
								{
									  // dereference slice from t,k and z.
                                      loadSlice(t1, is, z + (t + k) * depth + 1);
                                      for (int y = 0; y < height; y++)
		  	                                    for (int x = 0; x < width; x++)
                                                    hv[t-(centralVolume-length/2)][z][y][x] += t1[y][x]*kernel.k[k+kernel.halfwidth];
                                }
				  }
				  edge = kernel.halfwidth;
      }
	  private void load(ImageStack is, int start)
	  // Load length volumes into this hypervolume from stack is starting at VOLUME start.
      {
	  			float[][] t1 = new float[height][width];

				for (int t = start; t < start + length; t++)
                        for (int z = 0; z < depth; z++)
                        {
                              // dereference slice from t and z.
							  loadSlice(t1, is, z + t * depth + 1);
                              for (int y = 0; y < height; y++)
		  	                            for (int x = 0; x < width; x++)
				  	                              hv[t-start][z][y][x] = t1[y][x];
						}
				edge = 0;
      }
	  public int getLength() { return length; }
      public boolean valid(int x, int y, int z, int t)
      { return (x >= edge && x < width-edge && y >= edge && y < height - edge && z >= edge && z < depth - edge && t >= edge && t < length - edge); }
      private int InitParams(HyperVolume hv, Kernel kernel)
      /* Check and prepare the various parameters. Return -1 if problem, otherwise the difference in depths. */
      {
            if (width < hv.getWidth() || height < hv.getHeight() || depth < hv.getDepth() || length > hv.getLength())
            {
                  IJ.error("hypervolume: convolution volume wrong size.");
                  return -1;
            }
            int lengthoffset = 0;
            if (length < hv.getLength())
                  lengthoffset = (hv.getLength() - length) / 2;
            edge = Math.max(hv.edge, edge);
			edge = Math.max(kernel.halfwidth, edge);
			return lengthoffset;
      }
      /**
          Compute separated 1d convolution: hv=hv1*kernel  in t dimension in place.
          Center around center of this volume.
      */
      public void convolvet(Kernel1D kernel)
      {
			edge = (int) Math.max(kernel.halfwidth, edge);
			double [] s = new double[length];
            for (int z = 0; z < depth; z++)
                  for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
                        {
                                      for (int t = 0; t < length; t++)
                                      {
                                              s[t] = 0;
                                              if (valid(x, y, z))
                                                    for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                          s[t] += hv[t+k][z][y][x]*kernel.k[k+kernel.halfwidth];
                                      }
                                      for (int t = 0; t < length; t++)
                                              hv[t][z][y][x] = (float) s[t];
                        }

      }
      /**
          Compute separated 1d convolution: hv=hv1*kernel  in t dimension.
          Center around center of this volume.
      */
      public void convolvet(HyperVolume hv1, Kernel1D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = -length/2; t <= length/2; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                          for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                s += hv1.hv[t+k+lengthoffset][z][y][x]*kernel.k[k+kernel.halfwidth];
                                    hv[t+length/2][z][y][x] = (float) s;
                              }

      }
      /**
          Compute separated 1d convolution: hv=hv1*kernel  in z dimension.
          Center around center of this hypervolume.
      */
      public void convolvez(HyperVolume hv1, Kernel1D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
			if (lengthoffset < 0) return;
            for (int t = 0; t < length; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                          for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                s += hv1.hv[t+lengthoffset][z+k][y][x]*kernel.k[k+kernel.halfwidth];
                                    hv[t][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 1d convolution: hv=hv1*kernel  in y dimension.
          Center around center of this volume.
      */
      public void convolvey(HyperVolume hv1, Kernel1D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = 0; t < length; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                          for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                s += hv1.hv[t+lengthoffset][z][y+k][x]*kernel.k[k+kernel.halfwidth];
                                    hv[t][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 1d convolution: v=v1*kernel  in y dimension.
          Center around center of this volume.
      */
      public void convolvex(HyperVolume hv1, Kernel1D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = 0; t < length; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                    {
                                          for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                s += hv1.hv[t+lengthoffset][z][y][x+k]*kernel.k[k+kernel.halfwidth];
                                    }
                                    hv[t][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 3d convolution: hv=hv1*kernel  in t dimension.
          Center around center of this volume.
          Disregard the z dimension.
      */
      public void convolvet(HyperVolume hv1, Kernel3D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = -length/2; t <= length/2; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                    {
                                          for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
                                              for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
                                                  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                      s += hv1.hv[t+lengthoffset+k][z][y+l][x+m]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth][k+kernel.halfwidth];
                                    }
                                    hv[t+length/2][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 3d convolution: hv=hv1*kernel  in z dimension.
          Center around center of this volume.
          Disregard the t dimension.
      */
      public void convolvez(HyperVolume hv1, Kernel3D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = -length/2; t <= length/2; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                    {
                                          for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
                                              for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
                                                  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                      s += hv1.hv[t+lengthoffset][z+k][y+l][x+m]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth][k+kernel.halfwidth];
                                    }
                                    hv[t+length/2][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 3d convolution: hv=hv1*kernel  in y dimension.
          Center around center of this volume.
          Disregard the t dimension.
      */
      public void convolvey(HyperVolume hv1, Kernel3D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = -length/2; t <= length/2; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                    {
                                          for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
                                              for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
                                                  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                      s += hv1.hv[t+lengthoffset][z+m][y+k][x+l]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth][k+kernel.halfwidth];
                                    }
                                    hv[t+length/2][z][y][x] = (float) s;
                              }
      }
      /**
          Compute separated 3d convolution: hv=hv1*kernel  in x dimension.
          Center around center of this volume.
          Disregard the t dimension.
      */
      public void convolvex(HyperVolume hv1, Kernel3D kernel)
      {
            int lengthoffset = InitParams(hv1, kernel);
            if (lengthoffset < 0) return;
            for (int t = -length/2; t <= length/2; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                              {
                                    double s = 0;
                                    if (valid(x, y, z))
                                    {
                                          for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
                                              for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
                                                  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                      s += hv1.hv[t+lengthoffset][z+m][y+l][x+k]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth][k+kernel.halfwidth];
                                    }
                                    hv[t+length/2][z][y][x] = (float) s;
                              }
      }
      /**
       * Convolve the hypervolume by kernel in xyz in place (so no extra hypervolume needed).
       */
      public void convolvexyz(Kernel1D kernel)
      {
            if (kernel.halfwidth > edge) edge = kernel.halfwidth;
            for (int t = 0; t < length; t++)
            {
                    VolumeFloat t1 = new VolumeFloat(width, height, depth);
                    VolumeFloat t2 = new VolumeFloat(width, height, depth);
                    // Convolve in x direction.
		        	      for (int z = 0; z < depth; z++)
                          for (int y = 0; y < height; y++)
								for (int x = 0; x < width; x++)
                                {
                                      double s = 0;
                                      if (valid(x, y, z))
									  {
                                              // Around x, convolve over -kernel.halfwidth ..x.. +kernel.halfwidth.
					                                    for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                    s += hv[t][z][y][x+k]*kernel.k[k+kernel.halfwidth];
                                      }
                                      t1.v[z][y][x] = (float) s;
                                }
						  // Convolve in y direction.
		        	      for (int z = 0; z < depth; z++)
						  for (int y = 0; y < height; y++)
								for (int x = 0; x < width; x++)
								{
										double s = 0;
										if (valid(x, y, z))
										{
												// Around y, convolve over -kernel.halfwidth ..y.. +kernel.halfwidth.
												for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
													s += t1.v[z][y+k][x]*kernel.k[k+kernel.halfwidth];
										}
										t2.v[z][y][x] = (float) s;
								}
						  // Convolve in z direction.
		        	      for (int z = 0; z < depth; z++)
                          for (int y = 0; y < height; y++)
						  		for (int x = 0; x < width; x++)
                                {
                                      double s = 0;
                                      if (valid(x, y, z))
									  {
                                              // Around z, convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth.
											  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                    s += t2.v[z+k][y][x]*kernel.k[k+kernel.halfwidth];
                                      }
                                      hv[t][z][y][x] = (float) s;
                                }
            }
      } // convolvexyz
      /** Multiply all voxels in this hypervolume by a constant.*/
      public void mul(double constant)
      {
            for (int t = 0; t < length; t++)
                  for (int z = 0; z < depth; z++)
		                    for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                                    hv[t][z][y][x] *= constant;
      }
      /** Multiply all voxels in this hypervolume by the voxels in b (NOT the inner product).*/
      public void mul(HyperVolume a, HyperVolume b)
      {
            if (width != a.getWidth() || height != a.getHeight() || depth != a.getDepth() || length != a.getLength()
                || width != b.getWidth() || height != b.getHeight() || depth != b.getDepth() || length != a.getLength())
                IJ.error("mul undefined.");
            for (int t = 0; t < length; t++)
		              for (int z = 0; z < depth; z++)
                        for (int y = 0; y < height; y++)
 		  	                      for (int x = 0; x < width; x++)
                                    hv[t][z][y][x] =  (float) ((double) a.hv[t][z][y][x] * (double) b.hv[t][z][y][x]);
      }
		private void loadSlice(float [][] i, ImageStack s, int io)
		// Load a slice from stack s into image i. Convert different image types.
		{
            if (io < 1 || io > s.getSize())
            {
			            IJ.error("loadSlice: slice index out of bounds (" + io+"><1-"+s.getSize()+")");
			            return;
            }
			ImageProcessor ip = s.getProcessor(io);
			if (ip instanceof ColorProcessor)
						loadPixels(i, (ColorProcessor) ip, s.getWidth(), s.getHeight());
			else if (ip instanceof ByteProcessor)
						loadPixels(i, (ByteProcessor) ip, s.getWidth(), s.getHeight());
			else if (ip instanceof ShortProcessor)
						loadPixels(i, (ShortProcessor) ip, s.getWidth(), s.getHeight());
			else if (ip instanceof FloatProcessor)
						loadPixels(i, (FloatProcessor) ip, s.getWidth(), s.getHeight());
		}
		private void loadPixels(float [][] i, ColorProcessor cp, int width, int height)
		{
				int [] ii = (int []) cp.getPixels();
				// RGB 32 bit image image deserves separate treatment. Take brightness from RGB value.
				for (int y = 0; y < height; y++)
				{
						  int offset = y * width;
						  for (int x = 0; x < width; x++)
						  {
								  int c = ii[offset + x];
								  int r = (c&0xff0000)>>16;
								  int g = (c&0xff00)>>8;
								  int b = c&0xff;
								  i[y][x] = (float) (r*0.30 + g*0.59 + b*0.11);
						  }
				}
		}
		private void loadPixels(float [][] i, ByteProcessor bp, int width, int height)
		{
					byte [] b = (byte []) bp.getPixels();
				  for (int y = 0; y < height; y++)
				  {
						int offset = y * width;
						for (int x = 0; x < width; x++)
							  i[y][x] = (float) (b[offset + x] & 0xff);
				  }
		}
		private void loadPixels(float [][] i, ShortProcessor sp, int width, int height)
		{
					short [] u = (short []) sp.getPixels();
				  for (int y = 0; y < height; y++)
				  {
						int offset = y * width;
						for (int x = 0; x < width; x++)
                              i[y][x] = (float) u[offset + x];
				  }
		}
		private void loadPixels(float [][] i, FloatProcessor fp, int width, int height)
		{
					float [] f = (float []) fp.getPixels();
				  for (int y = 0; y < height; y++)
				  {
						int offset = y * width;
						for (int x = 0; x < width; x++)
                              i[y][x] = f[offset + x];
				  }
		}
        /**
         * Transfer hv into stack is.
         */
	    public void intoStack(ImageStack is)
   {
		  // Find min and max.
		  float min = Float.MAX_VALUE;
		  float max = -Float.MAX_VALUE;
		  for (int t = 0; t < length; t++)
				for (int z = 0; z < depth; z++)
					  for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
							{
									if (min > hv[t][z][y][x])
                                          min = hv[t][z][y][x];
                                    if (max < hv[t][z][y][x])
                                          max = hv[t][z][y][x];
                            }
          float scale = 1 / (max - min);
          Object [] stack = is.getImageArray();
          for (int t = 0; t < length; t++)
              for (int z = 0; z < depth; z++)
              {
                    // Determine type of image in stack.
                    if (stack[0] instanceof byte[])
                    {
                          byte b[] = (byte[]) is.getPixels(t * depth + z+1);
                          for (int y = 0; y < height; y++)
                          {
                                  int offset = y * width;
                                  for (int x = 0; x < width; x++)
                                        b[offset + x] = (byte) (((hv[t][z][y][x]-min)*scale) * 255);
                          }
                    }
                    else if (stack[0] instanceof short[])
                    {
                          short u[] = (short[]) is.getPixels(t * depth + z+1);
                          for (int y = 0; y < height; y++)
                          {
                                int offset = y * width;
                                for (int x = 0; x < width; x++)
                                      u[offset + x] = (short) (((hv[t][z][y][x]-min)*scale) * (float) (0x7fff));
                          }
                    }
                    else if (stack[0] instanceof float[])
                    {
                          float f[] = (float[]) is.getPixels(t * depth + z+1);
                          for (int y = 0; y < height; y++)
                          {
                                int offset = y * width;
                                for (int x = 0; x < width; x++)
                                      f[offset + x] = hv[t][z][y][x];
                          }
                    }
                    else if (stack[0] instanceof int[])
                    {
                            // RGB 32 bit image image deserves separate treatment.
                            int[] ii = (int[]) is.getPixels(t * depth + z+1);
                            for (int y = 0; y < height; y++)
                            {
                                    int offset = y * width;
                                    for (int x = 0; x < width; x++)
                                    {
                                            int b = (int) (((hv[t][z][y][x]-min)*scale) * 255);
                                            ii[offset + x] = (b << 16) | (b << 8) | b;
                                    }
                            }
                    }
                }
    }
    public String toString()
    { return "Hypervolume: "+width+"x"+height+"x"+depth+"x"+length; }
}
