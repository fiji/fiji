/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

/** The list function.
 * Returns a Vector comprising all the children.
 * 
 * @author Rich Morris
 * Created on 29-Feb-2004
 */
public class Range extends PostfixMathCommand
{
	public Range()
	{
		numberOfParameters = -1;
	}

	/**
	 * Generates a range [low,low+inc,...,low+inc*(steps-1)]
	 * @param low
	 * @param inc
	 * @param steps
	 * @return a Vector
	 */
	public Object genRange(double low,double inc,int steps)
	{
		Vector res = new Vector(steps);
		res.setSize(steps);
		for(int i=0;i<steps;++i)
			res.set(i,new Double(low+inc*i));
		return res;
	}
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack); // check the stack
		if(curNumberOfParameters <1)
			throw new ParseException("Empty list");
		
		Object res;
		if(curNumberOfParameters == 2)
		{
			Object lastObj = inStack.pop();
			Object firstObj  = inStack.pop();
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double diff = last-first;
			int steps = 1+(int) diff;
			res=genRange(first,1.0,steps);
		}
		else if(curNumberOfParameters == 3)
		{
			Object incObj = inStack.pop();
			Object lastObj = inStack.pop();
			Object firstObj  = inStack.pop();
			double inc = ((Number) incObj).doubleValue();
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double diff = (last-first)/inc;
			int steps = 1+(int) diff;
			res=genRange(first,inc,steps);
		}
		else if(curNumberOfParameters == 4)
		{
			Object stepsObj = inStack.pop();
			Object lastObj = inStack.pop();
			Object firstObj  = inStack.pop();
			int steps = ((Number) stepsObj).intValue();
			double last = ((Number) lastObj).doubleValue();
			double first = ((Number) firstObj).doubleValue();
			double inc = (last-first)/(steps-1);
			res=genRange(first,inc,steps);
		}
		else throw new ParseException("Range:only a maximum of four arguments can be specified");
		inStack.push(res);
		return;
	}
}
