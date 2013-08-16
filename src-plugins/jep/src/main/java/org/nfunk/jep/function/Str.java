/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/
package org.nfunk.jep.function;

import java.util.*;
import org.nfunk.jep.*;

/**
 * Converts an object into its string representation.
 * Calls the toString method of the object. 
 * 
 * @author Rich Morris
 * Created on 27-Mar-2004
 */
public class Str extends PostfixMathCommand
{
	public Str()
	{
		numberOfParameters = 1;
	}
	
	public void run(Stack inStack)
		throws ParseException 
	{
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(param.toString());//push the result on the inStack
		return;
	}
}
