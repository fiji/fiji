/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;

/**
 * Examples using assignment
 */
public class AssignmentExample {

	public static void main(String args[])
	{
		// standard initilisation
		JEP j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);

		// swith assignment facilities on
		j.setAllowAssignment(true);

		// parse assignment equations
		j.parseExpression("x=3");
		// evaluate it - no need to save the value returned
		j.getValueAsObject();
		// parse a second equation
		j.parseExpression("y=2");
		j.getValueAsObject();

		// an equation involving above variables
		j.parseExpression("x^y");
		Object val3 = j.getValueAsObject();
		System.out.println("Value is "+val3);
		
		try
		{
			// Alternative syntax
			Node node1 = j.parse("z=i*pi");
			j.evaluate(node1);
			Node node2 = j.parse("exp(z)");
			Object val2 = j.evaluate(node2);
			System.out.println("Value: "+val2);
			
			// getting and setting variable values
			Node node3 = j.parse("z=x^y");
			j.setVarValue("x",new Double(2));
			j.setVarValue("y",new Double(3));
			j.evaluate(node3);
			System.out.println(j.getVarValue("z")); // prints 8
		}
		catch(ParseException e)	{
			System.out.println("Error with parsing");
		}
		catch(Exception e)	{
			System.out.println("Error with evaluation");
		}
	}
}
