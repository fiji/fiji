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

/**
 * A PostfixMathCommandI which find the smallest integer above the number
 * ceil(pi) give 4
 * ceil(-i) give -3
 * 
 * @author Richard Morris
 * @see Math#ceil(double)
 */

public class Ceil extends PostfixMathCommand
{
	public Ceil()
	{
		numberOfParameters = 1;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(ceil(param));//push the result on the inStack
		return;
	}


	public Object ceil(Object param)
		throws ParseException
	{
		if (param instanceof Number)
		{
			return new Double(Math.ceil(((Number)param).doubleValue()));
		}

		throw new ParseException("Invalid parameter type");
	}

}
