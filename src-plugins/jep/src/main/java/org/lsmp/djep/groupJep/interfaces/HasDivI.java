/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.interfaces;

/**
 * An IntergralDomainI which also has a notion of division,
 * which is not necessarily closed i.e. the integers.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public interface HasDivI {
	/** get division of two numbers. i.e. a * ( b^-1).
	 * Strictly speeking  */
	public Number div(Number a,Number b);
}
