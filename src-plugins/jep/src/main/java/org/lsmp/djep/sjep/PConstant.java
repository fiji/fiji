/* @author rich
 * Created on 14-Dec-2004
 */
package org.lsmp.djep.sjep;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
/**
 * Represents a constant.
 * 
 * @author Rich Morris
 * Created on 14-Dec-2004
 */
public class PConstant extends AbstractPNode {

	Object value;
	
	/**
	 * 
	 */
	public PConstant(PolynomialCreator pc,Object o) {
		super(pc);
		value = o;
	}

	public PNodeI negate() throws ParseException
	{
		return new PConstant(pc,pc.neg(value));
	}
	public PNodeI add(PNodeI c) throws ParseException {
		if(this.isZero()) return c;
		if(c instanceof PConstant)
			return new PConstant(pc,pc.add(value,((PConstant) c).value));
		
		return super.add(c);
	}
	
	public PNodeI sub(PNodeI c) throws ParseException {
		if(this.isZero()) return c.negate();
		if(c instanceof PConstant)
			return new PConstant(pc,pc.sub(value,((PConstant) c).value));
		return super.sub(c);
	}

	public PNodeI mul(PNodeI c) throws ParseException {
		if(this.isZero()) return pc.zeroConstant;
		if(this.isOne()) return c;
		if(c.isZero()) return pc.zeroConstant;
		if(c.isOne()) return this;
		
		if(c instanceof PConstant)
			return new PConstant(pc,pc.mul(value,((PConstant) c).value));

		return super.mul(c);
	}

	public PNodeI div(PNodeI c) throws ParseException {
		if(this.isZero()) {
			if(c.isZero())
				return pc.nanConstant;
			return pc.zeroConstant; 
		}
		if(c.isZero()) return pc.infConstant;
		if(c.isOne()) return this;
		
		if(c instanceof PConstant)
			return new PConstant(pc,pc.div(value,((PConstant) c).value));
		
		return super.div(c);
	}

	public PNodeI invert() throws ParseException
	{
		return new PConstant(pc,pc.div(pc.one,value));
	}

	public PNodeI pow(PNodeI c) throws ParseException {
		if(this.isZero()){
			if(c.isZero()) return pc.nanConstant;
			return pc.zeroConstant;
		}
		if(this.isOne()) return pc.oneConstant;
		if(c.isZero()) return pc.oneConstant;
		 
		if(c instanceof PConstant)
			return new PConstant(pc,pc.raise(value,((PConstant) c).value));
	
		return super.pow(c);
	}
	
	public String toString()
	{
		if(isZero()) return "0";
		if(isOne()) return "1";
		if(isInfinity()) return "inf";
		if(isNan()) return "NaN";
		
		if(isInteger())
				return String.valueOf(intValue()); 
		return value.toString(); 
	}
	
	public boolean isZero() {return value.equals(pc.zero);}
	public boolean isOne() {return value.equals(pc.one);}
	public boolean isMinusOne() {return value.equals(pc.minusOne);}
	public boolean isInfinity() {
		if(value.equals(pc.infinity)) return true;
		if(value instanceof Double)
			return ((Double) value).isInfinite();
		if(value instanceof Complex)
			return ((Complex) value).isInfinite();
		return false;
	}
	public boolean isNan() {
		if(value.equals(pc.nan)) return true;
		if(value instanceof Double)
			return ((Double) value).isNaN();
		if(value instanceof Complex)
			return ((Complex) value).isNaN();
		return false;
	}
	public boolean isPositive() {
		try	{
			return ((Double) value).compareTo((Double) pc.zero) > 0;
		} catch(Exception e) { return false; }
	}
	public boolean isNegative() {
		try	{
			return ((Double) value).compareTo((Double) pc.zero) < 0;
		} catch(Exception e) { return false; }
	}
	public boolean isInteger() {
		try	{
			double val = ((Double) value).doubleValue();
			return val == Math.floor(val);
		} catch(Exception e) { return false; }
	}
	public int intValue() {
		return ((Number) value).intValue();
	}
	
	public Node toNode() throws ParseException
	{
		return pc.nf.buildConstantNode(value);
	}

	public int compareTo(PConstant c)
	{
			return ((Comparable) value).compareTo(c.value);
	}


	public boolean equals(PNodeI node)
	{
		if(node instanceof PConstant)
			return value.equals(((PConstant)node).value);
		return false;
	}

	public PNodeI expand()	{ return this;	}
}
