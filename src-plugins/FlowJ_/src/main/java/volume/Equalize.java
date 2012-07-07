package volume;
import bijnum.*;
import ij.*;
/**
 * Plugin containing methods to equalize imageas and volumes.
 * It is mainly useful to correct vignetting, i.e. anisotropic nonlinear illumination differences over the retinal image.
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
public class Equalize
{
	private final static int DEFAULTWINDOWSIZE = 20;

	public static int getDefaultWindowSize() { return DEFAULTWINDOWSIZE; }
	/**
	 * Perform sliding window averaging on image. Does NOT modify image.
	 * center will be the average value of the equalized image.
	 * Implements:
	 * I(x) = I(x) + m - Iavg(x, w)
	 * where Iavg(x,w) is the average intensity around x, and w the window size.
	 * Hoover, Goldbaum, IEEE TMI 22,8, pp 955, 2003.
	 * Can process masked images, where non-valid pixels have been set to NaN.
	 *
	 * @param image a float[] with the image vector.
	 * @param width the width of image in pixels.
	 * @param center a float value relative to which all pixels are equalized.
	 * @param windowSize the size of the sliding windows in pixels.
	 * @return a float[] with the equalized version of image.
	 */
	public static float [] sliding(float [] image, int width, float center, int windowSize, boolean doShowProgress)
	{
		float [] equalized = new float[image.length];
		int height = image.length / width;
		for (int y = 0; y < height; y++)
		{
		        if (doShowProgress) IJ.showProgress(y, height);
			for (int x = 0; x < width; x++)
			{
				float iPixel = image[y*width+x];
				float aggr = 0; int n = 0;
				for (int j = -windowSize/2; j < windowSize/2; j++)
				for (int i = -windowSize/2; i < windowSize/2; i++)
				{
					int q = y + j;
					if (q < 0) q = 0;
					if (q >= height) q = height-1;
					int p = x + i;
					if (p < 0) p = 0;
					if (p >= width) p = width-1;
					float pixel = image[q*width+p];
					if (pixel != Float.NaN)
					{
						aggr += pixel;
						n++;
					}
				}
				equalized[y*width+x] = iPixel + center - aggr / n;
			}
		}
		return equalized;
	}
}

