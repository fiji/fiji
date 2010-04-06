/* @author rich
 * Created on 04-Nov-2003
 */
package org.lsmp.djep.vectorJep.values;

import org.lsmp.djep.vectorJep.*;
import org.nfunk.jep.type.Complex;

/**
 * Degenerate i.e. rank 0 Tensor. Just has a single element.
 * @author Rich Morris
 * Created on 04-Nov-2003
 * @version 1.3.0.2 now extends number
 * TODO don't implement number! So what if a scaler is a boolean
 */
public class Scaler extends Number implements MatrixValueI {

	private static final long serialVersionUID = 336717881577257953L;
	Object value;
	protected Scaler() {
		value = new Double(0.0);
	}
	protected Scaler(Object o) {
		value = o;
	}
	public static MatrixValueI getInstance(Object o) {
		return new Scaler(o);
	}
	public Dimensions getDim() {return Dimensions.ONE; }
	public int getNumEles() { return 1;	}
	public void setEle(int i, Object value) {if(value!=null) this.value = value;}
	public Object getEle(int i) {return value; }
//	public void setValue(Object value) { this.value = value;}
//	public Object getValue() {return value; }
	public String toString() { return value.toString(); }
	/** sets the elements to those of the arguments. */
	public void setEles(MatrixValueI val)
	{
		if(!(val.getDim().equals(Dimensions.ONE))) return;
		value = val.getEle(0);
	}
	
	/** value of constant coeff. */	
	public int intValue() {
		if(value instanceof Complex) return ((Complex) value).intValue();
		if(value instanceof Boolean) return ((Boolean) value).booleanValue()?1:0;
		return ((Number) value).intValue(); // throws a cast exception if not a number
	}
	/** value of constant coeff. */	
	public long longValue() {
		if(value instanceof Complex) return ((Complex) value).longValue();
		if(value instanceof Boolean) return ((Boolean) value).booleanValue()?1l:0l;
		return ((Number) value).longValue(); // throws a cast exception if not a number
	}
	/** value of constant coeff. */	
	public float floatValue() {	
		if(value instanceof Complex) return ((Complex) value).floatValue();
		if(value instanceof Boolean) return ((Boolean) value).booleanValue()?1f:0f;
		return ((Number) value).floatValue(); // throws a cast exception if not a number
	}
	/** value of constant coeff. */	
	public double doubleValue() {
		if(value instanceof Complex) return ((Complex) value).doubleValue();
		if(value instanceof Boolean) return ((Boolean) value).booleanValue()?1d:0d;
		return ((Number) value).doubleValue();	 // throws a cast exception if not a number
	}

	public boolean equals(Object obj) {
		if(!(obj instanceof Scaler)) return false;
		Scaler s = (Scaler) obj;
		if(!s.getDim().equals(getDim())) return false;
		if(!value.equals(s.value)) return false;
		return true;
	}
	
	/**
	 * Always override hashCode when you override equals.
	 * Effective Java, Joshua Bloch, Sun Press
	 */
	public int hashCode() {
		int result = 17;
			result = 37*result+ value.hashCode();
		return result;
	}

	public MatrixValueI copy() {
		return new Scaler(value);
	}
}
