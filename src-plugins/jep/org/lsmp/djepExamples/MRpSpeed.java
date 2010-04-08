/* @author rich
 * Created on 26-Feb-2004
 */

package org.lsmp.djepExamples;
import org.nfunk.jep.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.mrpe.MRpCommandList;
import org.lsmp.djep.mrpe.MRpEval;
import org.lsmp.djep.vectorJep.*;
/**
 * Compares the speed of matrix operations
 * using mrpe, vectorJep and matrixJep.
 */
public class MRpSpeed {
	static MatrixJep mj;
	static VectorJep vj;
	static int num_itts = 100000; // for normal use
//	static int num_itts = 1000;	  // for use with profiler
		
	public static void main(String args[])	{
		if(args.length>0)
			num_itts = Integer.parseInt(args[0]);
		System.out.println("VectorJep, MatrixJep, MRPEval Speed comparison");
		System.out.println("Number of iterations: "+num_itts);
		long t1 = System.currentTimeMillis();
		initVec();
		long t2 = System.currentTimeMillis();
		System.out.println("Vec initialise "+(t2-t1));
		initMat();
		long t3 = System.currentTimeMillis();
		System.out.println("Mat initialise "+(t3-t2));

		doRawAdd();		
		doObjAdd();		
		doRawMult();		
		doObjMult();		

		doAll(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*y");

		doAll(new String[]{},"y=[[1,2,3],[4,5,6],[7,8,9]]");
		doAll(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"z=y*y");
		doAll(new String[0],"[[1,2,3],[4,5,6],[7,8,9]]*[[1,2,3],[4,5,6],[7,8,9]]");

		doAll(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y+y");
		doAll(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y-y");

		doAll(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*y+y");
		
		doAll(new String[]{"x=[1,2,3]","y=[[1,2,3],[4,5,6],[7,8,9]]"},"x*y");
		doAll(new String[]{"x=[1,2,3]","y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*x");
		
		doAll(new String[]{"y=[1,2,3]"},"y+y");
		doAll(new String[]{"y=[1,2,3]"},"y . y");
		doAll(new String[]{"y=[1,2,3]"},"y^^y");
		
		doAll(new String[]{"y=[[1,2],[3,4]]"},"y*y");
		doAll(new String[]{"y=[[1,2],[3,4]]"},"y+y");
		doAll(new String[]{"y=[[1,2],[3,4]]"},"y-y");
		doAll(new String[]{"y=[[1,2],[3,4]]"},"y*y+y");
		
		doAll(new String[]{"x=[1,2]","y=[[1,2],[3,4]]"},"x*y");
		doAll(new String[]{"x=[1,2]","y=[[1,2],[3,4]]"},"y*x");
		
		doAll(new String[0],"1*2*3+4*5*6+7*8*9");
		doAll(new String[]{"x1=1","x2=2","x3=3","x4=4","x5=5","x6=6","x7=7","x8=8","x9=9"},
			"x1*x2*x3+x4*x5*x6+x7*x8*x9");
		doAll(new String[]{"y=[1,2,3,4,5]"},"y+y");
		doAll(new String[]{"y=[[1,2,3,4,5],[6,7,8,9,10],[11,12,13,14,15],[16,17,18,19,20],[21,22,23,24,25]]"},"y*y");
		doAll(new String[]{"x=0.7"},"cos(x)^2+sin(x)^2");
	
		for(int scale=2;scale<=6;++scale) {
			StringBuffer sb = new StringBuffer("y=[");
			int k=1;
			for(int i=0;i<scale;++i) {
				if(i>0)  sb.append(',');
				sb.append('[');
				for(int j=0;j<scale;++j) {
					if(j>0) sb.append(',');
					sb.append(k);
					++k;
				}
				sb.append(']');
			}
			sb.append(']');
			doAll(new String[]{sb.toString()},"y*y");
		}
	}
	
	public static void doAll(String eqns[],String eqn2)
	{
		System.out.println("Testing speed for <"+eqn2+"> Where ");
		for(int i=0;i<eqns.length;++i) System.out.println("\t"+eqns[i]);
		long v = doVec(eqns,eqn2);
		long m = doMat(eqns,eqn2);
		long r = doRpe(eqns,eqn2);
		if(m!=0 && r!=0)
			System.out.println("v/m "+v/m+" v/r "+v/r+" m/r "+m/r);
		System.out.println();
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
	
	static long doVec(String eqns[],String eqn2)
	{
	//	System.out.println("vec init"+(t4-t3));
		try
		{
			for(int i=0;i<eqns.length;++i)	{
				Node node2 = vj.parse(eqns[i]);
				vj.evaluate(node2);
			}
			Node node = vj.parse(eqn2);
			long t1 = System.currentTimeMillis();
	//		System.out.println("vec parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				vj.evaluate(node);
			long t2 = System.currentTimeMillis();
			Object res = vj.evaluate(node);
			System.out.print("Using VectorJep:\t"+(t2-t1));
			System.out.println("\t"+res.toString());			
			return t2-t1;
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());}
		return 0;
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
	
	static long doMat(String eqns[], String eqn2)
	{
		try
		{
			for(int i=0;i<eqns.length;++i)	{
				Node node2 = mj.simplify(mj.preprocess(mj.parse(eqns[i])));
				mj.evaluate(node2);
			}
			Node node3 = mj.simplify(mj.preprocess(mj.parse(eqn2)));
			long t1 = System.currentTimeMillis();
	//		System.out.println("mat parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				mj.evaluateRaw(node3);
			long t2 = System.currentTimeMillis();
			System.out.print("Using MatrixJep:\t"+(t2-t1));
			Object res = mj.evaluate(node3);
			System.out.println("\t"+res.toString());			
			return t2-t1;
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());}
		return 0;
	}
	
	static long doRpe(String eqns[], String eqn2)
	{
		try
		{
			for(int i=0;i<eqns.length;++i)	{
				Node node2 = mj.simplify(mj.preprocess(mj.parse(eqns[i])));
				mj.evaluate(node2);
			}
			Node node3 = mj.simplify(mj.preprocess(mj.parse(eqn2)));
			MRpEval rpe = new MRpEval(mj);
			MRpCommandList list = rpe.compile(node3);
			long t1 = System.currentTimeMillis();
	//		System.out.println("mat parse"+(t1-t4));
			for(int i=0;i<num_itts;++i)
				rpe.evaluate(list);
			long t2 = System.currentTimeMillis();
			System.out.print("Using MRpEval:\t\t"+(t2-t1));
			Object res = rpe.evaluate(list);
			System.out.println("\t"+res.toString());			
			rpe.cleanUp();
			return t2-t1;
		}
		catch(Exception e) {System.out.println("Error"+e.getMessage());e.printStackTrace();}
		return 0;
	}


	static void doRawAdd()
	{
		double mat1[][] = new double[][]{{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		double mat2[][] = new double[][]{{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		double mat3[][] = new double[3][3];
		
		long t1 = System.currentTimeMillis();
		for(int i=0;i<num_itts;++i)
		{
			for(int row=0;row<3;++row)
				for(int col=0;col<3;++col)
					mat3[row][col] = mat1[row][col]+mat2[row][col];
		}
		long t2 = System.currentTimeMillis();
		System.out.println("RawAdd:"+(t2-t1)+"\tTime to add two double[3][3] arrays");
	}

	static void doRawMult()
	{
		double mat1[][] = new double[][]{{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		double mat2[][] = new double[][]{{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		double mat3[][] = new double[3][3];
		
		long t1 = System.currentTimeMillis();
		for(int i=0;i<num_itts;++i)
		{
			for(int row=0;row<3;++row)
				for(int col=0;col<3;++col)
				{
					mat3[row][col] = mat1[row][0] * mat2[0][col];
					for(int j=1;j<3;++j)
						mat3[row][col] += mat1[row][j] * mat2[j][col];
				}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("RawMult:"+(t2-t1)+"\tTime to multiply two double[3][3] arrays");
	}

	static void doObjAdd()
	{
		Double mat1[][] = new Double[][]{{new Double(1.0),new Double(2.0),new Double(3.0)},{new Double(4.0),new Double(5.0),new Double(6.0)},{new Double(7.0),new Double(8.0),new Double(9.0)}};
		Double mat2[][] = new Double[][]{{new Double(1.0),new Double(2.0),new Double(3.0)},{new Double(4.0),new Double(5.0),new Double(6.0)},{new Double(7.0),new Double(8.0),new Double(9.0)}};
		Double mat3[][] = new Double[3][3];
		
		long t1 = System.currentTimeMillis();
		for(int i=0;i<num_itts;++i)
		{
			for(int row=0;row<3;++row)
				for(int col=0;col<3;++col)
					mat3[row][col] = new Double(mat1[row][col].doubleValue()+mat2[row][col].doubleValue());
		}
		long t2 = System.currentTimeMillis();
		System.out.println("ObjAdd:"+(t2-t1)+"\tTime to add two Double[3][3] arrays");
	}

	static void doObjMult()
	{
		Double mat1[][] = new Double[][]{{new Double(1.0),new Double(2.0),new Double(3.0)},
					{new Double(4.0),new Double(5.0),new Double(6.0)},
					{new Double(7.0),new Double(8.0),new Double(9.0)}};
		Double mat2[][] = new Double[][]{{new Double(1.0),new Double(2.0),new Double(3.0)},
				{new Double(4.0),new Double(5.0),new Double(6.0)},
				{new Double(7.0),new Double(8.0),new Double(9.0)}};
		Double mat3[][] = new Double[3][3];
		
		long t1 = System.currentTimeMillis();
		for(int i=0;i<num_itts;++i)
		{
			for(int row=0;row<3;++row)
				for(int col=0;col<3;++col)
				{
//					mat2[row][col] = new Double(mat1[row][0].doubleValue() * mat1[0][col].doubleValue());
					double tmp = mat1[row][0].doubleValue() * mat2[0][col].doubleValue();
					for(int j=1;j<3;++j)
//						mat2[row][col] = new Double(mat2[row][col].doubleValue() + 
//							mat1[row][j].doubleValue() * mat1[j][col].doubleValue());
						tmp +=  
							mat1[row][j].doubleValue() * mat2[j][col].doubleValue();
					mat3[row][col] = new Double(tmp);	
				}
		}
		long t2 = System.currentTimeMillis();
		System.out.println("ObjMult:"+(t2-t1)+"\tTime to multiply two Double[3][3] arrays");
	}
}
