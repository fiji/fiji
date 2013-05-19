package bijnum;
import java.awt.Rectangle;
import ij.*;

/**
 * This class implements a one-dimensional real->complex fast fourier transform
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
public class BIJfft
{
	/** Local variables used in BIJfht algorithms. */
	protected int n, nu, n2, nu1;
        protected double [] C;
        protected double [] S;
        protected double [] dx;
        protected float [] x;

        /**
         * Set up a BIJfft a matrix x of length power of 2.
         * @param x the real array for which you will compute the 1-D FFT.
         */
        public BIJfft(float [] x)
        {
                // assume n is a power of 2
                this.n = x.length;
                this.nu = (int)(Math.log(n)/Math.log(2));
                this.n2 = n/2;
                this.nu1 = nu - 1;
                this.x = x;
                this.dx = null;
                // Precompute the sin/cos tables.
                makeSinCosTables();
        }
        /**
         * Set up a BIJfft a matrix x of length power of 2.
         * @param x the real array for which you will compute the 1-D FFT.
         */
        public BIJfft(double [] dx)
        {
                // assume n is a power of 2
                this.n = dx.length;
                this.nu = (int)(Math.log(n)/Math.log(2));
                this.n2 = n/2;
                this.nu1 = nu - 1;
                this.x = null;
                this.dx = dx;
        }
        /**
         * Compute the 1-D Fast fourier transform for x.
         * x must have the same length as BIJfft was initialized with to make sure all tables are still valid.
         * @param a vector x (power 2 length)
         * @return the power spectrum as a float[].
         */
        public float [] compute(float [] x)
        {
                if (x.length != n)
                        throw new IllegalArgumentException("BIJfft not properly initialized");
                float[] xre = new float[n];
                float[] xim = new float[n];
                for (int i = 0; i < n; i++)
                {
                        xre[i] = x[i];
                        xim[i] = 0;
                }
                int k = 0;
                int localnu1 = nu1;
                int localn2 = n2;
                for (int l = 1; l <= nu; l++)
                {
                        while (k < n)
                        {
                                for (int i = 1; i <= localn2; i++)
                                {
                                        // Get the proper values from the sin and cos tables.
                                        int p = bitrev (k >> localnu1);
                                        float c = (float) C[p];
                                        float s = (float) S[p];
                                        double arg = 2 * Math.PI * p / n;
                                        //if (c != (float) Math.cos (arg) || s != (float) Math.sin (arg))
                                        //        System.out.println("k="+k+" "+c+" "+(float) Math.cos (arg)+" "+s+" "+(float) Math.sin (arg));
                                        c = (float) Math.cos (arg);
                                        s = (float) Math.sin (arg);
                                        float tr = xre[k+localn2]*c + xim[k+localn2]*s;
                                        float ti = xim[k+localn2]*c - xre[k+localn2]*s;
                                        xre[k+localn2] = xre[k] - tr;
                                        xim[k+localn2] = xim[k] - ti;
                                        xre[k] += tr;
                                        xim[k] += ti;
                                        k++;
                                }
                                k += localn2;
                        }
                        k = 0;
                        localnu1--;
                        localn2 = localn2/2;
                }
                k = 0;
                int r;
                while (k < n)
                {
                        r = bitrev (k);
                        if (r > k)
                        {
                                float tr = xre[k];
                                float ti = xim[k];
                                xre[k] = xre[r];
                                xim[k] = xim[r];
                                xre[r] = tr;
                                xim[r] = ti;
                        }
                        k++;
                }
                float[] mag = new float[n/2];
                mag[0] = (float) (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/n;
                for (int i = 1; i < n/2; i++)
                        mag[i]= 2 * (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/n;
                return mag;
        }
        /**
         * Compute power spectrum of FFT 1-D.
         * Similar implementation.
         */
        public final double [] mag()
        {
                // assume n is a power of 2
                n = dx.length;
                nu = (int)(Math.log(n)/Math.log(2));
                int n2 = n/2;
                int nu1 = nu - 1;
                double[] xre = new double[n];
                double[] xim = new double[n];
                double[] mag = new double[n2];
                double tr, ti, p, arg, c, s;
                for (int i = 0; i < n; i++)
                {
                        xre[i] = dx[i];
                        xim[i] = 0;
                }
                int k = 0;

                for (int l = 1; l <= nu; l++)
                {
                        while (k < n)
                        {
                                for (int i = 1; i <= n2; i++)
                                {
                                        p = bitrev (k >> nu1);
                                        arg = 2 * Math.PI * p / n;
                                        c = Math.cos (arg);
                                        s = Math.sin (arg);
                                        tr = xre[k+n2]*c + xim[k+n2]*s;
                                        ti = xim[k+n2]*c - xre[k+n2]*s;
                                        xre[k+n2] = xre[k] - tr;
                                        xim[k+n2] = xim[k] - ti;
                                        xre[k] += tr;
                                        xim[k] += ti;
                                        k++;
                                }
                                k += n2;
                        }
                        k = 0;
                        nu1--;
                        n2 = n2/2;
                }
                k = 0;
                int r;
                while (k < n)
                {
                        r = bitrev (k);
                        if (r > k)
                        {
                                tr = xre[k];
                                ti = xim[k];
                                xre[k] = xre[r];
                                xim[k] = xim[r];
                                xre[r] = tr;
                                xim[r] = ti;
                        }
                        k++;
                }

                mag[0] = (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/n;
                for (int i = 1; i < n/2; i++)
                        mag[i]= 2 * (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/n;
                return mag;
        }
	/**
	* Can be used to show progress in other programs.
	*/
	protected void progress(double percent)
	{
		IJ.showProgress(percent);
	}
        protected int bitrev(int j)
        {
                int j1 = j;
                int k = 0;
                for (int i = 1; i <= nu; i++)
                {
                        int j2 = j1/2;
                        k  = 2*k + j1 - 2*j2;
                        j1 = j2;
                }
                return k;
        }
        /**
         * Calculate the sin and cos tables for the current vector length.
         * Saves a lot of time in the main loop.
        */
	protected void makeSinCosTables()
	{
                // First determine max k.
                int pmax = -Integer.MAX_VALUE;
                int localnu1 = nu1;
                int localn2 = n2;
                int k = 0;
                for (int l = 1; l <= nu; l++)
                {
                        while (k < n)
                        {
                                for (int i = 1; i <= localn2; i++)
                                {
                                        int p = bitrev (k >> localnu1);
                                        if (p > pmax)
                                                pmax = p;
                                        k++;
                                }
                                k += localn2;
                        }
                        k = 0;
                        localnu1--;
                        localn2 = localn2/2;
                }
                localnu1 = nu1;
                localn2 = n2;
                try
                {
                        C = new double[pmax+1];
                        S = new double[pmax+1];
                        // Now calculate actual table values.
                        k = 0;
                        for (int l = 1; l <= nu; l++)
                        {
                                while (k < n)
                                {
                                        for (int i = 1; i <= localn2; i++)
                                        {
                                                int p = bitrev (k >> localnu1);
                                                double arg = 2 * Math.PI * (double) p / n;
                                                C[p] = Math.cos (arg);
                                                S[p] = Math.sin (arg);
                                                k++;
                                        }
                                        k += localn2;
                                }
                                k = 0;
                                localnu1--;
                                localn2 = localn2/2;
                        }
                } catch (Exception e) { e.printStackTrace(); }
                System.out.println("FFT sin and cos tables length "+(pmax+1));
	}
        public static void testsin(float [] wave)
        {
                double l = 16;
                for (int i = 0; i < wave.length; i++)
                        wave[i] = (float) Math.sin(((double)i) * (2 * Math.PI) / l);
        }
        public final float[][] compute2(float[] x)
        {
            // assume n is a power of 2
            n = x.length;
            nu = (int)(Math.log(n)/Math.log(2));
            int n2 = n/2;
            int nu1 = nu - 1;
            float[] xre = new float[n];
            float[] xim = new float[n];
            float[][] mag = new float[2][n2];
            float tr, ti, p, arg, c, s;
            for (int i = 0; i < n; i++) {
                xre[i] = x[i];
                xim[i] = 0.0f;
            }
            int k = 0;

            for (int l = 1; l <= nu; l++) {
                while (k < n) {
                    for (int i = 1; i <= n2; i++) {
                        p = bitrev (k >> nu1);
                        arg = 2 * (float) Math.PI * p / n;
                        c = (float) Math.cos (arg);
                        s = (float) Math.sin (arg);
                        tr = xre[k+n2]*c + xim[k+n2]*s;
                        ti = xim[k+n2]*c - xre[k+n2]*s;
                        xre[k+n2] = xre[k] - tr;
                        xim[k+n2] = xim[k] - ti;
                        xre[k] += tr;
                        xim[k] += ti;
                        k++;
                    }
                    k += n2;
                }
                k = 0;
                nu1--;
                n2 = n2/2;
            }
            k = 0;
            int r;
            while (k < n) {
                r = bitrev (k);
                if (r > k) {
                    tr = xre[k];
                    ti = xim[k];
                    xre[k] = xre[r];
                    xim[k] = xim[r];
                    xre[r] = tr;
                    xim[r] = ti;
                }
                k++;
            }

            mag[0][0] = (float) (Math.sqrt(xre[0]*xre[0] + xim[0]*xim[0]))/n;
            mag[1][0] = 0;
            for (int i = 1; i < n/2; i++)
            {
                        mag[0][i]= 2 * (float) (Math.sqrt(xre[i]*xre[i] + xim[i]*xim[i]))/n;
                        mag[1][i] = i;
            }
            return mag;
        }
        /*
         * Extend x to a power of 2 size vector by mirroring, and also convert to float on the fly.
        */
        public static double [] extend(double [] x)
        {
                // Determine minimal size.
                int n = BIJutil.minPower2(x.length);
                double [] xx = new double[n];
                for (int i = 0; i < x.length; i++)
                        xx[i] = x[i];
                for (int i = x.length; i < xx.length; i++)
                        // wrap
                        xx[i] = x[x.length + (x.length - i) - 1];
                        //xx[i] = 0;
                return xx;
        }
        /*
         * Extend x to a power of 2 size vector by mirroring, and also convert to float on the fly.
        */
        public static double [] extend(float [] x)
        {
                // Determine minimal size.
                int n = BIJutil.minPower2(x.length);
                double [] xx = new double[n];
                for (int i = 0; i < x.length; i++)
                        xx[i] = x[i];
                for (int i = x.length; i < xx.length; i++)
                        // wrap
                        xx[i] = x[x.length + (x.length - i) - 1];
                        //xx[i] = 0;
                return xx;
        }
}

