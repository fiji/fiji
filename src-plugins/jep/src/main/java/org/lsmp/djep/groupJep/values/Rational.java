/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.values;
import java.math.*;
/**
 * A Rational number with full precision. Represented as quotien of two
 * numbers (always in most reduced form with posative denominator).
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class Rational extends Number implements Comparable {

	private BigInteger numerator;
	private BigInteger denominator;
	/**
	 */
	private Rational() {	}
	
	public Rational(BigInteger num) { 
		numerator = num; denominator = BigInteger.ONE; 
	}
	
	/** Rationals will always be represented in most reduced
	 * form with a positive denominator.
	 */
	public Rational(BigInteger num,BigInteger den) {
		BigInteger gcd = num.gcd(den);
		if(gcd.equals(BigInteger.ZERO))	{
			numerator = denominator = BigInteger.ZERO;
		}
		else if(den.signum() > 0){
			numerator = num.divide(gcd);
			denominator = den.divide(gcd); 
		}
		else {
			numerator = num.divide(gcd).negate();
			denominator = den.divide(gcd).negate(); 
		}
	}
	
	public int intValue()
	{
		if(denominator.equals(BigInteger.ZERO))
		{
			int sign = numerator.signum();
			if(sign == 0)
				return Integer.MAX_VALUE;
			else if(sign > 0 )
				return Integer.MAX_VALUE;
			else
				return Integer.MIN_VALUE;
		}
		return numerator.divide(denominator).intValue();
	}
	public long longValue()
	{
		return numerator.divide(denominator).longValue();
	}
	public float floatValue()
	{
		if(denominator.equals(BigInteger.ZERO))
		{
			int sign = numerator.signum();
			if(sign == 0)
				return Float.NaN;
			else if(sign > 0 )
				return Float.POSITIVE_INFINITY;
			else
				return Float.NEGATIVE_INFINITY;
		}
		return numerator.divide(denominator).floatValue();
	}
	public double doubleValue()
	{
		if(denominator.equals(BigInteger.ZERO))
		{
			int sign = numerator.signum();
			if(sign == 0)
				return Double.NaN;
			else if(sign > 0 )
				return Double.POSITIVE_INFINITY;
			else
				return Double.NEGATIVE_INFINITY;
		}
		return numerator.divide(denominator).doubleValue();
	}
	
	public Rational add(Rational arg)
	{
		BigInteger ad = this.numerator.multiply(arg.denominator); 
		BigInteger bc = this.denominator.multiply(arg.numerator); 
		BigInteger bd = this.denominator.multiply(arg.denominator);
		BigInteger top = ad.add(bc);
		return new Rational(top,bd);
	}

	public Rational sub(Rational arg)
	{
		BigInteger ad = this.numerator.multiply(arg.denominator); 
		BigInteger bc = this.denominator.multiply(arg.numerator); 
		BigInteger bd = this.denominator.multiply(arg.denominator);
		BigInteger top = ad.subtract(bc);
		return new Rational(top,bd);
	}

	public Rational mul(Rational arg)
	{
		BigInteger ac = this.numerator.multiply(arg.numerator); 
		BigInteger bd = this.denominator.multiply(arg.denominator);
		return new Rational(ac,bd);
	}

	public Rational div(Rational arg)
	{
		BigInteger ad = this.numerator.multiply(arg.denominator); 
		BigInteger bc = this.denominator.multiply(arg.numerator);
		return new Rational(ad,bc);
	}
	
	public Rational pow(Rational arg)
	{
		if(!arg.denominator.equals(BigInteger.ONE))
			throw new ArithmeticException("Can only raise rationals to integer powers");
		int exponant = arg.numerator.intValue();
		if(exponant == 0)
			return new Rational(BigInteger.ONE);
		else if(exponant > 0)
		{
			BigInteger top = this.numerator.pow(exponant); 
			BigInteger bot = this.denominator.pow(exponant);
			return new Rational(top,bot);
		}
		else
		{   // (a/b)^(-c) -> (b/a)^c -> (b^c/a^c)
			BigInteger top = this.numerator.pow(-exponant); 
			BigInteger bot = this.denominator.pow(-exponant);
			return new Rational(bot,top);
		}
	}

	public Rational negate()
	{
			return new Rational(numerator.negate(),denominator);
	}
	
	public Rational inverse()
	{
			return new Rational(denominator,numerator);
	}
	
	public static Number valueOf(String s) {
		int pos = s.indexOf('/');
		if(pos==-1)	return new Rational(new BigInteger(s));
		
		return new Rational(
				new BigInteger(s.substring(pos-1)),
				new BigInteger(s.substring(pos+1,-1))); 
	}
	/**
	 * * Returns the bottom half of the rational.
	 */
	public BigInteger getDenominator() {
		return denominator;
	}

	/**
	 * Returns the top half of the rational.
	 */
	public BigInteger getNumerator() {
		return numerator;
	}

	public String toString() {
		if(denominator.equals(BigInteger.ONE))
			return numerator.toString();
		
		return numerator.toString() +"/" + denominator.toString();
	}
	
	public int compareTo(Object arg)
	{
		Rational num = (Rational) arg;
		if(this.denominator.compareTo(num.denominator) == 0)
		{
			return this.numerator.compareTo(num.numerator);
		}
		BigInteger ad = this.numerator.multiply(num.denominator); 
		BigInteger bc = this.denominator.multiply(num.numerator); 
		return ad.compareTo(bc);
	}
}
