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
 * Converts a pair of real numbers to a complex number Complex(x,y)=x+i y.
 * 
 * @author Rich Morris
 * Created on 24-Mar-2004
 */
public class Polar extends PostfixMathCommand
{
	public Polar()
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
			inStack.push(Complex.polarValueOf((Number) param1,(Number) param2));
		}
		else
		{
			throw new ParseException("Complex: Invalid parameter types "+param1.getClass().getName()+" "+param1.getClass().getName());
		}
		return;
	}
}
