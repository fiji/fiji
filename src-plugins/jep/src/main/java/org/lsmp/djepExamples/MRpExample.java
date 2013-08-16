/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.mrpe.MRpCommandList;
import org.lsmp.djep.mrpe.MRpEval;
import org.lsmp.djep.mrpe.MRpRes;
import org.lsmp.djep.vectorJep.values.*;
/**
 * Examples using fast reverse polish calculator with vectors and matrices
 */
public class MRpExample {
	static MatrixJep mj;
	static MRpEval rpe;
	
	public static void main(String args[])	{
		mj = new MatrixJep();
		mj.addStandardConstants();
		mj.addStandardFunctions();
		mj.addComplex();
		mj.setAllowUndeclared(true);
		mj.setImplicitMul(true);
		mj.setAllowAssignment(true);
		rpe = new MRpEval(mj);
		// parse and evaluate each equation in turn
		
		doStuff("[1,2,3]");               // Value: [1.0,2.0,3.0]
		doStuff("[1,2,3].[4,5,6]");       // Value: 32.0
		doStuff("[1,2,3]^[4,5,6]");      // Value: [-3.0,6.0,-3.0]
		doStuff("[1,2,3]+[4,5,6]");       // Value: [5.0,7.0,9.0]
		doStuff("[[1,2],[3,4]]");         // Value: [[1.0,2.0],[3.0,4.0]]
		doStuff("[[1,2],[3,4]]*[1,0]");   // Value: [1.0,3.0]
		doStuff("[1,0]*[[1,2],[3,4]]");   // Value: [1.0,2.0]
		doStuff("[[1,2],[3,4]]*[[1,2],[3,4]]");   // Value: [[7.0,10.0],[15.0,22.0]]
		// vectors and matrices can be used with assignment
		doStuff("x=[1,2,3]");             // Value: [1.0,2.0,3.0]
		doStuff("x+x");                   // Value: [2.0,4.0,6.0]
		doStuff("x . x");                 // Value: 14.0
		doStuff("x^x");                  // Value: [0.0,0.0,0.0]
		doStuff("y=[[1,2],[3,4]]");       // Value: [[1.0,2.0],[3.0,4.0]]
		doStuff("y * y");                 // Value: [[7.0,10.0],[15.0,22.0]]
		// accessing the elements on an array or vector
//		doStuff("ele(x,2)");              // Value: 2.0
//		doStuff("ele(y,[1,2])");          // Value: 2.0
		// using differentation
//		doStuff("x=2");					  // 2.0
//		doStuff("y=[x^3,x^2,x]");		  // [8.0,4.0,2.0]
//		doStuff("z=diff(y,x)");			  // [12.0,4.0,1.0]
//		doStuff("diff([x^3,x^2,x],x)");
//		System.out.println("dim(z) "+((MatrixVariableI) mj.getVar("z")).getDimensions());
	}

	public static void doStuff(String str)	{
		try	{
			Node node = mj.parse(str);
			Node proc = mj.preprocess(node);
			Node simp = mj.simplify(proc);

	
			MRpCommandList list = rpe.compile(simp);
			MRpRes res = rpe.evaluate(list);

			mj.print(node);
			
			// conversion to String
			System.out.println("\nres " + res.toString());
			
			// conversion to MatrixValueI
			MatrixValueI mat = res.toVecMat(); 
			System.out.println("matrix " + mat.toString());
			
			// conversion to array
			if(res.getDims().is1D())
			{
				double vecArray[] = (double []) res.toArray();
				System.out.print("[");
				for(int i=0;i<vecArray.length;++i) System.out.print(""+vecArray[i]+" ");
				System.out.println("]");
			}
			else if(res.getDims().is2D())
			{
				double matArray[][] = (double [][]) res.toArray();
				System.out.print("[");
				for(int i=0;i<matArray.length;++i) {
					System.out.print("[");
					for(int j=0;j<matArray[i].length;++j)
						System.out.print(""+matArray[i][j]+" ");
					System.out.print("]");
				}
				System.out.println("]");
			}
			// List of commands
			System.out.println("Commands");
			System.out.println(list.toString());
		}
		catch(ParseException e) { System.out.println("Parse error "+e.getMessage()); }		
		catch(Exception e) { System.out.println("evaluation error "+e.getMessage()); e.printStackTrace(); }		
	}
}
