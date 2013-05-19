/* @author rich
 * Created on 06-Mar-2004
 */
package org.lsmp.djep.groupJep.interfaces;

/**
 * Groups which have a total ordering, i.e <, >= make sense.
 * @see java.lang.Comparable
 * @author Rich Morris
 * Created on 06-Mar-2004
 */
public interface OrderedSetI {
	/** Returns -1,0,1 depending on whether a is less than, equal to or greater than b. */
	public int compare(Number a,Number b);
}
