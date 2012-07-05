package VolumeJ;
import ij.*;
import volume.*;

/**
 * This class implements cubic spline interpolation
 * and interpolation of gradients.
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
public abstract class VJSplineInterpolator extends VJInterpolator
{
        /**
         * Cubic spline interpolation.
         * Also known as Catmull-Rom spline
         * Calculate sample = h(x) a + h(x) b + h(x) c + h(x) d,
         * where a,b,c,d are the values at the sample locations, and x (-2,2) is the location between
         * a and d.
         * @param x {0,1} the location at which to interpolate the sample
         * @param a,b,c,d the values of the samples at -1, 0, 1, 2
         * @return the interpolated value
         */
        private static double cubicspline(double x, double a, double b, double c, double d)
        {
                return h(-1.0-x) * a + h(-x) * b + h(1.0-x) * c + h(2.0-x) * d;
        }
        /**
         * Calculate spline h(x) value.
         *              h(-x)                           x<0
         * h(x) =       3/2x^3-5/2x^2+1                 0 <= x < 1
         *              -1/2x^3 + 5/2x^2 - 4x + 2       1 <= x < 2
         *              0                               otherwise
         *  @param x the value for which to calculate h.
         *  @return the h value as above.
         */
        private static double h(double x)
        {
                if (x < 0) x = -x;
                if (x >= 0 && x < 1)
                        return Math.pow(x, 3) * 1.5 - 2.5 * Math.pow(x, 2) + 1.0;
                else if (x >= 1 && x < 2)
                        return - Math.pow(x, 3) * 0.5 + 2.5 * Math.pow(x, 2) - 4.0 * x + 2.0;
                else
                        return 0;
        }
}
