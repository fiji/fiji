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
 * Implements the arcSinH function.
 * 
 * @author Nathan Funk
 * @since 2.3.0 beta 2 - Now returns Double result rather than Complex for Double arguments. 
 */
public class ArcSineH extends PostfixMathCommand
{
	public ArcSineH()
	{
		numberOfParameters = 1;
	}

	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(asinh(param));//push the result on the inStack
		return;
	}

	public Object asinh(Object param)
		throws ParseException
	{
		if (param instanceof Complex)
		{
			return ((Complex)param).asinh();
		}
		else if (param instanceof Number)
		{
			double val = ((Number)param).doubleValue();
			double res = Math.log(val+Math.sqrt(val*val+1));
			return new Double(res);
//			Complex temp = new Complex(((Number)param).doubleValue(),0.0);
//			return temp.asinh();
		}
		throw new ParseException("Invalid parameter type");
	}
}
