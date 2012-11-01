package imagescience.utility;

import java.lang.Math;

/** Carries out elementary mathematical operations and function evaluations. */
public class FMath {
	
	/** Default constructor. */
	public FMath() { }
	
	/** Returns the floor of the given value.
		
		@param d the value whose floor is to be returned.
		
		@return the largest integral value that is not greater than {@code d}. This method works only for {@code double} values that fall within the range spanned by the {@code int}egers. In that case, this method yields the same result as the corresponding method in Java's {@code Math} class, but is in general much faster and returns an {@code int} rather than a {@code double}.
	*/
	public static int floor(final double d) {
		
		if (d >= 0 || d == (int)d) return (int)d;
		return (int)(d - 1);
	}
	
	/** Returns the ceiling of the given value.
		
		@param d the value whose ceiling is to be returned.
		
		@return the smallest integral value that is not less than {@code d}. This method works only for {@code double} values that fall within the range spanned by the {@code int}egers. In that case, this method yields the same result as the corresponding method in Java's {@code Math} class, but is in general much faster and returns an {@code int} rather than a {@code double}.
	*/
	public static int ceil(final double d) {
		
		if (d <= 0 || d == (int)d) return (int)d;
		return (int)(d + 1);
	}
	
	/** Returns the given value rounded to the nearest integral value.
		
		@param d the value whose rounded value is to be returned.
		
		@return the integral value closest to {@code d}. This method works only for {@code double} values that fall within the range spanned by the {@code int}egers. In that case, this method yields the same result as the corresponding method in Java's {@code Math} class, but is in general much faster and returns an {@code int} rather than a {@code double}.
	*/
	public static int round(final double d) {
		
		// round(d) = floor(d + 0.5):
		final double d05 = d + 0.5;
		if (d05 >= 0 || d05 == (int)d05) return (int)d05;
		return (int)(d05 - 1);
	}
	
	/** Returns the value of {@code d} clipped to the range {@code [min,max]}.
		
		@param d the value to be clipped.
		
		@param min the lower bound (minimum value) of the clip range.
		
		@param max the upper bound (maximum value) of the clip range.
		
		@return the value of {@code d} clipped to the range {@code [min,max]}. Guaranteed to be correct only if {@code min <= max}.
	*/
	public static double clip(final double d, final double min, final double max) {
		
		return ((d >= max) ? max : ((d <= min) ? min : d));
	}
	
	/** Returns the minimum of the given values.
		
		@param d1 {@code d2} - the values.
		
		@return the minimum of {@code d1} and {@code d2}.
	*/
	public static double min(final double d1, final double d2) {
		
		return ((d1 < d2) ? d1 : d2);
	}
	
	/** Returns the minimum of the given values.
		
		@param d1 {@code d2} - {@code d3} - {@code d4} - the values.
		
		@return the minimum of {@code d1,...,d4}.
	*/
	public static double min(final double d1, final double d2, final double d3, final double d4) {
		
		final double d12 = min(d1,d2);
		final double d34 = min(d3,d4);
		return ((d12 < d34) ? d12 : d34);
	}
	
	/** Returns the minimum of the given values.
		
		@param d1 {@code d2} - {@code d3} - {@code d4} - {@code d5} - {@code d6} - {@code d7} - {@code d8} - the values.
		
		@return the minimum of {@code d1,...,d8}.
	*/
	public static double min(
		final double d1, final double d2,
		final double d3, final double d4,
		final double d5, final double d6,
		final double d7, final double d8
	) {
		
		final double d1234 = min(d1,d2,d3,d4);
		final double d5678 = min(d5,d6,d7,d8);
		return ((d1234 < d5678) ? d1234 : d5678);
	}
	
	/** Returns the maximum of the given values.
		
		@param d1 {@code d2} - the values.
		
		@return the maximum of {@code d1} and {@code d2}.
	*/
	public static double max(final double d1, final double d2) {
		
		return ((d1 > d2) ? d1 : d2);
	}
	
	/** Returns the maximum of the given values.
		
		@param d1 {@code d2} - {@code d3} - {@code d4} - the values.
		
		@return the maximum of {@code d1,...,d4}.
	*/
	public static double max(final double d1, final double d2, final double d3, final double d4) {
		
		final double d12 = max(d1,d2);
		final double d34 = max(d3,d4);
		return ((d12 > d34) ? d12 : d34);
	}
	
