/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/


package  org.nfunk.jep.type;
import java.text.NumberFormat;

/**
 * Represents a complex number with double precision real and imaginary
 * components. Includes complex arithmetic functions.<p>
 * The two main sources of reference used for creating this class were:<br>
 * - "Numerical Recipes in C - The Art of Scientific Computing"
 *    (ISBN 0-521-43108-5) http://www.nr.com and <br>
 * - The org.netlib.math.complex package (http://www.netlib.org) which was 
 *   developed by Sandy Anderson and Priyantha Jayanetti (published under
 *   GPL).<p>
 * Some of the arithmetic functions in this class are based on the mathematical
 * equations given in the source of the netlib package. The functions were
 * validated by comparing results with the netlib complex class.<p>
 * It is important to note that the netlib complex package is more
 * extensive and efficient (e.g. Garbage collector friendly) than this
 * implementation. If high precision and efficiency if of necessity it is 
 * recommended to use the netlib package.
 *
 * @author Nathan Funk
 * @version 2.3.0 alpha now extends Number, has add and sub methods.
 * @version 2.3.0 beta 1 now overrides equals and hashCode.
/* @version 2.3.0 beta 2 does not implement Number anymore, as caused too many problems.
 */

public class Complex
{
	/** the real component */
	private double re;
	
	/** the imaginary component */
	private double im;
	

//------------------------------------------------------------------------
// Constructors

	/**
	 * Default constructor.
	 */
	public Complex() {
		re = 0;
		im = 0;
	}

	/**
	 * Constructor from a single double value. The complex number is
	 * initialized with the real component equal to the parameter, and
	 * the imaginary component equal to zero.
	 */
	public Complex(double re_in) {
		re = re_in;
		im = 0;
	}

	/**
	 * Construct from a Number. This constructor uses the doubleValue()
	 * method of the parameter to initialize the real component of the
	 * complex number. The imaginary component is initialized to zero.
	 */
	public Complex(Number re_in) {
		re = re_in.doubleValue();
		im = 0;
	}
	
	/**
	 * Copy constructor
	 */
	public Complex(Complex z) {
		re = z.re;
		im = z.im;
	}

	/**
	 * Initialize the real and imaginary components to the values given
	 * by the parameters.
	 */
	public Complex(double re_in, double im_in) {
		re = re_in;
		im = im_in;
	}

	/**
	 * Returns the real component of this object
	 */
	public double re() {
		return re;	
	}

	/**
	 * Returns the imaginary component of this object
	 */
	public double im() {
		return im;
	}
	
	/**
	 * Copies the values from the parameter object to this object
	 */
	public void set(Complex z) {
		re = z.re;
		im = z.im;
	}
	
	/**
	 * Sets the real and imaginary values of the object.
	 */
	public void set(double re_in, double im_in) {
		re = re_in;
		im = im_in;
	}

	/**
	 * Sets the real component of the object
	 */
	public void setRe(double re_in) {
		re = re_in;
	}

	/**
	 * Sets the imaginary component of the object
	 */
	public void setIm(double im_in) {
		im = im_in;
	}

//------------------------------------------------------------------------
// Various functions

