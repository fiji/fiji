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
 * Modulus operator for group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class GMod extends PostfixMathCommand {
	private HasModI group=null;
	/**
	 * 
	 */
	private GMod() {	}
	public GMod(GroupI group)
	{
		numberOfParameters = 2;
		if(group instanceof HasModI)
			this.group = (HasModI) group;
	}

	/**
	 * Calculates the result of applying the "+" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		Object sum = stack.pop();
		Object param;
		param = stack.pop();
		sum = mod(param, sum);
		stack.push(sum);
		return;
	}

	public Object mod(Object param1, Object param2) throws ParseException {
		if(group==null) throw new ParseException("Modulus not implemented for this group.");
		if (param1 instanceof Number) {
			if (param2 instanceof Number) {
				return group.mod((Number)param1, (Number)param2);
			}
		}
		
		throw new ParseException("Invalid parameter type");
	}
}
