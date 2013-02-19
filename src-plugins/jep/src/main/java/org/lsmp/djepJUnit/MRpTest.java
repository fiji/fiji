package org.lsmp.djepJUnit;

import junit.framework.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
import org.lsmp.djep.matrixJep.*;
import org.lsmp.djep.mrpe.MRpCommandList;
import org.lsmp.djep.mrpe.MRpEval;
import org.lsmp.djep.mrpe.MRpRes;
import org.lsmp.djep.vectorJep.values.*;

/* @author rich
 * Created on 19-Nov-2003
 */

/**
 * JUnit test for full Matrix Rp evaluator
 * 
 * @author Rich Morris
 * Created on 19-Nov-2003
 */
public class MRpTest extends TestCase {
	MatrixJep mj;
	public static final boolean SHOW_BAD=false;
	
	public MRpTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(MRpTest.class);
	}

	public static void main(String args[]) {
		// Create an instance of this class and analyse the file

		TestSuite suite= new TestSuite(MRpTest.class);
//		DJepTest jt = new DJepTest("DJepTest");
//		jt.setUp();
		suite.run(new TestResult());
	}	
	/** strings for each variable */
	String matStrs[][] = new String[10][10];
	String vecStrs[] = new String[10];

	protected void setUp() {
		mj = new MatrixJep();
		mj.addStandardConstants();
		mj.addStandardFunctions();
		mj.addComplex();
		//j.setTraverse(true);
		mj.setAllowAssignment(true);
		mj.setAllowUndeclared(true);
		mj.setImplicitMul(true);
		mj.addStandardDiffRules();

		for(int i=2;i<=9;++i)
			for(int j=2;j<=9;++j)
			{
				int num=1;
				StringBuffer sb = new StringBuffer("[");
				for(int k=0;k<i;++k)
				{
						if(k>0)sb.append(",");
						sb.append("[");
						for(int l=0;l<j;++l)
						{
							if(l>0)sb.append(",");
							sb.append(String.valueOf(num++));
						}
						sb.append("]");
				}
				sb.append("]");
				matStrs[i][j] = sb.toString();
			}

		for(int i=2;i<=9;++i)
			{
				int num=1;
				StringBuffer sb = new StringBuffer("[");
				for(int k=0;k<i;++k)
				{
						if(k>0)sb.append(",");
						sb.append(String.valueOf(num++));
				}
				sb.append("]");
				vecStrs[i] = sb.toString();
			}
	}

	public void testGood()
	{
		assertEquals(1,1);
	}

	public void myAssertEquals(String msg,String actual,String expected)
	{
		if(!actual.equals(expected))
			System.out.println("Error \""+msg+"\" is \n<"+actual+"> should be \n<"+expected+">");
		assertEquals("<"+msg+">",expected,actual);
		System.out.println("Success: Value of <"+msg+"> is <"+actual+">");
	}

	public void valueTest(String expr,double dub) throws ParseException
	{
		valueTest(expr,new Double(dub));
	}
	public void valueTest(String expr,Object expected) throws ParseException
	{
		Node node = mj.parse(expr);
		Node matEqn = mj.preprocess(node);
		Object res = mj.evaluate(matEqn);
		if(mj.hasError())
			fail("Evaluation Failure: "+expr+mj.getErrorInfo());
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Sucess value of <"+expr+"> is "+res);
	}

	public void valueTest(String expr,String expected) throws ParseException
	{
		Node node = mj.parse(expr);
		Node matEqn = mj.preprocess(node);
		Object res = mj.evaluate(matEqn);
		if(mj.hasError())
			fail("Evaluation Failure: "+expr+mj.getErrorInfo());
		assertEquals("<"+expr+">",expected,res.toString());
		System.out.println("Sucess value of <"+expr+"> is "+res.toString());
	}

	public void complexValueTest(String expr,Complex expected,double tol) throws Exception
	{
		Node node = mj.preprocess(mj.parse(expr));
		Object res = mj.evaluate(node);
		assertTrue("<"+expr+"> expected: <"+expected+"> but was <"+res+">",
			expected.equals((Complex) res,tol));
		System.out.println("Sucess value of <"+expr+"> is "+res);
	}

	public Object calcValue(String expr) throws ParseException
	{
		Node node = mj.parse(expr);
		Node matEqn = mj.preprocess(node);
		Object res = mj.evaluate(matEqn);
		return res;
	}

	public void simplifyTest(String expr,String expected) throws ParseException
	{
		Node node = mj.parse(expr);
		Node matEqn = mj.preprocess(node);
		Node simp = mj.simplify(matEqn);
		String res = mj.toString(simp);
		
		Node node2 = mj.parse(expected);
		Node matEqn2 = mj.preprocess(node2);
		Node simp2 = mj.simplify(matEqn2);
		String res2 = mj.toString(simp2);


		if(!res2.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+res2+"\"");
		assertEquals("<"+expr+">",res2,res);
		System.out.println("Sucess: Value of \""+expr+"\" is \""+res+"\"");
			
//		System.out.print("Full Brackets:\t");
//		j.pv.setFullBrackets(true);
//		j.pv.println(simp);
//		j.pv.setFullBrackets(false);

	}

	public void simplifyTestString(String expr,String expected) throws ParseException
	{
		Node node = mj.parse(expr);
		Node matEqn = mj.preprocess(node);
		String res = mj.toString(matEqn);
		
		if(!expected.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+expected+"\"");
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Sucess: Value of \""+expr+"\" is \""+res+"\"");
			