	/**
	 * Compares this object with the Complex number given as parameter
	 * <pre>b</pre>. The <pre>tolerance</pre> parameter is the radius
	 * within which the <pre>b</pre> number must lie for the two
	 * complex numbers to be considered equal.
	 *
	 * @return <pre>true</pre> if the complex number are considered equal,
	 * <pre>false</pre> otherwise.
	 */
	public boolean equals(Complex b, double tolerance) {
		double temp1 = (re - b.re);
		double temp2 = (im - b.im);
		
		return (temp1*temp1 + temp2*temp2) <= tolerance*tolerance;
	}
	/**
	 * Compares this object against the specified object. 
	 * The result is true if and only if the argument is not null 
	 * and is a Complex object that represents the same complex number. 
	 * Equality follows the same pattern as Double aplies to each field:
	 * <ul>
     * <li>If d1 and d2 both represent Double.NaN, then the equals method returns true, even though Double.NaN==Double.NaN has the value false.
     * <li>If d1 represents +0.0 while d2 represents -0.0, or vice versa, the equal test has the value false, even though +0.0==-0.0 has the value true.
     * </ul>
     * This definition allows hash tables to operate properly.

	 * @since 2.3.0.2
	 */
	public boolean equals(Object o) {
		if(!(o instanceof Complex)) return false;
		Complex c = (Complex) o;
		return(Double.doubleToLongBits(this.re) == Double.doubleToLongBits(c.re) 
			&& Double.doubleToLongBits(this.im) == Double.doubleToLongBits(c.im));
	}
	/**
	 * Always override hashCode when you override equals.
	 * Efective Java, Joshua Bloch, Sun Press
	 */
	public int hashCode() {
		int result = 17;
		long xl = Double.doubleToLongBits(this.re);
		long yl = Double.doubleToLongBits(this.im);
		int xi = (int)(xl^(xl>>32));
		int yi = (int)(yl^(yl>>32));
		result = 37*result+xi;
		result = 37*result+yi;
		return result;
	}
	/**
	 * Returns the value of this complex number as a string in the format:
	 * <pre>(real, imaginary)</pre>.
	 */
	public String toString() {
		return "(" + re	+ ", " + im + ")";
	}

	public String toString(NumberFormat format)
	{
		return "(" + format.format(re) +", "+format.format(im)+")";	
	}
	
	/** Prints using specified number format in format or "2" or "3 i"
	 * or "(2+3 i)"  if flag is true
	 * or "2+3 i" if flag is false
	 */
	
	public String toString(NumberFormat format,boolean flag)
	{
		if(im == 0.0)
			return format.format(re);
		else if(re == 0.0)
			return format.format(im)+" i)";	
		else if(flag)
			return "(" + format.format(re) +"+"+format.format(im)+" i)";	
		else
			return format.format(re) +"+"+format.format(im)+" i";	
	}

	/**
	 * Returns <tt>true</tt> if either the real or imaginary component of this
	 * <tt>Complex</tt> is an infinite value.
	 *
	 * <p>
	 * @return  <tt>true</tt> if either component of the <tt>Complex</tt> object is infinite; <tt>false</tt>, otherwise.
	 * <p>
	 **/
	public boolean isInfinite() {
		return (Double.isInfinite(re) || Double.isInfinite(im));
	}

	/**
	 * Returns <tt>true</tt> if either the real or imaginary component of this
	 * <tt>Complex</tt> is a Not-a-Number (<tt>NaN</tt>) value.
	 *
	 * <p>
	 * @return  <tt>true</tt> if either component of the <tt>Complex</tt> object is <tt>NaN</tt>; <tt>false</tt>, otherwise.
	 * <p>
	 **/
	public boolean isNaN() {
		return (Double.isNaN(re) || Double.isNaN(im));
	}
	
	/**
	 * Returns the absolute value of the complex number.
	 * <p>
	 * Adapted from Numerical Recipes in C -
	 * The Art of Scientific Computing<br>
	 * ISBN 0-521-43108-5
	 */
	public double abs() {
		double absRe = Math.abs(re);
		double absIm = Math.abs(im);
		
		if (absRe == 0 && absIm == 0) {
			return 0;
		} else if (absRe>absIm) {
			double temp = absIm/absRe;
			return absRe*Math.sqrt(1 + temp*temp);	
		} else {
			double temp = absRe/absIm;
			return absIm*Math.sqrt(1 + temp*temp);
		}
	}

	/**
	 * Returns the square of the absolute value (re*re+im*im).
	 */
	public double abs2() {
		return re*re+im*im;	
	}

	/**
	 * Returns the argument of this complex number (Math.atan2(re,im))
	 */
	public double arg() {
		return Math.atan2(im,re);
	}

