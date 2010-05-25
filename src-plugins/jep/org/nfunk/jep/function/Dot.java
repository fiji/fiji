/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

public class Dot extends PostfixMathCommand
{
	static Add add = new Add();
	static Multiply mul = new Multiply();	
	public Dot()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		inStack.push(dot(param1, param2));

		return;
	}
	
	public Object dot(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Vector && param2 instanceof Vector)
		{
			return dot((Vector) param1,(Vector) param2);
		}
		throw new ParseException("Dot: Invalid parameter type, both arguments must be vectors");
	}
	

	public Object dot(Vector v1,Vector v2) throws ParseException
	{
		if(v1.size()!=v2.size())
			throw new ParseException("Dot: both sides of dot must be same length");
		int len = v1.size();
		if(len<1)
			throw new ParseException("Dot: empty vectors parsed");
		
		Object res = mul.mul(v1.elementAt(0),v2.elementAt(0));
		for(int i=1;i<len;++i)
		{
			res = add.add(res,
				mul.mul(v1.elementAt(i),v2.elementAt(i)));
		}
		return res;
	}
}
