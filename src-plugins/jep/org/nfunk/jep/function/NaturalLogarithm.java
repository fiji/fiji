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

/**
 * Natural logarithm.
 *
 * RJM Change: fixed so ln(positive Double) is Double.
 */
public class NaturalLogarithm extends PostfixMathCommand
{
	public NaturalLogarithm()
	{
		numberOfParameters = 1;

	}

	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(ln(param));//push the result on the inStack
		return;
	}

	public Object ln(Object param)
		throws ParseException
	{
		if (param instanceof Complex)
		{
			return ((Complex)param).log();
		}
		else if (param instanceof Number)
		{
			// Now returns Complex if param is <0
			double num = ((Number) param).doubleValue();
			if( num >= 0)
				return new Double(Math.log(num));
			else if(num != num)
				return new Double(Double.NaN);
			else
			{	
				Complex temp = new Complex(num);
				return temp.log();
			}
		}

		throw new ParseException("Invalid parameter type");
	}
}
