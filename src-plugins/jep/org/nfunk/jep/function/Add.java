/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;

/**
 * Addition function. Supports any number of parameters although typically
 * only 2 parameters are used. 
 * @author nathan
 */
public class Add extends PostfixMathCommand
{
	
	public Add()
	{
		numberOfParameters = -1;
	}
	
	/**
	 * Calculates the result of applying the "+" operator to the arguments from
	 * the stack and pushes it back on the stack.
	 */
	public void run(Stack stack) throws ParseException {
		checkStack(stack);// check the stack
		
		Object sum = stack.pop();
		Object param;
		int i = 1;
        
		// repeat summation for each one of the current parameters
		while (i < curNumberOfParameters) {
			// get the parameter from the stack
			param = stack.pop();
			
			// add it to the sum (order is important for String arguments)
			sum = add(param, sum);
			
			i++;
		}
        		
		stack.push(sum);
		
		return;
	}

	/**
	 * Adds two numbers together. The parameters can be of type Number,
	 * Complex, or String. If a certain combination of types is not supported,
	 * a ParseException is thrown.
	 * 
	 * @param param1 The first parameter to be added.
	 * @param param2 The second parameter to be added.
	 * @return The sum of param1 and param2, or concatenation of the two if
	 *         they are Strings.
	 * @throws ParseException
	 */
	public Object add(Object param1, Object param2) throws ParseException {
		if (param1 instanceof Complex) 
		{
			if (param2 instanceof Complex) 
				return add((Complex)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return add((Complex)param1, (Number)param2);
		}
		else if (param1 instanceof Number) 
		{
			if (param2 instanceof Complex) 
				return add((Complex)param2, (Number)param1);
			else if (param2 instanceof Number) 
				return add((Number)param1, (Number)param2);
		}
		else if ((param1 instanceof String) && (param2 instanceof String)) {
			return (String)param1 + (String)param2;
		}
		
		throw new ParseException("Invalid parameter type");
	}
	
	public Double add(Number d1, Number d2) {
		return new Double(d1.doubleValue() + d2.doubleValue());
	}
	
	public Complex add(Complex c1, Complex c2) {
		return new Complex(c1.re() + c2.re(), c1.im() + c2.im());
	}
	
	public Complex add(Complex c, Number d) {
		return new Complex(c.re() + d.doubleValue(), c.im());
	}	
}