	/**
	 * Returns the negative value of this complex number.
	 */
	public Complex neg() {
		return new Complex(-re,-im);
	}

	/**
	 * Multiply the complex number with a double value.
	 * @return The result of the multiplication
	 */
	public Complex mul(double b) {
		return new Complex(re*b, im*b);
	}

	/**
	 * Adds the complex number with another complex value.
	 * @return The result of the addition
	 * @since 2.3.0.1
	 */
	public Complex add(Complex b) {
		return new Complex(re+b.re,im+b.im);
	}

	/**
	 * Adds the complex number with another complex value.
	 * @return The result of the addition
	 * @since 2.3.0.1
	 */
	public Complex sub(Complex b) {
		return new Complex(re-b.re,im-b.im);
	}
	/**
	 * Multiply the complex number with another complex value.
	 * @return The result of the multiplication
	 */
	public Complex mul(Complex b) {
		return new Complex(re*b.re - im*b.im,
						   im*b.re + re*b.im);
	}
	
	/**
	 * Returns the result of dividing this complex number by the parameter.
	 */
	public Complex div(Complex b) {
		// Adapted from Numerical Recipes in C - The Art of Scientific Computing
		// ISBN 0-521-43108-5
		double resRe, resIm;
		double r, den;
		
		if (Math.abs(b.re) >= Math.abs(b.im)) {
			r = b.im/b.re;
			den = b.re + r*b.im;
			resRe = (re+r*im)/den;
			resIm = (im-r*re)/den;
		} else {
			r = b.re/b.im;
			den = b.im + r*b.re;
			resRe = (re*r+im)/den;
			resIm = (im*r-re)/den;
		}
		
		return new Complex(resRe, resIm);
	}

	/**
	  * Returns the value of this complex number raised to the power
	  * of a real component (in double precision).<p>
	  * This method considers special cases where a simpler algorithm
	  * would return "ugly" results.<br>
	  * For example when the expression (-1e40)^0.5 is evaluated without
	  * considering the special case, the argument of the base is the
	  * double number closest to pi. When sin and cos are used for the
	  * final evaluation of the result, the slight difference of the
	  * argument from pi causes a non-zero value for the real component
	  * of the result. Because the value of the base is so high, the error
      * is magnified.Although the error is normal for floating 
	  * point calculations, the consideration of commonly occuring special
	  * cases improves the accuracy and aesthetics of the results.<p>
	  * If you know a more elegant way to solve this problem, please let
	  * me know at nathanfunk@hotmail.com .
	  */
	public Complex power(double exponent) {
		// z^exp = abs(z)^exp * (cos(exp*arg(z)) + i*sin(exp*arg(z)))
		double scalar = Math.pow(abs(),exponent);
		boolean specialCase = false;
		int factor = 0;

		// consider special cases to avoid floating point errors
		// for power expressions such as (-1e20)^2
		if (im==0 && re<0) {specialCase = true; factor = 2;}
		if (re==0 && im>0) {specialCase = true; factor = 1;}
		if (re==0 && im<0) {specialCase = true; factor = -1;}
		
		if (specialCase && factor*exponent == (int)(factor*exponent)) {
			short[] cSin = {0,1,0,-1}; //sin of 0, pi/2, pi, 3pi/2
			short[] cCos = {1,0,-1,0}; //cos of 0, pi/2, pi, 3pi/2
			
			int x = ((int)(factor*exponent))%4;
			if (x<0) x = 4+x;
			
			return new Complex(scalar*cCos[x], scalar*cSin[x]);
		}
		
		double temp = exponent * arg();
		
		return new Complex(scalar*Math.cos(temp), scalar*Math.sin(temp));
	}
	
