/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.rpe.*;
/**
 * Examples using vectors and matricies
 */
public class RpExample {
	static JEP j;
	
	public static void main(String args[])	{
		j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);

		// parse and evaluate each equation in turn
		
		doStuff("1*2*3+4*5*6+7*8*9");
		doAll(new String[]{"x1=1","x2=2","x3=3","x4=4","x5=5","x6=6","x7=7","x8=8","x9=9",
			"x1*x2*x3+x4*x5*x6+x7*x8*x9"});
		doAll(new String[]{"x=0.7","cos(x)^2+sin(x)^2"});
		// vectors and matricies can be used with assignment
//		doStuff("x=[1,2,3]");             // Value: [1.0,2.0,3.0]
//		doStuff("x+x");                   // Value: [2.0,4.0,6.0]
//		doStuff("x . x");                 // Value: 14.0
//		doStuff("x^x");                  // Value: [0.0,0.0,0.0]
//		doStuff("y=[[1,2],[3,4]]");       // Value: [[1.0,2.0],[3.0,4.0]]
//		doStuff("y * y");                 // Value: [[7.0,10.0],[15.0,22.0]]
		// accessing the elements on an array or vector
//		doStuff("ele(x,2)");              // Value: 2.0
//		doStuff("ele(y,[1,2])");          // Value: 2.0
		// using differentation
//		doStuff("x=2");					  // 2.0
//		doStuff("y=[x^3,x^2,x]");		  // [8.0,4.0,2.0]
//		doStuff("z=diff(y,x)");			  // [12.0,4.0,1.0]
//		doStuff("diff([x^3,x^2,x],x)");
//		System.out.println("dim(z) "+((MatrixVariableI) j.getVar("z")).getDimensions());
	}

	public static void extendedPrint(RpCommandList list) throws ParseException
	{
		int num = list.getNumCommands();
		for(int i=0;i<num;++i)
		{
			RpCommand com=list.getCommand(i);
			int type = com.getType();
			int ref = com.getRef();
			if(type == RpEval.CONST) {
				double val = com.getConstantValue();
				System.out.println("Constant\t"+val+"\t"+ref);
			}
			else if(type == RpEval.VAR) {
				Variable var = com.getVariable();
				System.out.println("Variable\t"+var.toString()+"\t"+ref);
			}
			else if(type == RpEval.FUN) {
				String name = com.getFunction();
				System.out.println("Function\t"+name+"\t"+ref);
			}
			else {
				System.out.println("Operator\t"+com.toString());
			}
		}
	}

	public static void doStuff(String str)	{
		try	{
			Node node = j.parse(str);

			RpEval rpe = new RpEval(j);
			RpCommandList list = rpe.compile(node);
			double res = rpe.evaluate(list);

			// conversion to String
			System.out.println("Expression:\t"+str+"\nresult\t" + res);
			
			// List of commands
			System.out.println("Commands:");
			//System.out.println(list.toString());
			extendedPrint(list);
			System.out.println();
		}
		catch(ParseException e) { System.out.println("Parse error "+e.getMessage()); }		
		catch(Exception e) { System.out.println("evaluation error "+e.getMessage()); e.printStackTrace(); }		
	}

	public static void doAll(String str[])	{
		try	{
			RpEval rpe = new RpEval(j);

			for(int i=0;i<str.length;++i)
			{
				Node node = j.parse(str[i]);
				RpCommandList list = rpe.compile(node);
				double res = rpe.evaluate(list);

				// conversion to String
				System.out.println("Expression "+i+":\t"+str[i]+"\nresult\t" + res);
			
				// List of commands
				System.out.println("Commands:");
				//System.out.println(list.toString());
				extendedPrint(list);
				System.out.println();
			}

		}
		catch(ParseException e) { System.out.println("Parse error "+e.getMessage()); }		
		catch(Exception e) { System.out.println("evaluation error "+e.getMessage()); e.printStackTrace(); }		
	}
}
