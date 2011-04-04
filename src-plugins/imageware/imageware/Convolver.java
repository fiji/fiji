package imageware;

/**
 * Class Convolver.
 * Routines to convolve a 1D signal applying mirror boundary conditions.
 * 
 *
 * @author	Daniel Sage
 *			Biomedical Imaging Group
 *			Ecole Polytechnique Federale de Lausanne, Lausanne, Switzerland
 */

public class Convolver extends Object {
	private static double tolerance = 10e-6;

	/**
	* Convolution with a Finite Impulse Response (FIR) filter.
	*
	* Note: Only with the periodic boundary conditions.
	*
	* @param input   1D input signal
	* @param kernel   kernel of the filter
	*/
	public static double[] convolveFIR(double[] input, double[] kernel) 
	{
		int l = input.length;
		if (l <= 1) 
			throw new IllegalArgumentException("convolveFIR: input signal too short");
		double[] output = new double[l];
	
		int indexq = kernel.length - 1;
		int indexp = 0;
		int n2 = 2 * (l - 1);
		int origin = kernel.length/2;
		int m = 1 + origin - kernel.length;
		m -= (m < 0L) ? (n2 * ((m + 1 - n2) / n2)) : (n2 * (m / n2));
		int k;
		for (int i = 0; i < l; i++) {
			int j = -kernel.length;
			k = m;
			indexq = kernel.length - 1;
			double Sum = 0.0;
			while (j < 0) {
				indexp = k;
				int kp = ((k - l) < j) ? (j) : (k - l);
				if (kp < 0L) {
					for (int n = kp; n < 0; n++) {
						Sum += input[indexp] * kernel[indexq];
						indexq--;
						indexp++;
					}
					k -= kp;
					j -= kp;
				}
				indexp = n2 - k;
				int km = ((k - n2) < j) ? (j) : (k - n2);
				if (km < 0L) {
					for (int n = km; n < 0; n++) {
						Sum += input[indexp] * kernel[indexq];
						indexq--;
						indexp--;
					}
					j -= km;
				}
				k = 0;
			}
			if (++m == n2) {
				m = 0;
			}
			output[i] = Sum;
		}
		return output;
	}

	/**
	* Convolve with with a Infinite Impluse Response filter (IIR)
	*
	* @param input  	1D input signal
	* @param poles    	1D array containing the poles of the filter
	*/
	public static double[] convolveIIR(double[] input, double poles[]) {
		double lambda = 1.0;
		int l = input.length;
		double[] output = new double[l];
		for (int k = 0; k < poles.length; k++) {
			lambda = lambda * (1.0 - poles[k]) * (1.0 - 1.0 / poles[k]);
		}
		for (int n = 0; n < l; n++) {
			output[n] = input[n] * lambda;
		}
		for (int k = 0; k < poles.length; k++) {
			output[0] = getInitialCausalCoefficientMirror(output, poles[k]);
			for (int n = 1; n < l; n++) {
				output[n] = output[n] + poles[k] * output[n - 1];
			}
			output[l-1] = getInitialAntiCausalCoefficientMirror(output, poles[k]);
			for (int n = l - 2; 0 <= n; n--) {
				output[n] = poles[k] * (output[n+1] - output[n]);
			}
		}
		return output;
	}

