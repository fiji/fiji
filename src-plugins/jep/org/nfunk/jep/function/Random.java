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
/**
* Encapsulates the Math.random() function.
*/
public class Random extends PostfixMathCommand
{
	public Random()
	{
		numberOfParameters = 0;

	}

	public void run(Stack inStack)
		throws ParseException
	{
		checkStack(inStack);// check the stack
		inStack.push(new Double(Math.random()));
		return;
	}
}
