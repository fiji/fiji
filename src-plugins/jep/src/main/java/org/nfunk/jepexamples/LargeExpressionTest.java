/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jepexamples;

import java.util.*;

import org.nfunk.jep.JEP;

/**
 * This example tests how the evaluation time is influenced by the size of the
 * expression and symbol table.
 */
public class LargeExpressionTest {
	public static void main(String args[]) {
		int nEvals = 500;
		int nVars = 1000;
		Date start, finish;
		String str = "";
		
		JEP myParser = new JEP();

		// Test small symbol table
		for (int i=0; i<10; i++) {
			myParser.addVariable("v"+i, 0);
			str += "+" + "v" + i;
		}
		myParser.parseExpression(str);
		System.out.print("Evaluating with small symbol table... ");
		start = new Date();
		for (int i=0; i<nEvals; i++) {
			myParser.getValue();
		}
		finish = new Date();
		System.out.println("done.");
		System.out.println("Time: " +
							(finish.getTime() - start.getTime()));
		
		// Test large symbol table
		str = "";
		for (int i=0; i<nVars; i++) {
			myParser.addVariable("v" + i, 0);
			str += "+" + "v" + i;
		}
		myParser.parseExpression(str);
		System.out.print("Evaluating with large symbol table... ");
		start = new Date();
		for (int i=0; i<nEvals; i++) {
			myParser.getValue();
		}
		finish = new Date();
		System.out.println("done.");
		System.out.println("Time: " +
							(finish.getTime() - start.getTime()));
	}	
}
