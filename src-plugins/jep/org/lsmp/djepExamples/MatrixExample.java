/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.matrixJep.nodeTypes.*;
/**
 * Examples using vectors and matricies
 */
public class MatrixExample {
	static MatrixJep j;
	
	public static void main(String args[])	{
		j = new MatrixJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);

		// parse and evaluate each equation in turn
		
		doStuff("[1,2,3]");               // Value: [1.0,2.0,3.0]
		doStuff("[1,2,3].[4,5,6]");       // Value: 32.0
		doStuff("[1,2,3]^[4,5,6]");      // Value: [-3.0,6.0,-3.0]
		doStuff("[1,2,3]+[4,5,6]");       // Value: [5.0,7.0,9.0]
		doStuff("[[1,2],[3,4]]");         // Value: [[1.0,2.0],[3.0,4.0]]
		doStuff("[[1,2],[3,4]]*[1,0]");   // Value: [1.0,3.0]
		doStuff("[1,0]*[[1,2],[3,4]]");   // Value: [1.0,2.0]
		doStuff("[[1,2],[3,4]]*[[1,2],[3,4]]");   // Value: [[7.0,10.0],[15.0,22.0]]
		// vectors and matricies can be used with assignment
		doStuff("x=[1,2,3]");             // Value: [1.0,2.0,3.0]
		doStuff("x+x");                   // Value: [2.0,4.0,6.0]
		doStuff("x . x");                 // Value: 14.0
		doStuff("x^x");                  // Value: [0.0,0.0,0.0]
		doStuff("y=[[1,2],[3,4]]");       // Value: [[1.0,2.0],[3.0,4.0]]
		doStuff("y * y");                 // Value: [[7.0,10.0],[15.0,22.0]]
		// accessing the elements on an array or vector
		doStuff("ele(x,2)");              // Value: 2.0
		doStuff("ele(y,[1,2])");          // Value: 2.0
		// using differentation
		doStuff("x=2");					  // 2.0
		doStuff("y=[x^3,x^2,x]");		  // [8.0,4.0,2.0]
		doStuff("z=diff(y,x)");			  // [12.0,4.0,1.0]
		doStuff("diff([x^3,x^2,x],x)");
		System.out.println("dim(z) "+((MatrixVariableI) j.getVar("z")).getDimensions());
	}

	public static void doStuff(String str)	{
		try	{
			Node node = j.parse(str);
			Node proc = j.preprocess(node);
			Node simp = j.simplify(proc);
			Object value = j.evaluate(simp);
			//j.println(proc);
			j.print(simp);
			System.out.print("\t dim "+((MatrixNodeI) simp).getDim());
			System.out.println("\tvalue " + value.toString());
		}
		catch(ParseException e) { System.out.println("Parse error "+e.getMessage()); }		
		catch(Exception e) { System.out.println("evaluation error "+e.getMessage()); e.printStackTrace(); }		
	}
}
