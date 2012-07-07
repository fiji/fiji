/* @author rich
 * Created on 09-Mar-2004
 */
package org.lsmp.djep.groupJep.values;
import org.lsmp.djep.groupJep.interfaces.*;
import org.lsmp.djep.groupJep.*;
import org.nfunk.jep.type.*;
/**
 * The ring of polynomials over a ring R.
 * 
 * @author Rich Morris
 * Created on 09-Mar-2004
 */
public class Polynomial extends Number {
	private RingI baseRing;
	private String symbol;
	private Number coeffs[];
	private int degree;
	/**
	 * Construct a polynomial over a ring.
	 * In general the valueOf method should be used to construct a new polynomial.
	 * 
	 * @param baseRing the underlying ring of the polynomial.
	 * @param symbol the symbol used to display the polynomial
	 * @param coeffs an array of coeficients in the base ring coeff[0] is constant, coeff[1] is coefficient of t etc.
	 */
	public Polynomial(RingI baseRing,String symbol,Number coeffs[]) {
		this.baseRing = baseRing;
		this.symbol = symbol;
		int deg=0;
		for(int i=coeffs.length-1;i>0;--i)
			if(!baseRing.equals(coeffs[i],baseRing.getZERO()))
			{
				deg=i;
				break;
			}
		if(deg == coeffs.length-1)
			this.coeffs = coeffs;
		else
		{
			this.coeffs = new Number[deg+1];
			System.arraycopy(coeffs,0,this.coeffs,0,deg+1);
		}
		this.degree = deg;
	}

	/** Sub classes can change the coefficients. Other methods
	 * should treat polynomials as imutable. */
	protected void setCoeffs(Number coeffs[])
	{
		this.coeffs = coeffs;
		this.degree = coeffs.length-1;
	}
	/** Factory method to create a polynomial with the given coefficients.
	 * Sub classes should overwrite this method to costruct objects of the correct type. */
	protected Polynomial valueOf(Number lcoeffs[])
	{
		Polynomial p = new Polynomial(baseRing,symbol,lcoeffs);
		return p;
	}
	public Polynomial add(Polynomial poly)
	{
		int deg = degree > poly.degree ? degree : poly.degree;
		Number lcoeffs[] = new Number[deg+1];
		for(int i=0;i<=deg;++i)
		{
			if(i<=degree && i <= poly.degree)
				lcoeffs[i] = baseRing.add(coeffs[i],poly.coeffs[i]);
			else if(i<=degree)
				lcoeffs[i] = coeffs[i];
			else
				lcoeffs[i] = poly.coeffs[i];
		}
		return valueOf(lcoeffs);
	}

	public Polynomial sub(Polynomial poly)
	{
		int deg = degree > poly.degree ? degree : poly.degree;
		Number lcoeffs[] = new Number[deg+1];
		for(int i=0;i<=deg;++i)
		{
			if(i<=degree && i <= poly.degree)
				lcoeffs[i] = baseRing.sub(coeffs[i],poly.coeffs[i]);
			else if(i<=degree)
				lcoeffs[i] = coeffs[i];
			else
				lcoeffs[i] = baseRing.getInverse(poly.coeffs[i]);
		}
		return valueOf(lcoeffs);
	}
	
	public Polynomial mul(Polynomial poly)
	{
		int deg = degree + poly.degree;
		Number lcoeffs[] = new Number[deg+1];
		for(int i=0;i<=deg;++i)
			lcoeffs[i] = baseRing.getZERO();

		for(int i=0;i<=degree;++i)
			for(int j=0;j<=poly.degree;++j)
			{
				lcoeffs[i+j] = baseRing.add(lcoeffs[i+j],
					baseRing.mul(coeffs[i],poly.coeffs[j]));			
			}
		return valueOf(lcoeffs);
	}

