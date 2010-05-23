/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

/**
 * Console - JEP Example Applet
 * Copyright (c) 2000 Nathan Funk
 *
 * @author Nathan Funk 
 */

package org.nfunk.jepexamples;

import java.io.*;
import org.nfunk.jep.JEP;
//import org.nfunk.sovler.*;

/**
* This class implements a simple command line utility for evaluating
* mathematical expressions.
*
*   Usage: java org.nfunk.jepexamples.Console [expression]
*
* If an argument is passed, it is interpreted as an expression
* and evaluated. Otherwise, a prompt is printed, and the user can enter
* expressions to be evaluated. To exit from the command prompt a 'q' must
* be entered.
*/
class Console {
	
	/** The prompt string */
	private String prompt;
	
	/** The input reader */
	private BufferedReader br;
	
	/** Constructor */
	public Console() {
		prompt = "JEP > ";
		br = new BufferedReader(new InputStreamReader(System.in));

	}

	/** Creates a new Console object and calls run() */
	public static void main(String args[]) throws IOException {
		Console c = new Console();
		c.run(args);
	}
	
	/** The input loop */
	public void run(String args[]) throws IOException {
		String command="";
		JEP j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		//j.setTraverse(true);

		if (args.length>0) {
			// evaluate the expression passed as arguments
			String temp = args[0];
			for (int i=1; i<args.length; i++) temp += " " + args[i];
			j.parseExpression(temp);
			if (j.hasError())
				System.out.println(j.getErrorInfo());
			else
				System.out.println(j.getValueAsObject());
		} else {
			// no arguments - interactive mode
				
			System.out.println("JEP - Enter q to quit");	
			System.out.print(prompt);

			while ((command = getCommand()) != null) {
				j.parseExpression(command);
				
				if (j.hasError()) {
					System.out.println(j.getErrorInfo());
				} else {
					// expression is OK, get the value
					Object value = j.getValueAsObject();
					
					// did error occur during evaluation?
					if (j.hasError()) {
						System.out.println(j.getErrorInfo());
					} else {
						System.out.println(value);
					}

/*
					System.out.println(
						(LinearVisitor.isLinear(j.getTopNode())) ?
						"Linear" : "Not Linear");
					System.out.println(
						(ConstantVisitor.isConstant(j.getTopNode())) ?
						"Constant" : "Not Constant");
*/
				}
					
				System.out.print(prompt);
			}
		}
		
	}
	
	/**
	 * Get a command from the input.
	 * @return null if an error occures, or if the user enters a terminating
	 *  command
	 */
	private String getCommand() throws IOException {
		String s;
		
		if (br == null)
			return null;

		if ( (s = br.readLine()) == null)
			return null;

		if (s.equals("q")
			|| s.equals("quit")
			|| s.equals("exit"))
			return null;
		
		return s;
	}
}
