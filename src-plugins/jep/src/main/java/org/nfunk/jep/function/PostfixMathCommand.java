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
 * Function classes extend this class. It is an implementation of the 
 * PostfixMathCommandI interface.
 * <p>
 * It includes a numberOfParameters member, that is checked when parsing the
 * expression. This member should be initialized to an appropriate value for
 * all classes extending this class. If an arbitrary number of parameters
 * should be allowed, initialize this member to -1.
 */
public class PostfixMathCommand implements PostfixMathCommandI
{
	/**
	 * Number of parameters a the function requires. Initialize this value to
	 * -1 if any number of parameters should be allowed.
	 */
	protected int numberOfParameters;
	
	/**
	 * Number of parameters to be used for the next run() invocation. Applies
	 * only if the required umber of parameters is variable
	 * (numberOfParameters = -1).
	 */
	protected int curNumberOfParameters;
	
	/**
	 * Creates a new PostfixMathCommand class.
	 */
	public PostfixMathCommand() {
		numberOfParameters = 0;
		curNumberOfParameters = 0;
	}

	/**
	 * Check whether the stack is not null, throw a ParseException if it is.
	 */
	protected void checkStack(Stack inStack) throws ParseException {
		/* Check if stack is null */
		if (null == inStack) {
			throw new ParseException("Stack argument null");
		}
	}

	/**
	 * Return the required number of parameters.
	 */
	public int getNumberOfParameters() {
		return numberOfParameters;
	}
	
	/**
	 * Sets the number of current number of parameters used in the next call
	 * of run(). This method is only called when the reqNumberOfParameters is
	 * -1.
	 */
	public void setCurNumberOfParameters(int n) {
		curNumberOfParameters = n;
	}
	
	/**
	 * Checks the number of parameters of the function.
 	 * Functions which set numberOfParameter=-1 should overload this method

	 * @param n number of parameters function will be called with.
	 * @return False if an illegal number of parameters is supplied, true otherwise.
	 */
	public boolean checkNumberOfParameters(int n) {
		if(numberOfParameters == -1) return true;
		return (numberOfParameters==n);
	}
	/**
	 * Throws an exception because this method should never be called under
	 * normal circumstances. Each function should use it's own run() method
	 * for evaluating the function. This includes popping off the parameters
	 * from the stack, and pushing the result back on the stack.
	 */
	public void run(Stack s) throws ParseException {
		throw new ParseException("run() method of PostfixMathCommand called");
	}
}
