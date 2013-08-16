package volume;
import ij.*;

/**
 * This class implement a scale space Gaussian n-th order derivative 1d convolution kernel for separated convolutions.
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
public class GaussianDerivative extends Kernel1D
{
	protected double scale;
	protected int order;

        /**
        * Create a Gaussian first order derivative kernel for scale.
        * @param scale, the scale space of the kernel (in pixels).
        */
        public GaussianDerivative(double scale)
        {
                this.scale = scale;
                double sigma = Math.sqrt(2* scale);
                this.order = 1;
                if (sigma != 0)
                        kFirst(sigma);
        }
        /**
        * Create a Gaussian n'th order derivative kernel for scale.
        * @param scale, the scale space of the kernel (in pixels).
        */
        public GaussianDerivative(double scale, int order)
        {
                this.scale = scale;
                double sigma = Math.sqrt(2 * scale);
		this.order = order;
                if (sigma != 0)
                {
			switch (order)
			{
					case 0: kZero(sigma); break;
					case 1: kFirst(sigma); break;
					case 2: kSecond(sigma); break;
					default: System.out.println("GaussianDerivative: order too large.");
			}
		}
                else System.out.println("GaussianDerivative: order too large.");
	}
	private void kZero(double sigma)
	{
                int width = (int) (6 * sigma + 1);
                if (width % 2 == 0) width++;
                halfwidth = width / 2;
                k = new double[halfwidth*2+1];
                double sigmaSquare = sigma * sigma;
                for (int l = -halfwidth; l <= halfwidth; l++)
                        k[l + halfwidth] = (1.0 / (Math.sqrt(2 * Math.PI) * sigma)) *
                                Math.exp(-l*l / (2 * sigmaSquare));
	}
	private void kFirst(double sigma)
	{
                int width = (int) (6 * sigma + 1);
                if (width % 2 == 0) width++;
                        halfwidth = width / 2;
                k = new double[halfwidth*2+1];
                double sigmaSquare = sigma * sigma;
                double factor = 1.0 / (Math.sqrt(2 * Math.PI) * sigma * sigma * sigma);
                for (int l = -halfwidth; l <= halfwidth; l++)
                        k[l + halfwidth] =  - factor * l * Math.exp(-l*l / (2 * sigmaSquare));
	}
	private void kSecond(double sigma)
	{
                int width = (int) (6 * sigma + 1);
                if (width % 2 == 0) width++;
                        halfwidth = width / 2;
                k = new double[halfwidth*2+1];
                double sigmaSquare = sigma * sigma;
                double factor = 1.0 / (Math.sqrt(2 * Math.PI) * sigma * sigma * sigma);
                for (int l = -halfwidth; l <= halfwidth; l++)
                        k[l + halfwidth] =  (Math.pow(l, 2) / sigmaSquare - 1)
                                        * factor * Math.exp(-l*l / (2 * sigmaSquare));
	}
	public String toString() { return "Gaussian kernel order "+order+" scale="+scale;  }
    /*Kernel:
    -0.0012
    -0.0035
    -0.0086
    -0.0173
    -0.0284
    -0.0373
    -0.0371
    -0.0236
    0.0000
    0.0236
    0.0371
    0.0373
    0.0284
    0.0173
    0.0086
    0.0035
    0.0012
    sum: -0.0000
  sum: -0.0000  */
}
