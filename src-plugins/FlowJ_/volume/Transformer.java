package volume;
import bijnum.*;
import ij.process.*;
import ij.*;
/**
 * This class implements static methods for transforming images and volumes.
 *
 * @see bijnum.BIJtransform for transformation matrix operations.
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
public class Transformer
{
        /**
         * Transform image using the transformation parameters in p.
         * The transformation parameters do not include centering the image before rotation around
         * the rotation axis. The rotation is around the center of the image.
         * @param the image to be transformed.
         * @param width the width of the image.
         * @param p a float[] with the transformation parameters:
         * @see convertParametersIntoTransformationMatrix
         * @return the transformation of image.
         */
        public static float [] transform(float [] image, int width, float [] p)
        throws Exception
        {
                return transform(image, width, p, width/2, (image.length / width) / 2);
        }
        /**
         * Transform image using the transformation parameters in p.
         * The transformation parameters do not include centering the image before rotation around
         * the rotation axis.
         * @param the image to be transformed.
         * @param width the width of the image.
         * @param p a float[] with the transformation parameters:
         * @see convertParametersIntoTransformationMatrix
         * @param xcenter the x center for rotation.
         * @param ycenter the ycenter for rotation.
         * @return the transformation of image.
         */
        public static float [] transform(float [] image, int width, float [] p, float xcenter, float ycenter)
        throws Exception
        {
                // Create a 2-D transformation matrix out of p.
                float [][] m = convertParametersIntoTransformationMatrix(p, xcenter, ycenter);
                return transform(image, width, m);
        }
	/**
	 * Bilinearly transform an image using a
	 * 2-D transformation matrix m.
	 * @param image a float[] image
	 * @param width the width of image
	 * @param m the transformation matrix, a homogenuous matrix.
	 */
	public static float [] transform(float [] image, int width, float [][] m)
	throws Exception
	{
		// Invert the transformation matrix.
		float [][] mi = null;
		mi = BIJmatrix.inverse(m);
		int height = image.length / width;
		float [] tr = new float[image.length];
		float [] v = new float[3];
		for (int y = 0; y < height; y++)
		for (int x = 0; x < width; x++)
		{
			// Create the x,y vector with coordinates of the transformed image.
			v[0] = x; v[1] = y; v[2] = 1;
			// Transform coordinates into coordinates into the original image.
			float [] tv = null;
			tv = BIJmatrix.mul(mi, v);
			// Now interpolate using the transformed coordinates.
		        tr[y * width + x] = bilinearExtend(image, width, height, tv[0], tv[1]);
		}
		//(new ImagePlus("A transformation of b being tested", new FloatProcessor(width, height, tr, null))).show();
		return tr;
	}
        /**
         * Convert a bunch of float transformation parameters into a transformation matrix.
         * @param p the parameters (either first 2, first 3 or 5).
         * <pre>
         * p[0] translation x-dir
         * p[1] translation y-dir
         * p[2] rotation (degrees)
         * p[3] scaling x
         * p[4] scaling y
         * </pre>
         * @param xcenter, ycenter the x and y center for rotation if any.
         * @return a transformation matrix.
         */
        public static float [][] convertParametersIntoTransformationMatrix(float [] p, float xcenter, float ycenter)
        {
                float [][] m = BIJtransform.newMatrix(2);
                // Implement all transformations in p into m.
                if (p.length == 2)
                       m = BIJtransform.translate(m, p[0], p[1]);
                if (p.length == 3)
                {
                        // Translate so that rotation center is at xcenter, ycenter, then perform translation for p, then rotation around 0,0,
                        // then translate back to original position.
                        m = BIJtransform.rotatez(BIJtransform.translate(BIJtransform.translate(m, xcenter, ycenter), p[0], p[1]), p[2]);
                        m = BIJtransform.translate(m, -xcenter, -ycenter);
                        //m = BIJtransform.rotatez(BIJtransform.translate(m, p[0], p[1]), p[2]);
                }
                if (p.length == 5)
                       m = BIJtransform.scale(BIJtransform.rotatez(BIJtransform.translate(m, p[0], p[1]), p[2]), p[3], p[4]);
                return m;
        }
        /**
         * Convert a bunch of float transformation parameters into a transformation matrix.
         * @param p the parameters (either first 2, first 3 or 5).
         * <pre>
         * p[0] translation x-dir
         * p[1] translation y-dir
         * p[2] rotation (degrees)
         * p[3] scaling x
         * p[4] scaling y
         * </pre>
         * @return a transformation matrix.
         */
        public static float [][] convertParametersIntoTransformationMatrix(float [] p)
        {
                return convertParametersIntoTransformationMatrix(p, 0, 0);
        }
	/**
	 * Convert a bunch of double transformation parameters into a transformation matrix.
	 * @param p the parameters
	 * <pre>
	 * p[0] translation x-dir
	 * p[1] translation y-dir
	 * p[2] rotation (degrees)
	 * p[3] scaling x
	 * p[4] scaling y
	 * </pre>
	 * @return a transformation matrix.
	 */
	public static float [][] convertParametersIntoTransformationMatrix(double [] p)
	{
		float [][] m = BIJtransform.newMatrix(2);
		// Implement all transformations in p into m.
		if (p.length == 2)
		       m = BIJtransform.translate(m, p[0], p[1]);
		if (p.length == 3)
		       m = BIJtransform.rotatez(BIJtransform.translate(m, p[0], p[1]), p[2]);
		if (p.length == 5)
	               m = BIJtransform.scale(BIJtransform.rotatez(BIJtransform.translate(m, p[0], p[1]), p[2]), p[3], p[4]);
                return m;
	}
	/**
	 * Interpolate the pixel at x,y in image, extend if x,y not in image .
	 * @param x
	 * @param y
	 * @param image
	 * @return the interpolated value at x,y
	 */
	public static float bilinearExtend(float[] image, int width, int height, float x, float y)
	{
		// Extend beyond boundaries of image.
		if (x < 0) x = 0;
		if (x >= width - 1) x = width - 1.001f;
		if (y < 0) y = 0;
		if (y >= height - 1) y = height - 1.001f;
		return bilinear(image, width, x, y);
	}
	/**
	 * Bilinearly interpolate the pixel at x,y. Take care of NaN pixels.
	 * @param image the image in which to interpolate
	 * @param width the width of the image.
	 * @param x,y the position at which to interpolate.
	 * @return the interpolated value at x,y, NaN if any of the involved pixels are NaN.
	 */
	public static float bilinear(float[] image , int width, float x, float y)
	{
		int xbase = (int) x;
		int ybase = (int) y;
		float xFraction = x - xbase;
		float yFraction = y - ybase;
		int offset = ybase * width + xbase;
		float lowerLeft = image [offset];
		float lowerRight = image [offset + 1];
		float upperRight = image [offset + width + 1];
		float upperLeft = image [offset + width];
		if (lowerLeft == Float.NaN || lowerRight == Float.NaN || upperRight == Float.NaN || upperLeft == Float.NaN)
	                 return Float.NaN;
		float upperAverage = upperLeft + xFraction * (upperRight - upperLeft);
		float lowerAverage = lowerLeft + xFraction * (lowerRight - lowerLeft);
		return lowerAverage + yFraction * (upperAverage - lowerAverage);
	}
	/**
	 * Transform the image in fp by translation parameters p
	 * @param fp and ImageProcessor
	 * @param p the transformation parameters (translation only).
	 * <pre>
	 * p[0] translation x-dir
	 * p[1] translation y-dir
	 * </pre>
	 * @return the transformed ImageProcessor.
	 */
	public static ImageProcessor transform(ImageProcessor ip, float [] p)
	{
		// Shift the image to the correct place.
                ImageProcessor regip = ip.duplicate();
                int width = ip.getWidth();
                int height = ip.getHeight();
                //IJ.write("ImageProcessor "+regip.getClass().getName());
                if (ip instanceof ColorProcessor)
                {
                        for (int y = 0; y < height; y++)
                        for (int x = 0; x < width; x++)
                                regip.putPixel(x, y, ((ColorProcessor) ip).getInterpolatedRGBPixel(x - p[0], y - p[1]));
                }
                else
                {
                       for (int y = 0; y < height; y++)
                       for (int x = 0; x < width; x++)
                               regip.putPixelValue(x, y, ip.getInterpolatedPixel(x - p[0], y - p[1]));
                }
                return regip;
	}
        /**
         * Quickly transform an image for whole pixel shifts.
         * @param image a float[] image
         * @param width the width of image
         * @param x the x shift
         * @param y the y-shift.
         * @return the shifted image.
         */
        public static float [] quick(float [] image, int width, int xshift, int yshift)
        {
                int height = image.length / width;
                float [] tr = new float[image.length];
                for (int y = 0; y < height; y++)
                for (int x = 0; x < width; x++)
                {
                        int nx = x + xshift;
                        int ny = y + yshift;
                        if (nx < 0) nx = 0;
                        if (nx >= width) nx = width-1;
                        if (ny < 0) ny = 0;
                        if (ny >= height) y = height-1;
                        tr[y * width + x] = image[ny * width + nx];
                }
                return tr;
        }
        /**
         * Extract a new float [] image from an image.
         * @param image the image
         * @param width the original width of the image
         * @param newx the x location of the part to be extracted
         * @param newy the y location of the part to be extracted
         * @param newwidth the the width of the part to be extracted
         * @param newheight the the width of the part to be extracted
         * @return a float[] of newwidth*newheight pixels.
         */
        public static float [] extract(float [] image, int width, int newx, int newy, int newwidth, int newheight)
        {
                float [] newimage = new float[newwidth * newheight];
                for (int y = 0; y < newheight; y++)
                for (int x = 0; x < newwidth; x++)
                        newimage[y * newwidth + x] = image[(newy+y)*width + (newx + x)];
                return newimage;
        }
}
