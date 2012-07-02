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

public class Multiply extends PostfixMathCommand
{
	
	public Multiply() {
		numberOfParameters = -1;
	}
	
	public void run(Stack stack) throws ParseException 
	{
		checkStack(stack); // check the stack

		Object product = stack.pop();
		Object param;
        int i = 1;
        
        // repeat summation for each one of the current parameters
        while (i < curNumberOfParameters) {
        	// get the parameter from the stack
            param = stack.pop();
            
            // multiply it with the product, order is important
            // if matricies are used
            product = mul(param,product);
                
            i++;
        }
        		
		stack.push(product);

		return;
	}
	
	public Object mul(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Complex)
		{
			if (param2 instanceof Complex)
				return mul((Complex)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return mul((Complex)param1, (Number)param2);
			else if (param2 instanceof Vector)
				return mul((Vector)param2, (Complex)param1);
		}
		else if (param1 instanceof Number)
		{
			if (param2 instanceof Complex)
				return mul((Complex)param2, (Number)param1);
			else if (param2 instanceof Number)
				return mul((Number)param1, (Number)param2);
			else if (param2 instanceof Vector)
				return mul((Vector)param2, (Number)param1);
		}
		else if (param1 instanceof Vector)
		{
			if (param2 instanceof Complex)
				return mul((Vector)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return mul((Vector)param1, (Number)param2);
		}
		
		throw new ParseException("Invalid parameter type");
	}
	
	public Double mul(Number d1, Number d2)
	{
		return new Double(d1.doubleValue()*d2.doubleValue());	
	}	
	
	public Complex mul(Complex c1, Complex c2)
	{
		return c1.mul(c2);
	}
	
	public Complex mul(Complex c, Number d)
	{
		return c.mul(d.doubleValue());	
	}
	
	public Vector mul(Vector v, Number d)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(mul((Number)v.elementAt(i), d));
		
		return result;
	}
	
	public Vector mul(Vector v, Complex c)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(mul(c, (Number)v.elementAt(i)));
		
		return result;
	}	
}
