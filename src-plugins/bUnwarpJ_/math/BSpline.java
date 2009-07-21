package math;

import ij.IJ;

/**
 * BSpline library.
 * 2009 Ignacio Arganda-Carreras and Arrate Munoz-Barrutia 
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

/**
 * This class implements basic operations to work with cubic B-splines.
 * 
 * @author Ignacio Arganda-Carreras - ignacio.arganda@gmail.com 
 */
public class BSpline 
{

	// --------------------------------------------------------------
	/**
	 * <p>
	 * Ref: A. Muñoz-Barrutia, T. Blu, M. Unser, "Least-Squares Image Resizing
	 * Using Finite Differences," IEEE Transactions on Image Processing, vol.
	 * 10, n. 9, September 2001.
	 * Use:
	 *  out(x) = input(x/scale); scale > 1 expansion; a < 1 reduction.
	 * Use n=n2
	 *  if (n1==-1) the standard method (interpolation and resampling) is
	 *              applied.
	 * If (n=n1=n2), the orthogonal projection is computed. The error is the
	 *               minimum in the least-squares sense.
	 * If ((n1 > -1) && (n1<n2)), the oblique projection is computed. The error
	 *                            is slightly greater than the case above.
	 * </p>
	 *  
	 * @param input input signal
	 * @param n degree of the interpolation spline. It can vary from 0 to 3.
	 * @param n1 degree of the analysis spline. It can vary from -1 to 3.
	 * @param n2 degree of the synthesis spline. It can vary from 0 to 3. 
	 * @param scale zoom factor (scale > 1 expansion; scale < 1 reduction)
	 * @param coefOrSamples if working with coefficients coefOrSamples is true; if working with samples coefOrSamples is false
	 * @return out signal
	 */
	public static double[] resize1D(
			double[] input, 
			int n, 
			int n1, 
			int n2, 
			double scale, 
			boolean coefOrSamples)
	{
		int nxIn = input.length;
		
		double bb = Math.round( ((nxIn-1) * scale) * 1 / scale);
		while ((nxIn-1-bb) != 0)
		{
		    nxIn = nxIn + 1;
		    bb = Math.round(((nxIn-1)*scale)*1/scale);
		}
		
		// Output signal
		int nxOut = (int) Math.round((nxIn-1)*scale)+1;
		double[] out = new double[nxOut];
		
		// From samples to coefficients
		double[] coef = null;
		if (coefOrSamples == false)
		{
		    if (n < 2)
		    	coef = input;
		    else
		    {
		    	double[] pole = tableOfPoles(n);
		    	double tolerance = 1e-11;
		    	coef = convertToInterpCoef(input, pole, tolerance);
		    }
		}
		
		// (n1+1)-running sums
		double[] integ = new double[input.length];
		double med = 0;
		if (n1 == -1)
		{
		    integ = input;
		    med = 0;
		}    
		if (n1 == 0)
		{
		    med = integSA(input, integ);
		}
		if (n1 == 1)
		{
		   med = integSA(input, integ);
		   integ = integAS(integ);
		}
		
		if (n1 == 2)
		{
		   med = integSA(input, integ);
		   integ = integAS(integ);
		   integSA(integ, integ);
		}
		if (n1 == 3)
		{
		   med = integSA(input, integ);
		   integ = integAS(integ);
		   integSA(integ, integ);
		   integ = integAS(integ);
		}  
		
		// geometric transformation and resampling
		double [] resize = resampling(integ, nxIn, nxOut, scale, n1, n+n1+1);
		
		//(n1+1)-centered finite differences
		double [] fd = null;
		if (n1 == -1)
		    fd=resize;		
		else if (n1 == 0)
		    fd = finDiffAS(resize);
		else if (n1 == 1)
		{
			fd=finDiffSA(resize);
		    fd=finDiffAS(fd);
		}
		else if (n1==2)
		{
		    fd=finDiffAS(resize);
		    fd=finDiffSA(fd);
		    fd=finDiffAS(fd);
		}
		else if (n1==3)
		{
		    fd=finDiffSA(resize);
		    fd=finDiffAS(fd);
		    fd=finDiffSA(fd);
		    fd=finDiffAS(fd);
		}  
		
		//Recover the output size and add the mean
		final int n11 = n1+1;
		final int val1 = (n11*0.5 == (int) Math.floor(n11*0.5)) ? (int) (n11*0.5) : (int) Math.floor(n11*0.5)+1;
		
		//final int val2 = (int) Math.floor(n11*0.5);
				
		double[] fdShort = new double[nxOut];
		
		for(int i = 0 ; i < nxOut ; i ++)
			fdShort[i] = fd[val1 + i] + med;		
		//fdShort = [fdShort(1+val2:nxOut) fdShort(val2:-1:1)];		

		// post-filtering - q=a^(-1)
		double[] coefFull = null;
		if ( (n1+n2+1) < 2 )
		    coefFull = fdShort;
		else    
		{
			double[] pole = tableOfPoles(n1+n2+1);		    
		    double tolerance = 1e-11;
		    coefFull=convertToInterpCoef(fdShort, pole, tolerance);
		} 
		
		// from coeficients to samples
		if (coefOrSamples==true)
		{
		    if (n2 < 2)
		        out = coefFull;
		    else
		    {
		        double [] samples = tableOfSamples(n2);
		        out = convertToSamples(coefFull, samples);  
		    } 
		}
		
		return out;
	} // end method resize1D

