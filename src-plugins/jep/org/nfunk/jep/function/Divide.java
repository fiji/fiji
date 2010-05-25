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

public class Divide extends PostfixMathCommand
{
	public Divide()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		inStack.push(div(param1, param2)); //push the result on the inStack
		return;
	}
	
	public Object div(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Complex)
		{
			if (param2 instanceof Complex)
				return div((Complex)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return div((Complex)param1, (Number)param2);
			else if (param2 instanceof Vector)
				return div((Complex)param1, (Vector)param2);
		}
		else if (param1 instanceof Number)
		{
			if (param2 instanceof Complex)
				return div((Number)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return div((Number)param1, (Number)param2);
			else if (param2 instanceof Vector)
				return div((Number)param1, (Vector)param2);
		}
		else if (param1 instanceof Vector)
		{
			if (param2 instanceof Complex)
				return div((Vector)param1, (Complex)param2);
			else if (param2 instanceof Number)
				return div((Vector)param1, (Number)param2);
		}

		throw new ParseException("Invalid parameter type");
	}


	public Double div(Number d1, Number d2)
	{
		return new Double(d1.doubleValue() / d2.doubleValue());
	}
	
	public Complex div(Complex c1, Complex c2)
	{
		return c1.div(c2);
	}
	
	public Complex div(Number d, Complex c)
	{
		Complex c1 = new Complex(d.doubleValue(), 0);

		return c1.div(c);
	}

	public Complex div(Complex c, Number d)
	{
		return new Complex(c.re()/d.doubleValue(), c.im()/d.doubleValue());
	}
	
	public Vector div(Vector v, Number d)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(div((Number)v.elementAt(i), d));
		
		return result;
	}
	
	public Vector div(Number d, Vector v)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(div(d, (Number)v.elementAt(i)));
		
		return result;
	}
	
	public Vector div(Vector v, Complex c)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(div((Number)v.elementAt(i), c));
		
		return result;
	}
	
	public Vector div(Complex c, Vector v)
	{
		Vector result = new Vector();

		for (int i=0; i<v.size(); i++)
			result.addElement(div(c, (Number)v.elementAt(i)));
		
		return result;
	}	
}
