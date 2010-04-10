/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.lsmp.djep.matrixJep.*;
/**
 * @author Rich Morris
 * Created on 21-Mar-2005
 */
public class MatrixConsole extends DJepConsole
{
	private static final long serialVersionUID = -4768856862892634425L;

	public static void main(String[] args)
	{
		Console c = new MatrixConsole();
		c.run(args);
	}
	
	public String getPrompt()
	{
		return "MatrixJep > ";
	}

	public void initialise()
	{
		j = new MatrixJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);
		((MatrixJep) j).addStandardDiffRules();
	}

	public void printHelp()
	{
		super.printHelp();
		println("Dot product: [1,2,3].[4,5,6]");
		println("Cross product: [1,2,3]^[4,5,6]");
		println("Matrix Multiplication: [[1,2],[3,4]]*[[1,2],[3,4]]");
	}

	public void printIntroText()
	{
		println("MatrixJep: advanced vector and matrix handling");
		super.printStdHelp();
	}

	public void processEquation(Node node) throws ParseException
	{
		MatrixJep mj = (MatrixJep) j;
		
		if(verbose) {
			print("Parsed:\t\t"); 
			println(mj.toString(node));
		}
		Node processed = mj.preprocess(node);
		if(verbose) {
			print("Processed:\t"); 
			println(mj.toString(processed));
		}
					
		Node simp = mj.simplify(processed);
		if(verbose) 
			print("Simplified:\t"); 
		println(mj.toString(simp));


		Object val = mj.evaluate(simp);
		String s = mj.getPrintVisitor().formatValue(val);
		println("Value:\t\t"+s);
	}
}
