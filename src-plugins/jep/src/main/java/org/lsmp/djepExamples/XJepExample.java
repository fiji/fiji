/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.xjep.*;

/**
 * Examples using differentation
 */
public class XJepExample {

	public static void main(String args[])
	{
		/* initilisation */
		XJep j = new XJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setAllowAssignment(true);
		j.setImplicitMul(true);

		try
		{
			Node node10 = j.parse("x=3");
			Node node11 = j.preprocess(node10);
			System.out.println(j.evaluate(node11));
			Node node12 = j.parse("y=x^2");
			Node node13 = j.preprocess(node12);
			System.out.println(j.evaluate(node13));
			Node node14 = j.parse("z=y+x");
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
			
			// now see if reentrancy works
			
			j.restartParser("x=1; // semi colon; in comment \ny=2; z=3;");
			Node node21;
			while((node21 = j.continueParsing()) != null)
				j.println(node21);
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
