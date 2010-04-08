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
 * Multiplication operator for a group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class GMultiply extends PostfixMathCommand {
	private RingI group = null;
	/**
	 * 
	 */
	private GMultiply() {	}
	public GMultiply(GroupI group)
	{
		numberOfParameters = -1;
		if(group instanceof RingI)
		this.group = (RingI) group;
	}

	/**
	 * Calculates the result of applying the "*" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public void run(Stack stack) throws ParseException {
		if(group==null) throw new ParseException("Multiply not implemented for this group.");
		checkStack(stack);// check the stack
		
		Object sum = stack.pop();
		Object param;
		int i = 1;
        
		// repeat summation for each one of the current parameters
		while (i < curNumberOfParameters) {
			// get the parameter from the stack
			param = stack.pop();
			// add it to the sum (order is important for String arguments)
			sum = mul(param, sum);
			i++;
		}
		stack.push(sum);
		return;
	}

	public Object mul(Object param1, Object param2) throws ParseException {
		if (param1 instanceof Number) {
			if (param2 instanceof Number) {
				return group.mul((Number)param1, (Number)param2);
			}
		}
		
		throw new ParseException("Invalid parameter type");
	}
}
