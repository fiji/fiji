package volume;
import bijnum.*;

/**
 * This class implements convolution operations on image planes and volumes.
 * All convolutions mirror the edges to avoid edge effects.
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
public class Convolver
{
	/**
	 * Convolution of plane with 1D separated kernel.
	 * The image plane is organized as one 1D vector of width*height. plane is modified!
	 * Can process images masked using NaN values.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel1D kernel object.
	 * @see Kernel1D
	 */
	public static void convolvexy(float [] plane, int width, int height, Kernel1D kernel)
	{
		// Convolve in x-direction.
		float [] t1 = convolvex(plane, width, height, kernel);
		// Convolve in y direction in place.
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around y, convolve over -kernel.halfwidth ..  y .. +kernel.halfwidth.
			for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			{
				// Mirror edges if needed.
				int xi = x;
				int yi = y+k;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				float pixel = t1[yi*width+xi];
				if (pixel == Float.NaN)
				{
					d = Float.NaN;
					break;
				}
				else d += pixel * kernel.k[k + kernel.halfwidth];
			}
			plane[y*width+x] = d;
		}
	}
	/**
	 * Convolution of plane with 1D separated kernel along the x-axis.
	 * The image plane is organized as one 1D vector of width*height.
	 * Return the result as a float array. plane is not touched.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel1D kernel object.
	 * @see Kernel1D
	 * @return a float[] with the resulting convolution.
	 */
	public static float [] convolvex(float [] plane, int width, int height, Kernel1D kernel)
	{
		float [] result = new float[plane.length];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x, convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth.
			for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			{
				// Mirror edges if needed.
				int xi = x+k;
				int yi = y;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				d += plane[yi*width+xi] * kernel.k[k + kernel.halfwidth];
			}
			result[y*width+x] = d;
		}
		return result;
	}
	/**
	 * Convolution of plane with 1D separated kernel along the y-axis.
	 * The image plane is organized as one 1D vector of width*height.
	 * Return the result as a float array. plane is not touched.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel1D kernel object.
	 * @see Kernel1D
	 * @return a float[] with the resulting convolution.
	 */
	public static float [] convolvey(float [] plane, int width, int height, Kernel1D kernel)
	{
		float [] result = new float[plane.length];
		// Convolve in y direction.
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around y, convolve over -kernel.halfwidth ..  y .. +kernel.halfwidth.
			for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			{
				// Mirror edges if needed.
				int xi = x;
				int yi = y+k;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				d += plane[yi*width+xi] * kernel.k[k + kernel.halfwidth];
			}
			result[y*width+x] = d;
		}
		return result;
	}
	/**
	 * Convolution of plane with 1D separated kernel.
	 * The image plane is organized as one 1D vector of width*height.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel1D kernel object.
	 * @see Kernel1D
	 */
	public static void convolvexy(short [] plane, int width, int height, Kernel1D kernel)
	{
		short [] tl = new short[plane.length];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x, convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth.
			for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			{
				// Mirror edges if needed.
				int xi = x+k;
				int yi = y;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				d += plane[yi*width+xi] * kernel.k[k + kernel.halfwidth];
			}
			tl[y*width+x] = (short) d;
		}
		// Convolve in y direction into v.
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around y, convolve over -kernel.halfwidth ..  y .. +kernel.halfwidth.
			for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
			{
				// Mirror edges if needed.
				int xi = x;
				int yi = y+k;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				d += tl[yi*width+xi] * kernel.k[k + kernel.halfwidth];
			}
			plane[y*width+x] = (short) d;
		}
	}
	/**
	 * Convolution of plane with symmetric 2D kernel in both directions.
	 * The image plane is organized as one 1D vector of width*height.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel2D kernel object.
	 * @see Kernel2D
	 */
	public static void convolvexy(float [] plane, int width, int height, Kernel2D kernel)
	{
		float [] t1 = new float[plane.length];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y, convolve over -kernel.halfwidth ..  x,y .. +kernel.halfwidth in 2-D.
			for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
			for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
			{
				// Mirror edges if needed.
				int xi = x+l;
				int yi = y+m;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				d += plane[yi*width+xi] * kernel.k[m+kernel.halfwidth][l+kernel.halfwidth];
			}
			t1[y*width+x] = d;
		}
		// Copy into image
		for (int i = 0; i < plane.length; i++)
			plane[i] = t1[i];
	}
	/**
	 * Convolution of float volume with symmetric 3D kernel in all dimensions.
	 * The volume is organized as one 1D vector of width*height*depth.
	 * @param v the volume.
	 * @param width the width in pixels of the volume.
	 * @param height the height of the volume in pixels.
	 * @param depth the height of the volume in pixels.
	 * @param kernel a Kernel3D kernel object.
	 * @see Kernel3D
	 */
	public static void convolvexyz(float [] volume, int width, int height, int depth, Kernel3D kernel)
	{
		// Create a buffer.
		float [] t1 = new float[volume.length];
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y,z convolve over -kernel.halfwidth ..  x,y,z .. +kernel.halfwidth in 2-D.
			for (int n = -kernel.halfwidth; n <= kernel.halfwidth; n++)
			for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
			for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
			{
				// Mirror edges if needed.
				int xi = x+l;
				int yi = y+m;
				int zi = z+n;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				if (zi < 0) zi = -zi;
				else if (zi >= width) zi = 2 * depth - zi - 1;
				d += volume[zi*height*width+yi*width + xi]*kernel.k[n+kernel.halfwidth][m+kernel.halfwidth][l+kernel.halfwidth];
			}
			t1[z*height*width+y*width+x] = (float) d;
		}
		// Copy into volume
		for (int i = 0; i < volume.length; i++)
			volume[i] = t1[i];
	}
	/**
	 * Convolution of float volume with symmetric 1D kernel in x dimension.
	 * The volume is organized as one 1D vector of width*height*depth.
	 * @param v the volume.
	 * @param width the width in pixels of the volume.
	 * @param height the height of the volume in pixels.
	 * @param depth the height of the volume in pixels.
	 * @param kernel a Kernel1D kernel.
	 * @see Kernel1D
	 */
	public static void convolvex(float [] volume, int width, int height, int depth, Kernel1D kernel)
	{
		// Create a buffer.
		float [] t1 = new float[volume.length];
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y,z convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth in 2-D.
			for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
			{
				// Mirror edges if needed.
				int xi = x+l;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				d += volume[z*height*width+y*width + xi]*kernel.k[l+kernel.halfwidth];
			}
			t1[z*height*width+y*width+x] = (float) d;
		}
		// Copy into volume
		for (int i = 0; i < volume.length; i++)
			volume[i] = t1[i];
	}
	/**
	 * Convolution of float volume with symmetric 1D kernel in y dimension.
	 * The volume is organized as one 1D vector of width*height*depth.
	 * @param v the volume.
	 * @param width the width in pixels of the volume.
	 * @param height the height of the volume in pixels.
	 * @param depth the height of the volume in pixels.
	 * @param kernel a Kernel1D kernel.
	 * @see Kernel1D
	 */
	public static void convolvey(float [] volume, int width, int height, int depth, Kernel1D kernel)
	{
		// Create a buffer.
		float [] t1 = new float[volume.length];
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y,z convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth in 2-D.
			for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
			{
				// Mirror edges if needed.
				int yi = y+m;
				if (yi < 0) yi = -yi;
				else if (yi >= width) yi = 2 * width - yi - 1;
				d += volume[z*height*width+yi*width + x]*kernel.k[m+kernel.halfwidth];
			}
			t1[z*height*width+y*width+x] = (float) d;
		}
		// Copy into volume
		for (int i = 0; i < volume.length; i++)
			volume[i] = t1[i];
	}
	/**
	 * Convolution of float volume with symmetric 1D kernel in x dimension.
	 * The volume is organized as one 1D vector of width*height*depth.
	 * @param v the volume.
	 * @param width the width in pixels of the volume.
	 * @param height the height of the volume in pixels.
	 * @param depth the height of the volume in pixels.
	 * @param kernel a Kernel1D kernel.
	 * @see Kernel1D
	 */
	public static void convolvez(float [] volume, int width, int height, int depth, Kernel1D kernel)
	{
		// Create a buffer.
		float [] t1 = new float[volume.length];
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y,z convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth in 2-D.
			for (int n = -kernel.halfwidth; n <= kernel.halfwidth; n++)
			{
				// Mirror edges if needed.
				int zi = z+n;
				if (zi < 0) zi = -zi;
				else if (zi >= width) zi = 2 * width - zi - 1;
				d += volume[zi*height*width+y*width + x]*kernel.k[n+kernel.halfwidth];
			}
			t1[z*height*width+y*width+x] = (float) d;
		}
		// Copy into volume
		for (int i = 0; i < volume.length; i++)
			volume[i] = t1[i];
	}
	/**
	 * Convolution of short volume with symmetric 3D kernel in all dimensions.
	 * The volume is organized as one 1D vector of width*height*depth.
	 * @param v the volume.
	 * @param width the width in pixels of the volume.
	 * @param height the height of the volume in pixels.
	 * @param depth the height of the volume in pixels.
	 * @param kernel a Kernel3D kernel object.
	 * @see Kernel3D
	 */
	public static void convolvexyz(short [] volume, int width, int height, int depth, Kernel3D kernel)
	{
		// Create a buffer.
		short [] t1 = new short[volume.length];
		for (int z = 0; z < depth; z++)
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			float d = 0;
			// Around x,y,z convolve over -kernel.halfwidth ..  x,y,z .. +kernel.halfwidth in 2-D.
			for (int n = -kernel.halfwidth; n <= kernel.halfwidth; n++)
			for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
			for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
			{
				// Mirror edges if needed.
				int xi = x+l;
				int yi = y+m;
				int zi = z+n;
				if (xi < 0) xi = -xi;
				else if (xi >= width) xi = 2 * width - xi - 1;
				if (yi < 0) yi = -yi;
				else if (yi >= height) yi = 2 * height - yi - 1;
				if (zi < 0) zi = -zi;
				else if (zi >= width) zi = 2 * depth - zi - 1;
				d += volume[zi*height*width+yi*width + xi]*kernel.k[n+kernel.halfwidth][m+kernel.halfwidth][l+kernel.halfwidth];
			}
			t1[z*height*width+y*width+x] = (short) d;
		}
		// Copy into volume
		for (int i = 0; i < volume.length; i++)
			volume[i] = t1[i];
	}
	/**
	 * Magnitude of convolution of plane with asymetric 1D separated kernel in both directions.
	 * The image plane is organized as one 1D vector of width*height.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel1D kernel object.
	 * @deprecated
	 * @see Kernel1D
	 */
	public static void magConvolutionxy(float [] plane, int width, int height, Kernel1D kernel)
	{
		float [] dx = new float[plane.length];
		float [] dy = new float[plane.length];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			dx[y*width+x] = 0;
			if (valid(width, height, x, y, kernel.halfwidth))
			{
				// Around x, convolve over -kernel.halfwidth ..  x .. +kernel.halfwidth.
				for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					dx[y*width+x] += plane[y*width + (x + k)] * kernel.k[k + kernel.halfwidth];
			}
		}
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			dy[y*width+x] = 0;
			if (valid(width, height, x, y, kernel.halfwidth))
			{
				// Around y, convolve over -kernel.halfwidth ..  y .. +kernel.halfwidth.
				for (int k = -kernel.halfwidth; k <= kernel.halfwidth; k++)
					dy[y*width+x] += plane[(y + k)*width+x] * kernel.k[k + kernel.halfwidth];
			}
		}
		// Result is sqrt(*x^2 + *y^2).
		for (int i = 0; i < plane.length; i++)
			plane[i] = (float) Math.sqrt(Math.pow(dx[i],2)+Math.pow(dy[i],2));
	}
	/**
	 * Magnitude of convolution of plane with symmetric 2D kernel in both directions.
	 * The image plane is organized as one 1D vector of width*height.
	 * @param plane the image.
	 * @param width the width in pixels of the image.
	 * @param height the height of the image in pixels.
	 * @param kernel a Kernel2D kernel object.
	 * @see Kernel2D
	 * @deprecated
	 */
	public static void magConvolutionxy(float [] plane, int width, int height, Kernel2D kernel)
	{
		float [] t1 = new float[plane.length];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			double dx = 0, dy = 0;
			if (valid(width, height, x, y, kernel.halfwidth))
			{
				// Compute sqrt(*x^2 + *y^2).
				for (int m = -kernel.halfwidth; m <= kernel.halfwidth; m++)
				for (int l = -kernel.halfwidth; l <= kernel.halfwidth; l++)
				{
					float pixel = plane[(y+m)*width + (x+l)];
					dx += pixel*kernel.k[m+kernel.halfwidth][l+kernel.halfwidth];
					dy += pixel*kernel.k[l+kernel.halfwidth][m+kernel.halfwidth];
				}
			}
			t1[y*width+x] = (float) Math.sqrt(dx*dx+dy*dy);
		}
		// Copy into image
		for (int i = 0; i < plane.length; i++)
			plane[i] = t1[i];
	}
	/**
	 * Compute Math.pow(image, pow) for image.
	 * The image or volume is organized as one 1D vector.
	 * @param image the image.
	 * @deprecated
	 */
	public static void pow(float [] image, double pow)
	{
		for (int i = 0; i < image.length; i++)
			image[i]= (float) Math.pow(image[i], pow);
	}
	/**
	 * Compute image1 = (image1 - image2) for images or volumes.
	 * The image or volume is organized as one 1D vector.
	 * @param image1.
	 * @param image2.
	 * @deprecated
	 */
	public static float [] subtract(float [] image1, float [] image2)
	{
		float [] r = new float[image1.length];
		for (int i = 0; i < image1.length; i++)
			r[i]= image1[i] - image2[i];
		return r;
	}
	/**
	 * Compute threshold of image. Everything < threshold is set to to 0, rest stays.
	 * The image or volume is organized as one 1D vector.
	 * @param image the image.
	 * @param threshold a double with the treshold.
	 * @deprecated
	 */
	public static void nonlinear(float [] image, double threshold)
	{
		for (int i = 0; i < image.length; i++)
			image[i]= image[i] >= threshold ? image[i] : 0;
	}
	/**
	 * Normalize. Everything < threshold is set to to 0, over threshold to 255.
	 * The image plane is organized as one 1D vector of width*height.
	 * @param plane the image.
	 * @param threshold a double with the treshold.
	 * @deprecated
	 */
	public static void normalize(float [] plane, double threshold)
	{
		for (int i = 0; i < plane.length; i++)
			plane[i]= plane[i] >= threshold ? 255 : 0;
	}
	public static boolean valid(int width, int height, int x, int y, int edge)
	{ return (x >= edge && x < width-edge && y >= edge && y < height - edge); }
	public static boolean valid(int width, int height, int depth, int x, int y, int z, int edge)
	{ return (x >= edge && x < width-edge && y >= edge && y < height - edge && z >= edge && z < depth - edge); }
}
