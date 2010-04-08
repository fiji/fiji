/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;
import org.lsmp.djep.djep.*;

/**
 * Examples using differentation
 */
public class DiffExample {

	public static void main(String args[])
	{
		/* initilisation */
		DJep j = new DJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);
		j.addStandardDiffRules();

		try
		{
			// parse the string
			Node node = j.parse("sin(x^2)");
			// differentiate wrt x
			Node diff = j.differentiate(node,"x");
			// print
			j.println(diff);
			// simplify
			Node simp = j.simplify(diff);
			// print
			j.println(simp);
			
			// This time the differentation is specified by
			// the diff(eqn,var) function
			Node node2 = j.parse("diff(cos(x^3),x)");
			// To actually make diff do its work the
			// equation needs to be preprocessed
			Node processed = j.preprocess(node2);
			j.println(processed);
			// finally simplify
			Node simp2 = j.simplify(processed);
			j.println(simp2);
			
			// Now combine assignment and differentation
			Node node3 = j.parse("y=x^5");
			j.preprocess(node3);
			Node node4 = j.parse("diff(y^2+x,x)");
			Node simp3 = j.simplify(j.preprocess(node4));

			j.println(simp3); // default printing will be 2*y*5*x^4+1

			PrintVisitor pv = j.getPrintVisitor();
			pv.setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,false);
			j.println(simp3); // no expansion will be 2*y*dy/dx+1

			pv.setMode(DPrintVisitor.PRINT_PARTIAL_EQNS,true);
			pv.setMode(DPrintVisitor.PRINT_VARIABLE_EQNS,true);
			j.println(simp3); // full expansion: 2*x^5*5*x^4+1

			pv.setMode(DPrintVisitor.PRINT_VARIABLE_EQNS,false);

//			Node node5 = j.parse("y");
//			j.println(node5);
//			((DPrintVisitor)j.getPrintVisitor()).setPrintVariableEquations(true);
//			j.println(node5);
			
			j.getSymbolTable().setVarValue("x",new Double(5));
			System.out.println(j.evaluate(simp3));
			j.evaluate(node3);
			System.out.println(j.getSymbolTable().getVar("y").getValue());
			j.getSymbolTable().setVarValue("x",new Double(0));
			System.out.println(j.evaluate(simp));
			
			Node node10 = j.parse("x=3");
			Node node11 = j.preprocess(node10);
			System.out.println(j.evaluate(node11));
			Node node12 = j.parse("y=x^2");
			Node node13 = j.preprocess(node12);
			System.out.println(j.evaluate(node13));
			Node node14 = j.parse("z=diff(y,x)");
			Node node15 = j.simplify(j.preprocess(node14));
			System.out.println(j.evaluate(node15));

			// If a variable is changed then any expresion tree
			// it depends on needs to be re-evaluated to bring
			// values of other variables upto date
			j.setVarValue("x",new Double(4));
			System.out.println(j.evaluate(node13));
			System.out.println(j.evaluate(node15));
			System.out.println("z: "+j.getVarValue("z").toString());
			
			// the findVarValue method will automatically
			// re-calculate the value of variables specified by
			// equations if needed. However a lazy
			
			j.setVarValue("x",new Double(5));
			System.out.println("j.setVarValue(\"x\",new Double(5));");
			System.out.println("j.findVarValue(y): "+j.calcVarValue("y").toString());
			System.out.println("j.findVarValue(z): "+j.calcVarValue("z").toString());

			// if j.getSymbolTable().clearValues();
			// is called before values of equations are set
			// then the values of intermediate equations
			// are automatically calculated, so you can jump
			// straight to the chase: no need to calculate 
			// y explititly to find the value of z.
			j.getSymbolTable().clearValues();
			j.setVarValue("x",new Double(6));
			System.out.println("j.setVarValue(\"x\",new Double(6));");
			System.out.println("j.findVarValue(z): "+j.calcVarValue("z").toString());

			j.getSymbolTable().clearValues();
			j.setVarValue("x",new Double(7));
			System.out.println(j.evaluate(node15));
			System.out.println("z: "+j.getVarValue("z").toString());
		}
		catch(ParseException e)
		{
			System.out.println("Error with parsing");
		}
		catch(Exception e)
		{
			System.out.println("Error with evaluation");
		}
	}
}
