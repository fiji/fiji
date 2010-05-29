/*****************************************************************************

  JEP 2.4.1, Extensions 1.1.1
       April 30 2007
       (c) Copyright 2007, Nathan Funk and Richard Morris
       See LICENSE-*.txt for license information.

 *****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

/**
 * This class serves mainly as an example of a function that accepts any number
 * of parameters. Note that the numberOfParameters is initialized to -1.
 */
public class Sum extends PostfixMathCommand {
	private Add addFun = new Add();

	/**
	 * Constructor.
	 */
	public Sum() {
		// Use a variable number of arguments
		numberOfParameters = -1;
	}

	/**
	 * Calculates the result of summing up all parameters, which are assumed to
	 * be of the Double type.
	 */
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack

		if (curNumberOfParameters < 1) throw new ParseException("No arguments for Sum");

		// initialize the result to the first argument
		Object sum = stack.pop();
		Object param;
		int i = 1;
        
		// repeat summation for each one of the current parameters
		while (i < curNumberOfParameters) {
			// get the parameter from the stack
			param = stack.pop();
			
			// add it to the sum (order is important for String arguments)
			sum = addFun.add(param, sum);
			
			i++;
		}

		// push the result on the inStack
		stack.push(sum);
	}
}
