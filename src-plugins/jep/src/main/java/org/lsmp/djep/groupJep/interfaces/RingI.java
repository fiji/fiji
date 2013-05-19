/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.interfaces;

/**
 * Defines the operations on a ring, i.e. an abelian group
 * under + with a closed * operator and * distributitive over +.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public interface RingI extends AbelianGroupI {
	/** Returns the product of two numbers, a*b */
	public Number mul(Number a,Number b);
	/** Get multiplicative identity i.e. 1.
	 * Strictly speaking a ring need not have a mul indentity.
	 * However most useful ones do, and they are not
	 * all integral domains. */
	public Number getONE();

}
