/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.interfaces;

/**
 * Represents a field.
 * Abelian group for + with inverse 0.
 * Elements excluding 0 form a abelian group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public interface FieldI extends IntegralDomainI,HasDivI {
	/** get mul inverse */
	public Number getMulInverse(Number num);
}
