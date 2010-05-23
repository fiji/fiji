/* @author rich
 * Created on 28-Feb-2004
 */
package org.lsmp.djep.xjep;
import org.nfunk.jep.*;

/**
 * A VariableFactory which creates XVariables (which have equations).
 * 
 * @author Rich Morris
 * Created on 28-Feb-2004
 */
public class XVariableFactory extends VariableFactory {

	public Variable createVariable(String name, Object value) {
		return new XVariable(name,value);
	}

	public Variable createVariable(String name) {
		return new XVariable(name);
	}
}
