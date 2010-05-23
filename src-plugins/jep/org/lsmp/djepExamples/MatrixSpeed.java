/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.vectorJep.*;

/**
 * Compares the speed of matrix operations
 * using both VectorJep or MatrixJep.
 */
public class MatrixSpeed {
	static MatrixJep mj;
	static VectorJep vj;
	static int num_itts = 100000; // for normal use
//	static int num_itts = 1000;	  // for use with profiler
		
	public static void main(String args[])	{
		long t1 = System.currentTimeMillis();
		initVec();
		long t2 = System.currentTimeMillis();
		System.out.println("Vec initialise "+(t2-t1));
		initMat();
		long t3 = System.currentTimeMillis();
		System.out.println("Mat initialise "+(t3-t2));
		
		doBoth("y=[[1,2,3],[3,4,5],[6,7,8]]","y*y");
		doBoth("y=[[1,2,3],[3,4,5],[6,7,8]]","y+y");
		doBoth("y=[[1,2,3],[3,4,5],[6,7,8]]","y-y");
		doBoth("y=[[1,2,3],[3,4,5],[6,7,8]]","y*y+y");
		doBoth("y=[1,2,3]","y+y");
		doBoth("y=[1,2,3]","y . y");
		doBoth("y=[1,2,3]","y^^y");
	}
	
	public static void doBoth(String eqn1,String eqn2)
	{
		System.out.println("Testing speed for <"+eqn1+"> and <"+eqn2+">");
		doVec(eqn1,eqn2);
		doMat(eqn1,eqn2);
	}
	static void initVec()
	{
		vj = new VectorJep();
		vj.addStandardConstants();
		vj.addStandardFunctions();
		vj.addComplex();
		vj.setAllowUndeclared(true);
		vj.setImplicitMul(true);
		vj.setAllowAssignment(true);
	}
	
	static void doVec(String eqn1,String eqn2)
	{
	//	System.out.println("vec init"+(t4-t3));
		try
		{
			Node node1 = vj.parse(eqn1);
			vj.evaluate(node1);
			Node node = vj.parse(eqn2);
			long t1 = System.currentTimeMillis();
	//		System.out.println("vec parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				vj.evaluate(node);
			long t2 = System.currentTimeMillis();
			System.out.println("Using VectorJep :"+(t2-t1));
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());}
	}
	
	static void initMat()
	{
		mj = new MatrixJep();
		mj.addStandardConstants();
		mj.addStandardFunctions();
		mj.addComplex();
		mj.setAllowUndeclared(true);
		mj.setImplicitMul(true);
		mj.setAllowAssignment(true);
	}
	
	static void doMat(String eqn1, String eqn2)
	{
		try
		{
			Node node2 = mj.simplify(mj.preprocess(mj.parse(eqn1)));
			mj.evaluate(node2);
			Node node3 = mj.simplify(mj.preprocess(mj.parse(eqn2)));
			long t1 = System.currentTimeMillis();
	//		System.out.println("mat parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				mj.evaluateRaw(node3);
			long t2 = System.currentTimeMillis();
			System.out.println("Using MatrixJep :"+(t2-t1));
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());}
	}
}
