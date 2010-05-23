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

public class Sine extends PostfixMathCommand
{
	public Sine()
	{
		numberOfParameters = 1;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(sin(param));//push the result on the inStack
		return;
	}

	public Object sin(Object param)
		throws ParseException
	{
		if (param instanceof Complex) {
			return ((Complex)param).sin();
		}
		else if (param instanceof Number) {
			return new Double(Math.sin(((Number)param).doubleValue()));
		}
		
		throw new ParseException("Invalid parameter type");
	}
}
