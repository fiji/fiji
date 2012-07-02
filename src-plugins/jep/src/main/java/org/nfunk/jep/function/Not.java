/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

public class Not extends PostfixMathCommand
{
	public Not()
	{
		numberOfParameters = 1;
	
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		if (param instanceof Number)
		{
			int r = (((Number)param).doubleValue() == 0) ? 1 : 0;
			inStack.push(new Double(r));//push the result on the inStack
		}
		else if(param instanceof Boolean)
		{
			int r = (((Boolean)param).booleanValue()) ? 0 : 1;
			inStack.push(new Double(r));//push the result on the inStack
		}
		else
			throw new ParseException("Invalid parameter type");
		return;
	}

}
