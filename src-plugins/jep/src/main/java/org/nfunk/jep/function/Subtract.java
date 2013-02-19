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

public class Subtract extends PostfixMathCommand
{
	public Subtract()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		inStack.push(sub(param1, param2));

		return;
	}
	
	public Object sub(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Complex)
		{
			if (param2 instanceof Complex)
			{
				return sub((Complex)param1, (Complex)param2);
			}
			else if( param2 instanceof Number)
			{
				return sub((Complex)param1, (Number)param2);
			}
		}
		else if (param1 instanceof Number)
		{
			if (param2 instanceof Complex)
			{
				return sub((Number)param1, (Complex)param2);
			}
			else if (param2 instanceof Number)
			{
				return sub((Number)param1, (Number)param2);
			}
		} 
		throw new ParseException("Invalid parameter type");
	}
	

	public Double sub(Number d1, Number d2)
	{
		return new Double(d1.doubleValue() - d2.doubleValue());
	}
	
	public Complex sub(Complex c1, Complex c2)
	{
		return new Complex(c1.re() - c2.re(), c1.im() - c2.im());
	}
	
	public Complex sub(Complex c, Number d)
	{
		return new Complex(c.re() - d.doubleValue(), c.im());
	}

	public Complex sub(Number d, Complex c)
	{
		return new Complex(d.doubleValue() - c.re(), -c.im());
	}
}
