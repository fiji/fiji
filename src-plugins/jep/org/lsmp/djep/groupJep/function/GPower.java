/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.function;
import org.nfunk.jep.function.*;
import org.lsmp.djep.groupJep.*;
import org.lsmp.djep.groupJep.interfaces.*;

import java.util.*;
import org.nfunk.jep.*;
/**
 * Power operator for a group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class GPower extends PostfixMathCommand {
	/** null if power not implemented */
	private HasPowerI group=null;
	/**
	 * 
	 */
	private GPower() {	}
	public GPower(GroupI group)
	{
		numberOfParameters = 2;
		if(group instanceof HasPowerI)
			this.group = (HasPowerI) group;
	}

	/**
	 * Calculates the result of applying the "+" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		Object exponant = stack.pop();
		Object param = stack.pop();
		Object res = pow(param, exponant);
		stack.push(res);
		return;
	}

	public Object pow(Object param1, Object param2) throws ParseException {
		if(group==null) throw new ParseException("Power not implemented for this group.");
		if (param1 instanceof Number) {
			if (param2 instanceof Number) {
				return group.pow((Number)param1, (Number)param2);
			}
		}
		
		throw new ParseException("Invalid parameter type");
	}
}
