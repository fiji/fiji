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

public class Imaginary extends PostfixMathCommand
{
	public Imaginary() {
		numberOfParameters = 1;
	}
	
	public void run(Stack inStack) throws ParseException {
		
		checkStack(inStack);// check the stack
		Object param = inStack.pop();
		inStack.push(im(param));//push the result on the inStack
		return;
	}
	
	public Number im(Object param) throws ParseException {
		
		if (param instanceof Complex)
			return new Double(((Complex)param).im());
		else if (param instanceof Number)
			return new Double(0);
		

		throw new ParseException("Invalid parameter type");
	}

}
