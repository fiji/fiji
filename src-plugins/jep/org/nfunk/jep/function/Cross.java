/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

public class Cross extends PostfixMathCommand
{
	static Subtract sub = new Subtract();
	static Multiply mul = new Multiply();	
	public Cross()
	{
		numberOfParameters = 2;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		
		Object param2 = inStack.pop();
		Object param1 = inStack.pop();
		
		inStack.push(cross(param1, param2));

		return;
	}
	
	public Object cross(Object param1, Object param2)
		throws ParseException
	{
		if (param1 instanceof Vector && param2 instanceof Vector)
		{
			return cross((Vector) param1,(Vector) param2);
		}
		throw new ParseException("Cross: Invalid parameter type, both arguments must be vectors");
	}

	public Object cross(Vector lhs,Vector rhs) throws ParseException
	{
		int len = lhs.size();
		if((len!=2 && len!=3) || len !=rhs.size())
			throw new ParseException("Cross: both sides must be of length 3");
		if(len==3)
		{
			Vector res = new Vector(3);
			res.setSize(3);
			res.setElementAt(sub.sub(
					mul.mul(lhs.elementAt(1),rhs.elementAt(2)),
					mul.mul(lhs.elementAt(2),rhs.elementAt(1))),0);
			res.setElementAt(sub.sub(
					mul.mul(lhs.elementAt(2),rhs.elementAt(0)),
					mul.mul(lhs.elementAt(0),rhs.elementAt(2))),1);
			res.setElementAt(sub.sub(
					mul.mul(lhs.elementAt(0),rhs.elementAt(1)),
					mul.mul(lhs.elementAt(1),rhs.elementAt(0))),2);
			return res;
		}
		else
		{
			return sub.sub(
				mul.mul(lhs.elementAt(0),rhs.elementAt(1)),
				mul.mul(lhs.elementAt(1),rhs.elementAt(0)));
			
		}
	}
}