	// --------------------------------------------------------------
	/**
	 * Convert the spline coefficients to samples
	 * 
	 * @param coef input coefficients values
	 * @param samples output samples
	 * @return 
	 */
	public static double[] convertToSamples(double[] coef, double[] samples) 
	{
		final int NbCoef = coef.length;
		final int kn = 2*NbCoef-2;
		
		double[] out = new double[NbCoef];

		for (int k = 0; k < NbCoef; k++)
		{
		    double yaux = coef[k] * samples[0];
		    for (int i = 1; i < samples.length-1; i ++)
		    {
		        int k1 = k-i;
		        int k2 = k+i;
		        if (k1 < 0)
		        {
		            k1 = -k1;
		            if (k1 >= NbCoef)
		                k1 = kn-k1;
		                
		        }
		        if (k2 >= NbCoef)
		        {
		            k2 = 2*(NbCoef-1)-k2;
		            if (k2 >= NbCoef)
		                k2 = kn-k2;
		                
		        }    
		        yaux = yaux + coef[k1] + coef[k2] * samples[i];
		    }    
		    out[k] = yaux;
		
		}
		
		return out;
	} // end method convertToSamples

	// --------------------------------------------------------------
	/**
	 * Finite Difference. Symmetric input boundary conditions. Anti-symmetric
	 * output boundary conditions.
	 * 
	 * @param c input signal
	 * @return output signal
	 */
	private static double[] finDiffSA(double[] c) 
	{
		int N = c.length;
		double[] y = new double[N];
		for(int i = 0; i < N-1; i++)
			y[i] = c[i] - c[i+1];
		y[N-1] = c[N-1] - c[N-2];
		
		for(int i = 0; i < N; i++)
			y[i] *= -1;
		
		return y;
	} // end method finDiffSA

	// --------------------------------------------------------------
	/**
	 * Finite Difference. Anti-symmetric input boundary conditions. Symmetric
	 * output boundary conditions.
	 * 
	 * @param c input signal
	 * @return output signal
	 */
	public static double[] finDiffAS(double[] c) 
	{
		int N = c.length;
		double[] y = new double[N];
		for (int i = 0; i < N-1; i ++)
		    y[i+1] = c[i+1] - c [i];
		
		y[0]=2*c[0];
		
		return y;
	} // end method finDiffAS