	/**
	 * Returns the value of this complex number raised to the power of
	 * a complex exponent
	 */
	public Complex power(Complex exponent) {
		if (exponent.im == 0) return power(exponent.re);
		
		double temp1Re = Math.log(abs());
		double temp1Im = arg();
		
		double temp2Re = (temp1Re*exponent.re) - (temp1Im*exponent.im);
		double temp2Im = (temp1Re*exponent.im) + (temp1Im*exponent.re);

		double scalar = Math.exp(temp2Re);
		
		return new Complex(scalar*Math.cos(temp2Im), scalar*Math.sin(temp2Im));
	}
	/** Returns the complex conjugate. */
	public Complex conj() {
		return new Complex(re,-im);
	}
	/**
	 * Returns the logarithm of this complex number.
	 */	
	public Complex log() {
		return new Complex(Math.log(abs()), arg());	
	}
	
	/**
	 * Calculates the square root of this object.
	 * Adapted from Numerical Recipes in C - The Art of Scientific Computing
	 * (ISBN 0-521-43108-5)
	 */
	public Complex sqrt() {
		Complex c;
		double absRe,absIm,w,r;
		
		if (re == 0 && im == 0) {
			c = new Complex(0,0);
		} else {
			absRe = Math.abs(re);
			absIm = Math.abs(im);
			
			if (absRe>=absIm) {
				r = absIm/absRe;
				w = Math.sqrt(absRe)*Math.sqrt(0.5*(1.0+Math.sqrt(1.0+r*r)));
			} else {
				r = absRe/absIm;
				w = Math.sqrt(absIm)*Math.sqrt(0.5*(r  +Math.sqrt(1.0+r*r)));
			}
			
			if (re>=0) {
				c = new Complex(w, im/(2.0*w));
			} else {
				if (im<0) w = -w;
				c = new Complex(im/(2.0*w), w);
			}
		}
		
		return c;
	}

//------------------------------------------------------------------------
// Trigonometric functions

	/**
	 * Returns the sine of this complex number.
	 */
	public Complex sin() {
		double izRe, izIm;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		double scalar;
		
		//  sin(z)  =  ( exp(i*z) - exp(-i*z) ) / (2*i)
		izRe = -im;
		izIm =  re;
		
		// first exp
		scalar = Math.exp(izRe);
		temp1Re = scalar * Math.cos(izIm);
		temp1Im = scalar * Math.sin(izIm);
		
		// second exp
		scalar = Math.exp(-izRe);
		temp2Re = scalar * Math.cos(-izIm);
		temp2Im = scalar * Math.sin(-izIm);
		
		temp1Re -= temp2Re;
		temp1Im -= temp2Im;
		
		return new Complex(0.5*temp1Im, -0.5*temp1Re);
	}

	/**
	 * Returns the cosine of this complex number.
	 */
	public Complex cos() {
		double izRe, izIm;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		double scalar;
		
		//  cos(z)  =  ( exp(i*z) + exp(-i*z) ) / 2
		izRe = -im;
		izIm =  re;
		
		// first exp
		scalar = Math.exp(izRe);
		temp1Re = scalar * Math.cos(izIm);
		temp1Im = scalar * Math.sin(izIm);
		
		// second exp
		scalar = Math.exp(-izRe);
		temp2Re = scalar * Math.cos(-izIm);
		temp2Im = scalar * Math.sin(-izIm);
		
		temp1Re += temp2Re;
		temp1Im += temp2Im;
		
		return new Complex(0.5*temp1Re, 0.5*temp1Im);
	}