	/** Returns the maximum of the given values.
		
		@param d1 {@code d2} - {@code d3} - {@code d4} - {@code d5} - {@code d6} - {@code d7} - {@code d8} - the values.
		
		@return the maximum of {@code d1,...,d8}.
	*/
	public static double max(
		final double d1, final double d2,
		final double d3, final double d4,
		final double d5, final double d6,
		final double d7, final double d8
	) {
		
		final double d1234 = max(d1,d2,d3,d4);
		final double d5678 = max(d5,d6,d7,d8);
		return ((d1234 > d5678) ? d1234 : d5678);
	}
	
	private static final double[] LNGAMMAC = {
		76.18009172947146, -86.50532032941677,
		24.01409824083091, -1.231739572450155,
		0.1208650973866179E-2, -0.5395239384953E-5
	};
	
	/** Returns the natural logarithm of the Gamma function at {@code x}.
		
		@param x the input value. Must be larger than {@code 0}.
		
		@return the natural logarithm of the Gamma function at {@code x}. Computed using the approximation described by C. Lanczos, "A Precision Approximation of the Gamma Function", SIAM Journal on Numerical Analysis, Series B, vol. 1, 1964, pp. 86-96, as implemented by W. H. Press et al., Numerical Recipes in C: The Art of Scientific Computing, 2nd ed., Cambridge University Press, Cambridge, 1992, Section 6.1.
		
		@throws IllegalArgumentException if {@code x} is less than or equal to {@code 0}.
	*/
	public static double lngamma(final double x) {
		
		if (x <= 0) throw new IllegalArgumentException("Input value less than or equal to 0");
		
		double y = x;
		double t = x + 5.5;
		t -= (x + 0.5)*Math.log(t);
		double ser = 1.000000000190015;
		for (int i=0; i<=5; ++i) ser += LNGAMMAC[i]/++y;
		
		return -t + Math.log(2.5066282746310005*ser/x);
	}
	
	/** Returns the value at {@code x} of the {@code d}th derivative of the {@code n}th-degree centered uniform B-spline basis function.
		
		@param x the input value.
		
		@param n the B-spline degree. Must be larger than or equal to {@code 0}.
		
		@param d the derivate order. Must be larger than or equal to {@code 0} and less than or equal to {@code n}.
		
		@return the value at {@code x} of the {@code d}th derivative of the {@code n}th-degree centered uniform B-spline basis function.
		
		@throws IllegalArgumentException if {@code n} or {@code d} is out of range.
	*/
	public static double bspline(final double x, final int n, final int d) {
		
		// Check parameters:
		if (n < 0) throw new IllegalArgumentException("B-spline degree less than 0");
		if (d < 0 || d > n) throw new IllegalArgumentException("B-spline derivative order out of range");
		
		final double xo = x + 0.5*(n+1);
		
		// Always 0 outside support [0,n+1]:
		if (xo < 0 || xo >= n+1) return 0;
		
		// Initialize all relevant N_i,0:
		final double[] N = new double[n+1];
		for (int i=0; i<=n; ++i) N[i] = (xo >= i && xo < i+1) ? 1 : 0;
		
		// Compute all relevant N_i,p:
		final int nd = n - d;
		for (int p=1, pp=2, np=n-1; p<=nd; ++p, ++pp, --np)
			for (int i=0; i<=np; ++i) N[i] = ((xo-i)*N[i] - (xo-i-pp)*N[i+1])/p;
		
		// Compute all relevant N^(k)_i,p:
		for (int p=nd+1, np=n-nd-1; p<=n; ++p, --np)
			for (int i=0; i<=np; ++i) N[i] = N[i] - N[i+1];
		
		// Return:
		return N[0];
	}
	
	private static final double INVSQRT2PI = 1.0/Math.sqrt(2*Math.PI);
	
	/** Returns the value at {@code x} of the Gaussian distribution with standard deviation {@code s}. The distribution is normalized to unit integral.
		
		@param x the input value.
		
		@param s the standard deviation of the Gaussian distribution.
		
		@return the value at {@code x} of the Gaussian distribution with standard deviation {@code s}.
		
		@throws IllegalArgumentException if {@code s} is less than or equal to {@code 0}.
	*/
	public static double gauss(final double x, final double s) {
		
		if (s <= 0) throw new IllegalArgumentException("Standard deviation less than or equal to 0");
		
		return INVSQRT2PI*Math.exp(-x*x/(2*s*s))/s;
	}
	
