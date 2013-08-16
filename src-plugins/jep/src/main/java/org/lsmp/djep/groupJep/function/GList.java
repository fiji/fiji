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
 * List function for use with arbitary groups.
 * Converts elements on stack and returns a list
 * actual behaviour defined by the Group.
 * 
 * @author Rich Morris
 * Created on 05-Mar-2004
 */
public class GList extends PostfixMathCommand {
	private GroupI group;
	/**
	 * 
	 */
	private GList() {	}
	public GList(GroupI group)
	{
		numberOfParameters = -1;
		this.group = group;
	}

	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		if(!(group instanceof HasListI))
			throw new ParseException("List not defined for this group");
		Number res[] = new Number[curNumberOfParameters]; 
		// repeat summation for each one of the current parameters
		for(int i=curNumberOfParameters-1;i>=0;--i) {
			res[i] = (Number) stack.pop();
		}
		stack.push(((HasListI) group).list(res));
		return;
	}
}
