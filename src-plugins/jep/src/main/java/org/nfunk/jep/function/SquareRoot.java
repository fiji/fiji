/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
package org.nfunk.jep.function;

import java.lang.Math;
import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;

public class SquareRoot extends PostfixMathCommand
{
	public SquareRoot() {
		numberOfParameters = 1;
	}
	
	/**
	 * Applies the function to the parameters on the stack.
	 */
	public void run(Stack inStack) throws ParseException {
			
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(sqrt(param));//push the result on the inStack
		return;
	}

	/**
	 * Calculates the square root of the parameter. The parameter must
	 * either be of type Double or Complex.
	 *
	 * @return The square root of the parameter.
	 */
	public Object sqrt(Object param) throws ParseException 
	{
		if (param instanceof Complex)
			return ((Complex)param).sqrt();
		if (param instanceof Number) {
			double value = ((Number)param).doubleValue();
			
			// a value less than 0 will produce a complex result
			if (value < 0.0) {
				return (new Complex(value).sqrt());
			} else {
				return new Double(Math.sqrt(value));
			}
		}

		throw new ParseException("Invalid parameter type");
	}
}
