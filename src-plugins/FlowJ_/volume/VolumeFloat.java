package volume;
import java.io.*;
import ij.*;
import ij.process.*;

/**
 * This class implements float volumes and operations including
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
public class VolumeFloat extends Volume
{
        public  float [][][]      v;

        /** creates the null volume. */
        public VolumeFloat() {}
        /**
        * Creates a float volume of defined size and aspect ratio.
        * @param width, height, depth the dimensions of the volume
        * @param aspectx, aspecty, aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeFloat(int width, int height, int depth, double aspectx, double aspecty, double aspectz)
        {
			this.width = width; this.height = height; this.depth = depth;
			v = new float[depth][height][width];
			setAspects(aspectx, aspecty, aspectz);
        }
        /**
        * Creates a float volume of defined size and default aspect ratios.
        * @param width, height, depth the dimensions of the volume
        */
        public VolumeFloat(int width, int height, int depth)
        {
			this(width, height, depth, 1.0, 1.0, 1.0);
        }
        /**
        * Creates a float volume which is an exact copy of vl.
        * @param vl the VolumeFloat to be copied.
        */
        public VolumeFloat(VolumeFloat v1)
        {
                this(v1.getWidth(), v1.getHeight(), v1.getDepth(), v1.getAspectx(), v1.getAspecty(), v1.getAspectz());
                copy(v1.v);
        }
        /**
        * Creates a float volume from an ImageJ ImageStack.
        * @param s the ImageStack to be used.
        */
        public VolumeFloat(ImageStack s)
        {
                this(s.getWidth(), s.getHeight(), s.getSize(), 1.0, 1.0, 1.0);
                load(s, 0);
        }
        /**
        * Creates a float volume from an ImageJ ImageStack.
        * @param s the ImageStack to be used.
        * @param aspectx, aspecty, aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeFloat(ImageStack s, double aspectx, double aspecty, double aspectz)
        {
                this(s, s.getSize(), 0, aspectx, aspecty, aspectz);
                load(s, 0);
        }
        /**
        * Creates a float volume from an ImageJ ImageStack.
        * with depth slices from n * depth.
        * This is used to extract the n-th volume from an ImageStack representing a
        * hypervolume (multiple volumes in one stack).
        * @param s the ImageStack to be used.
        * @param depth the numebr of slices per volume.
        * @param n the number of volumes in the hypervolume.
        */
        public VolumeFloat(ImageStack s, int depth, int n)
        {
	    this(s, depth, n, 1.0, 1.0, 1.0);
        }
        /**
        * Creates a float volume from a float vector with the same contents.
        * @param fv the array of float to be used.
        */
        public VolumeFloat(float [][][] fv)
        {
                this(fv[0][0].length, fv[0].length, fv.length, 1.0, 1.0, 1.0);
                copy (fv);
        }
        /**
        * Creates a float volume from an ImageJ ImageStack.
        * with depth slices from n * depth.
        * This is used to extract the n-th volume from an ImageStack representing a
        * hypervolume (multiple volumes in one stack).
        * @param s the ImageStack to be used.
        * @param depth the numebr of slices per volume.
        * @param n the number of volumes in the hypervolume.
        * @param aspectx, aspecty, aspectz the aspect ratios of the volume dimensions.
        */
        public VolumeFloat(ImageStack s, int depth, int n, double aspectx, double aspecty, double aspectz)
        {
	        this(s.getWidth(), s.getHeight(), depth, aspectx, aspecty, aspecty);
	        load(s, n * depth);
        }
        /**
        * Creates a float volume 1 voxel deep from the contents of an ImageJ ImageProcessor.
        * @param ip the ImageProcessor from which to obtain the voxels.
        */
        public VolumeFloat(ImageProcessor ip)
        {
                this(ip.getWidth(), ip.getHeight(), 1);
                if (ip instanceof ColorProcessor)
                        loadPixels(v[0], (ColorProcessor) ip, width, height);
                else if (ip instanceof ByteProcessor)
                        loadPixels(v[0], (ByteProcessor) ip, width, height);
                else if (ip instanceof ShortProcessor)
                        loadPixels(v[0], (ShortProcessor) ip, width, height);
                else if (ip instanceof FloatProcessor)
                        loadPixels(v[0], (FloatProcessor) ip, width, height);
        }
        public float [][][] getVolume() { return v; }
        /**
         * Get the voxel value as a Number.
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         * @return a Number with the voxel value at x,y,z
         */
        public Object get(int x, int y, int z) { return new Float(v[z][y][x]); }
        /**
         * Set the voxel value to a Number.
         * @value a Number with the voxel value at x,y,z
         * @param x the x position of the voxel
         * @param y the y position of the voxel
         * @param z the z position of the voxel
         */
        public void set(Object value, int x, int y, int z) { v[z][y][x] = ((Number) value).floatValue(); }
        /**
         * Load a volume from a ImageStack, convert to float and do 1D z convolution on the fly.
         * @param s the ImageStack to use.
         * @param center the index of the central slice in s.
         * @param kernel the kernel to use for 1D convolution.
        */
        public void convolvet(ImageStack s, int center, Kernel1D kernel)
        {
	        // depth slices centered around center.
	        for (int t = -depth/2; t <= depth/2; t++)
	        {
		        float[][] t1 = new float[height][width];
		        if (kernel instanceof Kernel)
		        {
			        /*
			    Around t, convolve over -kernel.halfwidth .. t .. +kernel.halfwidth.
			    For reasons of speed, calculate the contribution of each image
			    image by image instead of pixelwise.
			        */
			        for (int y = 0; y < height; y++)
                                        for (int x = 0; x < width; x++)
                                                v[t+depth/2][y][x] = 0;
			        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			        {
			                loadSlice(t1, s, center + t + k + 1);
                                        for (int y = 0; y < height; y++)
                                                for (int x = 0; x < width; x++)
                                                        v[t+depth/2][y][x] += t1[y][x] * kernel.k[k + kernel.halfwidth];
                                }
                        }
		        else
		        {
			        // No temporal filtering: load into v.
			        loadSlice(t1, s, center + t);
                                for (int y = 0; y < height; y++)
                                        for (int x = 0; x < width; x++)
                                                v[t+depth/2][y][x] = t1[y][x];
		        }
	        }
        }
    public void load(ImageStack s, int start)
    /* Fill the volume from stack s with depth slices from slice start. */
    {
	  for (int t = start; t < Math.min(start + depth, depth); t++)
	  {
		float[][] t1 = new float[height][width];
		loadSlice(t1, s, t+1);
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                        v[t-start][y][x] = t1[y][x];
	  }
    }
    public void loadInverse(ImageStack s, int center)
    /* Inversely load a volume from the stack centered around center with the first one last. */
    {
	  for (int t = -depth/2; t <= depth/2; t++)
	  {
		float[][] t1 = new float[height][width];
		loadSlice(t1, s, center + depth/2 - t);
			    for (int y = 0; y < height; y++)
				      for (int x = 0; x < width; x++)
							v[t+depth/2][y][x] = t1[y][x];
	  }
    }
        /** 2D xy convolution separated over x and y. */
        public void convolvexy(Kernel1D kernel)
        {
	        if (kernel.halfwidth > edge) edge = kernel.halfwidth;
	        for (int t = 0; t < depth; t++)
	        {
		        float t1[][] = new float[height][width];
		        // Convolve in x direction.
                        for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
                        {
			        t1[y][x] = 0;
			        if (valid(x, y))
                                {
                                        // Around x, convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth.
                                        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
                                                t1 [y][x] += v[t][y][x + k] * kernel.k[k + kernel.halfwidth];
			        }
                        }
		        // Convolve in y direction into v.
		        for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
                        {
                                v[t][y][x] = 0;
				if (valid(x, y))
				{
                                        // Around y, convolve over -kernel.halfwidth ..  y .. +kernel.halfwidth.
                                        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
						v[t][y][x] += t1[y + k][x] * kernel.k[k + kernel.halfwidth];
				}
                        }
	        } // for t
        }
        public void convolvexy(Kernel2D kernel)
        // 2D xy convolution.
        {
                if (kernel.halfwidth > edge) edge = kernel.halfwidth;
				// Create a copy.
				VolumeFloat t = new VolumeFloat(v);
				for (int z = 0; z < depth; z++)
				for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
				{
						v[z][y][x] = 0;
						if (valid(x, y))
						{
								for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
								for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
										v[z][y][x] += t.v[z][y+m][x+l]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth];
						}
				}
		}
        public void convolvexyz(Kernel3D kernel)
        // 3D xyz convolution.
        {
                if (kernel.halfwidth > edge) edge = kernel.halfwidth;
                // Create a copy.
                VolumeFloat t = new VolumeFloat(v);
                for (int z = 0; z < depth; z++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                {
                        v[z][y][x] = 0;
                        if (valid(x, y, z))
                        {
                                for (int n = -kernel.halfwidth; n <= kernel.halfwidth; n++)
                                for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
                                for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
                                        v[z][y][x] += t.v[z+n][y+m][x+l]*kernel.k[n+kernel.halfwidth][m+kernel.halfwidth][l+kernel.halfwidth];
                        }
                }
        }
        /**
         * XYZ convolution with separated 1D kernel.
         * @param kernel a 1D convolution kernel.
         */
        public void convolvexyz(Kernel1D kernel)
        // 1D xyz convolution.
        {
                if (kernel.halfwidth > edge) edge = kernel.halfwidth;
                // copy into buffer volume t1.
                VolumeFloat t1 = new VolumeFloat(v);
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
                                        s += t1.v[z][y][x+k]*kernel.k[k+kernel.halfwidth];
                        }
                        v[z][y][x] = (float) s;
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
                                        s += v[z][y+k][x]*kernel.k[k+kernel.halfwidth];
                        }
                        t1.v[z][y][x] = (float) s;
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
                                        s += t1.v[z+k][y][x]*kernel.k[k+kernel.halfwidth];
                        }
                        v[z][y][x] = (float) s;
	        }
        } // convolvexyz
	public void convolvex(VolumeFloat v1, Kernel1D kernel)
    /*
	   1D x convolution
	   This method will use the planes from v1 centered around depth (of this).
	*/
    {
	if (width < v1.getWidth() || height < v1.getHeight() || depth > v1.getDepth())
	    IJ.error("convolve: destination volume wrong size.");
	int depthoffset = 0;
	if (depth < v1.getDepth())
	      depthoffset = (v1.getDepth() - depth) / 2;
	if (v1.edge > edge) edge = v1.edge;
	if (kernel.halfwidth > edge) edge = kernel.halfwidth;
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
				v[z][y][x] = 0;
				if (valid(x, y))
					for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
						v[z][y][x] += v1.v[z+depthoffset][y][x+k]*kernel.k[k+kernel.halfwidth];
		}
    }
    public void convolvey(VolumeFloat v1, Kernel1D kernel)
    /*
		1D y convolution
	   This method will use the planes from v1 centered around depth (of this).
   */
   {
	if (width < v1.getWidth() || height < v1.getHeight() || depth > v1.getDepth())
	    IJ.error("convolve: destination volume wrong size.");
	int depthoffset = 0;
	if (depth < v1.getDepth())
	      depthoffset = (v1.getDepth() - depth) / 2;
	if (v1.edge > edge) edge = v1.edge;
	if (kernel.halfwidth > edge) edge = kernel.halfwidth;
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
			    for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
				    v[t][y][x] += v1.v[t+depthoffset][y+k][x]*kernel.k[k+kernel.halfwidth];
		}
    }
    public void convolvex(VolumeFloat v1, Kernel2D kernel)
    /*
       2D x convolution with a 2D kernel
       This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;
	// business
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
				  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					v[t][y][x] += v1.v[t+depthoffset][y+l][x+k]*kernel.k[l+kernel.halfwidth][k+kernel.halfwidth];
		      }
		}
    }
    public void convolvey(VolumeFloat v1, Kernel2D kernel)
    /*
       compute 2d convolution with a 2D kernel in the y direction: v=v1*kernel  in y dimension
       This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;

	// business
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
				  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					v[t][y][x] += v1.v[t+depthoffset][y+l][x+k]*kernel.k[k+kernel.halfwidth][l+kernel.halfwidth];
		      }
		}
    }
   public void convolvez(VolumeFloat v1, Kernel1D kernel)
   /*
	   1D z convolution
   */
   {
	if (width < v1.getWidth() || height < v1.getHeight() || depth > v1.getDepth())
				IJ.error("convolve: destination volume wrong size.");
	if (v1.getDepth() < kernel.halfwidth*2)
				IJ.error("volume depth doesn't fit kernel");
		if (v1.edge > edge) edge = v1.edge;
		int x=0, y=0, z=0;
		try{
				for (z = -depth/2; z < (depth+1)/2; z++)
				for (y = 0; y < height; y++)
				for (x = 0; x < width; x++)
				{
						v[z+depth/2][y][x] = 0;
						if (z >= -v1.getDepth()/2+kernel.halfwidth && z < (v1.getDepth()+1)/2-kernel.halfwidth)
								for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
							v[z+depth/2][y][x] += v1.v[z+v1.getDepth()/2+k][y][x]*kernel.k[k+kernel.halfwidth];
				}
		} catch(ArrayIndexOutOfBoundsException e) { IJ.write("array "+e
					+" x,y,z "+x+","+y+","+z+"("
					+" k "+kernel.halfwidth+": "+width+"x"+height+"x"+depth+ "?= "
					+v1.getDepth()+"x"+v1.getHeight()+"x"+v1.getDepth()); }
	}
   public void convolvez(VolumeFloat v1)
   /*
	   "Convolution" without kernel computes the difference for the adjacent slices in v1.
   */
   {
		if (width < v1.getWidth() || height < v1.getHeight() || depth > v1.getDepth())
				IJ.error("convolvez: destination volume wrong size.");
		if (v1.getDepth() < 2)
				IJ.error("convolvez: volume depth doesn't fit kernel");
		if (v1.edge > edge) edge = v1.edge;
                IJ.write("z: "+(-depth/2)+"index "+(-depth/2 + depth/2));
		for (int z = -depth/2; z < (depth+1)/2; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
				int index = z + depth/2;
                                v[index][y][x] = v1.v[index+1][y][x]-v1.v[index][y][x];
		}
	}
	public void convolvez(VolumeFloat v1, Kernel2D kernel)
	/*
	   compute 2d convolution with a 2D kernel in the z direction: v=v1*kernel  in t dimension
	   This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;

	// business
	for (int z = 0; z < depth; z++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[z][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
				  for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					v[z][y][x] += v1.v[z+depthoffset+l][y+k][x]*kernel.k[k+kernel.halfwidth][l+kernel.halfwidth];
		      }
		}
    }
    public void convolvex(VolumeFloat v1, Kernel3D kernel)
    /*
       compute 3d convolution with a 3D kernel in the x direction: v=v1*kernel  in t dimension
       This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;

	// business
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
				  for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
					for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					      v[t][y][x] += v1.v[t+depthoffset+m][y+l][x+k]*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth][k+kernel.halfwidth];
		      }
		}
    }
    public void convolvey(VolumeFloat v1, Kernel3D kernel)
    /*
       compute 3d convolution with a 3D kernel in the y direction: v=v1*kernel  in t dimension
       This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;

	// business
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
				  for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
					for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					      v[t][y][x] += v1.v[t+depthoffset+m][y+l][x+k]*kernel.k[m+kernel.halfwidth][k+kernel.halfwidth][l+kernel.halfwidth];
		      }
		}
    }
    public void convolvez(VolumeFloat v1, Kernel3D kernel)
    /*
       compute 3d convolution with a 3D kernel in the t direction: v=v1*kernel  in t dimension
       This method will use the planes from v1 centered around depth (of this).
    */
    {
	int depthoffset = InitParams(v1, kernel);
	if (depthoffset < 0) return;

	// business
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		{
		      v[t][y][x] = 0;
		      if (valid(x, y))
		      {
			    for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
				  for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
					for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					      v[t][y][x] += v1.v[t+depthoffset+m][y+l][x+k]*kernel.k[k+kernel.halfwidth][l+kernel.halfwidth][m+kernel.halfwidth];
		      }
		}
    }
	private void copy(float [][][] fv)
	// Copy float array fv to this volume.
    {
	 for (int t = 0; t < depth; t++)
		 for (int y = 0; y < height; y++)
		 for (int x = 0; x < width; x++)
				v[t][y][x] = fv[t][y][x];
    }
	public void copy(VolumeFloat v1)
	// Copy v1 to this volume.
    {
		 copy(v1.v);
    }
	public void add(VolumeFloat v1)
    // Add volume v1 to this volume.
    {
	 for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		      v[t][y][x] = v[t][y][x] + v1.v[t][y][x];
    }
        /** Subtract volume v1 from this volume. */
        public void sub(VolumeFloat v1)
        {
	        for (int t = 0; t < depth; t++)
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
		        v[t][y][x] -= v1.v[t][y][x];
        }
    public void mul(double constant)
    // Multiply volume by a constant.
    {
	for (int t = 0; t < depth; t++)
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++)
		      v[t][y][x] *= constant;
    }
	public void mul(VolumeFloat a, VolumeFloat b)
	// Multiply voxels in a by voxels in b (not the inner product).
    {
	  if (width != a.getWidth() || height != a.getHeight() || depth != a.getDepth()
	    || width != b.getWidth() || height != b.getHeight() || depth != b.getDepth())
	      IJ.error("mul undefined.");
	  for (int t = 0; t < depth; t++)
			  for (int y = 0; y < height; y++)
				  for (int x = 0; x < width; x++)
		      v[t][y][x] =  (float) ((double) a.v[t][y][x] * (double) b.v[t][y][x]);
    }
    public void mul(VolumeFloat a)
	// Multiply the voxels in a by the voxels in v (not the inner product).
	{
	  if (width != a.getWidth() || height != a.getHeight() || depth != a.getDepth())
	      IJ.error("mul undefined.");
		  for (int z = 0; z < depth; z++)
		  for (int y = 0; y < height; y++)
		  for (int x = 0; x < width; x++)
				v[z][y][x] = (float) ((double) v[z][y][x] * (double) a.v[z][y][x]);
    }
    public void sqrt()
	// Multiply the voxels in a by the voxels in b (not the inner product).
    {
	  for (int t = 0; t < depth; t++)
			  for (int y = 0; y < height; y++)
				  for (int x = 0; x < width; x++)
		      v[t][y][x] =  (float) Math.sqrt(v[t][y][x]);
    }
    private int InitParams(VolumeFloat v1, Kernel kernel)
    /* Check and prepare the various parameters. Return -1 if problem, otherwise the difference in depths. */
    {
	if (width < v1.getWidth() || height < v1.getHeight() || depth > v1.getDepth())
	{
	    IJ.error("volume: convolution volume wrong size.");
	    return -1;
	}
	int depthoffset = 0;
	if (depth < v1.getDepth())
	      depthoffset = (v1.getDepth() - depth) / 2;
	if (v1.edge > edge) edge = v1.edge;
	if (kernel.halfwidth > edge) edge = kernel.halfwidth;
	return depthoffset;
        }
    // Perform 1D convolution at (x,y,z) along x.
	public float dx(int x, int y, int z, Kernel1D kernel)
        {
	        float d = 0; if (edge < kernel.halfwidth) edge = kernel.halfwidth;
	        if (valid(x,y,z))
	        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
		      d += v[z][y][x+k] * kernel.k[k+kernel.halfwidth];
	        return d;
        }
        // Perform 1D convolution at (x,y,z) along y.
        public float  dy(int x, int y, int z, Kernel1D kernel)
        {
	        float d = 0; if (edge < kernel.halfwidth) edge = kernel.halfwidth;
	        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
	        {
		        if (valid(x,y,z))
		                d += v[z][y+k][x] * kernel.k[k+kernel.halfwidth];
	        }
	        return d;
        }
        public float  dz(int x, int y, int z, Kernel1D kernel)
        // Perform 1D convolution at (x,y,z) along z.
        {
	        float d = 0; if (edge < kernel.halfwidth) edge = kernel.halfwidth;
	        for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
	        {
		        if (valid(x,y,z))
		                d += v[z+k][y][x] * kernel.k[k+kernel.halfwidth];
	        }
	        return d;
        }
    public float  ux(int j, int i, Kernel2D kernel)
    // Perform 2D convolution along dx with kernel kernel.
    {
	float  d = 0;
	for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
	{
	    for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
	    {
		if (valid(i, j, kernel.halfwidth))
		    d += v[depth/2][j+l][i+k] * kernel.k[l+kernel.halfwidth][k+kernel.halfwidth];
	    }
	}
	return d;
    }
    public float uy(int j, int i, Kernel2D kernel)
    // Perform 2D convolution along dy with kernel kernel.
    {
	float  d = 0;
	for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
	{
	    for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
	    {
		if (valid(i, j, kernel.halfwidth))
		    d += v[depth/2][j+l][i+k] * kernel.k[k+kernel.halfwidth][l+kernel.halfwidth];
	    }
	}
	return d;
    }
    public float [] map()
    /* map the volume to a (max) 5x5 image. */
    {
	    float [] pixels = new float[5*5*width*height]; // 5 rows of 5 images.
	    for (int z = 0; z < 5*5 && z < depth; z++)
	    {
		  int col = z % 5; int row = z / 5;
		  for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++)
			{
			      int i = col * width + row * width * height * 5 + x + y * 5 * width;
			      double value = v[z][y][x];
			      pixels[i++] = (float) value;
			}
	    }
	    return pixels;
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
   public void intoStack(ImageStack is)
   /*
	  Transfer contents of v into stack is.
   */
   {
	  // Find min and max.
	  float min = Float.MAX_VALUE;
	  float max = -Float.MAX_VALUE;
	  for (int z = 0; z < depth; z++)
		      for (int y = 0; y < height; y++)
			    for (int x = 0; x < width; x++)
			    {
				    if (min > v[z][y][x])
					  min = v[z][y][x];
				    if (max < v[z][y][x])
					  max = v[z][y][x];
			    }
	  float scale = 1 / (max - min);
	  Object [] stack = is.getImageArray();
	  for (int z = 0; z < depth; z++)
	  {
		    // Determine type of image in stack.
		    if (stack[0] instanceof byte[])
		    {
			  byte b[] = (byte[]) is.getPixels(z+1);
			  for (int y = 0; y < height; y++)
			  {
				  int offset = y * width;
				  for (int x = 0; x < width; x++)
					b[offset + x] = (byte) (((v[z][y][x]-min)*scale) * 255);
			  }
		    }
		    else if (stack[0] instanceof short[])
		    {
			  short u[] = (short[]) is.getPixels(z+1);
			  for (int y = 0; y < height; y++)
			  {
				int offset = y * width;
				for (int x = 0; x < width; x++)
				      u[offset + x] = (short) (((v[z][y][x]-min)*scale) * (float) (0x7fff));
			  }
		    }
		    else if (stack[0] instanceof float[])
		    {
			  float f[] = (float[]) is.getPixels(z+1);
			  for (int y = 0; y < height; y++)
			  {
				int offset = y * width;
				for (int x = 0; x < width; x++)
				      f[offset + x] = v[z][y][x];
			  }
		    }
		    else if (stack[0] instanceof int[])
		    {
			    // RGB 32 bit image image deserves separate treatment.
			    int[] ii = (int[]) is.getPixels(z+1);
			    for (int y = 0; y < height; y++)
			    {
				    int offset = y * width;
				    for (int x = 0; x < width; x++)
				    {
					    int b = (int) (((v[z][y][x]-min)*scale) * 255);
					    ii[offset + x] = (b << 16) | (b << 8) | b;
				    }
			    }
		    }
	  }
    }
   public ImageStack getImageStack()
   /*
		  Transfer contents of v into stack is.
   */
   {
		  ImageStack is = new ImageStack(width, height);
		  for (int z = 0; z < depth; z++)
		  {
                                // create a float processor.
                                FloatProcessor fp = new FloatProcessor(width, height);
                                float [] im = (float [])fp.getPixels();
                                for (int y= 0; y < height; y++)
                                {
                                        int offset = y * width;
                                        for (int x = 0; x < width; x++)
                                                im[offset + x] = v[z][y][x];
                                }
                                is.addSlice("slice"+z, fp);
		  }
		  return is;
   }
}
