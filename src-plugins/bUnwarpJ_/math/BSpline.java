package math;

import ij.IJ;

/**
 * BSpline library.
 * Copyright (C) 2009 Ignacio Arganda-Carreras and Arrate Munoz-Barrutia 
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
 * 
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
		if (n1==3)
		{
		   med = integSA(input, integ);
		   integ = integAS(integ);
		   integSA(integ, integ);
		   integ = integAS(integ);
		}  
		
		return null;
	} // end method resize1D

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
	
} // end class BSpline
