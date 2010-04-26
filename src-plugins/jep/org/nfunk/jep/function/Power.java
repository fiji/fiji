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

public class Power extends PostfixMathCommand
{
	public Power()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		inStack.push(power(param1, param2));
	}
	
	public Object power(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Complex) {
			if (param2 instanceof Complex)
				return power((Complex)param1, (Complex)param2);
			else if (param2 instanceof Number) 
				return power((Complex)param1, (Number)param2);
		}
		else if (param1 instanceof Number) {
			if (param2 instanceof Complex)
				return power((Number)param1, (Complex)param2);
			else if (param2 instanceof Number) 
				return power((Number)param1, (Number)param2);
		}

		throw new ParseException("Invalid parameter type");
	}
	

	public Object power(Number d1, Number d2)
	{
		if (d1.doubleValue()<0 && d2.doubleValue() != d2.intValue())
		{
			Complex c = new Complex(d1.doubleValue(), 0.0);
			return c.power(d2.doubleValue());
		}
		else
			return new Double(Math.pow(d1.doubleValue(),d2.doubleValue()));
	}
	
	public Object power(Complex c1, Complex c2)
	{
		Complex temp = c1.power(c2);

		if (temp.im()==0)
			return new Double(temp.re());
		else
			return temp;
	}
	
	public Object power(Complex c, Number d)
	{
		Complex temp = c.power(d.doubleValue());
		
		if (temp.im()==0)
			return new Double(temp.re());
		else
			return temp;
	}

	public Object power(Number d, Complex c)
	{
		Complex base = new Complex(d.doubleValue(), 0.0);
		Complex temp = base.power(c);
		
		if (temp.im()==0)
			return new Double(temp.re());
		else
			return temp;
	}
	
}
