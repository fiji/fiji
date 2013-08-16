package bijnum;
import java.lang.Math.*;
/**
  * This class implements useful numerical functions from
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
public class BIJfunctions
{
	public static double gammaln(double xx)
        {
		double stp = 2.5066282746310005;
		double cof[] = new double[6];
		cof[0]= 76.18009172947146;
		cof[1]=-86.50532032941677;
		cof[2]= 24.01409824083091;
		cof[3]=-1.231739572450155;
		cof[4]= 0.1208650973866179e-02;
		cof[5]=-0.5395239384953E-05;

		double x;
                double y = x = xx;
		double tmp = x + 5.5;
		tmp -= (x + 0.5)*java.lang.Math.log(tmp);
		double ser = 1.000000000190015;
		for (int j=0; j<=5; j++) ser += cof[j]/++y;
                return -tmp + java.lang.Math.log(stp*ser/x);
	}
        public static double betai(double a, double b, double x)
        {
                double bt;

                if (x < 0 || x > 1)
                {
                        System.out.println("Bad x="+x+" in routine betai");
                        bt = Double.NaN;
                }
                else if (x == 0 || x == 1) bt = 0;
                else  bt = Math.exp(gammaln(a+b)-gammaln(a)-gammaln(b)+a*Math.log(x)+b*Math.log(1-x));
                if (x < (a+1)/(a+b+2))
                        bt = bt*betacf(a, b, x)/a;
                else bt = 1-bt*betacf(b, a, 1-x)/b;
                return bt;
        }
        public static double betacf(double a, double b, double x)
        {
                double MAXIT = 100;
                double EPS = 3e-7;
                double FPMIN = 1e-30;
                int m, m2;
                double aa, c, d, del, h, qab, qam, qap;

                qab = a+b;
                qap = a+1;
                qam = a-1;
                c = 1;
                d = 1-qab*x/qap;
                if (Math.abs(d) < FPMIN) d = FPMIN;
                d = 1/d;
                h = d;
                for (m = 1; m <= MAXIT; m++)
                {
                        m2 = 2*m;
                        aa = m*(b-m)*x/((qam+m2)*(a+m2));
                        d = 1+aa*d;
                        if (Math.abs(d) < FPMIN) d = FPMIN;
                        c = 1+aa/c;
                        if (Math.abs(c) < FPMIN) c = FPMIN;
                        d = 1/d;
                        h *= d*c;
                        aa = -(a+m)*(qab+m)*x/((a+m2)*(qap+m2));
                        d = 1+aa*d;
                        if (Math.abs(d) < FPMIN) d = FPMIN;
                        c = 1+aa/c;
                        if (Math.abs(c) < FPMIN) c = FPMIN;
                        d = 1/d;
                        del = d*c;
                        h *= del;
                        if (Math.abs(del-1) < EPS) break;
                }
                if (m > MAXIT)
                {
                        System.out.println("a or b too big, or MAXIT too small in betacf");
                        return Double.NaN;
                }
                else
                        return h;
        }
}
