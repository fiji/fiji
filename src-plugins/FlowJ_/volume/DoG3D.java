package volume;
import ij.*;

/**
 * This is a 3D Difference of Gaussian convolution kernel.
 *
 * DoG2D(x) = 1 / Math.sqrt(2 Math.PI * sigma^2) e^(-(x^2+y^2) / 2 sigma^2) - 1 / Math.sqrt(2 Math.PI * (1.6 sigma)^2) e^(-(x^2+y^2) / 2 (1.6 sigma)^2)
 *
 * @author: Michael Abramoff, (c) 1999-2003 Michael Abramoff. All rights reserved.
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
public class DoG3D extends Gaussian3D
{
        public DoG3D(double sigma)
        {
                this.sigma = sigma;
                if (sigma != 0)
                {
                        int width = (int) (6 * (sigma*1.6) + 1);
                        if (width % 2 == 0) width++;
                        halfwidth = width / 2;
                        k = new double[halfwidth*2+1][halfwidth*2+1][halfwidth*2+1];
                        for (int n = -halfwidth; n <= halfwidth; n++)
                        for (int m = -halfwidth; m <= halfwidth; m++)
                        for (int l = -halfwidth; l <= halfwidth; l++)
                                k[n+halfwidth][m+halfwidth][l+halfwidth] = function(l,m,n);
                }
                else
                        halfwidth = 0;
        }
        /**
        * Compute 3D Difference of Gaussian function (DoG) at x, y.
        * @param x the x position
        * @param y the y position.
        * @param z the z position.
        * @return a double with the value of the 3D Difference of Gaussian at x, y.
        */
        protected double function(double x, double y, double z)
        {
                return 1.0 / (Math.pow(2 * Math.PI, 2) * Math.pow(sigma, 3)) * Math.exp(-(x*x + y*y + z*z) / (2 * Math.pow(sigma,2)))
                        -
                        1.0 / (Math.pow(2 * Math.PI, 2) * Math.pow(sigma*1.6, 3)) * Math.exp(-(x*x + y*y + z*z) / (2 * Math.pow(sigma*1.6,2)));
        }
        public String toString() { return "DoG 2D "+sigma;  }
}