	/**
	 * Returns the tangent of this complex number.
	 */
	public Complex tan() {
		// tan(z) = sin(z)/cos(z)
		double izRe, izIm;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		double scalar;
		Complex sinResult, cosResult;
		
		//  sin(z)  =  ( exp(i*z) - exp(-i*z) ) / (2*i)
		izRe = -im;
		izIm =  re;
		
		// first exp
		scalar = Math.exp(izRe);
		temp1Re = scalar * Math.cos(izIm);
		temp1Im = scalar * Math.sin(izIm);
		
		// second exp
		scalar = Math.exp(-izRe);
		temp2Re = scalar * Math.cos(-izIm);
		temp2Im = scalar * Math.sin(-izIm);
		
		temp1Re -= temp2Re;
		temp1Im -= temp2Im;
		
		sinResult = new Complex(0.5*temp1Re, 0.5*temp1Im);

		//  cos(z)  =  ( exp(i*z) + exp(-i*z) ) / 2
		izRe = -im;
		izIm =  re;
		
		// first exp
		scalar = Math.exp(izRe);
		temp1Re = scalar * Math.cos(izIm);
		temp1Im = scalar * Math.sin(izIm);
		
		// second exp
		scalar = Math.exp(-izRe);
		temp2Re = scalar * Math.cos(-izIm);
		temp2Im = scalar * Math.sin(-izIm);
		
		temp1Re += temp2Re;
		temp1Im += temp2Im;
		
		cosResult = new Complex(0.5*temp1Re, 0.5*temp1Im);
		
		return sinResult.div(cosResult);
	}
	

//------------------------------------------------------------------------
// Inverse trigonometric functions
	
	public Complex asin() {
		Complex result;
		double tempRe, tempIm;
		
		//  asin(z)  =  -i * log(i*z + sqrt(1 - z*z))

		tempRe =  1.0 - ( (re*re) - (im*im) );
		tempIm =  0.0 - ( (re*im) + (im*re) );

		result =  new Complex(tempRe, tempIm);
		result = result.sqrt();
		
		result.re += -im;
		result.im +=  re;
		
		tempRe = Math.log(result.abs());
		tempIm = result.arg();
		
		result.re =   tempIm;
		result.im = - tempRe;
		
		return result;
	}

	public Complex acos() {
		Complex result;
		double tempRe, tempIm;
		
		//  acos(z)  =  -i * log( z + i * sqrt(1 - z*z) )

		tempRe =  1.0 - ( (re*re) - (im*im) );
		tempIm =  0.0 - ( (re*im) + (im*re) );

		result =  new Complex(tempRe, tempIm);
		result = result.sqrt();
		
		tempRe = -result.im;
		tempIm =  result.re;
		
		result.re = re + tempRe;
		result.im = im + tempIm;
		
		tempRe = Math.log(result.abs());
		tempIm = result.arg();
		
		result.re =   tempIm;
		result.im = - tempRe;
		
		return result;
	}

	public Complex atan() {
		// atan(z) = -i/2 * log((i-z)/(i+z))
		
		double tempRe, tempIm;
		Complex result = new Complex(-re, 1.0 - im);
		
		tempRe = re;
		tempIm = 1.0 + im;
		
		result = result.div(new Complex(tempRe, tempIm));
		
		tempRe = Math.log(result.abs());
		tempIm = result.arg();
		
		result.re =  0.5*tempIm;
		result.im = -0.5*tempRe;
		
		return result;
	}


//------------------------------------------------------------------------
// Hyperbolic trigonometric functions

	public Complex sinh() {
		double scalar;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		//  sinh(z)  =  ( exp(z) - exp(-z) ) / 2
		
		// first exp
		scalar = Math.exp(re);
		temp1Re = scalar * Math.cos(im);
		temp1Im = scalar * Math.sin(im);
		
		// second exp
		scalar = Math.exp(-re);
		temp2Re = scalar * Math.cos(-im);
		temp2Im = scalar * Math.sin(-im);
		
		temp1Re -= temp2Re;
		temp1Im -= temp2Im;
		
		return new Complex(0.5*temp1Re, 0.5*temp1Im);
	}


