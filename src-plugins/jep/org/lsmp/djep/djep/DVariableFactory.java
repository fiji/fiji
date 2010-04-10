/* @author rich
 * Created on 19-Dec-2003
 */
package org.lsmp.djep.djep;

import org.nfunk.jep.Variable;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;
/**
 * A VariableFactory which can work with PartialDerivatives.
 * @author Rich Morris
 * Created on 19-Dec-2003
 */
public class DVariableFactory extends XVariableFactory {

	/** Creates a partial derivative of a given variable. */
	public PartialDerivative createDerivative(DVariable var, String[] dnames,Node eqn) {
		return new PartialDerivative(var,dnames,eqn);
	}

	public Variable createVariable(String name, Object value) {
		return new DVariable(name,value);
	}

	public Variable createVariable(String name) {
		return new DVariable(name);
	}

}
