package volume;
import ij.*;

/**
 * This is a 1D separated 0-th order Gaussian convolution kernel.
 * G(x) = 1.0 / (Math.sqrt(2 Math.PI) * sigma)) * e^(-l * x / (2.0 * sigma^2))
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
public class Gaussian extends Kernel1D
{
        protected double sigma;

        public Gaussian() {}
        /**
        * Create a Gaussian kernel from standard deviation sigma.
        * @param sigma, the standard deviation of the associated probability function of the Gaussian function.
        */
        public Gaussian(double sigma)
        {
                this.sigma = sigma;
                if (sigma != 0)
                {
                        int width = (int) (6 * sigma + 1);
                        if (width % 2 == 0) width++;
                        halfwidth = width / 2;
                        k = new double[halfwidth*2+1];
                        for (int l = -halfwidth; l <= halfwidth; l++)
                                    k[l + halfwidth] = function(l);
                }
                else
                        halfwidth = 0;
        }
        public double getSigma() { return sigma; }
        /**
        * Compute Difference of Gaussian (DoG) function at x symmetric around 0.
        * @param x the x position
        * @return a double with the value of the Gaussian at x.
        */
        protected double function(double x)
        {
                return (1.0 / (Math.sqrt(2.0 * Math.PI) * sigma)) *  Math.exp(-x*x / (2.0 * sigma * sigma));
        }
        /**
         * Preset Gaussian for a specific kernel width.
         * 3 and 5 widths only!
         * @deprecated
         */
        public Gaussian(int width)
        {
                if (width == 3)
                {
                    halfwidth = width/2;
                    k = new double[halfwidth*2+1];
                    k[0] = 0.25; k[1] = 0.5; k[2] = 0.25;
                }
                else if (width == 5)
                {
                    halfwidth = width/2;
                    k = new double[halfwidth*2+1];
                    k[0] = 0.0625; k[1] =0.25; k[2] = 0.375; k[3] = 0.25; k[4] = 0.0625;
                }
                else
                {
                    IJ.error("Error initializing Gaussian kernel.");
                }
        }
        public String toString() { return "Gauss"+sigma;  }
}
