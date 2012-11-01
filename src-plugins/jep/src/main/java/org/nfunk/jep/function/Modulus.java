/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

public class Modulus extends PostfixMathCommand
{
	public Modulus()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		if ((param1 instanceof Number) && (param2 instanceof Number))
		{
			double divisor = ((Number)param2).doubleValue();
			double dividend = ((Number)param1).doubleValue();
		
			double result = dividend % divisor;
	
			inStack.push(new Double(result));
		}
		else
		{
			throw new ParseException("Invalid parameter type");
		}
		return;
	}
}
