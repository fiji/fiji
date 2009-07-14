package volume;
import java.io.*;
import ij.*;
import ij.process.*;

/**
 * This class implements int volumes and operations including
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
public class VolumeInteger extends Volume
{
        public int [][][]      	v;
        // creates the null volume.
        public VolumeInteger() { this(0,0,0); }
        public VolumeInteger(int width, int height, int depth)
        // Creates a float volume with width, height, depth
        {
                this(width, height, depth, 1, 1, 1);
        }
        public VolumeInteger(int width, int height, int depth, double aspectx, double aspecty, double aspectz)
        // Creates an int volume with width, height, depth and aspect ratios.
        {
                this.width = width; this.height = height; this.depth = depth;
                v = new int[depth][height][width];
                setAspects(aspectx, aspecty, aspectz);
                edge = 0;
        }
        public VolumeInteger(ImageStack s, double aspectx, double aspecty, double aspectz)
        // Creates a volume from an ImageStack.
        {
                this(s.getWidth(), s.getHeight(), s.getSize(), aspectx, aspecty, aspectz);
                load(s, 0);
        }
        public VolumeInteger(ImageStack s)
        // Creates a volume from an ImageStack.
        {
                this(s, 1, 1, 1);
        }
        public VolumeInteger(ImageProcessor ip)
        // Creates a volume with depth 1 from an ImageProcessor.
        {
                this(ip.getWidth(), ip.getHeight(), 1, 1, 1, 1);
                load(ip, 0);
        }
        /**
         * Creates a volume from an ImageStack, with depth slices from n * depth.
         * This is used to extract n-th volume from an ImageStack representing a
         * hypervolume (multiple volumes in one stack).
        */
        public VolumeInteger(ImageStack s, int depth, int n)
        {
                this(s, depth, n, 1, 1, 1);
        }
        public VolumeInteger(ImageStack s, int depth, int n, double aspectx, double aspecty, double aspectz)
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
        public int [][][] getVolume() { return v; }
        /** Fill the volume from stack s with depth slices from start. */
        public Object get(int x, int y, int z) { return new Integer(v[z][y][x]); }
        public void set(Object i, int x, int y, int z) { v[z][y][x] = ((Integer) i).intValue(); }
        public void load(ImageStack s, int start)
        {
          for (int t = start; t < start + depth; t++)
  				loadSlice(v[t-start], s, t+1);
	  }
        public void load(ImageProcessor ip, int t)
        /* Fill the volume with depth 1 with a single image ip. */
        {
			if (ip instanceof ByteProcessor)
				   loadImage(v[t], (byte []) ip.getPixels());
			else if (ip instanceof FloatProcessor)
				   loadImage(v[t], (int []) ip.getPixels());
			else
				   IJ.error("load: image type not supported.");
        }
        public void loadInverse(ImageStack s, int center)
        /* Inversely load a volume from the stack centered around center with the first one last. */
        {
          for (int t = -depth/2; t <= depth/2; t++)
          {
                int[][] t1 = new int[height][width];
                loadSlice(t1, s, center + depth/2 - t);
				for (int y = 0; y < height; y++)
						for (int x = 0; x < width; x++)
							v[t+depth/2][y][x] = t1[y][x];
		  }
	  }
	  protected void loadSlice(int [][] i, ImageStack s, int io)
	  // Load a slice io from stack s into image i.
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
	  		else if (stack[0] instanceof int[])
				   loadImage(i, (int []) s.getPixels(io));
			else
				   IJ.error("loadSlice: image type not supported.");
	  }
	  protected void loadImage(int [][] i, byte [] b)
	  // Load an image b into i.
	  {
			for (int y = 0; y < height; y++)
			{
                        int offset = y * width;
                        for (int x = 0; x < width; x++)
							  i[y][x] = (int) (b[offset + x] & 0xff);
			}
	  }
	  protected void loadImage(int [][] i, int [] sh)
	  // Load an image sh into i.
	  {
			for (int y = 0; y < height; y++)
			{
                        int offset = y * width;
                        for (int x = 0; x < width; x++)
							  i[y][x] = (int) sh[offset + x];
			}
	  }
}