//		System.out.print("Full Brackets:\t");
//		j.pv.setFullBrackets(true);
//		j.pv.println(simp);
//		j.pv.setFullBrackets(false);

	}

	void rpTest(String eqns[], String eqn2) throws ParseException
	{
		for(int i=0;i<eqns.length;++i)	{
			System.out.println("eqns "+eqns[i]);
			Node node = mj.simplify(mj.preprocess(mj.parse(eqns[i])));
			mj.evaluate(node);
		}
		Node node3 = mj.simplify(mj.preprocess(mj.parse(eqn2)));
		MRpEval rpe = new MRpEval(mj);
		MRpCommandList list = rpe.compile(node3);
		MRpRes rpRes = rpe.evaluate(list);
		MatrixValueI mat = rpRes.toVecMat();

		Object matRes = mj.evaluateRaw(node3);
//		System.out.println("rpRes: "+rpRes.getClass().getName()+" = "+rpRes.toString());
		if(mj.hasError())
			fail("Evaluation Failure: "+eqn2+mj.getErrorInfo());
		myAssertEquals("<"+eqn2+">",rpRes.toString(),matRes.toString());

		if(!mat.equals(matRes))
			fail("Expected <"+matRes+"> found <"+mat+">");

		if(rpRes.getDims().is1D())
		{
			double vecArray[] = (double []) rpRes.toArray();
			for(int i=0;i<vecArray.length;++i)
				if(vecArray[i] != ((Double) ((MVector) matRes).getEle(i)).doubleValue())
					fail("Problem with toArray");
		}
		else if(rpRes.getDims().is2D())
		{
			double matArray[][] = (double [][]) rpRes.toArray();
			for(int i=0;i<matArray.length;++i)
				for(int j=0;j<matArray[i].length;++j)
				if(matArray[i][j] != ((Double) ((Matrix) matRes).getEle(i,j)).doubleValue())
					fail("Problem with toArray");
		}
		rpe.cleanUp();
	}

	/** As before but don't test with MatrixJep.evaluate */
	void rpTest2(String eqns[]) throws ParseException
	{
		Node nodes[] = new Node[eqns.length];
		MatrixValueI rpMats[] = new MatrixValueI[eqns.length];
		MRpEval rpe = new MRpEval(mj);
		for(int i=0;i<eqns.length;++i)	{
			System.out.println("eqns "+eqns[i]);
			nodes[i] = mj.simplify(mj.preprocess(mj.parse(eqns[i])));
			MRpCommandList list = rpe.compile(nodes[i]);
			MRpRes rpRes = rpe.evaluate(list);
			rpMats[i] = rpRes.toVecMat();
			System.out.println("<"+eqns[i]+"> "+rpRes.toString());
		}
		for(int i=0;i<eqns.length;++i)	{
			Object matRes = mj.evaluateRaw(nodes[i]);
			if(!rpMats[i].equals(matRes))
					fail("Expected <"+matRes+"> found <"+rpMats[i]+">");
		}		
		rpe.cleanUp();
	}

	public void testRp() throws ParseException
	{
		rpTest(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*y");
		rpTest(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y+y");
		rpTest(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y-y");
		rpTest(new String[]{"y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*y+y");
		rpTest(new String[]{"x=[1,2,3]","y=[[1,2,3],[4,5,6],[7,8,9]]"},"x*y");
		rpTest(new String[]{"x=[1,2,3]","y=[[1,2,3],[4,5,6],[7,8,9]]"},"y*x");
		rpTest(new String[0],"[[1,2,3],[4,5,6],[7,8,9]]*[[1,2,3],[4,5,6],[7,8,9]]");

		rpTest(new String[]{"y=[1,2]"},"y+y");
		rpTest(new String[]{"y=[1,2,3]"},"y+y");
		rpTest(new String[]{"y=[1,2,3,4]"},"y+y");
		rpTest(new String[]{"y=[1,2]"},"-y");
		rpTest(new String[]{"y=[1,2,3]"},"-y");
		rpTest(new String[]{"y=[1,2,3,4]"},"-y");
		rpTest(new String[]{"y=[1,2]"},"y-y");
		rpTest(new String[]{"y=[1,2,3]"},"y-y");
		rpTest(new String[]{"y=[1,2,3,4]"},"y-y");
		rpTest(new String[]{"y=[1,2]"},"y*3");
		rpTest(new String[]{"y=[1,2,3]"},"y*3");
		rpTest(new String[]{"y=[1,2,3,4]"},"y*3");
		rpTest(new String[]{"y=[1,2]"},"5*y");
		rpTest(new String[]{"y=[1,2,3]"},"5*y");
		rpTest(new String[]{"y=[1,2,3,4]"},"5*y");

		rpTest(new String[]{"y=[1,2,3]"},"y . y");
		rpTest(new String[]{"y=[1,2,3]"},"y^^y");

		rpTest(new String[]{"y=[[1,2],[3,4]]"},"y*y");
		rpTest(new String[]{"y=[[1,2],[3,4]]"},"y+y");
		rpTest(new String[]{"y=[[1,2],[3,4]]"},"y-y");
		rpTest(new String[]{"y=[[1,2],[3,4]]"},"y*y+y");
		rpTest(new String[]{"x=[1,2]","y=[[1,2],[3,4]]"},"x*y");
		rpTest(new String[]{"x=[1,2]","y=[[1,2],[3,4]]"},"y*x");
		rpTest(new String[0],"1*2*3+4*5*6+7*8*9");
		
		rpTest(new String[]{"x1=1","x2=2","x3=3","x4=4","x5=5","x6=6","x7=7","x8=8","x9=9"},
			"x1*x2*x3+x4*x5*x6+x7*x8*x9");

	}

	public void testRpAllDim() throws ParseException
	{
		
		for(int i=2;i<=4;++i)
			for(int j=2;j<=4;++j)
			{
				int num=1;
				StringBuffer sb = new StringBuffer("x=[");
				for(int k=0;k<i;++k)
				{
						if(k>0)sb.append(",");
						sb.append("[");
						for(int l=0;l<j;++l)
						{
							if(l>0)sb.append(",");
							sb.append(String.valueOf(num++));
						}
						sb.append("]");
				}
				sb.append("]");
				String varStr = sb.toString();
				rpTest(new String[]{varStr},"x+x");
				rpTest(new String[]{varStr},"x-x");

				rpTest(new String[]{varStr},"3*x");
				rpTest(new String[]{varStr},"x*5");
				rpTest(new String[]{varStr},"-x");
			}
	}


	public void testMul() throws ParseException
	{
		rpTest(new String[]{"x=[1,2]","y="+matStrs[2][2]},"x*y");
		rpTest(new String[]{"x=[1,2]","y="+matStrs[2][3]},"x*y");
		rpTest(new String[]{"x=[1,2]","y="+matStrs[2][4]},"x*y");

		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[3][2]},"x*y");
		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[3][3]},"x*y");
		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[3][4]},"x*y");

		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[4][2]},"x*y");
		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[4][3]},"x*y");
		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[4][4]},"x*y");

		rpTest(new String[]{"x=[1,2]","y="+matStrs[2][2]},"y*x");
		rpTest(new String[]{"x=[1,2]","y="+matStrs[3][2]},"y*x");
		rpTest(new String[]{"x=[1,2]","y="+matStrs[4][2]},"y*x");

		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[2][3]},"y*x");
		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[3][3]},"y*x");
		rpTest(new String[]{"x=[1,2,3]","y="+matStrs[4][3]},"y*x");

		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[2][4]},"y*x");
		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[3][4]},"y*x");
		rpTest(new String[]{"x=[1,2,3,4]","y="+matStrs[4][4]},"y*x");

		rpTest(new String[]{"x="+matStrs[2][2],"y="+matStrs[2][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][2],"y="+matStrs[2][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][2],"y="+matStrs[2][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[2][3],"y="+matStrs[3][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][3],"y="+matStrs[3][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][3],"y="+matStrs[3][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[2][4],"y="+matStrs[4][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][4],"y="+matStrs[4][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[2][4],"y="+matStrs[4][4]},"x*y");
		//
		rpTest(new String[]{"x="+matStrs[3][2],"y="+matStrs[2][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][2],"y="+matStrs[2][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][2],"y="+matStrs[2][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[3][3],"y="+matStrs[3][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][3],"y="+matStrs[3][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][3],"y="+matStrs[3][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[3][4],"y="+matStrs[4][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][4],"y="+matStrs[4][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[3][4],"y="+matStrs[4][4]},"x*y");
		//
		rpTest(new String[]{"x="+matStrs[4][2],"y="+matStrs[2][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][2],"y="+matStrs[2][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][2],"y="+matStrs[2][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[4][3],"y="+matStrs[3][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][3],"y="+matStrs[3][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][3],"y="+matStrs[3][4]},"x*y");

		rpTest(new String[]{"x="+matStrs[4][4],"y="+matStrs[4][2]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][4],"y="+matStrs[4][3]},"x*y");
		rpTest(new String[]{"x="+matStrs[4][4],"y="+matStrs[4][4]},"x*y");
	}
	
	public void testAssign() throws ParseException
	{
		rpTest2(new String[]{"x=[[5,6],[7,8]]","x+x"});
		rpTest2(new String[]{"x=[5,6]","x+x"});
		rpTest2(new String[]{"x=[5,6,7]","x+x"});
		rpTest2(new String[]{"x=[5,6,7,8]","x+x"});
		rpTest2(new String[]{"x=5","x+x"});

		for(int i=2;i<=4;++i)
			for(int j=2;j<=4;++j)
			{
				rpTest2(new String[]{"x="+matStrs[i][j],"x+x"});
			}
	}
	
	public void testLogical() throws ParseException
	{
		rpTest2(new String[]{"1&&1","1&&0","0&&0","0&&1","3.14&&1"});
		rpTest2(new String[]{"1||1","1||0","0||0","0||1","3.14||0"});
		rpTest2(new String[]{"!0","!1","!3.14","!-3.14"});
		
		rpTest2(new String[]{"1>1","1>0","0>0","0>1","3.14>1"});
		rpTest2(new String[]{"1<1","1<0","0<0","0<1","3.14<1"});
		rpTest2(new String[]{"1>=1","1>=0","0>=0","0>=1","3.14>=1"});
		rpTest2(new String[]{"1<=1","1<=0","0<=0","0<=1","3.14<=1"});
		rpTest2(new String[]{"1==1","1==0","0==0","0==1","3.14==1"});
		rpTest2(new String[]{"1!=1","1!=0","0!=0","0!=1","3.14!=1"});

		rpTest2(new String[]{"[1,2]==[1,2]"});		
		rpTest2(new String[]{"[1,2]!=[1,2]"});		
		rpTest2(new String[]{"[1,2]==[5,6]"});		
		rpTest2(new String[]{"[1,2]!=[5,6]"});		

		rpTest2(new String[]{"[1,2,3]==[1,2,3]"});		
		rpTest2(new String[]{"[1,2,3]!=[1,2,3]"});		
		rpTest2(new String[]{"[1,2,3]==[5,6,7]"});		
		rpTest2(new String[]{"[1,2,3]!=[5,6,7]"});		
		rpTest2(new String[]{"[1,2,3]==[1,2,4]"});		
		rpTest2(new String[]{"[1,2,3]!=[1,2,4]"});		

		rpTest2(new String[]{"[1,2,3,4]==[1,2,3,4]"});		
		rpTest2(new String[]{"[1,2,3,4]!=[1,2,3,4]"});		
		rpTest2(new String[]{"[1,2,3,4]==[5,6,7,8]"});		
		rpTest2(new String[]{"[1,2,3,4]!=[5,6,7,8]"});	
		
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"=="+matStrs[2][2]});	

		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
		rpTest2(new String[]{matStrs[2][2]+"!="+matStrs[2][2]});	
	}
	boolean TESTALL = false;
	public void testVn() throws ParseException {
		rpTest2(new String[]{"x=[5,6,7,8,9]","x+x","x-x","2*x","x*3","x.x"});
		rpTest2(new String[]{"x=[[1,2,3,4,5],[5,6,7,8,9]]","x+x","x-x","2*x","x*3"});
		rpTest2(new String[]{"x=[[1,2],[3,4]]","y=[[1,2,3,4,5],[5,6,7,8,9]]","x*y"});
		rpTest2(new String[]{"x=[[1,2],[3,4]]","y=[[1,2],[3,4],[5,6],[7,8],[9,10]]","y*x"});
		rpTest2(new String[]{"x=[[1,2,3,4,5],[5,6,7,8,9]]","y=[[1,2],[3,4],[5,6],[7,8],[9,10]]","y*x"});

	  if(TESTALL)
	  {
	    for(int i=2;i<10;++i)
			for(int j=2;j<10;++j)
				for(int k=2;k<10;++k)
				{
					System.out.println("\n["+i+","+j+"]*["+j+","+k+"]");
					rpTest2(new String[]{"x="+matStrs[i][j],"y="+matStrs[j][k],"x*y"});
				}

		for(int i=2;i<10;++i)
			for(int j=2;j<10;++j)
				{
					System.out.println("\n["+i+","+j+"]*["+j+"]");
					rpTest2(new String[]{"x="+matStrs[i][j],"y="+vecStrs[j],"x*y"});
				}

			for(int j=2;j<10;++j)
				for(int k=2;k<10;++k)
				{
					System.out.println("\n["+j+"]*["+j+","+k+"]");
					rpTest2(new String[]{"x="+vecStrs[j],"y="+matStrs[j][k],"x*y"});
				}
	  }
	}

	public void testFun() throws ParseException {
		rpTest2(new String[]{"x=5","y=4","x/y","x%y","x^y"});
		rpTest2(new String[]{"x=0.5","cos(x)","sin(x)","tan(x)","asin(x)","acos(x)","atan(x)"});
		rpTest2(new String[]{"x=0.5","cosh(x)","sinh(x)","tanh(x)","asinh(x)","acosh(x+1)","atanh(x)"});
		rpTest2(new String[]{"x=0.5","sqrt(x)","ln(x)","log(x)","exp(x)","abs(x)"});
		rpTest2(new String[]{"x=0.5","sec(x)","cosec(x)","cot(x)"});
	}
	
	public void testUndecVar() throws ParseException {
		mj.setAllowUndeclared(true);
		MRpEval rpe = new MRpEval(mj);
		Node node1 = mj.parse("zap * gosh");
		Node node3 = mj.preprocess(node1);
		rpe.compile(node3);
	}

	/*	public void testSimpleSum() throws ParseException
	{
		valueTest("1+2",3);		
		valueTest("2*6+3",15);		
		valueTest("2*(6+3)",18);
	}
	
	public void testOperators()  throws ParseException
	{
//		if(!Operator.OP_MULTIPLY.isDistributiveOver(Operator.OP_ADD))
//			fail("* should be distrib over +");
//		if(Operator.OP_MULTIPLY.isDistributiveOver(Operator.OP_DIVIDE))
//			fail("* should not be distrib over /");
//		if(Operator.OP_MULTIPLY.getPrecedence() > Operator.OP_ADD.getPrecedence())
//			fail("* should have a lower precedence than +");

		valueTest("T=1",1);
		valueTest("F=0",0);
		calcValue("a=F"); calcValue("b=F"); calcValue("c=F");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=F"); calcValue("b=F"); calcValue("c=T");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=F"); calcValue("b=T"); calcValue("c=F");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=F"); calcValue("b=T"); calcValue("c=T");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);

		calcValue("a=T"); calcValue("b=F"); calcValue("c=F");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=T"); calcValue("b=F"); calcValue("c=T");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=T"); calcValue("b=T"); calcValue("c=F");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
		calcValue("a=T"); calcValue("b=T"); calcValue("c=T");
		valueTest("(a&&(b||c)) == ((a&&b)||(a&&c))",1);
		valueTest("(a||(b&&c)) == ((a||b)&&(a||c))",1);
	}
	
	public void testSimp() throws ParseException
	{
		simplifyTest("2+3","5");
		simplifyTest("2*3","6");
		simplifyTest("2^3","8");
		simplifyTest("3/2","1.5");
		simplifyTest("2*3+4","10");
		simplifyTest("2*(3+4)","14");

		simplifyTest("0+x","x");
		simplifyTest("x+0","x");
		simplifyTest("0-x","0-x");
		simplifyTest("x-0","x");
		simplifyTest("0*x","0");
		simplifyTest("x*0","0");
		simplifyTest("1*x","x");
		simplifyTest("x*1","x");
		simplifyTest("-1*x","-x");
		simplifyTest("x*-1","-x");
		simplifyTest("-(-x)","x");
		simplifyTest("-(-(-x))","-x");
		simplifyTest("(-1)*(-1)*x","x");
		simplifyTest("(-1)*(-1)*(-1)*x","-x");
		
		simplifyTest("0/x","0");
		simplifyTest("x/0","1/0");
		
		simplifyTest("x^0","1");
		simplifyTest("x^1","x");
		simplifyTest("0^x","0");
		simplifyTest("1^x","1");

		// (a+b)+c
		simplifyTest("(2+3)+x","5+x");
		simplifyTest("(2+x)+3","5+x");
		simplifyTest("(x+2)+3","5+x");
		// a+(b+c)
		simplifyTest("x+(2+3)","5+x");
		simplifyTest("2+(x+3)","5+x");
		simplifyTest("2+(3+x)","5+x");
		// (a+b)-c
		simplifyTest("(2+3)-x","5-x");
		simplifyTest("(2+x)-3","x-1");
		simplifyTest("(x+2)-3","x-1");
		// (a-b)+c
		simplifyTest("(2-3)+x","-1+x");
		simplifyTest("(2-x)+3","5-x");
		simplifyTest("(x-2)+3","1+x");
		// a-(b+c)
		simplifyTest("x-(2+3)","x-5");
		simplifyTest("2-(x+3)","-1-x");
		simplifyTest("2-(3+x)","-1-x");
		// a+(b-c)
		simplifyTest("x+(2-3)","x-1");
		simplifyTest("2+(x-3)","-1+x");
		simplifyTest("2+(3-x)","5-x");
		// a-(b-c)
		simplifyTest("x-(2-3)","1+x");
		simplifyTest("2-(x-3)","5-x");
		simplifyTest("2-(3-x)","-1+x");
		// (a-b)-c
		simplifyTest("(2-3)-x","-1-x");
		simplifyTest("(2-x)-3","-1-x");
		simplifyTest("(x-2)-3","x-5");

		// (a*b)*c
		simplifyTest("(2*3)*x","6*x");
		simplifyTest("(2*x)*3","6*x");
		simplifyTest("(x*2)*3","6*x");
		// a+(b+c)
		simplifyTest("x*(2*3)","6*x");
		simplifyTest("2*(x*3)","6*x");
		simplifyTest("2*(3*x)","6*x");
		// (a+b)-c
		simplifyTest("(2*3)/x","6/x");
		simplifyTest("(3*x)/2","1.5*x");
		simplifyTest("(x*3)/2","1.5*x");
		// (a-b)+c
		simplifyTest("(3/2)*x","1.5*x");
		simplifyTest("(3/x)*2","6/x");
		simplifyTest("(x/2)*3","1.5*x");
		// a-(b+c)
		simplifyTest("x/(2*3)","x/6");
		simplifyTest("3/(x*2)","1.5/x");
		simplifyTest("3/(2*x)","1.5/x");
		// a+(b-c)
		simplifyTest("x*(3/2)","1.5*x");
		simplifyTest("3*(x/2)","1.5*x");
		simplifyTest("3*(2/x)","6/x");
		// a-(b-c)
		simplifyTest("x/(3/2)","x/1.5");
		simplifyTest("2/(x/3)","6/x");
		simplifyTest("3/(2/x)","1.5*x");
		// (a-b)-c
		simplifyTest("(3/2)/x","1.5/x");
		simplifyTest("(3/x)/2","1.5/x");
		simplifyTest("(x/3)/2","x/6");


		simplifyTest("x*(3+2)","5*x");
		simplifyTest("3*(x+2)","6+3*x");
		simplifyTest("3*(2+x)","6+3*x");
		simplifyTest("(3+2)*x","5*x");
		simplifyTest("(3+x)*2","6+2*x");
		simplifyTest("(x+3)*2","6+x*2");

		simplifyTest("x*(3-2)","x");
		simplifyTest("3*(x-2)","-6+3*x");
		simplifyTest("3*(2-x)","6-3*x");
		simplifyTest("(3-2)*x","x");
		simplifyTest("(3-x)*2","6-2*x");
		simplifyTest("(x-3)*2","-6+2*x");

		simplifyTest("3+(x/4)","3+x/4");
		simplifyTest("2*(x/4)","0.5*x");
		simplifyTest("(2*(3+(x/4)))","6+0.5*x");
		simplifyTest("1+(2*(3+(x/4)))","7+0.5*x");
		simplifyTest("((3+(x/4))*2)+1","7+0.5*x");
		
	}

	public void testComplex() throws Exception
	{
		double tol = 0.00000001;

		complexValueTest("z=complex(3,2)",new Complex(3,2),tol);
		complexValueTest("z*z-z",new Complex(2,10),tol);
		complexValueTest("z^3",new Complex(-9,46),tol);
		complexValueTest("(z*z-z)/z",new Complex(2,2),tol);
		complexValueTest("w=polar(2,pi/2)",new Complex(0,2),tol);
		
	}

	public void testIf()  throws ParseException
	{
		valueTest("if(1,2,3)",2);		
		valueTest("if(-1,2,3)",3);		
		valueTest("if(0,2,3)",3);		
		valueTest("if(1,2,3,4)",2);		
		valueTest("if(-1,2,3,4)",3);		
		valueTest("if(0,2,3,4)",4);		
		valueTest("if(0>=0,2,3,4)",2);		
		valueTest("x=3",3);		
		valueTest("if(x==3,1,-1)",1);		
		valueTest("if(x!=3,1,-1)",-1);		
		valueTest("if(x>=3,1,-1)",1);		
		valueTest("if(x>3,1,-1)",-1);		
		valueTest("if(x<=3,1,-1)",1);		
		valueTest("if(x<3,1,-1)",-1);		
	}

	public void testAssign()  throws ParseException
	{
		valueTest("x=3",3);
		valueTest("y=3+4",7);
		valueTest("z=x+y",10);
		valueTest("a=b=c=z",10);
		valueTest("b",10);
		valueTest("d=f=a-b",0);
	}

						
	public void testDiff() throws ParseException
	{
		simplifyTest("diff(x^2,x)","2 x");
		simplifyTest("diff(x^3,x)","3 x^2");
		simplifyTest("diff(x,x)","1");
		simplifyTest("diff(1,x)","0");
		simplifyTest("diff(x^2+x+1,x)","2 x+1");
		simplifyTest("diff((x+x^2)*(x+x^3),x)","(1+2*x)*(x+x^3)+(x+x^2)*(1+3*x^2)");
		simplifyTest("diff((x+x^2)/(x+x^3),x)","((1+2*x)*(x+x^3)-(x+x^2)*(1+3*x^2))/((x+x^3)*(x+x^3))");
		simplifyTest("diff(sin(x),x)","cos(x)");
		simplifyTest("diff(-(x-5)^3,x)","-(3.0*(x-5.0)^2.0)");
		

		simplifyTest("diff((x+1)^2,x)","2+2*x");
		simplifyTest("diff((x+y)^2,x)","2*(x+y)");
		simplifyTest("diff((x+x^2)^3,x)","3*(x+x^2)^2*(1+2*x)");
		
		simplifyTest("diff(sin(x+1),x)","cos(x+1)");
		simplifyTest("diff(sin(x+x^2),x)","cos(x+x^2)*(1+2*x)");

		simplifyTest("diff(cos(x),x)","-sin(x)"); 	
		simplifyTest("diff(tan(x),x)","1/((cos(x))^2)");

		simplifyTest("diff(sec(x),x)","sec(x)*tan(x)");
		simplifyTest("diff(cosec(x),x)","-cosec(x) * cot(x)");
		simplifyTest("diff(cot(x),x)","-(cosec(x))^2");
		
		simplifyTest("diff(sec(x),x)","sec(x) * tan(x)");
		simplifyTest("diff(cosec(x),x)","-cosec(x) * cot(x)");
		simplifyTest("diff(cot(x),x)","-(cosec(x))^2");
			
		simplifyTest("diff(asin(x),x)","1/(sqrt(1-x^2))");
		simplifyTest("diff(acos(x),x)","-1/(sqrt(1-x^2))");
		simplifyTest("diff(atan(x),x)","1/(1+x^2)");

		simplifyTest("diff(sinh(x),x)","cosh(x)");
		simplifyTest("diff(cosh(x),x)","sinh(x)");
		simplifyTest("diff(tanh(x),x)","1-(tanh(x))^2");

		simplifyTest("diff(asinh(x),x)","1/(sqrt(1+x^2))");
		simplifyTest("diff(acosh(x),x)","1/(sqrt(x^2-1))");
		simplifyTest("diff(atanh(x),x)","1/(1-x^2)");

		simplifyTest("diff(sqrt(x),x)","1/(2 (sqrt(x)))");
		
		simplifyTest("diff(exp(x),x)","exp(x)");
//		simplifyTest("diff(pow(x,y),x)","y*(pow(x,y-1))");
//		simplifyTest("diff(pow(x,y),y)","(ln(x)) (pow(x,y))");
		simplifyTest("diff(ln(x),x)","1/x");
		simplifyTest("diff(log(x),x)","(1/ln(10)) /x");
		simplifyTest("diff(abs(x),x)","abs(x)/x");
		simplifyTest("diff(angle(x,y),x)","y/(x^2+y^2)");
		simplifyTest("diff(angle(x,y),y)","-x/(x^2+y^2)");
		simplifyTest("diff(mod(x,y),x)","1");
		simplifyTest("diff(mod(x,y),y)","0");
		simplifyTest("diff(sum(x,x^2,x^3),x)","sum(1,2 x,3 x^2)");

//		addDiffRule(new PassThroughDiffRule(this,"sum"));
//		addDiffRule(new PassThroughDiffRule(this,"re"));
//		addDiffRule(new PassThroughDiffRule(this,"im"));
//		addDiffRule(new PassThroughDiffRule(this,"rand"));
//		
//		MacroFunction complex = new MacroFunction("complex",2,"x+i*y",xjep);
//		xjep.addFunction("complex",complex);
//		addDiffRule(new MacroFunctionDiffRules(this,complex));
//		
//		addDiffRule(new PassThroughDiffRule(this,"\"<\"",new Comparative(0)));
//		addDiffRule(new PassThroughDiffRule(this,"\">\"",new Comparative(1)));
//		addDiffRule(new PassThroughDiffRule(this,"\"<=\"",new Comparative(2)));
//		addDiffRule(new PassThroughDiffRule(this,"\">=\"",new Comparative(3)));
//		addDiffRule(new PassThroughDiffRule(this,"\"!=\"",new Comparative(4)));
//		addDiffRule(new PassThroughDiffRule(this,"\"==\"",new Comparative(5)));
	}

	public void myAssertEquals(String msg,String actual,String expected)
	{
		if(!actual.equals(expected))
			System.out.println("Error \""+msg+"\" is \""+actual+" should be "+expected+"\"");
		assertEquals("<"+msg+">",expected,actual);
		System.out.println("Success: Value of \""+msg+"\" is \""+actual+"\"");
	}
	*/
	public void testAssignDiff() throws ParseException
	{
		//TODO Used to be an error but new procedure for working with derivs should add test
		//rpTest2(new String[]{"x=2","y=x^5","z=diff(y,x)"});
	}
	
/*
	public void testMatrix() throws ParseException
	{
		j.getSymbolTable().clearValues();
		valueTest("x=2",2);
		valueTest("y=[x^3,x^2,x]","[8.0,4.0,2.0]");
		valueTest("z=diff(y,x)","[12.0,4.0,1.0]");
		valueTest("3*y","[24.0,12.0,6.0]");
		valueTest("y*4","[32.0,16.0,8.0]");
		valueTest("y*z","[[96.0,32.0,8.0],[48.0,16.0,4.0],[24.0,8.0,2.0]]");
		valueTest("z*y","[[96.0,48.0,24.0],[32.0,16.0,8.0],[8.0,4.0,2.0]]");
		valueTest("w=y^z","[-4.0,16.0,-16.0]");
		simplifyTestString("diff(w,x)","[3.0*x^2.0,2.0*x,1.0]^z+y^[6.0*x,2.0,0.0]");
		simplifyTestString("diff(y . z,x)","[3.0*x^2.0,2.0*x,1.0].z+y.[6.0*x,2.0,0.0]");
		valueTest("w.y",0.0);
		valueTest("w.z",0.0);
		valueTest("sqrt(w . z)",0.0);
		valueTest("sqrt([3,4].[3,4])",5.0); // tests result is unwrapped from scaler
		valueTest("y+z","[20.0,8.0,3.0]");
		valueTest("y-z","[-4.0,0.0,1.0]");
		j.getSymbolTable().clearValues();
		// the following two tests insure that ^ is printed correctly
		simplifyTestString("y^z","y^z");
		simplifyTestString("[8.0,4.0,2.0]^[12.0,4.0,1.0]","[8.0,4.0,2.0]^[12.0,4.0,1.0]");
		simplifyTestString("y=[cos(x),sin(x)]","y=[cos(x),sin(x)]");
		simplifyTestString("z=diff(y,x)","z=[-sin(x),cos(x)]");
		valueTest("y.y",1.0);
		valueTest("y.z",0.0);
		valueTest("z.z",1.0);
		j.getSymbolTable().clearValues();
		valueTest("x=[[1,2],[3,4]]","[[1.0,2.0],[3.0,4.0]]");
		valueTest("y=[1,-1]","[1.0,-1.0]");
		valueTest("x*y","[-1.0,-1.0]");			
		valueTest("y*x","[-2.0,-2.0]");
		valueTest("x+[y,y]","[[2.0,1.0],[4.0,3.0]]");	
		valueTest("ele(y,1)","1.0");              // Value: 2.0
		valueTest("ele(y,2)","-1.0");              // Value: 2.0
		valueTest("ele(x,[1,1])","1.0");          // Value: 2.0
		valueTest("ele(x,[1,2])","2.0");          // Value: 2.0
		valueTest("ele(x,[2,1])","3.0");          // Value: 2.0
		valueTest("ele(x,[2,2])","4.0");          // Value: 2.0
	}
	public void testBad() throws ParseException
	{
		if(SHOW_BAD)
		{
			simplifyTest("1&&(1||x)","1");
			simplifyTest("diff(sgn(x),x)","0");	// sgn not implemented
			simplifyTest("diff(re(x+i y),x)","1"); // not smart enought to work out re(i) = 1
			simplifyTest("diff(re(x+i y),y)","0");
			simplifyTest("diff(im(x+i y),x)","0");
			simplifyTest("diff(im(x+i y),y)","1");
			simplifyTest("(x/2)*3","x*1.5");
		}
	}
	*/
}
