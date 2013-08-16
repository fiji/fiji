/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.groups;
import java.math.*;

import org.lsmp.djep.groupJep.interfaces.*;
import org.lsmp.djep.groupJep.values.*;

/**
 * The Field of rational numbers.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class Rationals extends Group implements FieldI,OrderedSetI,HasPowerI {

	private Rational ZERO = new Rational(BigInteger.valueOf(0));
	private Rational ONE = new Rational(BigInteger.valueOf(1));

	public Rationals() {}

	public Number getZERO() {
		return ZERO;
	}

	public Number getONE() {
		return ONE;
	}

	public Number getInverse(Number num) {
		return ((Rational) num).negate();
	}

	public Number getMulInverse(Number num) {
		return ((Rational) num).inverse();
	}

	public Number add(Number a, Number b) {
		return ((Rational) a).add((Rational) b);
	}

	public Number sub(Number a, Number b) {
		return ((Rational) a).sub((Rational) b);
	}

	public Number mul(Number a, Number b) {
		return ((Rational) a).mul((Rational) b);
	}

	public Number div(Number a, Number b) {
		return ((Rational) a).div((Rational) b);
	}

	public Number pow(Number a, Number b) {
		return ((Rational) a).pow((Rational) b);
	}

	public boolean equals(Number a,Number b)
	{
		return ((Rational) a).compareTo(b) == 0;
	}
	
	public int compare(Number a,Number b)
	{
		return ((Rational) a).compareTo(b);
	}
	
	public Number valueOf(String s) {
		return Rational.valueOf(s); 
	}
	
	public String toString() { return "Q"; }
}
