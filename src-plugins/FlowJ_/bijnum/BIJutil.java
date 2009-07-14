package bijnum;
import java.io.*;
import java.awt.*;
import ij.*;
import ij.process.*;
import ij.gui.*;
import volume.*;
import bijnum.*;

/**
 * Utilities to link BIJ things to ImageJ.
 *
 * Copyright (c) 1999-2004, Michael Abramoff. All rights reserved.
 * @author: Michael Abramoff
 *
 * Small print:
 * This source code, and any derived programs ('the software')
 * are the intellectual property of Michael Abramoff.
 * Michael Abramoff asserts his right as the sole owner of the rights
 * to this software.
 * Commercial licensing of the software is available by contacting the author.
 * THE SOFTWARE IS PROVIDED "AS IS" AND WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS, IMPLIED OR OTHERWISE, INCLUDING WITHOUT LIMITATION, ANY
 * WARRANTY OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 */
public class BIJutil
{
        /**
         * Create an ImageJ ImageStack out of a float matrix. width is the width of each image.
         * @param a a float[][] matrix with the images as column vectors.
         * @param width the width of each image in pixels.
         * @return an ImageStack.
         */
        public static ImageStack imageStackFromMatrix(float [][] a, int width)
        {
                ImageStack is = new ImageStack(width, a[0].length / width);
                for (int j = 0; j < a.length; j++)
                        is.addSlice(""+j, new FloatProcessor(width, a[j].length / width, a[j], null));
                return is;
        }
        /**
         * Create an ImageJ ImageStack out of a float matrix. width is the width of each image.
         * @param a a float[][] matrix with the images as column vectors.
         * @param width the width of each image in pixels.
         * @return an ImageStack.
         */
        public static ImageStack imageStackFromMatrix(short [][] a, int width)
        {
                ImageStack is = new ImageStack(width, a[0].length / width);
                for (int j = 0; j < a.length; j++)
                        is.addSlice(""+j, new ShortProcessor(width, a[0].length / width, a[j], null));
                return is;
        }
        /**
         * Compute the width of the images in imp, or the ROI if one has been selected.
         * @imp an ImageJ ImagePlus
         * @return the width of the images in imp.
         */
        public static int getMatrixWidth(ImagePlus imp)
        {
                int width = imp.getWidth();
                if (imp.getRoi() != null)
                {
                        Rectangle roiRect = imp.getRoi().getBoundingRect();
                        width = roiRect.width;
                }
                return width;
        }
        /**
         * Compute the width of the images in imp, containing a stack, including scaling.
         * @imp an ImageJ ImagePlus
         * @param scaleFactor the scaling factor.
         * @return the width of the images in imp.
         */
        public static int getMatrixWidth(ImagePlus imp, double scaleFactor)
        {
                int width = imp.getWidth();
                int height = imp.getHeight();
                ImageProcessor ipp = imp.getStack().getProcessor(1);
                // Make sure resize using interpolation.
                ipp.setInterpolate(true);
                if (scaleFactor != 1f && scaleFactor != 0f)
                        ipp = ipp.resize((int) (width * scaleFactor), (int) (height * scaleFactor));
                return ipp.getWidth();
        }
        /**
         * Convert the data in the ImageStack in imp into a float[][] matrix
         * for further processing. Do not use ROI.
         * Uses getFloatPixels(ipp.getPixels()), which converts 16-bit images to
         * @return a float[][] with the images.
         */
        public static float [][] matrixFromImageStack(ImagePlus imp)
        {
                return matrixFromImageStack(imp.getStack());
        }
        /**
         * Access the data in the ImageStack in imp into a float[][] matrix
         * for further processing. Do not use ROI.
         * If the is contains float images, does not convert and the float[][] are the actual pixels.
         * If not, the float[][] are copies of whatever is in the is.
         * Uses getFloatPixels(ipp.getPixels()), which converts 16-bit images to
         * @return a float[][] with the images.
         */
        public static float [][] matrixFromImageStack(ImageStack is)
        {
                float [][] a = new float[is.getSize()][];
                try{
                int width = is.getWidth();
                int height = is.getHeight();
                int x = 0; int y = 0;
                // Make a matrix out of the images in the stack.
                for (int i = 0; i < a.length; i++)
                {
                        ImageProcessor ipp = is.getProcessor(i+1);
                        a[i] = getFloatPixels(ipp.getPixels());
                        // (float []) (ipp.convertToFloat()).duplicate().getPixels();
                }
                }catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
                return a;
        }
        public static ImagePlus showVectorAsImage(float [] v, int width)
        {
                ImagePlus imp = new ImagePlus("v", new FloatProcessor(width, v.length / width, v, null));
                imp.show();
                return imp;
        }
	/**
	 * Convert the data in the ImagePlus image stack into a float[] vector
	 * for further processing. Do not use ROI.
         * @param imp the imageplus containing the stack.
         * @param i the image number in the stack, starting with 0.
	 * @return a float[] with the image as a vector.
	 */
	public static float [] vectorFromImageStack(ImagePlus imp, int i)
	{
                ImageProcessor ipp = imp.getStack().getProcessor(i+1);
                return getFloatPixels(ipp.getPixels());
	}
        /**
         * Convert the data in the ImageStack in imp into a float[][] matrix
         * and scale by scaleFactor on the fly. Do not use Roi.
         * @param imp the imageplus containing the stack.
         * @param i the image number in the stack, starting with 0.
         * @param scaleFactor the scaling factor.
         * @return a float[] with the image as a vector.
         */
        public static float [] vectorFromImageStack(ImagePlus imp, int i, float scaleFactor)
        {
                float [] a = null;
                try
                {
                        int width = imp.getWidth();
                        int height = imp.getHeight();
                        // Make a matrix out of the images in the stack. Take care of the ROI.
                        ImageProcessor ipp = imp.getStack().getProcessor(i+1);
                        // Make sure resize using interpolation.
                        ipp.setInterpolate(true);
                        if (scaleFactor != 1f)
                                ipp = ipp.resize((int) (width * scaleFactor), (int) (height * scaleFactor));
                        a = getFloatPixels(ipp.getPixels());
                }
                catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
                return a;
        }
        /**
         * Convert the data in the i'th ImagePlus image into a float[] vector
         * for further processing. Use ROI if present.
         * @return a float[] with the image.
         */
        public static float [] vectorFromImageStackRoi(ImagePlus imp)
        {
                return vectorFromImageStackRoi(imp, 0);
        }
        /**
         * Convert the data in the i'th ImagePlus image into a float[] vector
         * for further processing. Use ROI if present.
         * @return a float[] with the image.
         */
        public static float [] vectorFromImageStackRoi(ImagePlus imp, int i)
        {
                ImageProcessor ipp;
                if (imp.getStackSize() > 1)
                        ipp = imp.getStack().getProcessor(i+1);
                else
                        ipp = imp.getProcessor();
                float [] a = null;
                try
                {
                        /* All this to make sure you process ROI instead of the full images, if applicable. */
                        int width = imp.getWidth();
                        int height = imp.getHeight();
                        int x = 0; int y = 0;
                        if (imp.getRoi() != null)
                        {
                                Rectangle roiRect = imp.getRoi().getBoundingRect();
                                width = roiRect.width;
                                height = roiRect.height;
                                x = roiRect.x; y = roiRect.y;
                                ipp.setRoi(x, y, width, height);
                                ipp = ipp.crop();
                        }
                        a = getFloatPixels(ipp.getPixels());
                        //(new ImagePlus("ip vectorFromImageStackRoi", ip)).show();
                }
                catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
                return a;
        }
        /**
         * Crop a float image to the rectangular size of a ROI defined by x,y,newwidth,newheight
         * @param image a float[] vector image
         * @param width the width in pixels of image
         * @param height the width in pixels of image
         * @param x, y, newwidth, newheight the ROI
         * @return the cropped image as a float[].
         */
        public static float [] crop(float [] image, int width, int height, int x, int y, int newwidth, int newheight)
        {
                // Make an ImageProcessor out of image.
                ImageProcessor ipp = new FloatProcessor(width, height, image, null);
                float [] a = null;
                try
                {
                        ipp.setRoi(x, y, newwidth, newheight);
                        // Crop the imageprocessor.
                        ipp = ipp.crop();
                        a = (float []) ipp.getPixels();
                }
                catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
                return a;
        }
        /**
         * Convert the pixels in array into a float vector.
         */
        protected static float [] getFloatPixels(Object array)
        {
                float [] pixels32 = null;
                if (array instanceof float[])
                {
                        pixels32 = (float[]) array;
                }
                else if (array instanceof short[])
                {
                        short [] pixels16 = (short[]) array;
                        pixels32 = new float[pixels16.length];
                        for (int i = 0; i < pixels16.length; i++)
                                pixels32[i] = ((int) pixels16[i]&0xffff) - 32768;
                }
                else if (array instanceof byte[])
                {
                        byte [] pixels8 = (byte[]) array;
                        pixels32 = new float[pixels8.length];
                        for (int i = 0; i < pixels8.length; i++)
                                pixels32[i] = ((int) pixels8[i]&0xff);
                }
                else
                {
                        System.out.println("PLEASE IMPLEMENT pixel conversions!");
                        throw new Error("PLEASE IMPLEMENT pixel conversions!");
                }
                return pixels32;
        }
	/**
	 * Convert the data in the ImageStack in imp into a float[][] matrix
	 * and scale by scaleFactor on the fly. Do not use Roi.
         * @param imp an ImagePlus
         * @param scaleFactor the scalefactor between 100-0.01.
	 * @return a float[][] with the images.
	 */
	public static float [][] matrixFromImageStack(ImagePlus imp, double scaleFactor)
	{
		float [][] a = new float[imp.getStackSize()][];
		try{
		int width = imp.getWidth();
		int height = imp.getHeight();
		IJ.showStatus("Downsampling images "+width*scaleFactor+" x "+height*scaleFactor);
		// Make a matrix out of the images in the stack. Take care of the ROI.
		for (int i = 0; i < imp.getStackSize(); i++)
		{
			ImageProcessor ipp = imp.getStack().getProcessor(i+1);
			// Make sure resize using interpolation.
			ipp.setInterpolate(true);
			if (scaleFactor != 1f)
			          ipp = ipp.resize((int) (width * scaleFactor), (int) (height * scaleFactor));
			a[i] = (float []) (ipp.convertToFloat()).getPixels();
		}
		}catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
		return a;
	}
	/**
	 * Convert the data only within the rectangular Roi in the ImageStack in imp into a float[][] matrix
	 * for further processing.
	 * @return a float[][] with the images. The size of the resulting aray may not conform to that of the imp
	 * because it corresponds to the Roi size.
	 */
	public static float [][] matrixFromImageStackRoi(ImagePlus imp)
	{
		float [][] a = new float[imp.getStackSize()][];
		try{
		/* All this to make sure you process ROI instead of the full images, if applicable. */
		int width = imp.getWidth();
		int height = imp.getHeight();
		int x = 0; int y = 0;
		if (imp.getRoi() != null)
	        {
			Rectangle roiRect = imp.getRoi().getBoundingRect();
			width = roiRect.width;
			height = roiRect.height;
			x = roiRect.x; y = roiRect.y;
	        }
		IJ.showStatus("reading images "+width+" x "+height);
		// Make a matrix out of the images in the stack. Take care of the ROI.
		for (int i = 0; i < imp.getStackSize(); i++)
		{
			ImageProcessor ipp = imp.getStack().getProcessor(i+1);
			if (imp.getRoi() != null)
			{
				ipp.setRoi(x, y, width, height);
				ipp = ipp.crop();
			}
			a[i] = (float []) (ipp.convertToFloat()).getPixels();
		}
		}catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
		return a;
	}
	/**
	 * Convert the data in the ImageStack in imp into a short [][] matrix
	 * for further processing.
	 * @return a short[][] with the images.
	 */
	public static short [][] shortMatrixFromImageStack(ImagePlus imp)
	{
		short [][] a = new short[imp.getStackSize()][];
		try{
		/* All this to make sure you process ROI instead of the full images, if applicable. */
		int width = imp.getWidth();
		int height = imp.getHeight();
		int x = 0; int y = 0;
		if (imp.getRoi() != null)
	        {
			Rectangle roiRect = imp.getRoi().getBoundingRect();
			width = roiRect.width;
			height = roiRect.height;
			x = roiRect.x; y = roiRect.y;
	        }
		IJ.showStatus("reading images");
		// Make a matrix out of the images in the stack. Take care of the ROI.
		for (int i = 0; i < imp.getStackSize(); i++)
		{
			ImageProcessor ipp = imp.getStack().getProcessor(i+1);
			if (imp.getRoi() != null)
			{
				ipp.setRoi(x, y, width, height);
				ipp = ipp.crop();
			}
			a[i] = (short []) (ipp.convertToShort(false)).getPixels();
		}
		}catch (Exception e) { CharArrayWriter c = new CharArrayWriter(); e.printStackTrace(new PrintWriter(c)); IJ.write(c.toString()); }
		return a;
	}
        /**
        * Make a montage of all images in a, with columns images per row, so that the width of the returned image will be awidth*columns.
        * Uses ImageJ library to include lettering etc.
        * @param a a float[][] with the images.
        * @param awidth, the width of each image in pixels.
        * @param columns the number of columns desired.
        * @return a float[] image containing the montage.
        */
        public static float [] montage(float [][] a, int awidth, int columns)
        {
                int aheight = a[0].length / awidth;
                float scale = 1;
                int montageWidth = awidth*columns;
                int rows = a.length / columns;
                if (a.length % columns != 0) rows++;
                int montageHeight = aheight*rows;
                ImageProcessor montage = new FloatProcessor(montageWidth, montageHeight);
                montage.setColor(Color.white);
                montage.fill();
                montage.setColor(Color.black);
                int x = 0;
                int y = 0;
                ImageProcessor aSlice;
                boolean embellish = false;
                for (int j = 0; j < a.length; j++)
                {
                        ImageProcessor as = new FloatProcessor(awidth, aheight, a[j], null);
                        if (scale != 1.0)
                                as = as.resize(awidth, aheight);
                        montage.insert(as, x, y);
                        //drawBorder(montage, x, y, awidth, aheight);
                        if (embellish)
                        {
                                montage.moveTo(x, y);
                                montage.lineTo(x+awidth, y);
                                montage.lineTo(x+awidth, y+aheight);
                                montage.lineTo(x, y+aheight);
                                montage.lineTo(x, y);
                                //drawLabel(montage, j, x, y, awidth, aheight);
                                String s = ""+j;
                                int swidth = montage.getStringWidth(s);
                                montage.moveTo(x + awidth/2 - swidth/2, y + aheight);
                                montage.drawString(s);
                        }
                        x += awidth;
                        if (x >= montageWidth)
                        {
                                x = 0;
                                y += aheight;
                                if (y >= montageHeight)
                                        break;
                        }
                }
                return (float []) montage.getPixels();
        }
        /**
         * Find the minimal power of 2 that is larger than n.
         * @return the minimal power of 2 that larger than n.
         */
        public static int minPower2(int n)
        {
                int newn = 2;
                while (newn < n)
                         newn *= 2;
                return newn;
        }
        /**
         * Bin a vector x. By binning is meant add consecutive elements i...i+binwidth and put into
         * elements i...i+binwidth.
         */
        public static void bin(double [] x, int binwidth)
        {
                for (int i = binwidth/2; i < x.length - binwidth/2; i+=binwidth)
                {
                        double d = 0;
                        for (int j = 0; j < binwidth; j++)
                                d += x[i+j];
                        for (int j = 0; j < binwidth; j++)
                                x[i+j] = d;
                }
        }
        /**
         * Mirror a vector so that is has power of 2 length and does not contain extra edges.
         */
        public static float [] makePower2(float [] v)
        {
                int newlength = BIJutil.minPower2(v.length);
                float [] n = new float[newlength];
                // Center v into n and mirror the edges of v.
                int min = n.length / 2 - v.length / 2;
                int max = min + v.length;
                for (int i = 0; i < n.length; i++)
                {
                        if (i < min)
                                n[i] = v[min - i];
                        else if (i >= min && i < max)
                                n[i] = v[i - min];
                        else // i >= max
                                n[i] = v[max - i + (v.length-1)];
                }
                return n;
        }
	/**
	* Fit an image into a new image of newwidth * newwidth, where newwidth is next large power of 2 than width.
	* The image is centered in the center of the new image
	* and mirrored at the edges to avoid edge artifacts.
	* @param image a float[] with the image
	* @param width the width of image.
	* @return a float[] with a new tiled image newwidthxnewheight.
	*/
	public static float [] tile(float [] image, int width)
	{
		// Find the minimal power of 2 size equal to or larger than the size of a square widthxheight.
		int height = image.length / width;
                int maxSize = Math.max(width, height);
                int newwidth = minPower2(maxSize);
               // i now contains the minimal power of 2 size that fits around image.
                // Now center image in the center of the new bigger image.
		int x = (int) Math.round( (newwidth - width) / 2.0 );
		int y = (int) Math.round( (newwidth - height) / 2.0 );
		if (x < 0 || x > (newwidth -1) || y < 0 || y > (newwidth -1))
		{
			IJ.error("Image to be tiled is out of bounds.");
			return null;
		}
		ImageProcessor ip = new FloatProcessor(width, height, image, null);
		ImageProcessor ipout = ip.createProcessor(newwidth, newwidth);
		ImageProcessor ip2 = ip.crop();
		int w2 = ip2.getWidth();
		int h2 = ip2.getHeight();
		//how many times does ip2 fit into ipout?
		int i1 = (int) Math.ceil(x / (double) w2);
		int i2 = (int) Math.ceil( (newwidth - x) / (double) w2);
		int j1 = (int) Math.ceil(y / (double) h2);
		int j2 = (int) Math.ceil( (newwidth - y) / (double) h2);

		//tile
		if ( (i1%2) > 0.5)
			ip2.flipHorizontal();
		if ( (j1%2) > 0.5)
			ip2.flipVertical();

		for (int i=-i1; i<i2; i += 2) {
			for (int j=-j1; j<j2; j += 2) {
				ipout.insert(ip2, x-i*w2, y-j*h2);
			}
		}

		ip2.flipHorizontal();
		for (int i=-i1+1; i<i2; i += 2) {
			for (int j=-j1; j<j2; j += 2) {
				ipout.insert(ip2, x-i*w2, y-j*h2);
			}
		}

		ip2.flipVertical();
		for (int i=-i1+1; i<i2; i += 2) {
			for (int j=-j1+1; j<j2; j += 2) {
				ipout.insert(ip2, x-i*w2, y-j*h2);
			}
		}

		ip2.flipHorizontal();
		for (int i=-i1; i<i2; i += 2) {
			for (int j=-j1+1; j<j2; j += 2) {
				ipout.insert(ip2, x-i*w2, y-j*h2);
			}
		}
		float [] out = (float []) ipout.getPixels();
		return out;
	}
	/**
	* Fit a float image into a new power of 2 size image of newwidth * newwidth.
	* The image is centered in the center of the new image
	* and NOT mirrored
	* @param image a float[] with the image
	* @param width the width of image.
	* @return a float[] with a new square image newwidthxnewheight.
	*/
	public static float [] fit(float [] image, int width)
	{
		// Find the minimal power of 2 size equal to or larger than the size of a square widthxheight.
		int height = image.length / width;
		int maxSize = Math.max(width, height);
		int newwidth = 2;
		while (newwidth != maxSize && newwidth < 1.1 * maxSize)
	                 newwidth *= 2;
                // i now contains the minimal power of 2 size that fits around image.
                // Now center image in the center of the new bigger image.
		int x = (int) Math.round( (newwidth - width) / 2.0 );
		int y = (int) Math.round( (newwidth - height) / 2.0 );
		if (x < 0 || x > (newwidth -1) || y < 0 || y > (newwidth -1))
		{
			IJ.error("Image to be fitted is out of bounds.");
			return null;
		}

		ImageProcessor ip = new FloatProcessor(width, height, image, null);
		ImageProcessor ipout = ip.createProcessor(newwidth, newwidth);
                //ip.fill(0);
		ImageProcessor ip2 = ip.crop();
		int w2 = ip2.getWidth();
		int h2 = ip2.getHeight();
		ipout.insert(ip2, x, y);
		float [] out = (float []) ipout.getPixels();
		return out;
	}
	/**
	* Fit a square image into a smaller image widthxheight, with the center in the center of the new image.
	* @param spectrum a square image vector.
	* @param width the width in pixels of the new image.
	* @param height the height of the new image.
	* @return the centered resize spectrum.
	*/
 	public static float [] fit(float [] spectrum, int width, int height)
	{
		int oldwidth = (int) Math.sqrt(spectrum.length);
		float [] newimage = new float[width*height];
		int xcenterNew = width / 2;
		int ycenterNew = height / 2;
		int xcenterOld = oldwidth / 2;
		int ycenterOld = oldwidth / 2;
		for (int y = -height / 2; y < height / 2; y++)
		for (int x = -width / 2; x < width / 2; x++)
		{
			int indexnew = (ycenterNew + y)*width + xcenterNew + x;
			int indexold = (ycenterOld + y)*oldwidth + xcenterOld + x;
			newimage[indexnew] = spectrum[indexold];
		}
		return newimage;
	}
	/**
	* Fit a byte image into a new power of 2 size image of newwidth * newwidth.
	* The image is centered in the center of the new image
	* and NOT mirrored
	* @param image a byte[] with the image
	* @param width the width of image.
	* @return a byte[] with a new square image newwidthxnewheight.
	*/
	public static byte [] fit(byte [] image, int width)
	{
		// Find the minimal power of 2 size equal to or larger than the size of a square widthxheight.
		int height = image.length / width;
		int maxSize = Math.max(width, height);
		int newwidth = 2;
		while (newwidth != maxSize && newwidth < 1.1 * maxSize)
	                 newwidth *= 2;
                // i now contains the minimal power of 2 size that fits around image.
                // Now center image in the center of the new bigger image.
		int x = (int) Math.round( (newwidth - width) / 2.0 );
		int y = (int) Math.round( (newwidth - height) / 2.0 );

		if (x < 0 || x > (newwidth -1) || y < 0 || y > (newwidth -1))
		{
			IJ.error("Image to be fitted is out of bounds.");
			return null;
		}
		ImageProcessor ip = new ByteProcessor(width, height, image, null);
		ImageProcessor ipout = ip.createProcessor(newwidth, newwidth);
		ImageProcessor ip2 = ip.crop();
		ipout.insert(ip2, x, y);
		return (byte []) ipout.getPixels();
	}
        /**
         * Make a mask within an image widthxheight for Roi.
         * All pixels within Roi will be 1, all others 0.
         * March 2004, update to conform to ImageJ 1.32e and higher, where getMask returns a ByteProcessor.
         * @param roi the roi.
         * @param width the width of the mask.
         * @param height the height of the mask.
         * @return a float[] containing the mask bytes.
         */
        public static float [] getMask(Roi roi, int width, int height)
        {
                ImageProcessor mp = new ByteProcessor(width, height);
                ImagePlus np = new ImagePlus("mask in getMask", mp);
                np.setRoi(roi);
                np.show();
                ByteProcessor bmask = (ByteProcessor) roi.getMask();
                mp.setColor(1);
                mp.fill(bmask);
                // This is the mask.
                byte [] mask = (byte []) mp.getPixels();
                float [] fmask = new float[mask.length];
                for (int i = 0; i < mask.length; i++)
                        fmask[i] = (float) (mask[i] & 0xff);
                return fmask;
        }
        /**
        * Center a scaled image1 into the center of a larger image0.
        * The original image0 will not be modified.
        * @param image0 the base image
        * @param width0 the width in pixels of the base image
        * @param image1 the image to be inserted
        * @param width1 the width in pixels of the new image
        * @param scaling the amount by which image1 will be scaled.
        * @return a new image of the same size as image0.
        */
         public static float [] center(float [] image0, int width0, float [] image1, int width1, float scaling)
        {
                 return center(image0, width0, image1, width1, scaling, 0, 0);
        }
        /**
        * Center a scaled image1 into the center of a larger image0 with offset offset_x and offset_y.
        * The original image0 will not be modified.
        * @param image0 the base image
        * @param width0 the width in pixels of the base image
        * @param image1 the image to be inserted
        * @param width1 the width in pixels of the new image
        * @param scaling the amount by which image1 will be scaled.
        * @return a new image of the same size as image0.
        */
         public static float [] center(float [] image0, int width0, float [] image1, int width1, float scaling, int offset_x, int offset_y)
        {
                 int height0 = image0.length / width0;
                 int height1 = image1.length / width1;
                 ImageProcessor ip0 = new FloatProcessor(width0, height0, image0, null).duplicate();
                 ImageProcessor ip1 = new FloatProcessor(width1, height1, image1, null).duplicate();
                 int newwidth1 = (int) (width1 * scaling);
                 int newheight1 = (int) (height1 * scaling);
                 //System.out.println("1 "+ip1.getWidth()+" "+newwidth1+" ip1.height="+ip1.getHeight()+" "+newheight1);
                 ip1 = ip1.resize(newwidth1, newheight1);
                 //System.out.println("2"+ip1.getWidth()+" ip1.height="+ip1.getHeight());
                 int x = (int) Math.round( (width0 - ip1.getWidth()) / 2.0);
                 int y = (int) Math.round( (height0 - ip1.getHeight()) / 2.0);
                 //System.out.println("x="+x+" y="+y+" ip1.width="+ip1.getWidth()+" ip1.height="+ip1.getHeight());
                 ip0.insert(ip1, x + offset_x, y + offset_y);
                 return (float []) ip0.getPixels();
        }
        /**
         * Save vector v as a text file. Tab separated.
         * @param v the float[] vector
         * @param path a String with the path to save the vector to.
         * @return true if succesful, false if Exception.
         */
        public static boolean saveAsText(float [] v, String path)
        {
                PrintWriter pw = null;
                try
                {
                        FileOutputStream fos = new FileOutputStream(path);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        pw = new PrintWriter(bos);
                }
                catch (IOException e)
                {
                        IJ.error("Cannot save vector (saveAsText)"+path+" "+ e);
                        return false;
                }
                for (int j = 0; j < v.length; j++)
                        pw.println(v[j]);
                pw.close();
                return true;
        }
        public static float [] tofloat(double [] v)
        {
                float [] n = new float[v.length];
                for (int i = 0; i < v.length; i++) n[i] = (float) v[i];
                return n;
        }
        public static String toString(float [][] m)
        {
                StringBuffer sb = new StringBuffer("Matrix (");
                int iN = m.length; int iM = m[0].length;
                sb.append(iN); sb.append("x"); sb.append(iM);
                sb.append("):\n");
                for (int j = 0; j < iN; j++)
                {
                        for (int i = 0; i < iM; i++)
                        {
                                 sb.append(m[j][i]);
                                 sb.append("\t");
                        }
                        sb.append("\n");
                }
                return sb.toString();
        }
        public static String toString(double [][] m)
        {
                StringBuffer sb = new StringBuffer("Matrix (");
                int iN = m.length; int iM = m[0].length;
                sb.append(iN); sb.append("x"); sb.append(iM);
                sb.append("):\n");
                for (int j = 0; j < iN; j++)
                {
                        for (int i = 0; i < iM; i++)
                        {
                                 sb.append(m[j][i]);
                                 sb.append("\t");
                        }
                        sb.append("\n");
                }
                return sb.toString();
        }
        public static String toString(int [][] m)
        {
                StringBuffer sb = new StringBuffer("Matrix (");
                int iN = m.length; int iM = m[0].length;
                sb.append(iN); sb.append("x"); sb.append(iM);
                sb.append("):\n");
                for (int j = 0; j < iN; j++)
                {
                        for (int i = 0; i < iM; i++)
                        {
                                 sb.append(m[j][i]);
                                 sb.append("\t");
                        }
                        sb.append("\n");
                }
                return sb.toString();
        }
        public static String toString(float [] m)
        {
                StringBuffer sb = new StringBuffer("Vector (");
                int iN = m.length;
                sb.append(iN);
                sb.append("), ");
                for (int j = 0; j < iN; j++)
                {
                        sb.append(m[j]);
                        sb.append(", ");
                }
                return sb.toString();
        }
        public static String toString(double [] m)
        {
                StringBuffer sb = new StringBuffer("");
                int iN = m.length;
                sb.append(iN);
                sb.append("):\n");
                for (int j = 0; j < iN; j++)
                {
                        sb.append(m[j]);
                        sb.append("\t");
                }
                return sb.toString();
        }
}

