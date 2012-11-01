/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.groups;
import java.math.*;

import org.lsmp.djep.groupJep.interfaces.*;

/**
 * The field of Reals represented by BigDecimals.
 * TODO Sorry power is not implemented.
 * @see BigDecimal
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class BigReals extends Group implements FieldI,OrderedSetI {

	private BigDecimal ZERO = BigDecimal.valueOf(0);
	private BigDecimal ONE = BigDecimal.valueOf(1);
	private int roundMode;
	private int scale;
	
	/** private constructor as a scale must be specified.	 */
	private BigReals() {}
	/** Create a field of BigIntegers.
	 * The round mode and scale are used during the div
	 * method which calls {@link java.math.BigDecimal#divide(BigDecimal,int,int)}.
	 * if scale is negative then the
	 * {@link java.math.BigDecimal#divide(BigDecimal,int)}. is called instead. 
	 */
	public BigReals(int scale,int roundMode) {
		this.roundMode = roundMode;
		this.scale = scale;
	}

	/** Create a BigReals group with round mode set to
	 * BigDecimal.ROUND_HALF_DOWN.
	 * 
	 * @param scale
	 */
	public BigReals(int scale) {
		this.roundMode = BigDecimal.ROUND_HALF_DOWN;
		this.scale = scale;
	}
	public Number getZERO() {
		return ZERO;
	}

	public Number getONE() {
		return ONE;
	}

	public Number getInverse(Number num) {
		BigDecimal a = (BigDecimal) num;
		return a.negate();
	}

	public Number getMulInverse(Number num) {
		return div(ONE,num);
	}

	public Number add(Number num1, Number num2) {
		BigDecimal a = (BigDecimal) num1;
		BigDecimal b = (BigDecimal) num2;
		return a.add(b);
	}

	public Number sub(Number num1, Number num2) {
		BigDecimal a = (BigDecimal) num1;
		BigDecimal b = (BigDecimal) num2;
		return a.subtract(b);
	}

	public Number mul(Number num1, Number num2) {
		BigDecimal a = (BigDecimal) num1;
		BigDecimal b = (BigDecimal) num2;
		return a.multiply(b);
	}

	public Number div(Number num1, Number num2) {
		BigDecimal a = (BigDecimal) num1;
		BigDecimal b = (BigDecimal) num2;
		if(scale>0)
			return a.divide(b,scale,roundMode);
		return a.divide(b,roundMode);
	}

	public boolean equals(Number a,Number b)	{
		return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
	}
	
	public int compare(Number a,Number b)	{
		return ((BigDecimal) a).compareTo((BigDecimal) b);
	}
	
	public Number valueOf(String str) {
		return new BigDecimal(str);
	}
}
