/* @author rich
 * Created on 05-Mar-2004
 */
package org.lsmp.djep.groupJep.function;
import org.nfunk.jep.function.*;
import org.lsmp.djep.groupJep.*;
import java.util.*;
import org.nfunk.jep.*;
/**
 * Unitary division for a group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class GUMinus extends PostfixMathCommand {
	private GroupI group;
	/**
	 * 
	 */
	private GUMinus() {	}
	public GUMinus(GroupI group)
	{
		numberOfParameters = 1;
		this.group = group;
	}

	/**
	 * Calculates the result of applying the "+" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		Object sum = stack.pop();
		stack.push(uminus(sum));
		return;
	}

	public Object uminus(Object param1) throws ParseException {
		if (param1 instanceof Number) {
				return group.getInverse((Number)param1);
		}
		throw new ParseException("Invalid parameter type");
	}
}
