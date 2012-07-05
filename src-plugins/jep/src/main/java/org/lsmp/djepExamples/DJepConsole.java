/* @author rich
 * Created on 21-Mar-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepExamples;

import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;
import org.lsmp.djep.djep.*;
import org.lsmp.djep.xjep.PrintVisitor;

/**
 * @author Rich Morris
 * Created on 21-Mar-2005
 */
public class DJepConsole extends XJepConsole
{
	private static final long serialVersionUID = -5801701990800128777L;
	public void initialise()
	{
		j = new DJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);
		((DJep) j).addStandardDiffRules();
	}

	public void printHelp()
	{
		super.printHelp();
		println("'diff(x^2,x)' to differentiate x^2 with respect to x");
		println("'verbose on', 'verbose off' switch verbose mode on or off");
	}

	public void printIntroText()
	{
		println("DJep: differentiation in JEP. e.g. diff(x^2,x)");
		printStdHelp();
	}

	public String getPrompt()
	{
		return "DJep > ";
	}

	public void processEquation(Node node) throws ParseException
	{
		DJep dj = (DJep) this.j;
		if(verbose) {
			print("Parsed:\t\t"); 
			println(dj.toString(node));
		}
		Node processed = dj.preprocess(node);
		if(verbose) {
			print("Processed:\t"); 
			println(dj.toString(processed));
		}
					
		Node simp = dj.simplify(processed);
		if(verbose) 
			print("Simplified:\t"); 
		println(dj.toString(simp));
			
		if(verbose) {
			print("Full Brackets, no variable expansion:\n\t\t");
			dj.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,true);
			dj.getPrintVisitor().setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,false);
			println(dj.toString(simp));
			dj.getPrintVisitor().setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,true);
			dj.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,false);
		}

		Object val = dj.evaluate(simp);
		String s = dj.getPrintVisitor().formatValue(val);
		println("Value:\t\t"+s);
	}

	
	/** Creates a new Console object and calls run() */
	public static void main(String args[]) {
		Console c = new DJepConsole();
		c.run(args);
	}

}
