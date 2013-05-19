/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.groups;
import org.lsmp.djep.groupJep.interfaces.*;
import java.math.*;

/**
 * The group of integers mod n.
 * For prime values of n this is a field, with some
 * nice division tables. i.e. for Z5
 * <pre>* | 1 2 3 4
 * ------------
 * 1 | 1 2 3 4
 * 2 | 2 4 1 3
 * 3 | 3 1 4 2
 * 4 | 4 3 2 1
 * </pre>
 * <pre>/ | 1 2 3 4
 * ------------
 * 1 | 1 2 3 4
 * 2 | 3 1 4 2
 * 3 | 2 4 1 3
 * 4 | 4 3 2 1
 * </pre>
 *
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class Zn extends Group implements FieldI,
	OrderedSetI,HasModI,HasPowerI {
	BigInteger modulus;
	/**
	 * Operations on the reals (Implemented as BigInteger).
	 */
	private Zn() {}
	
	public Zn(BigInteger modulus) {
		this.modulus = modulus;
	}

	public Number getZERO() {
		return BigInteger.ZERO;
	}

	public Number getONE() {
		return BigInteger.ONE;
	}

	public Number getInverse(Number num) {
		BigInteger a = (BigInteger) num;
		return a.negate().mod(modulus);
	}

	public Number getMulInverse(Number num) {
		BigInteger a = (BigInteger) num;
		return a.modInverse(modulus);
	}

	public Number add(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.add(b).mod(modulus);
	}

	public Number sub(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.subtract(b).mod(modulus);
	}

	public Number mul(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.multiply(b).mod(modulus);
	}

	public Number div(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.multiply(b.modInverse(modulus)).mod(modulus);
	}
	
	public Number mod(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.mod(b).mod(modulus);
	}
	
	public Number pow(Number num1, Number num2) {
		BigInteger a = (BigInteger) num1;
		BigInteger b = (BigInteger) num2;
		return a.modPow(b,modulus);
	}
	public boolean equals(Number a,Number b)	{
		return ((Integer) a).compareTo((Integer) b) == 0;
	}
	
	public int compare(Number a,Number b)	{
		return ((Integer) a).compareTo((Integer) b);
	}


	public Number valueOf(String str) {
		BigInteger in = new BigInteger(str);
		return in.mod(modulus);
	}
	
	public String toString() { return "Integers mod "+this.modulus; }
}