	public Polynomial div(Polynomial poly)
	{
		if(!poly.isConstantPoly())
			throw new IllegalArgumentException("Can currently only divide by numbers and not polynomials");
		
		int deg = coeffs.length-1;
		Number lcoeffs[] = new Number[deg+1];
		for(int i=0;i<deg+1;++i)
			lcoeffs[i] = ((HasDivI) baseRing).div(coeffs[i],poly.getCoeff(0));

		return valueOf(lcoeffs);
	}
	
	
	public Polynomial pow(int exp)
	{
		if(exp == 0) return valueOf(new Number[]{baseRing.getONE()});
		if(exp == 1) return valueOf(this.getCoeffs());
		if(exp < 0)
			throw new IllegalArgumentException("Tried to raise a Polynomial to a negative power");

		Polynomial res = valueOf(new Number[]{baseRing.getONE()});
		Polynomial currentPower = this;
		
		while(exp != 0)
		{
			if((exp & 1) == 1)
				res = res.mul(currentPower);
			exp >>= 1;
			if(exp == 0) break;
			currentPower = currentPower.mul(currentPower);
		}
		return res;
	}
	private boolean needsBrackets(String s)
	{
		int i1 = s.indexOf('+');
		int i2 = s.lastIndexOf('-');
		return ( (i1 !=-1) || (i2>0) );
	}
	public String toString()
	{
		if(degree==0) return coeffs[0].toString();
		StringBuffer sb = new StringBuffer("");
		for(int i=degree;i>=0;--i)
		{
			String s = coeffs[i].toString();

			// don't bother if a zero coeff
			if(s.equals("0") ||
			  this.baseRing.equals(coeffs[i],baseRing.getZERO()))
				continue;

			// apart from first add a + sign if positive
			if(i!=degree && !s.startsWith("-")) sb.append("+");
			
			// always print the final coeff (if non zero)
			if( i==0 ) {
				String s1 = coeffs[i].toString();
				sb.append(s1);
				//if(s1.startsWith("(") && s1.endsWith(")"))
				//{
				//		sb.append(s1.substring(1,s1.length()-1));
				//}
				//else 	sb.append(s1);
				break;
			}
			// if its -1 t^i just print -
			if(s.equals("-1")) 
				sb.append("-");
			else if(s.equals("1")  ||
				this.baseRing.equals(
					coeffs[i],
					baseRing.getONE()))
				{} // don't print 1
			else {
				if(needsBrackets(coeffs[i].toString()))
				{
					sb.append("(");
					sb.append(coeffs[i].toString());
					sb.append(")");
				}
				else
					sb.append(coeffs[i].toString());
				//sb.append(stripBrackets(coeffs[i]));
				sb.append(" ");
			}
			if(i>=2) sb.append(symbol+"^"+i);
			else if(i==1) sb.append(symbol);
		}
		sb.append("");
		return sb.toString();
	}
	
	public int getDegree() { return degree; }
	public String getSymbol() { return symbol; }
	/** Returns the coefficients of polynomial.
	 * TODO use defensive copying
	 * @return the array of coefficients, constant coefficient is element 0.
	 */
	public Number[] getCoeffs() { return coeffs; }
	public Number getCoeff(int i) { return coeffs[i]; }
	public RingI getBaseRing() { return baseRing; }

	/** value of constant coeff. */	
	public int intValue() {return coeffs[0].intValue();	}
	/** value of constant coeff. */	
	public long longValue() {return coeffs[0].longValue();	}
	/** value of constant coeff. */	
	public float floatValue() {	return coeffs[0].floatValue();	}
	/** value of constant coeff. */	
	public double doubleValue() {return coeffs[0].doubleValue();	}

	/** Is this a constant polynomial? **/
	public boolean isConstantPoly() {
		if( coeffs.length > 1) return false;
		return baseRing.isConstantPoly(coeffs[0]);
	}
	public boolean equals(Polynomial n)
	{
		if(this.getDegree()!=n.getDegree()) return false;
		for(int i=0;i<=this.getDegree();++i)
			if(!baseRing.equals(this.getCoeff(i),n.getCoeff(i)))
				return false;
		return true;
	}

	/** returns the complex value of this polynomial. 
	 * Where the value of the symbol is replaced by rootVal. 
	 */
	public Complex calculateComplexValue(Complex rootVal) {
		Number val = coeffs[this.getDegree()];
		Complex cval = GroupJep.complexValueOf(val);
		
		for(int i=this.getDegree()-1;i>=0;--i)
		{
			Number val2 = coeffs[i];
			Complex cval2 = GroupJep.complexValueOf(val2);
			Complex prod = cval.mul(rootVal);
			cval = prod.add(cval2);
		}
		return cval;
	}

	public Number calculateValue(Number rootVal) {
		Number val = coeffs[this.getDegree()];
		
		for(int i=this.getDegree()-1;i>=0;--i)
		{
			Number val2 = coeffs[i];
			Number prod = baseRing.mul(val,rootVal);
			val = baseRing.add(prod,val2);
		}
		return val;
	}

}