	public Complex cosh() {
		double scalar;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		//  cosh(z)  =  ( exp(z) + exp(-z) ) / 2
		
		// first exp
		scalar = Math.exp(re);
		temp1Re = scalar * Math.cos(im);
		temp1Im = scalar * Math.sin(im);
		
		// second exp
		scalar = Math.exp(-re);
		temp2Re = scalar * Math.cos(-im);
		temp2Im = scalar * Math.sin(-im);
		
		temp1Re += temp2Re;
		temp1Im += temp2Im;
		
		return new Complex(0.5*temp1Re, 0.5*temp1Im);
	}

	public Complex tanh() {
		double scalar;
		double temp1Re, temp1Im;
		double temp2Re, temp2Im;
		Complex sinRes, cosRes;
		//  tanh(z)  =  sinh(z) / cosh(z)

		scalar = Math.exp(re);
		temp1Re = scalar * Math.cos(im);
		temp1Im = scalar * Math.sin(im);
		
		scalar = Math.exp(-re);
		temp2Re = scalar * Math.cos(-im);
		temp2Im = scalar * Math.sin(-im);
		
		temp1Re -= temp2Re;
		temp1Im -= temp2Im;
		
		sinRes = new Complex(0.5*temp1Re, 0.5*temp1Im);

		scalar = Math.exp(re);
		temp1Re = scalar * Math.cos(im);
		temp1Im = scalar * Math.sin(im);
		
		scalar = Math.exp(-re);
		temp2Re = scalar * Math.cos(-im);
		temp2Im = scalar * Math.sin(-im);
		
		temp1Re += temp2Re;
		temp1Im += temp2Im;
		
		cosRes = new Complex(0.5*temp1Re, 0.5*temp1Im);
		
		return sinRes.div(cosRes);
	}

	
//------------------------------------------------------------------------
// Inverse hyperbolic trigonometric functions

	public Complex asinh() {
		Complex result;
		//  asinh(z)  =  log(z + sqrt(z*z + 1))

		result = new Complex(
			((re*re) - (im*im)) + 1,
			(re*im) + (im*re));

		result = result.sqrt();
		
		result.re += re;
		result.im += im;
		
		double temp = result.arg();
		result.re = Math.log(result.abs());
		result.im = temp;
		
		return result;
	}

	public Complex acosh() {
		Complex result;
		
		//  acosh(z)  =  log(z + sqrt(z*z - 1))

		result = new Complex(
			((re*re) - (im*im)) - 1,
			(re*im) + (im*re));

		result = result.sqrt();
		
		result.re += re;
		result.im += im;
		
		double temp = result.arg();
		result.re = Math.log(result.abs());
		result.im = temp;
		
		return result;
	}

	public Complex atanh() {
		//  atanh(z)  =  1/2 * log( (1+z)/(1-z) )
		
		double tempRe, tempIm;
		Complex result = new Complex(1.0 + re, im);
		
		tempRe = 1.0 - re;
		tempIm =     - im;
		
		result = result.div(new Complex(tempRe, tempIm));
		
		tempRe = Math.log(result.abs());
		tempIm = result.arg();
		
		result.re = 0.5*tempRe;
		result.im = 0.5*tempIm;
		
		return result;
	}

	/**
	 * Converts an [r,theta] pair to a complex number r * e^(i theta).
	 * @param r The radius
	 * @param theta The angle
	 * @return The complex result.
	 * @since 2.3.0.1
	 */
	public static Complex polarValueOf(Number r,Number theta)
	{
		double rad = r.doubleValue();
		double ang = theta.doubleValue();
		return new Complex(rad*Math.cos(ang), rad*Math.sin(ang));
		
	}
	/** Returns real part.
	 * @since 2.3.0.1
	 */
	public double doubleValue() {
		return re;
	}

	/** Returns real part.
	 * @since 2.3.0.1
	 */
	public float floatValue() {
		return (float) re;
	}

	/** Returns real part.
	 * @since 2.3.0.1
	 */
	public int intValue() {
		return (int) re;
	}

	/** Returns real part.
	 * @since 2.3.0.1
	 */
	public long longValue() {
		return (long) re;
	}

}
