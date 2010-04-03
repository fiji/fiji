/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.rpe.*;
/**
 * Compares the speed of matrix operations
 * using both VectorJep or MatrixJep.
 * <p>
 * If you have some nice complicated examples, I'd love to
 * hear about them to see if we can tune things up. - rich
 */
public class RpSpeed {
	static JEP j;
	static int num_itts = 100000; // for normal use
//	static int num_itts = 1000;	  // for use with profiler
		
	public static void main(String args[])	{
		long t1 = System.currentTimeMillis();
		initJep();
		long t2 = System.currentTimeMillis();
		System.out.println("Jep initialise "+(t2-t1));

		doAll(new String[0],"1*2*3+4*5*6+7*8*9");
		doAll(new String[]{"x1=1","x2=2","x3=3","x4=4","x5=5","x6=6","x7=7","x8=8","x9=9"},
			"x1*x2*x3+x4*x5*x6+x7*x8*x9");
		doAll(new String[]{"x=0.7"},"cos(x)^2+sin(x)^2");
	}
	
	public static void doAll(String eqns[],String eqn2)
	{
		System.out.print("Testing speed for <");
		for(int i=0;i<eqns.length;++i) System.out.print(eqns[i]+",");
		System.out.println("> and <"+eqn2+">");
		doJep(eqns,eqn2);
		doRpe(eqns,eqn2);
		System.out.println();
	}

	static void initJep()
	{
		j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		j.setAllowAssignment(true);
	}
	
	static void doJep(String eqns[],String eqn2)
	{
	//	System.out.println("vec init"+(t4-t3));
		try
		{
			for(int i=0;i<eqns.length;++i)	{
				Node node2 = j.parse(eqns[i]);
				j.evaluate(node2);
			}
			Node node = j.parse(eqn2);
			long t1 = System.currentTimeMillis();
	//		System.out.println("vec parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				j.evaluate(node);
			long t2 = System.currentTimeMillis();
			System.out.println("Using Jep:\t"+(t2-t1));
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());}
	}

	static void doRpe(String eqns[], String eqn2)
	{
		try
		{
			for(int i=0;i<eqns.length;++i)	{
				Node node2 = j.parse(eqns[i]);
				j.evaluate(node2);
			}
			Node node3 = j.parse(eqn2);
			RpEval rpe = new RpEval(j);
			RpCommandList list = rpe.compile(node3);
			long t1 = System.currentTimeMillis();
	//		System.out.println("mat parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				rpe.evaluate(list);
			long t2 = System.currentTimeMillis();
			System.out.print("Using RpEval2:\t\t"+(t2-t1));
			double res = rpe.evaluate(list);
			System.out.println("\t"+res);			
			rpe.cleanUp();
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());e.printStackTrace();}
	}
}
