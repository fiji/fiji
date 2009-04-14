package volume;
import ij.*;

/**
 * This class implements a Gabor (even) cosine wave 1d convolution kernel
 * for separated convolutions.
 *
 * (c) 1999-2002 Michael Abramoff. All rights reserved.
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
public class GaborCos extends Kernel1D
{
    /**
     * Create a Gabor cosine kernel of width sigma tuned to w.
     * @param sigma, the standard deviation of the associated probability function of the Gaussian envelope function.
     * @param w, the frequency of the cosine function.
     */
    public GaborCos(double sigma, double w)
    {
        if (sigma != 0)
        {
            int width = (int) (6 * sigma + 1);
	          if ((width % 2) == 0) width++;     // center around an image (must be odd)
	                halfwidth = width / 2;       // on both sides!
            k = new double[halfwidth*2+1];
            final int sw = 2;
            for (int l = -halfwidth; l <= halfwidth; l++)
		            k[l+halfwidth-sw*l] = 1 / (Math.sqrt(2*Math.PI)*sigma) * Math.cos(1*l*w) * Math.exp(-(l*l) / (2*sigma*sigma));
        }
        else
            IJ.error("Error initializing Gabor Sin kernel");
    }
}