	/**
	* Convolve a 1D signal with a Infinite Impluse Response 2nd order (IIR2)
	*
	* Note: Only with the mirror (on bounds) boundary conditions.
	*
	* Purpose:	Recursive implementation of a symmetric 2nd order
	*			filter with mirror symmetry boundary conditions :
	*
	*					      1                1
	*			H[z] = --------------- * ---------------
	*				   (1-b1*z-b2*z^2)   (1-b1/z-b2/z^2)
	*				
	*			implemented in the following form:
	*				
	*					   a1+a2*z          a1+a2/z
	*			H[z] = --------------- + ---------------  - a1
	*				   (1-b1*z+b2*z^2)   (1-b1/z+b2/z^2) 
	*				
	*			where :
	*			a1 = -(b2 + 1.0) * (1 - b1 + b2) / ((b2 - 1.0) * (1 + b1 + b2));
	*			a2 = - a1 * b2 * b1 / (b2 + 1.0);
	*
	* @param input   	1D input signal
	* @param b1		  	first pole of the filter
	* @param b2   	  	second pole of the filter
	*/
	public static double[] convolveIIR2( double input[], double b1, double b2) {
		int l = input.length;
		int n2 = 2 * l;

		double a1 = -(b2 + 1.0) * (1 - b1 + b2) / ((b2 - 1.0) * (1 + b1 + b2));
		double a2 = - a1 * b2 * b1 / (b2 + 1.0);
		
		// cBuffer stores temporary spline coefficients
		double cBuffer[] = new double[n2];
	    
	    // sBuffer contains a copy of s[] and a time reversed version of s[]
		double sBuffer[] = new double[n2];   
	  	
	  	// copy signal s[] and its time reversed version to sBuffer[]
	    for( int n = 0; n < l; n++){
			sBuffer[n] = input[n];
			sBuffer[n2 - n - 1] = input[n];
		}
		
		// Determine the start index n0 for the causal recursion. n0 is chosen such
		// that the error of cBuffer[0] and cBuffer[1] is smaller than the
		// specified 'Tolerance'.
	  	int n0 = 2;
	   	if ((tolerance > 0.0) && (b2 != 1.0)) {
			n0 = n2 - (int) Math.ceil(2.0 * Math.log(tolerance) / Math.log(b2)); 
	   	}

		if (n0 < 2) {     
			n0 = 2;
		}
		
		cBuffer[n0 - 1] = 0.0;
		cBuffer[n0 - 2] = 0.0;
		for (int n = n0; n < n2; n++) {
			cBuffer[n] = a1 * sBuffer[n] + a2 * sBuffer[n-1] + b1 * cBuffer[n-1] - b2 * cBuffer[n-2];
		}

		cBuffer[0] = a1 * sBuffer[0] + a2 * sBuffer[n2-1] + b1 * cBuffer[n2-1] - b2 * cBuffer[n2-2];
		cBuffer[1] = a1 * sBuffer[1] + a2 * sBuffer[0] + b1 * cBuffer[0] - b2 * cBuffer[n2-1];

	   	// compute the remaining spline coefficients cBuffer(z) = H_{+}(z) * sBuffer(z) by 
	    // recursive filtering
	    for( int n = 2; n < n2; n++) {
			cBuffer[n] = a1 * sBuffer[n] + a2 * sBuffer[n-1] + b1 * cBuffer[n-1] - b2 * cBuffer[n-2];
		}
		
		// add together the temporary filter outputs to obtain the final  spline coefficients
		double[] output = new double[l];
	    for( int n = 0; n < l; n++) {
			output[n] = cBuffer[n]  + cBuffer[n2-n-1] - a1 * input[n];
		}
		return output;
	}

	/**
	 */
	private static double getInitialAntiCausalCoefficientMirror(double[] c, double z) 
	{
		return((z * c[c.length - 2] + c[c.length - 1]) * z / (z * z - 1.0));
	}

	/**
	 */
	private static double getInitialCausalCoefficientMirror(double[] c, double z) 
	{
		double z1 = z, zn = Math.pow(z, c.length - 1);
		double sum = c[0] + zn * c[c.length - 1];
		int horizon = c.length;

		if (0.0 < tolerance) {
			horizon = 2 + (int)(Math.log(tolerance) / Math.log(Math.abs(z)));
			horizon = (horizon < c.length) ? (horizon) : (c.length);
		}
		zn = zn * zn;
		for (int n = 1; (n < (horizon - 1)); n++) {
			zn = zn / z;
			sum = sum + (z1 + zn) * c[n];
			z1 = z1 * z;
		}
		return(sum / (1.0 - Math.pow(z, 2 * c.length - 2)));
	}

} // end of classe