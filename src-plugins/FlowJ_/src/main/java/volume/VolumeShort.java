package volume;
import java.io.*;
import ij.*;
import ij.process.*;

/**
 * This class implements short volumes and operations including
 * convolutions on it.
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
public class VolumeShort extends Volume
{
        public short [][][]      	v;
        private boolean                 indexed = false;

        /** creates the null volume. */
        public VolumeShort() { this(0,0,0); }
        /**
        * Creates a short volume of defined size and aspect ratio.
        * @param width, height, depth the dimensions of the volume
        * @param aspectx, aspecty, aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeShort(int width, int height, int depth, double aspectx, double aspecty, double aspectz)
        {
                this.width = width; this.height = height; this.depth = depth;
                v = new short[depth][height][width];
                setAspects(aspectx, aspecty, aspectz);
                edge = 0;
        }
        /**
        * Creates a short volume of defined size and default aspect ratios.
        * @param width, height, depth the dimensions of the volume
        */
        public VolumeShort(int width, int height, int depth)
        {
                this(width, height, depth, 1, 1, 1);
        }
        /**
        * Creates a float volume which is an exact copy of vl.
        * @param vl the VolumeFloat to be copied.
        */
        public VolumeShort(VolumeShort v1)
        {
                this(v1.getWidth(), v1.getHeight(), v1.getDepth(), v1.getAspectx(), v1.getAspecty(), v1.getAspectz());
                copy(v1.v);
        }
        public VolumeShort(ImageStack s, double aspectx, double aspecty, double aspectz)
        throws Exception
      // Creates a volume from an ImageStack.
      {
                this(s.getWidth(), s.getHeight(), s.getSize(), aspectx, aspecty, aspectz);
		if (! load(s, 0))
                        throw new Exception("Cannot instantiate volume from ImageStack");
      }
        /**
        * Creates a short volume from an ImageJ ImageStack.
        * @param s the ImageStack which has to contain at least one image.
        */
        public VolumeShort(ImageStack s)
        throws Exception
        {
                this(s, 1, 1, 1);
        }
        /** Creates a volume with depth 1 from an ImageProcessor.
         * Only needed for of3d library.
         * @deprecated
         * */
          public VolumeShort(ImageProcessor ip)
          {
			this(ip.getWidth(), ip.getHeight(), 1, 1, 1, 1);
			load(ip, 0);
	  }
 	  public VolumeShort(ImageStack s, int depth, int n)
      /*
           Creates a volume from an ImageStack,
           with depth slices from n * depth.
           This is used to extract n-th volume from an ImageStack representing a
           hypervolume (multiple volumes in one stack).
      */
      {
			this(s, depth, n, 1, 1, 1);
      }
      public VolumeShort(ImageStack s, int depth, int n, double aspectx, double aspecty, double aspectz)
      /*
           Creates a volume from an ImageStack,
           with depth slices from n * depth.
           This is used to extract n-th volume from an ImageStack representing a
           hypervolume (multiple volumes in one stack).
      */
      {
            this(s.getWidth(), s.getHeight(), depth, aspectx, aspecty, aspectz);
            load(s, n * depth);
      }
        /**
         * Get the voxel value as a Number.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @return a Number with the voxel value at x,y,z
         */
        public Object get(int x, int y, int z) { return new Short(v[z][y][x]); }
        /**
         * Set the voxel value to a Number.
         * @value a Number with the voxel value at x,y,z
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z) { v[z][y][x] = ((Number) value).shortValue(); }
        /**
         * @deprecated
         */
        public void set(int x, int y, int z, int value) { v[z][y][x] = (short) value; }
        /**
         * Fill this volume from stack s with from slice start.
         * @param s an ImageJ ImageStack
         * @param start the first slice to be loaded (starting with slice 1).
         * @return true if could be loaded, false if wrong volume type.
        */
        public boolean load(ImageStack s, int start)
        {
                if (! (s.getImageArray()[0] instanceof byte[] || s.getImageArray()[0] instanceof short []))
                {
                        IJ.error("VolumeShort: can only convert 8- and 16-bit images.");
                        return false;
                }
                for (int t = start; t < start + depth; t++)
                        loadSlice(v[t-start], s, t+1);
                return true;
        }
        /** Fill the volume with depth 1 with a single image ip. */
        public void load(ImageProcessor ip, int t)
      {
			if (ip instanceof ByteProcessor)
				   loadImage(v[t], (byte []) ip.getPixels());
			else if (ip instanceof ShortProcessor)
				   loadImage(v[t], (short []) ip.getPixels());
			else
				   IJ.error("load: image type not supported.");
	  }
      /** Inversely load a volume from the stack centered around center with the first one last. */
	  public void loadInverse(ImageStack s, int center)
      {
          for (int t = -depth/2; t <= depth/2; t++)
          {
                short[][] t1 = new short[height][width];
                loadSlice(t1, s, center + depth/2 - t);
				for (int y = 0; y < height; y++)
						for (int x = 0; x < width; x++)
							v[t+depth/2][y][x] = t1[y][x];
		  }
	  }
	  /** Load a slice io from stack s into image i. */
	  protected void loadSlice(short [][] i, ImageStack s, int io)
	  {
			if (io < 1 || io > s.getSize())
			{
				IJ.error("loadSlice: slice index out of bounds (" + io+"><1-"+s.getSize()+")");
				return;
			}
			Object [] stack = s.getImageArray();
			// Determine type of image in stack.
			if (stack[0] instanceof byte[])
				   loadImage(i, (byte []) s.getPixels(io));
	  		else if (stack[0] instanceof short[])
				   loadImage(i, (short []) s.getPixels(io));
			else
				   IJ.error("loadSlice: image type not supported.");
	  }
        /** Load an image b into i. */
        protected void loadImage(short [][] i, byte [] b)
        {
                for (int y = 0; y < height; y++)
                {
                        int offset = y * width;
                        for (int x = 0; x < width; x++)
                                i[y][x] = (short) (b[offset + x] & 0xff);
                }
        }
        /** Load an image sh into i. */
        protected void loadImage(short [][] i, short [] sh)
        {
                for (int y = 0; y < height; y++)
                {
                        int offset = y * width;
                        for (int x = 0; x < width; x++)
                                i[y][x] = (short) sh[offset + x];
                }
	  }
        /**
         * Combine the values of s into the index byte (high 8 bits) of the
         * voxel scalars of this volume.
         * @param s an ImageStack containing the index values.
        */
        public void setHighBits(ImageStack s)
        {
                for (int t = 0; t < depth; t++)
                {
                        byte [] pixels = (byte []) s.getPixels(t+1);
                        for (int y = 0; y < height; y++)
                                for (int x = 0; x < width; x++)
                                        v[t][y][x] |= (pixels[y*width+x]<<8)&0x0000ff00;
                }
                indexed = true;
        }
        public void setIndexed(boolean indexed) { this.indexed = indexed; }
        public boolean getIndexed() { return indexed; }
        /**
         * XYZ convolution with separated 1D kernel.
         * @param kernel a 1D convolution kernel.
         */
        public void convolvexyz(Kernel1D kernel)
        // 1D xyz convolution.
        {
                if (kernel.halfwidth > edge) edge = kernel.halfwidth;
                // copy into buffer volume t1.
                VolumeShort t1 = new VolumeShort(this);
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
                                        s += ((int) t1.v[z][y][x+k])*kernel.k[k+kernel.halfwidth];
                        }
                        v[z][y][x] = (short) s;
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
                                        s += ((int) v[z][y+k][x])*kernel.k[k+kernel.halfwidth];
                        }
                        v[z][y][x] = (short) s;
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
                                        s += ((int) t1.v[z+k][y][x])*kernel.k[k+kernel.halfwidth];
                        }
                        v[z][y][x] = (short) s;
	        }
        } // convolvexyz
	private void copy(short [][][] v1)
	// Copy float array fv to this volume.
        {
	        for (int t = 0; t < depth; t++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                        v[t][y][x] = v1[t][y][x];
        }
}
