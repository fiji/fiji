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
 * Implements the arcTanH function.
 * 
 * @author Nathan Funk
 * @since 2.3.0 beta 2 - Now returns Double result rather than Complex for -1<x<1 
 */
public class ArcTanH extends PostfixMathCommand
{
	public ArcTanH()
	{
		numberOfParameters = 1;
	}

	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(atanh(param));//push the result on the inStack
		return;
	}

	public Object atanh(Object param)
		throws ParseException
	{
		if (param instanceof Complex)
		{
			return ((Complex)param).atanh();
		}
		else if (param instanceof Number)
		{
			double val = ((Number)param).doubleValue();
			if(val > -1.0 && val < 1) {
				double res = Math.log((1+val)/(1-val))/2;
				return new Double(res);
			}
			else
			{
				Complex temp = new Complex(val,0.0);
				return temp.atanh();
			}
		}

		throw new ParseException("Invalid parameter type");
	}

}