	// --------------------------------------------------------------
	/**
	 * Geometric transform and resampling
	 * 
	 * @param input input signal
	 * @param nInput length of the input signal
	 * @param nOut length of the resize signal
	 * @param scale expansion/reduction
	 * @param n1 degree of the analysis spline
	 * @param nt (n+n1+1)
	 * @return re-sampled signal
	 */
	public static double[] resampling(double[] input, int nInput, int nOut,
			double scale, int n1, int nt) 
	{
		final boolean even = ( (n1+1)*0.5 == Math.floor((n1+1)*0.5) ) ? true : false;
		
		int m = factorial(nt);
		double[] temp = new double[nt + 1];
		
		final int val1 = (int) (even ? (n1+1)*0.5 : (int) Math.floor((n1+1)*0.5)+1);
		double newx0 = even ? (nt+1)*0.5-val1/scale : 0.5*(1/scale-1) + (nt+1)*0.5-val1/scale;
		
		final double aa = Math.pow(scale, n1+1);
		final double val2 = Math.floor((n1+1)*0.5);
		
		double[] out = new double[ nOut+n1+1 ];
		
		for(int k = -val1; k < (nOut + val2); k++)
		{
			final int val = (int) Math.ceil(-(nt+1) + newx0);
			
			for (int s = 0; s <= nt; s++)
				temp[s] = Math.pow(-newx0+(nt+1)+val+s, nt)/m;
			
			for (int s = 0; s <= nt; s++)
				for(int l = nt+1; l > 0; l--)
					temp[l] -= temp[l-1];
			
			if(even)
			{
				 for(int i = 0; i <= nt; i++)
				 {
	                if (i + val < 0)
	                    out[k+val1] += input[-i-val]*temp[i];
	                else
	                {
	                    if (i+val > nInput-1)
	                        out[k+val1] += input[2*(nInput-1)-i-val]*temp[i];
	                    else
	                    	out[k+val1] += input[i+val] * temp[i];
	                }
				 }
	
			}
			else
			{
				for(int i = 0; i <= nt; i++)
				{
					if (i+val < 0) 
						out[k+val1] -= input[-i-val]*temp[i];
					else
						if (i+val > nInput-1) 
							out[k+val1] -= input[2*(nInput-1)-i-val]*temp[i];
						else
							out[k+val1] += input[i+val]*temp[i];
				}
			}
			
			newx0 += 1/scale;
		}
		
		for(int i = 0; i < out.length; i++)
			out[i] *= aa;
		
		return out;
	}// end method resampling

	//-----------------------------------------------------------------------------
	/**
	 * Factorial
	 * @param n
	 * @return n factorial
	 */
	public static int factorial(int n) 
	{
		int fact = 0;
		switch(n)
		{
			case 0:
				fact = 1;
				break;
			case 1:  
				fact = 1;
				break;
			case 2:
				fact = 2;
				break;
			case 3:
				fact = 6;
				break;
			case 4:
				fact = 24;
				break;
			case 5:
				fact = 120;
				break;
			case 6:
				fact = 720;
				break;
			case 7:
				fact = 5040;
				break;
			default:
				fact = 1;
				for (int i=0 ; i < n; i++)
					fact *= i;
		}
		return fact;
	} // end factorial

	// --------------------------------------------------------------
	/**
	 * Begin computation of the running sums. The input boundary conditions are 
	 * anti-symmetric. The output boundary conditions are symmetric.
	 *   
	 * @param c input coefficients
	 * @return sums
	 */
	public static double[] integAS(double[] c) 
	{
		int N = c.length;
		double [] y = new double[N];
		y[0] = c[0];
		y[1] = 0;
		for(int i = 2; i < N; i++)
		    y[i] = y[i-1] - c[i-1];
		
		for(int i = 0; i < y.length; i++)
			y[i] *= -1;
		
		return y;
	}

