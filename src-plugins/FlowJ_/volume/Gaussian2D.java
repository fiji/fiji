package volume;
import ij.*;

/**
 * This is a 2D separated 0 order Gaussian convolution kernel.
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
public class Gaussian2D extends Kernel2D
{
        protected double sigma;

        public Gaussian2D() { sigma = 0; }
        public Gaussian2D(double sigma)
        {
                this.sigma = sigma;
                if (sigma != 0)
                {
                        int width = (int) (6 * sigma + 1);
                        if (width % 2 == 0) width++;
                        halfwidth = width / 2;
                        k = new double[halfwidth*2+1][halfwidth*2+1];
                        for (int m = -halfwidth; m <= halfwidth; m++)
                        for (int l = -halfwidth; l <= halfwidth; l++)
                                k[m+halfwidth][l+halfwidth] = function(l, m);
                }
                else
                        halfwidth = 0;
        }
        /**
        * Compute Gaussian function at x, y symmetric around 0,0.
        * @param x the x position
        * @param y the y position.
        * @return a double with the value of the 2D Gaussian at x, y.
        */
        protected double function(double x, double y)
        {
                return 1.0 / (2 * Math.PI * Math.pow(sigma,2)) * Math.exp(-(x*x + y*y) / (2 * Math.pow(sigma,2)));
        }
        public double getSigma() { return sigma; }
        public String toString() { return "Gauss 2D"+sigma;  }
}
