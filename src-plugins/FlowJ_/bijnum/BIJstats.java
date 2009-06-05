package bijnum;

import java.util.*;
import ij.*;
import ij.gui.*;
import volume.*;
import bijnum.*;

/**
 * BIJ statistical methods inherited from MatLab. Syntax is as close as possible to matlab.
 *
 * Copyright (c) 1999-2003, Michael Abramoff. All rights reserved.
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
public class BIJstats
{
        /**
         * Conversion from standard error to confidence intervals at different significance levels.
         * To make a confidence interval (99%, 2.576; 95%, 1.96; 99.9 3.39).
         */
        public static float CI95 = 1.96f; // 95% significance level.
        public static float CI99 = 2.576f;// 99% significance level.
        public static float CI99_9 = 3.39f;// 99.9%significance level.

        public static double getSignificanceLevel(double significance)
        {
                if (significance <= 0.95)
                        return CI95;
                else if (significance == 0.99)
                        return CI99;
                else
                        return CI99_9;
        }
	/**
	* Compute the covariance matrix for a matrix of size NxM. (= m (TRANSPOSE(m)/length(m))
	* The returned matrix will be size NxN.
	* @param m a matrix of float[N][M]
	* @return a matrix of covariances float[N][N]
	*/
	public static float [][] covariance(float [][] m, boolean doShowProgress)
	{
                float [][] cov = new float[m.length][m.length];
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m.length; i++)
                {
                        if (doShowProgress)
                                IJ.showProgress(j, m.length);
                        double r = 0;
                        // Multiply m with its own transpose.
                        for (int k = 0; k < m[0].length; k++)
                                 r += m[j][k] * m[i][k];
                        cov[j][i] = (float) r;
                }
                // Normalize.
                BIJmatrix.mul(cov, cov, 1f/m.length);
                return cov;
	}
        public static int n(float [] v) { return v.length; }
        public static int n(float [] v, float [] mask)
        {
                int n = 0;
                for (int j = 0; j < v.length; j++)
                {
                        if (mask[j] != 0)
                              n++;
                }
                return n;
        }
	/*
	* avg(2) computes the avg of the elements of vector v only where the mask value is not 0.
	* @param v a float[] vector.
	* @param mask a float[] with a value of ! 0 for all elements of v that are valid.
	* @return the avg of the masked elements in v.
	*/
	public static float avg(float [] v, float [] mask)
	{
		return (float) sum(v, mask) / n(v, mask);
	}
        public static String getSignificanceString(double sign)
        {
                String s = "<unknown>";
                if (sign == CI95)
                        s = "95%";
                else if (sign == CI99)
                        s = "99%";
                else if (sign == CI99_9)
                        s = "99.9%";
                return s;

        }
        /**
         * stdev(2) computes the stddev of the elements of only where the mask value is not 0.
         * See Press, Numerical recipes in C, pp 617 for the computation of variance using the two pass algorithm.
         * @param v a float[] vector.
         * @param mask a float[] with a value of ! 0 for all elements of v that are valid.
         * @return the stddev of the masked elements in v.
         */
        public static float stdev(float [] v, float [] mask)
        {
                double avg = avg(v, mask);
                double var = 0;
                double eps = 0;
                int n = 0;
                for (int j = 0; j < v.length; j++)
                {
                        if (mask[j] != 0)
                        {
                              double d = v[j] - avg;
                              eps += d;
                              var += d * d;
                              n++;
                        }
                }
                var = (var - eps * eps / n) / (n-1);
                float stdev = (float) Math.sqrt(var);
                return stdev;
        }
        /**
         * sem(1) computes the standard error of the mean of the elements of v.
         * Standard error is the standard deviation divided by the square root of the number of elements.
         * See Press, Numerical recipes in C, pp 617 for the computation of variance using the two pass algorithm.
         * @param v a float[] vector.
         * @return the standard error of the mean of all elements in v.
         */
        public static double sem(float [] v)
        {
                double avg = avg(v);
                double var = 0;
                double eps = 0;
                for (int j = 0; j < v.length; j++)
                {
                        double s = v[j] - avg;
                        eps += s;
                        var += s * s;
                }
                var = (var - eps * eps / v.length) / (v.length-1);
                double stdev = Math.sqrt(var);
                double sem = stdev / Math.sqrt(v.length);
                return sem;
        }
        /**
         * sem(2) computes the standard error of the mean of the elements of v only where the mask value is not 0.
         * Standard error is the standard deviation divided by the square root of the number of elements.
         * See Press, Numerical recipes in C, pp 617 for the computation of variance using the two pass algorithm.
         * @param v a float[] vector.
         * @param mask a float[] with a value of ! 0 for all elements of v that are valid.
         * @return the stderr of the masked elements in v.
         */
        public static double sem(float [] v, float [] mask)
        {
                double avg = avg(v, mask);
                double var = 0;
                double eps = 0;
                int n = 0;
                for (int j = 0; j < v.length; j++)
                {
                        if (mask[j] != 0)
                        {
                              double d = v[j] - avg;
                              eps += d;
                              var += d * d;
                              n++;
                        }
                }
                var = (var - eps * eps / n) / (n-1);
                double stdev = Math.sqrt(var);
                double sem = (float) (stdev / Math.sqrt(n));
                return sem;
        }
        /**
         * Compute the combined standard error of the mean of the two standard errors stderr0 and stderr1 and
         * corresponding averages avg0 and avg1. The combined SEM is used to compute the SEM correspnding to
         * the multiplication or division of two estimates.
         * The method used to calculate the SE of a calculated result involving multiplication and/or division
         * is similar to, but slightly more complicated than that used for addition and subtraction.
         * In this case, the relative standard deviations (or relative standard errors) of the values involved
         * in the calculation must be used rather than the absolute standard errors.
         * The relative SD (or SE) is obtained from the absolute SD (or SE) by dividing the (SE or SD) by the data value itself.
         * Note that these relative quantities are always unitless.
         * @param estimateAB the estimate of the combination A and B (determined by either multiplication or division, don't care here).
         * @param estimateA, semA the estimate and standard deviation of estimate.
         * @param estimateB, semBV the estimate and standard deviation of estimate.
         * @return standard error of the mean for estimateAB.
         */
        public static double sem(double estimateAB, double estimateA, double semA, double estimateB, double semB)
        {
                double rseA = semA / estimateA;
                double rseB = semB / estimateB;
                double rse = Math.sqrt(rseA * rseA + rseB * rseB);
                double sem = estimateAB * rse;
                //System.out.println("sem "+sem+" semE "+semA+", "+semB+" rse "+rseA+", "+rseB+" estimate "+estimateA+", "+estimateB);
                return sem;
        }
	/*
	* avgNoExtremes computes the average of all elements in v, but throws out all extreme values
	* more than nrstddev standard deviations from the average.
	* @param v a float[] vector.
	* @param nrstddev the standard deviation above which values will not be included in average.
	* @return the avg of the non-extreme elements in v.
        * @deprecated
	*/
	public static float avgNoExtremes(float [] v, float nrstddev)
	{
		double avg = avg(v);
		float stddev = stdev(v);
		float cum = 0;
		int n = 0;
		for (int j = 0; j < v.length; j++)
		{
			float val = v[j];
			if (Math.abs(val - avg) < nrstddev * stddev)
			{
	                      cum +=(double) val;
			      n++;
			}
		}
		cum /=(float) n;
		return cum;
	}
         /*
         * meanT(a) computes the mean values of each column of the <code>transpose</code> of a.
         * It is the same as mean(transpose(a)) but saves a lot of space.
         * @param m a float[][] matrix.
         * @return  a float[] with the mean of all row vectors.
         */
         public static float [] meanColumnT(float [][] m)
         {
                 int iN = m.length; int iM = m[0].length;
                 float [] v = new float[iM];
                 for (int i = 0; i < iM; i++)
                 for (int j = 0; j < iN; j++)
                        v[i] += m[j][i];
                 for (int i = 0; i < iM; i++)
                        v[i] /= (float) iN;
                 return v;
         }
         /*
         * mean(a) computes the mean values of each column of a.
         * @param m a float[][] matrix.
         * @return a float[] with the mean of all column vectors.
         */
         public static float [] meanColumn(float [][] m)
         {
                 int iN = m.length; int iM = m[0].length;
                 float [] v = new float[iM];
                 for (int j = 0; j < iN; j++)
                 for (int i = 0; i < iM; i++)
                        v[j] += m[j][i];
                 for (int j = 0; j < iN; j++)
                        v[j] /= (float) iM;
                 return v;
         }
	/*
	* mean(A) computes the means of all the elements of m.
	* @param m a float[][] matrix.
	* @return a float with the mean of all elements.
	*/
	public static double means(float [][] m)
	{
		int iN = m.length; int iM = m[0].length;
		double v = 0;
		for (int j = 0; j < iN; j++)
		for (int i = 0; i < iM; i++)
		       v += m[j][i];
		for (int j = 0; j < iN; j++)
		       v /= (double) (iM * iN);
		return (float) v;
	}
        /**
         * Compute average of all values in vector v.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double avg(float [] v)
        {
                return  (float) (sum(v) / v.length);
        }
        /**
         * Compute average of all values in vector v.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double avg(float [][] m)
        {
                return  (float) (sum(m) / (m.length * m[0].length));
        }
        /**
         * Compute average of all values in vector v.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double avg(double [] v)
        {
                return  (float) (sum(v) / v.length);
        }
        /**
         * sum(1) computes summation of all values in vector v.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double sum(float [] v)
        {
                double aggr = 0;
                for (int j = 0; j < v.length; j++)
                        aggr += v[j];
                return aggr;
        }
        /**
         * sum(1) computes summation of all values in matrix m.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double sum(float [][] m)
        {
                double aggr = 0;
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                        aggr += m[j][i];
                return aggr;
        }
        /**
         * sum(2) computes the sum of the elements of vector v only where the mask value is not 0.
         * @param v a float[] vector.
         * @param mask a float[] with a value of ! 0 for all elements of v that are valid.
         * @return the avg of the masked elements in v.
         */
        public static double sum(float [] v, float [] mask)
        {
                double agr = 0;
                for (int j = 0; j < v.length; j++)
                {
                        if (mask[j] != 0)
                              agr += v[j];
                }
                return agr;
        }
        /**
         * Compute average of all values in vector v.
         * @param v a vector of float[]
         * @return the average of all elements in v.
         */
        public static double sum(double [] v)
        {
                double aggr = 0;
                for (int j = 0; j < v.length; j++)
                        aggr += v[j];
                return aggr;
        }
        /**
         * Compute variance of all elements in vector v.
         * @param v a vector of float[]
         */
        public static double var(float [] v)
        {
                double avg = avg(v);
                double var = 0;
                double eps = 0;
                int n = 0;
                for (int j = 0; j < v.length; j++)
                {
                        double d = v[j] - avg;
                        eps += d;
                        var += d * d;
                        n++;
                }
                var = (var - eps * eps / n) / (n-1);
                return var;
        }
        /**
         * Make m unit variance and zero mean with respect to all elements of m.
         * m is modified in place.
         * @param m a vector of float[][]
         */
        public static void unitvar(float [][] m)
        {
                double avg = avg(m);
                double var = 0;
                double eps = 0;
                int n = 0;
                // compute var.
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                {
                        // zeromean.
                        double d = m[j][i] - avg;
                        eps += d;
                        var += d * d;
                        n++;
                }
                var = (var - eps * eps / n) / (n-1);
                double sigma = Math.sqrt(var);
                // zeromean.
                for (int j = 0; j < m.length; j++)
                for (int i = 0; i < m[0].length; i++)
                         m[j][i] = (float) ((m[j][i] - avg) / sigma);
        }
        /**
         * Compute variance of all elements in vector v.
         * @param v a vector of float[]
         */
        public static double var(double [] v)
        {
                double avg = avg(v);
                double var = 0;
                double eps = 0;
                int n = 0;
                for (int j = 0; j < v.length; j++)
                {
                        double d = v[j] - avg;
                        eps += d;
                        var += d * d;
                        n++;
                }
                var = (var - eps * eps / n) / (n-1);
                return var;
        }
	/**
	 * Make a new vector of v that has zero mean and unit variance.
	 * @param v a float[] vector
	 * @return a vector with proportionally the same elements as v but with zero mean and unit variance.
	 */
	public static float [] unitvar(float [] v)
	{
		float [] n = zeromean(v);
                // Make unit variance.
                double var = var(n);
		for (int j = 0; j < v.length; j++)
	                n[j] /= Math.sqrt(var);
                return n;
	}
        /**
         * Zero the mean of all elements of v.
         * @param v a vector.
         * @return a vector with zero mean and same variance as v.
         */
        public static double [] zeromean(double [] v)
        {
                double [] n = new double[v.length];
                double avg = sum(v) / v.length;
                // Make zero mean.
                for (int j = 0; j < v.length; j++)
                        n[j] = v[j] - avg;
                return n;
        }
        /**
         * Zero the mean of all elements of v.
         * @param v a vector.
         * @return a vector with zero mean and same variance as v.
         */
        public static float [] zeromean(float [] v)
        {
                float [] n = new float[v.length];
                double avg = avg(v);
                // Make zero mean.
                for (int j = 0; j < v.length; j++)
                        n[j] = (float) (v[j] - avg);
                return n;
        }
	/**
	 * Compute stdev (SQRT(var)) of all values in vector v.
	 * @param v a vector of float[]
	 */
	public static float stdev(float [] v)
	{
                float stdev = (float) Math.sqrt(var(v));
	        return stdev;
	}
	public static float thresholdFraction(float [] v, double fraction)
	{ return thresholdFraction(v, (float) fraction); }
	/**
	 * Find the value in vector over which fraction of all values in vector lie.
	 * @param vector a vector of values
	 * @param fraction a float [0-1].
	 */
	public static float thresholdFraction(float [] v, float fraction)
	{
		float [] minmax = BIJmatrix.minmax(v);
	        float scale =  (minmax[1] - minmax[0]) / 255f;
		//IJ.write("min "+minmax[0]+"max"+minmax[1]+"scale="+scale);
		int [] histogram = histogram(v, minmax[0], 1/scale, 255);
		int bin = binIndex(histogram, fraction);
		return minmax[0] + bin * scale;
	}
	/**
	 * Compute a histogram with n bins for the vector v, each bin separated by d.
	 * @param v a float[] vector
	 * @param min the lowest value for the first bin.
	 * @param d the difference in value between each bin
	 * @param n the number of bins.
	 * @return a float[] with the number of occurences for each bin value.
	 */
	public static int [] histogram(float [] v, float min, float d, int n)
	{
		int [] Pv = new int[n];
	        for (int i = 0; i < v.length; i++)
		{
                        // Scale the pixel value and compute bin index.
			int ix = Math.round((v[i] - min) * d);
			if (ix >= 0 && ix < Pv.length)
		              Pv[ix]++;
		}
		return Pv;
	}
	/**
	 * Compute the lowest bin into which the highest p percent of occurrences falls.
	 * Used to compute things as "give me the occurence above which 10% of the histogram falls."
	 * @param histogram an int[] with the ouccrence counts for each bin
	 * @param p the fraction of histogram values desired.
	 * @return an int, the index into the lowest bin that still falls among p.
	 */
	public static int binIndex(int [] histogram, float fraction)
	{
		// First compute total number of occurences in histogram.
		int total = 0;
		for (int i = 0; i < histogram.length; i++)
	              total += histogram[i];
                int aggr = 0;
		int bin = histogram.length - 1;
		while ((fraction*total) - (float) aggr >= 0)
		{
			//IJ.write("total "+total+" fraction="+(fraction*total)+" aggr="+aggr);
			aggr += histogram[bin--];
		}
		return bin;
	}
	/**
	* Compute the correlation of a vector with another vector b.
	* The mean of a and b is subtracted before further processing.
	* a and b therefore do not have to be zero-mean.
	* @param a a float[] vector.
	* @param b a float[] vector of same length.
	* @return r, the correlation coefficient.
	*/
	public static float correl(float [] a, float [] b)
	{
		int iN = Math.min(a.length, b.length);
		double avga = avg(a);
		double avgb = avg(b);
		double agr = 0; double vara = 0; double varb = 0;
		for (int i = 0; i < iN; i++)
		{
		       agr += (a[i]-avga)*(b[i]-avgb);
		       vara += Math.pow((a[i]-avga), 2);
		       varb += Math.pow((b[i]-avgb), 2);
		}
                double r;
                if (vara == 0 || varb == 0)
                        r = 0;
                else
                        r = (agr / Math.sqrt(vara*varb));
                if (Double.isNaN(r))
                        System.out.println("NAN->"+agr+" "+vara+" "+varb);
		return (float) r;
	}
	/**
	 * Compute the spectrum of v.
	 * The spectrum gives the ratio of each element to v to the sum of v.
	 * The variance spectrum gives the ratio of each coordinate to the sum of all coordinates.
	 * @param v a float[] vector
	 * @return the spectrum of v as a float[].
	 */
	public static float [] spectrum(float [] v)
	{
                float [] spectrum = new float[v.length];
		// Fractionalize.
		double sum = sum(v);
		BIJmatrix.mulElements(spectrum, v, (float) (1/sum));
                return spectrum;
	}
	/**
	 * Compute the erf of x.
	 * @param x the argument
	 * @return the erf of x.
	 */
	public static double erf (double x)
	{
		double a1 = .0705230784;
		double a2 = .0422820123;
		double a3 = .0092705272;
		double a4 = .0001520143;
		double a5 = .0002765672;
		double a6 = .0000430638;
		double xs,xc;

		xs = x*x;
		xc = xs*x;
		return (1.0-1.0/Math.pow(1+a1*x+a2*xs+a3*xc+a4*xs*xs+a5*xc*xs+a6*xc*xc,16));
	}
	/**
	 * Compute Mean Square Error (or residual) of vectors a and b.
	 * a and b are normalized to zero mean and unit variance.
	 * @param a a float[] vector
	 * @param b a float[] vector.
	 * @return a float that is the Mean Square Error of a and b.
	 */
	public static float mse(float [] a, float [] b)
	throws IllegalArgumentException
	{
		if (a.length != b.length)
	                throw new IllegalArgumentException("mse: vectors do not match");
                float [] auv = unitvar(a);
		float [] buv = unitvar(b);
		// Mean is zero, variance is 1.
		//IJ.write("mse: avg="+avg(auv)+", "+avg(buv)+" var="+var(auv)+", "+var(buv));
		float agr = 0;
		for (int i = 0; i < a.length; i++)
		       agr += Math.pow(auv[i]-buv[i], 2);
                float mse = agr / a.length;
		return mse;
	}
        /**
         * Compute residuals of b versus a.
         * @param a a float[] vector
         * @param b a float[] vector.
         * @return a float[] that contains the residuals for each b-a.
         */
        public static float [] residuals(float [] a, float [] b)
        throws IllegalArgumentException
        {
                float [] res = new float[Math.min(a.length, b.length)];
                for (int i = 0; i < res.length; i++)
                        res[i] = b[i] - a[i];
                return res;
        }
        /**
         * Compute Root Mean Square Error of vectors a and b.
         * a and b are normalized to zero mean and unit variance.
         * @param a a float[] vector
         * @param b a float[] vector.
         * @return a float that is the Root Mean Square Error of a and b.
         */
        public static float rmse(float [] a, float [] b)
        throws IllegalArgumentException
        {
                return (float) Math.sqrt(mse(a, b));
        }
        /**
        * Perform Student's t-test on two data sets data1 and data2, with the the t values into t and the probabilities
        * into prob. Variances should be the same.
        * From Press, Numerical Recipes in C, second edition.
        * @param data1 array of datapoints
        * @param data2 array of datapoints
        * @param t
        * @param prob
        */
        public static double ttest(float [] data1, float [] data2)
        {
                double ave1 = avg(data1);
                double ave2 = avg(data2);
                double var1 = var(data1);
                double var2 = var(data2);
                //System.out.println("Avg's "+ave1+" "+ave2+" var's "+var1+" "+var2);
                double df = data1.length+data2.length-2;
                double svar = ((data1.length-1)*var1+(data2.length-1)*var2)/df;
                double t = (ave1-ave2)/Math.sqrt(svar*(1d/(double)data1.length+1d/(double)data2.length));
                //System.out.println("t "+t+" df "+df);
                double prob = BIJfunctions.betai(0.5*df,0.5,df/(df+t*t));
                return prob;
        }
        /**
         * Computes autocovariance up to maxk for the time series data.
         */
        public static double [] autocov(double [] data, int maxk)
        {
                double [] ac = new double[maxk];
                data = zeromean(data);
                data = linearcorrect(data);
                double mu = avg(data);
                for (int k = 0; k < maxk; k++)
                {
                        for (int t = 0; t < data.length-k; t++)
                        {
                                ac[k] += (data[t] - mu)*(data[t+k] - mu);
                        }
                        ac[k] /= data.length - k;
                }
                return ac;
        }
        /**
         * Corrects data for a linear trend.
         */
        protected static double [] linearcorrect(double [] data)
        {
                double sumx = 0;
                double sumy = 0;
                double sumxy = 0;
                double sumxx = 0;
                double sumyy = 0;
                for (int k = 0; k < data.length; k++)
                {
                        sumx += k;
                        sumy += data[k];
                        sumxx += k*k;
                        sumyy += data[k]*data[k];
                        sumxy += data[k]*k;
                }
                double Sxx = sumxx-sumx*sumx/data.length;
                double yy = sumyy-sumy*sumy/data.length;
                double Sxy = sumxy-sumx*sumy/data.length;
                double b =Sxy/Sxx;
                double a = (sumy-b*sumx)/data.length;
                for (int k = 0; k < data.length; k++)
                {
                        data[k] = data[k] - b * k;
                }
                return data;
        }
        /**
         * Computes autocorrelation up to maxk for the time series data.
         */
        public static double [] autocorr(double [] data, int maxk)
        {
                double [] acov = autocov(data, maxk);
                double [] acorr = new double[acov.length];
                for (int k = 0; k < acov.length; k++)
                {
                        acorr[k] = acov[k] / acov[0];
                }
                return acorr;
        }
        /**
        * Perform Student's t-test on two data sets data1 and data2, with the the t values into t and the probabilities
        * into prob. Variances should be the same.
        * From Press, Numerical Recipes in C, second edition.
        * @param data1 array of datapoints
        * @param data2 array of datapoints
        */
        public static double ttest(double [] data1, double [] data2)
        {
                double ave1 = avg(data1);
                double ave2 = avg(data2);
                double var1 = var(data1);
                double var2 = var(data2);
                double df = data1.length+data2.length-2;
                double svar = ((data1.length-1)*var1+(data2.length-1)*var2)/df;
                double t = (ave1-ave2)/Math.sqrt(svar*(1d/(double)data1.length+1d/(double)data2.length));
                double prob = BIJfunctions.betai(0.5*df,0.5,df/(df+t*t));
                //System.out.println("ttest: "+df+" "+var1+" "+var2+" "+svar+" "+aved+" "+ncorr+" "+t+" "+prob+" ");
                return prob;
        }
        /**
        * Perform paired Student's t-test on two data sets data1 and data2, with the the t values into t and the probabilities
        * into prob. Variances should be the same.
        * From Press, Numerical Recipes in C, second edition, pp 618
        * @param data1 array of datapoints
        * @param data2 array of datapoints
        */
        public static double tptest(double [] data1, double [] data2)
        {
                double ave1 = avg(data1);
                double ave2 = avg(data2);
                double var1 = var(data1);
                double var2 = var(data2);
                double cov = 0;
                if (data1.length != data2.length)
                {
                        System.out.println("data1 and data2 are not paired");
                        return Double.MAX_VALUE;
                }
                for (int j = 0; j <data1.length; j++)
                        cov += (data1[j] - ave1)*(data2[j]-ave2);
                double df = data1.length - 1;
                cov /= df;
                double sd = Math.sqrt(var1 + var2 - 2 * cov) / data1.length;
                double t = (ave1-ave2)/sd;
                double prob = BIJfunctions.betai(0.5*df,0.5,df/(df+t*t));
                return prob;
        }
        /**
         * Randomly sample a fraction of the elements of vector v.
         * @param v a vector
         * @param fraction the fraction of elements from v to be included.
         * @return a float[] vector with all the sampled elements.
         */
         public static float [] randomFraction(float [] v, double fraction)
         {
                 // Take at least one sample.
                int length = 1 + (int) Math.round(v.length * fraction);
                float [] r = new float[length];
                Random random = new Random();
                for (int i = 0; i < r.length; i++)
                        r[i] = v[(int) (v.length*random.nextDouble())];
                return r;
         }
         /**
          * Compute the sensitivities of a test of which the result is in exp and the ground truth in truth,
          * for all classes n that occur in truth.
          * @param exp  a float[] vector of test results, where 0 <= exp[n] < n.
          * @param truth a float[] vector of ground truth, where 0 <= truth[n] < n.
          * @param n the number of classes to determine the sensitivity for.
          * @return a float[] vector with the sensitivities for all classes in truth.
          */
         public static float [] sensitivities(float [] truth, float [] exp, int n)
         throws IllegalArgumentException
         {
                float [] s = new float[n];
                for (int c = 0; c < s.length; c++)
                {
                        float [] table2x2 = table2x2(exp, truth, c);
                        s[c] = (float) sensitivity(table2x2);
                }
                return s;
         }
         /**
          * Compute the specificities of a test of which the result is in exp and the ground truth in truth,
          * for all classes n that occur in truth.
          * @param exp  a float[] vector of test results, where 0 <= exp[n] < n.
          * @param truth a float[] vector of ground truth, where 0 <= truth[n] < n.
          * @param n the number of classes to determine the sensitivity for.
          * @return a float[] vector with the specificities for all classes in truth.
          */
         public static float [] specificities(float [] exp, float [] truth, int n)
         throws IllegalArgumentException
         {
                float [] s = new float[n];
                for (int c = 0; c < s.length; c++)
                {
                        float [] table2x2 = table2x2(exp, truth, c);
                        s[c] = (float) specificity(table2x2);
                }
                return s;
         }
         /**
          * Make a 2x2 table for the observations obs0 and obs1, where an observation c is taken as correct.
          * The table is
          *             Obs0==c     Obs0!=c
          *     Obs1==c    a           b
          *     Obs1!=c    c           d
          * The table will contain a,b,c,d in that order
          * @return the 2x2 table.
          */
         public static float [] table2x2(float [] obs0, float [] obs1, int c)
         throws IllegalArgumentException
         {
                 if (obs0.length != obs1.length || c < 0)
                        throw new IllegalArgumentException("illegal argument in table2x2");
                float [] table = new float[4];
                for (int i = 0; i < obs0.length; i++)
                {
                        if (obs0[i] == c && obs1[i] == c)
                                table[0]++;
                        else if (obs0[i] != c && obs1[i] == c)
                                table[1]++;
                        else if (obs0[i] == c && obs1[i] != c)
                                table[2]++;
                        else if (obs0[i] != c && obs1[i] != c)
                                table[3]++;
                }
                return table;
         }
         /**
          * Make a nxn confusion matrix for the observations obs0 and obs1, for n classes.
          * The table is
          *             Obs0==0     Obs0==1     Obs0==2 ... Obs0==n-1
          *     Obs1==0    a            b        c              ...
          *     Obs1==1    g            h       ...
          *     Obs1==2    l            m
          *     Obs1==n-1  ..
          * @return the nxn confusion matrix.
          */
         public static int [][] tablenxn(float [] obs0, float [] obs1, int n)
         throws IllegalArgumentException
         {
                 if (obs0.length != obs1.length || n < 0)
                        throw new IllegalArgumentException("illegal argument in tablenxn");
                int [][] table = new int[n][n];
                for (int i = 0; i < obs0.length; i++)
                {
                        int c0 = (int) obs0[i]; int c1 = (int) obs1[i];
                        table[c0][c1]++;
                }
                return table;
         }
         /**
          * Compute kappa for a 2x2 table.
          * Kappa is
          * @return the kappa.
          */
         public static double kappa(float [] table2x2)
         {
                double g1 = table2x2[0] + table2x2[1];
                double g2 = table2x2[2] + table2x2[3];
                double f1 = table2x2[0] + table2x2[2];
                double f2 = table2x2[1] + table2x2[3];
                double n = f1+f2;
                double pObserved = (table2x2[0] + table2x2[3]) / n;
                double pExpected = ((f1 * g1) + (f2 * g2)) / (n*n);
                double kappa =  (pObserved - pExpected) / (1 - pExpected);
                //System.out.println("pObserved "+pObserved+" pExpected "+pExpected+" kappa "+kappa);
                return kappa;
          }
         /**
          * Compute the accuracy of a test of which the result is in exp and the ground truth in truth,
          * where test results in multiple classifications.
          * @param exp  an int[] vector of test results, where 0 <= exp[n] < n.
          * @param truth an int[] vector of ground truth, where 0 <= truth[n] < n.
          * @param n the number of classes to determine the sensitivity for.
          * @return the combined accuracy.
          */
         public static double accuracyMultipleClasses(float [] exp, float [] truth, int n)
         throws IllegalArgumentException
         {
                if (exp.length != truth.length)
                        throw new IllegalArgumentException("accuracy: "+exp.length+"!="+truth.length);
                int nom = 0; int denom = 0;
                for (int i = 0; i < exp.length; i++)
                {
                        denom++;
                        if (truth[i] == exp[i]) nom++;
                }
                //System.out.println("accuracy = "+nom+"/"+denom);
                /*int [] votesT = new int[n];
                int [] votesE = new int[n];
                int [] votesC = new int[n];
                for (int i = 0; i < exp.length; i++)
                {
                        votesE[(int) exp[i]]++;
                        votesT[(int) truth[i]]++;
                        votesC[(int) truth[i]] += (truth[i] == exp[i]) ? 1 : 0;
                }
                for (int c = 0; c < n; c++)
                {
                        System.out.print(""+c+": "+votesT[c]+"/"+votesE[c]+" (correct="+votesC[c]+"); ");
                }
                System.out.println();*/
                return nom / denom;
         }
         /**
          * Compute sensitivity of a test of which the result is in exp and the ground truth in truth,
          * for class c.
          * sensitivity = true pos / (true pos + false neg)
          * sensitivity = a / (a+c)
          * @param exp  a float[] vector of test results, where 0 <= exp[n] <= c.
          * @param truth a float[] vector of ground truth, where 0 <= truth[n] <= c.
          * @param c the class to determine sensitivity for.
          * @return the sensitivity.
          */
         public static double sensitivity(float [] table2x2)
         throws IllegalArgumentException
         {
                 return table2x2[0] / (table2x2[0] + table2x2[2]);
         }
         /**
          * Compute specificity of a test of which the result is in exp and the ground truth in truth,
          * for class c.
          * specificity = true neg / (true neg + false pos)
          * specificity = d / (b+d)
          * @param exp  an int[] vector of test results, where 0 <= exp[n] <= c.
          * @param truth an int[] vector of ground truth, where 0 <= truth[n] <= c.
          * @param c the class to determine specificity for.
          * @return the specificity.
          */
         public static double specificity(float [] table2x2)
         throws IllegalArgumentException
         {
                 return table2x2[3] / (table2x2[1] + table2x2[3]);
         }
         /**
          * Compute accuracy or observed proportion of overall agreement
          * of a test for which the result is in exp and the ground truth in truth
          * accuracy = true pos + true neg / (true neg + false pos + true pos + false neg)
          * accuracy = (a+d) / (a+b+c+d)
          * @param table2x2 a 2x2 table.
          * @return the accuracy.
          */
         public static double accuracy(float [] table2x2)
         throws IllegalArgumentException
         {
                 return (table2x2[0] + table2x2[3])/ (table2x2[0] + table2x2[1] + table2x2[2] + table2x2[3]);
         }
         /**
          * Compute the accuracy of a test of which the confusion matrix is in tablenxn.
          * @return the combined accuracy.
          */
         public static double accuracyMultipleClasses(int [][] tablenxn)
         throws IllegalArgumentException
         {
                double nom = 0; double denom = 0;
                for (int j = 0; j < tablenxn.length; j++)
                for (int i = 0; i < tablenxn[0].length; i++)
                {
                        if (i == j)
                                nom += tablenxn[i][j];
                        denom += tablenxn[i][j];
                }
                return nom / denom;
         }
}