	// --------------------------------------------------------------
	/**
	 * Begin computation of the running sums. The input boundary conditions are
	 * symmetric. The output boundary conditions are anti-symmetric.
	 * 
	 * @param c coefficients
	 * @param z sums
	 * 
	 * @return mean value on the period (2*length(c)-2)
	 */
	public static double integSA(double[] c, double[] z)
	{
		double m = mean(c);
		
		double[] coeffs = c.clone();
		
		for(int i = 0; i < coeffs.length; i++)
			coeffs[i] -= m;
		
		z[0] = coeffs[0] * 0.5;
		for(int i = 1; i < coeffs.length; i++)
		    z[i] = coeffs[i] + z[i-1];
				
		return m;
	} // end method integSA
	
	// --------------------------------------------------------------
	/**
	 * Calculate mean value on the period (2*length(A)-2)
	 * @param A
	 * @return mean value on the period (2*length(A)-2)
	 */
	public static double mean(double[] A) 
	{
		double sum = 0;
		for(int i = 0; i < A.length; i++)
			sum += A[i];
		double sumValue = 2 * sum - A[0] - A[A.length-1];
		return sumValue/(2*A.length-2);
	}// end method mean

	// --------------------------------------------------------------
	/**
	 * Convert to interpolation coefficients
	 * @param input
	 * @param poles
	 * @param tolerance
	 * @return
	 */
	public static double[] convertToInterpCoef(double[] input, double[] poles, double tolerance) 
	{
		double lambda  =1.0;
		
		int dataLength = input.length;
		double [] coef = new double[dataLength];

		// special case required by mirror boundaries 
		if (dataLength == 1)
		    return coef;		

		// compute the overall gain 
		for(int k = 0 ; k < poles.length; k++)
		    lambda = lambda * (1.0 - poles[k]) * (1.0 - 1.0 / poles[k]);
		
		// apply the gain 
		for(int n = 0; n < dataLength; n ++)
		    coef[n] = input[n] * lambda;
		
		    
		// loop over all poles
		for (int k = 0 ; k < poles.length; k++)
		{
		    // causal initialization 
		    coef[0] = initialCausalCoefficient(coef, poles[k], tolerance);
		    // causal recursion 
		    for (int n = 1; n < dataLength; n++)
		        coef[n] = coef[n] + poles[k] * coef[n - 1];
		    
			// anti-causal initialization 
			coef[dataLength-1] = initialAntiCausalCoefficient(coef, poles[k]);
			// anti-causal recursion 
			for (int n = dataLength - 2 ; n > 0; n--) 
				coef[n] = poles[k] * coef[n + 1] - coef[n];
			
		}
		
		return coef;
	} // end method convertToInterpCoef
	
	// --------------------------------------------------------------
	/**
	 * Get initial anti-causal coefficients
	 * 
	 * @param c coefficients
	 * @param z pole
	 * @return value of the initial anti-causal coefficient
	 */
	public static double initialAntiCausalCoefficient(double[] c, double z) 
	{		
		return  ((z / (z * z - 1.0)) * (z * c[c.length- 2] + c[c.length-1]));
	} //end method initialAntiCausalCoefficient

	// --------------------------------------------------------------
	/**
	 * Initialization of causal coefficients for mirror boundary conditions
	 * 
	 * @param c coefficients
	 * @param z pole
	 * @param tolerance accuracy
	 * @return value of the initial causal coefficient for the given accuracy
	 */
	public static double initialCausalCoefficient(double[] c, double z, double tolerance)
	{
		double sum = 0;
		final int dataLength = c.length;
		int horizon = dataLength;
		
		if (tolerance > 0.0)
		    horizon = (int) (Math.ceil(Math.log(tolerance))/Math.log(Math.abs(z)));		   

		if (horizon < dataLength)
		{
		    // accelerated loop
		    double zn = z;
		    sum = c[0];
		    for (int n = 1; n < horizon; n++)
		    {
		        sum = sum + zn * c[n];
		        zn = zn * z;
		    }
		}
		else
		{			
			// full loop
			double zn = z;
			double iz = 1.0 / z;
			double z2n = Math.pow(z, (dataLength - 1));
			sum = c[0] + z2n * c[dataLength-1];
			z2n = z2n * z2n * iz;
			for (int n = 1; n < dataLength - 1; n++)
			{
				sum = sum + (zn + z2n) * c[n];
				zn = zn * z;
				z2n = z2n * iz;
			}
			sum = (sum / (1.0 - zn * zn));
		}
		
		return sum;
	} // end method initialCausalCoefficient
	
