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
  * Not function for use with arbitary groups.
  * Expects Boolean arguments.
  * 
  * @author Rich Morris
  * Created on 13-Dec-2004
  */
public class GNot extends PostfixMathCommand
{
	public GNot()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		
		Object param = inStack.pop();

		if (param instanceof Boolean)
		{
			boolean a = ((Boolean)param).booleanValue();
			inStack.push(a ? Boolean.FALSE : Boolean.TRUE);//push the result on the inStack
		}
		else
		{
			throw new ParseException("Invalid parameter type");
		}
		return;
	}
}
