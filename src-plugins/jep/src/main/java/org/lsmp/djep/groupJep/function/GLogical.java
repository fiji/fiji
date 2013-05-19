/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.lsmp.djep.groupJep.function;
import org.nfunk.jep.function.*;

import java.util.*;
import org.nfunk.jep.*;

/**
 * Implements logical operators for a group.
 * 
 * @author Rich Morris
 * Created on 13-Dec-2004
 */
public class GLogical extends PostfixMathCommand
{
	int id;
	
	/**
	 * Constructs
	 * @param id should be Logical.AND or Logical.OR
	 * @see org.nfunk.jep.function.Logical
	 **/
	public GLogical(int id)
	{
		this.id = id;
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();

		if ((param1 instanceof Boolean) && (param2 instanceof Boolean))
		{
			boolean a = ((Boolean)param1).booleanValue();
			boolean b = ((Boolean)param2).booleanValue();
			boolean flag=false;
			
			switch (id)
			{
				case Logical.AND:
					flag = a && b;
					break;
				case Logical.OR:
					flag = a || b;
					break;
				default:
					throw new ParseException("Illegal logical operator");
			}
			inStack.push(flag ? Boolean.TRUE : Boolean.FALSE);//push the result on the inStack
		}
		else
		{
			throw new ParseException("Invalid parameter type");
		}
		return;
	}
}
