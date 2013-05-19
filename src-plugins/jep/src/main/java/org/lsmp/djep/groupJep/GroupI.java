/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep;
import org.nfunk.jep.type.*;
import org.nfunk.jep.*;
/**
 * Represents a group with an identity, and addition operator.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public interface GroupI {
	/** Returns the identity element under + */
	public Number getZERO();
	/** Get Inverse of a number */
	public Number getInverse(Number num);
	/** Get sum of the numbers */
	public Number add(Number a,Number b);
	/** Get the difference of the numbers.
	 * i.e. a + (-b) */
	public Number sub(Number a,Number b);
	/** whether two numbers are equal */
	public boolean equals(Number a,Number b);
	/** returns number given by the string */
	public Number valueOf(String s);
	/** returns a number factory for creating group elements from strings.
	 * Most groups which are subclasses of {@link org.lsmp.djep.groupJep.groups.Group Group} do not need to
	 * implement this method. */
	public NumberFactory getNumberFactory();
	/** adds the standard constants for this group */
	public void addStandardConstants(JEP j);
	/** adds the standard function for this group */
	public void addStandardFunctions(JEP j);
	/** For groups like rings of polynomials this determins if a given element is a constant polynomial. */	
	public boolean isConstantPoly(Number a);

}
