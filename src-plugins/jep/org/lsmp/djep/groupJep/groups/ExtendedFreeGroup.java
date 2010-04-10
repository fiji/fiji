/* @author rich
 * Created on 07-Dec-2004
 */
package org.lsmp.djep.groupJep.groups;

import org.lsmp.djep.groupJep.interfaces.*;
import org.lsmp.djep.groupJep.values.*;
import org.nfunk.jep.type.*;
/**
 * An extended version of a Free Group, limted seport for powers and division.
 * Positive integer powers are allowed and division by constants.
 * TODO implement polynomial division and remainder (mod).
 * 
 * @author Rich Morris
 * Created on 07-Dec-2004
 */
public class ExtendedFreeGroup
	extends FreeGroup
	implements HasPowerI, HasDivI {

	public ExtendedFreeGroup(RingI K, String symbol) {
		super(K, symbol);
	}

	/** Limited implementation of power, only works with integer powers.
	 * Second argument should be an Integer.
	 */
	public Number pow(Number a, Number b) {
		FreeGroupElement exp = (FreeGroupElement) b; 
		if(!isConstantPoly(exp))
			throw new IllegalArgumentException("Powers only supported for integer exponant. Current exponant is "+exp.toString());

		Complex c = exp.getComplexValue();
		if(c.im() != 0.0)
			throw new IllegalArgumentException("Powers only supported for integer exponant. Current exponant is "+exp.toString());
		double re = c.re();
		if(Math.floor(re) != re)
			throw new IllegalArgumentException("Powers only supported for integer exponant. Current exponant is "+exp.toString());

		return ((FreeGroupElement) a).pow((int) re);
	}

	/** Currently just division by constants. Polynomial division to come.
	 * 
	 */
	public Number div(Number a, Number b) {
		return ((FreeGroupElement) a).div((FreeGroupElement) b);
	}

	/** Division of Polynomials, discards remainder.
	 * Not yet implemented.
	 */
//	public Number mod(Number a, Number b) {
//		return null;
//		//return ((FreeGroupElement) a).mod((FreeGroupElement) b);
//	}

}