	/** Returns the value of the sinc function at {@code x}. The sinc function is defined as the limit of {@code sin(x)/x}.
		
		@param x the input value.
		
		@return the value of the sinc function at {@code x}.
	*/
	public static double sinc(final double x) {
		
		if (x == 0) return 1;
		else return Math.sin(x)/x;
	}
	
	/** Returns the value at {@code x} of the zeroth-order Bessel function of the first kind.
		
		@param x the input value.
		
		@return the value at {@code x} of the zeroth-order Bessel function of the first kind. Computed using the algorithm described by W. H. Press et al., Numerical Recipes in C: The Art of Scientific Computing, 2nd ed., Cambridge University Press, Cambridge, 1992, Section 6.5.
	*/
	public static double besselj0(final double x) {
		
		double ax, z, xx, y, a1, a2;
		
		if ((ax = Math.abs(x)) < 8) { // Direct rational approximation.
			y = x*x;
			a1 = 57568490574.0+y*(-13362590354.0+y*(651619640.7+y*(-11214424.18+y*(77392.33017+y*(-184.9052456)))));
			a2 = 57568490411.0+y*(1029532985.0+y*(9494680.718+y*(59272.64853+y*(267.8532712+y*1.0))));
			return a1/a2;
		} else { // Fitting function.
			z = 8/ax; y = z*z; xx = ax-0.785398164;
			a1 = 1+y*(-0.1098628627E-2+y*(0.2734510407E-4+y*(-0.2073370639E-5+y*0.2093887211E-6)));
			a2 = -0.1562499995E-1+y*(0.1430488765E-3+y*(-0.6911147651E-5+y*(0.7621095161E-6-y*0.934945152E-7)));
			return Math.sqrt(0.636619772/ax)*(Math.cos(xx)*a1-z*Math.sin(xx)*a2);
		}
	}
	
	/** Returns the value at {@code x} of the first-order Bessel function of the first kind.
		
		@param x the input value.
		
		@return the value at {@code x} of the first-order Bessel function of the first kind. Computed using the algorithm described by W. H. Press et al., Numerical Recipes in C: The Art of Scientific Computing, 2nd ed., Cambridge University Press, Cambridge, 1992, Section 6.5.
	*/
	public static double besselj1(final double x) {
		
		double ax, z, xx, y, a1, a2;
		
		if ((ax = Math.abs(x)) < 8) { // Direct rational approximation.
			y = x*x;
			a1 = x*(72362614232.0+y*(-7895059235.0+y*(242396853.1+y*(-2972611.439+y*(15704.48260+y*(-30.16036606))))));
			a2 = 144725228442.0+y*(2300535178.0+y*(18583304.74+y*(99447.43394+y*(376.9991397+y*1.0))));
			return a1/a2;
		} else { // Fitting function.
			z = 8/ax; y = z*z; xx = ax-2.356194491;
			a1 = 1+y*(0.183105E-2+y*(-0.3516396496E-4+y*(0.2457520174E-5+y*(-0.240337019E-6))));
			a2 = 0.04687499995+y*(-0.2002690873E-3+y*(0.8449199096E-5+y*(-0.88228987E-6+y*0.105787412E-6)));
			a1 = Math.sqrt(0.636619772/ax)*(Math.cos(xx)*a1-z*Math.sin(xx)*a2);
			if (x < 0) return -a1; else return a1;
		}
	}
	
	private static final double ACC = 40.0;
	private static final double BIGNO = 1.0E10;
	private static final double BIGNI = 1.0E-10;
	
