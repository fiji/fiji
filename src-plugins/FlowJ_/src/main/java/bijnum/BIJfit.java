package bijnum;
import java.lang.Math.*;
/**
  * This class implements useful linear and polynomial regression
  * from
  * Press, Flannery, Teukolsky, Vetterling, Numerical Recipes in C 2nd ed, Cambridge University Press, 1986
  * Copyright implementation (c) 1999-2004, Michael Abramoff. All rights reserved.
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
public class BIJfit
{
        /**
         * Given a line with points x, y, fit a line
         * y = a1 + a2x through them with linear least squares regression.
         * @param x a float array with x values.
         * @param y a float array with y values.
         * @return fit, a double[4] with fit[0] = a1, fit[1] = a2, fit[2] = var(a1), fit[3] = var(a2),
         * where var(a) = STDEV(a)^2.
         */
        static public double [] linear(float [] x, float [] y)
        {
               double [] parameters = new double[4];
               double sx=0.0; double sy=0.0;
               double ss = x.length;
               for (int i=0; i < x.length; i++)
               {
                       sx  += x[i];
                       sy  += y[i];
               }
               double sxoss = sx / ss;
               double b = 0; double st2 = 0;
               for (int i=0; i < x.length; i++)
               {
                       double t = x[i] - sxoss;
                       st2 += t*t;
                       b  += t * y[i];
               }
               // a1
               parameters[0] = (sy - sx * b) / ss;
               // a2
               parameters[1] = b / st2;
               // Errors (sd**2) on:
               // var(a1)
               parameters[2] =(1.0 + sx * sx/(ss * st2)) / ss;
               // var(a2)
               parameters[3] = 1.0 / st2;
               return parameters;
       }
       /**
        * Given a line with points x, y, fit a line
        * y = bx + a through them with linear least squares regression.
        * @param x a float array with x values.
        * @param y a float array with y values.
        * @return fit, a double[4] with fit[0] = a, fit[1] = b, fit[2] = var(a), fit[3] = var(b),
        * where var(a) = STDEV(a)^2.
        */
       static public double [] poly(float [] x, float [] y)
       {
              double [] parameters = new double[4];
              double sx=0.0; double sy=0.0;
              double ss = x.length;
              for (int i=0; i < x.length; i++)
              {
                      sx  += x[i];
                      sy  += y[i];
              }
              double sxoss = sx / ss;
              double b = 0; double st2 = 0;
              for (int i=0; i < x.length; i++)
              {
                      double t = x[i] - sxoss;
                      st2 += t*t;
                      b  += t * y[i];
              }
              // a
              parameters[0] = (sy - sx * b) / ss;
              // b
              parameters[1] = b / st2;
              // Errors (sd**2) on:
              // var(a)
              parameters[2] =(1.0 + sx * sx/(ss * st2)) / ss;
              // var(b)
              parameters[3] = 1.0 / st2;
              return parameters;
      }
}
