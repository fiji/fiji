/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import org.lsmp.djep.vectorJep.*;

/**
 * @author Rich Morris
 * Created on 21-Mar-2005
 */
public class VectorConsole extends Console
{
	private static final long serialVersionUID = -2335406063822614650L;

	public static void main(String[] args) 
	{
		Console c = new VectorConsole();
		c.run(args);
	}

	public String getPrompt()
	{
		return "VectorJep > ";
	}

	public void initialise()
	{
		j = new VectorJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowAssignment(true);
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
	}

	public void printHelp()
	{
		super.printHelp();
		println("Dot product: [1,2,3].[4,5,6]");
		println("Cross product: [1,2,3]^^[4,5,6]");
		println("Matrix Multiplication: [[1,2],[3,4]]*[[1,2],[3,4]]");
		println("setEleMult: sets element by element mode for multiplication");	
		println("setMatrixMult: sets matrix multiplication");	
	}

	public void printIntroText()
	{
		println("VectorJep: matrix and vector calculations in Jep");
		println("eg. [1,2,3].[4,5,6] [[1,2],[3,4]]*[1,0]");
		printStdHelp();
	}

	public boolean testSpecialCommands(String command)
	{
		if(command.equals("setEleMult"))
		{
			((VectorJep)j).setElementMultiply(true);
			return false;
		}
		if(command.equals("setMatrixMult"))
		{
			((VectorJep)j).setElementMultiply(true);
			return false;
		}
		return true;
	}

}