	/** Returns the value at {@code x} of the Bessel function of the first kind of order {@code n}.
		
		@param n the order of the Bessel function of the first kind. Must be larger than or equal to {@code 0}.
		
		@param x the input value.
		
		@return the value at {@code x} of the Bessel function of the first kind of order {@code n}. Computed using the algorithm described by W. H. Press et al., Numerical Recipes in C: The Art of Scientific Computing, 2nd ed., Cambridge University Press, Cambridge, 1992, Section 6.5.
		
		@throws IllegalArgumentException if {@code n} is less than {@code 0}.
	*/
	public static double besselj(final int n, final double x) {
		
		if (n < 0) throw new IllegalArgumentException("Order value less than 0");
		
		// Order 0 or 1:
		if (n == 0) return besselj0(x);
		if (n == 1) return besselj1(x);
		
		// Order 2 and larger:
		if (x == 0) return 0;
		final double ax = Math.abs(x);
		double bjm, bjp, sum, ans;
		if (ax > n) { // Upward recurrence from J0 and J1.
			final double tox = 2/ax;
			bjm = besselj0(ax);
			ans = besselj1(ax);
			for (int j=1; j<n; ++j) {
				bjp = j*tox*ans - bjm;
				bjm = ans;
				ans = bjp;
			}
		} else { // Downward recurrence from an even m.
			final double tox = 2/ax;
			final int m = 2*((n + (int)Math.sqrt(ACC*n))/2);
			boolean jsum = false;
			bjp = ans = sum = 0;
			double bj = 1;
			for (int j=m; j>0; --j) {
				bjm = j*tox*bj - bjp;
				bjp = bj;
				bj = bjm;
				if (Math.abs(bj) > BIGNO) {
					bj *= BIGNI;
					bjp *= BIGNI;
					ans *= BIGNI;
					sum *= BIGNI;
				}
				if (jsum) sum += bj;
				jsum = !jsum;
				if (j == n) ans = bjp;
			}
			sum = 2*sum - bj;
			ans /= sum;
		}
		
		return (x < 0 && (n & 1) == 1) ? -ans : ans;
	}
	
	/** Returns the value at {@code (u,v)} of the series expansion of the Lommel U-function of order {@code n} evaluated to {@code t} terms.
		
		@param n the order of the Lommel U-function. Must be larger than or equal to {@code 0}.
		
		@param t the number of terms to which the series expansion is to be computed. Must be larger than {@code 0}.
		
		@param u the first input value.
		
		@param v the second input value. Must be different from {@code 0}.
		
		@return the value at {@code (u,v)} of the series expansion of the Lommel U-function of order {@code n} evaluated to {@code t} terms. Computed using a straightforward implementation of the formula given by M. Born and E. Wolf, Principles of Optics, 5th ed., Pergamon Press, Oxford, 1975, Section 8.8.
		
		@throws IllegalArgumentException if any of the parameter values is out of range.
	*/
	public static double lommelu(final int n, final int t, final double u, final double v) {
		
		if (n < 0) throw new IllegalArgumentException("Order value less than 0");
		if (t <= 0) throw new IllegalArgumentException("Number of terms less than or equal to 0");
		if (v == 0) throw new IllegalArgumentException("Second input value is 0");
		
		final double uov2 = u*u/(v*v);
		double uovn = Math.pow(u/v,n);
		double sum = 0; int sign = 1;
		for (int s=0; s<t; ++s) {
			sum += sign*uovn*besselj(n+2*s,v);
			uovn *= uov2; sign *= -1;
		}
		return sum;
	}
	
	/** Returns the value at {@code (u,v)} of the series expansion of the Lommel V-function of order {@code n} evaluated to {@code t} terms.
		
		@param n the order of the Lommel V-function. Must be larger than or equal to {@code 0}.
		
		@param t the number of terms to which the series expansion is to be computed. Must be larger than {@code 0}.
		
		@param u the first input value. Must be different from {@code 0}.
		
		@param v the second input value.
		
		@return the value at {@code (u,v)} of the series expansion of the Lommel V-function of order {@code n} evaluated to {@code t} terms. Computed using a straightforward implementation of the formula given by M. Born and E. Wolf, Principles of Optics, 5th ed., Pergamon Press, Oxford, 1975, Section 8.8.
		
		@throws IllegalArgumentException if any of the parameter values is out of range.
	*/
	public static double lommelv(final int n, final int t, final double u, final double v) {
		
		if (n < 0) throw new IllegalArgumentException("Order value less than 0");
		if (t <= 0) throw new IllegalArgumentException("Number of terms less than or equal to 0");
		if (u == 0) throw new IllegalArgumentException("First input value is 0");
		
		final double vou2 = v*v/(u*u);
		double voun = Math.pow(v/u,n);
		double sum = 0; int sign = 1;
		for (int s=0; s<t; ++s) {
			sum += sign*voun*besselj(n+2*s,v);
			voun *= vou2; sign *= -1;
		}
		return sum;
	}
	
	private static final int MAXBIT = 30;
	
	/** Indicates whether the given integer value is a power of 2.
		
		@param i the integer value to be tested. Should not be larger than {@code 2^30} in magnitude.
		
		@return {@code true} if {@code i} is a power of 2; {@code false} if this is not the case.
	*/
	public static boolean power2(final int i) {
		
		for (int p=0, p2=1; p<=MAXBIT; ++p, p2<<=1)
			if (i == p2) return true;
		return false;
	}
	
}
