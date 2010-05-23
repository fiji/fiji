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
 * All function classes must implement this interface to ensure that the run()
 * method is implemented.
 */
public interface PostfixMathCommandI
{
	/**
	 * Run the function on the stack. Pops the arguments from the stack, and
	 * pushes the result on the top of the stack.
	 */
	public void run(Stack aStack) throws ParseException;
	
	/**
	 * Returns the number of required parameters, or -1 if any number of
	 * parameters is allowed.
	 */
	public int getNumberOfParameters();

	/**
	 * Sets the number of current number of parameters used in the next call
	 * of run(). This method is only called when the reqNumberOfParameters is
	 * -1.
	 */
	public void setCurNumberOfParameters(int n);
	
	/**
	 * Checks the number of parameters of the function.
	 * This method is called during the parsing of the equation to check syntax.
	 * Functions which set numberOfParameter=-1 should overwrite this method.
	 * @param n number of parameters function will be called with.
	 * @return False if an illegal number of parameters is supplied, true otherwise.
	 */
	public boolean checkNumberOfParameters(int n);

}