	// --------------------------------------------------------------
	/**
	 * Recover the poles from a lookup table
	 *  
	 * @param splineDegree degree of the spline
	 * @return Pole values
	 */
	public static double[] tableOfPoles(int splineDegree) 
	{		
		double[] pole = null;
		switch (splineDegree)
		{		
			case 2:
				pole = new double[1];
				pole[0] = Math.sqrt(8.0) - 3.0;
				break;
			case 3:
				pole = new double[1];
				pole[0] = Math.sqrt(3.0) - 2.0;
				break;
			case 4:
				pole = new double[2];
				pole[0] = Math.sqrt(664.0 - Math.sqrt(438976.0)) + Math.sqrt(304.0) - 19.0;
				pole[1] = Math.sqrt(664.0 + Math.sqrt(438976.0)) - Math.sqrt(304.0) - 19.0;
				break;
			case 5:
				pole = new double[2];
				pole[0] = Math.sqrt(135.0 / 2.0 - Math.sqrt(17745.0 / 4.0)) + Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
				pole[1] = Math.sqrt(135.0 / 2.0 + Math.sqrt(17745.0 / 4.0)) - Math.sqrt(105.0 / 4.0) - 13.0 / 2.0;
				break;
			case 6:
				pole = new double[3];
				pole[0] = -0.48829458930304475513011803888378906211227916123938;
				pole[1] = -0.081679271076237512597937765737059080653379610398148;
				pole[2] = -0.0014141518083258177510872439765585925278641690553467;
				break;
			case 7:
				pole = new double[3];
				pole[0] = -0.53528043079643816554240378168164607183392315234269;
				pole[1] = -0.12255461519232669051527226435935734360548654942730;
				pole[2] = -0.0091486948096082769285930216516478534156925639545994;
				break;
			default:
				IJ.error("Invalid spline degree");
		}
		
		return pole;
	}// end method tableOfPoles
	
	// --------------------------------------------------------------
	/**
	 * Recover the B-spline samples values from a lookup table 
	 *  
	 * @param splineDegree degree of the spline
	 * @return Value of the samples
	 */
	public static double[] tableOfSamples(int splineDegree) 
	{		
		double[] samples = null;
		switch (splineDegree)
		{		
			case 0:
			case 1:
				samples = null;
				break;
			case 2:
				samples = new double[2];
				samples[0] = 6/8;
				samples[1] = 1/8;
				break;
			case 3:
				samples = new double[2];
				samples[0] = 4/6;
				samples[1] = 1/6;
				break;
			case 4:
				samples = new double[3];
				samples[0] = 230/384;
				samples[1] = 76/384;
				samples[2] = 1/384;
				break;
			case 5:
				samples = new double[3];
				samples[0] = 66/120;
		        samples[1] = 26/120;
		        samples[2] = 1/120;
				break;
			case 6:
				samples = new double[4];
				samples[0] = 23548/46080;
		        samples[1] = 10543/46080;
		        samples[2] = 722/46080;
		        samples[3] = 1/46080;
				break;
			case 7:
				samples = new double[4];
				samples[0] = 2416/5040;
		        samples[1] = 1191/5040;
		        samples[2] = 120/5040;
		        samples[3] = 1/5040;
				break;
			default:
				IJ.error("Invalid spline degree");
		}
		
		return samples;
	}// end method tableOfSamples
	
} // end class BSpline
