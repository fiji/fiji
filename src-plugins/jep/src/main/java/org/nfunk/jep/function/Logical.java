/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

public class Logical extends PostfixMathCommand
{
	int id;
	public static final int AND = 0;
	public static final int OR = 1;
	public Logical(int id_in)
	{
		id = id_in;
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		double x,y;
		if ((param1 instanceof Number))
			x = ((Number)param1).doubleValue();
		else if((param1 instanceof Boolean))
			x= ((Boolean) param1).booleanValue()?1.0:0.0;
		else throw new ParseException("Logical: require a number or Boolean value on lhs, have "+
				param1.getClass().getName());
		if ((param2 instanceof Number))
			y = ((Number)param2).doubleValue();
		else if((param2 instanceof Boolean))
			y= ((Boolean) param2).booleanValue()?1.0:0.0;
		else throw new ParseException("Logical: require a number or Boolean value on lhs, have "+
				param1.getClass().getName());
		
		int r;
			
		switch (id)
		{
		case 0:
			// AND
			r = ((x!=0d) && (y!=0d)) ? 1 : 0;
			break;
		case 1:
			// OR
			r = ((x!=0d) || (y!=0d)) ? 1 : 0;
			break;
		default:
			r = 0;
		}
		inStack.push(new Double(r)); // push the result on the inStack
		return;
	}
}
