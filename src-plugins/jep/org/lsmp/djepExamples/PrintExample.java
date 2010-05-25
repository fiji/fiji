/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;
/**
 * @author Rich Morris
 * Created on 26-Feb-2004
 */
public class PrintExample {

	public static void main(String[] args) {
		XJep j = new XJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);

		try
		{
			// parse expression
			Node node = j.parse("a*b+c*(d+sin(x))");
			// print it
			j.println(node);
			// convert to string
			String str = j.toString(node);
			System.out.println("String is '"+str+"'");
			j.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,true);
			j.println(node);
			
			j.getPrintVisitor().setMode(PrintVisitor.FULL_BRACKET,false);
			Node node2=j.parse("1*x^1+0");
			j.println(node2);
			Node simp=j.simplify(node2);
			j.println(simp);

		}
		catch(ParseException e) { System.out.println("Parse error"); }
	}
}
