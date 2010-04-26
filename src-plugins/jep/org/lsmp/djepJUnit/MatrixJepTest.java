/* @author rich
 * Created on 22-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepJUnit;

import org.lsmp.djep.matrixJep.MatrixJep;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * @author Rich Morris
 * Created on 22-Apr-2005
 */
public class MatrixJepTest extends DJepTest {

	/**
	 * @param name
	 */
	public MatrixJepTest(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	/**
	 * Create a test suite.
	 * @return the TestSuite
	 */
	public static Test suite() {
		return new TestSuite(MatrixJepTest.class);
	}

	/**
	 * Main entry point.
	 * @param args
	 */
	public static void main(String args[]) {
		// Create an instance of this class and analyse the file

		TestSuite suite= new TestSuite(MatrixJepTest.class);
		suite.run(new TestResult());
	}	

	protected void setUp() {
		j = new MatrixJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		//j.setTraverse(true);
		j.setAllowAssignment(true);
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		((MatrixJep) j).addStandardDiffRules();
	}

	public Object calcValue(Node node) throws ParseException
	{
		Node matEqn = ((MatrixJep) j).preprocess(node);
		Object res = ((MatrixJep) j).evaluate(matEqn);
		return res;
	}

	public void valueTest(String expr,double a) throws Exception
	{
		valueTest(expr,new Double(a));
	}

	public void testMatrix() throws Exception
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
	
	public void testLength() throws ParseException,Exception
	{
		valueTest("len(5)","1");
		valueTest("len([1,2,3])","3");
		valueTest("len([[1,2,3],[4,5,6]])","6");
		valueTest("size(5)","1");
		valueTest("size([1,2,3])","3");
		valueTest("size([[1,2,3],[4,5,6]])","[2,3]");
		valueTest("size([[[1,2],[3,4],[5,6]],[[7,8],[9,10],[11,12]]])","[2,3,2]");

		valueTest("diag([1,2,3])","[[1.0,0.0,0.0],[0.0,2.0,0.0],[0.0,0.0,3.0]]");
		valueTest("getdiag([[1,2],[3,4]])","[1.0,4.0]");
		valueTest("trans([[1,2],[3,4]])","[[1.0,3.0],[2.0,4.0]]");
		valueTest("det([[1,2],[3,4]])","-2.0");
		valueTest("det([[1,2,3],[4,5,6],[9,8,9]])","-6.0");
		valueTest("det([[1,2,3],[4,5,6],[7,8,9]])","0.0");
		valueTest("det([[1,2,3,4],[5,6,77,8],[4,3,2,1],[17,9,23,19]])","9100.0");

		valueTest("trace([[1,2],[3,4]])","5.0");
		valueTest("trace([[1,2,3],[4,5,6],[7,8,9]])","15.0");
		valueTest("trace([[1,2,3,4],[5,6,7,8],[9,10,11,12],[13,14,15,16]])","34.0");

		valueTest("vsum([[1,2],[3,4]])","10.0");
		valueTest("vsum([1,2,3])","6.0");
		
		valueTest("Map(x^3,x,[1,2,3])","[1.0,8.0,27.0]");
		valueTest("Map(x*y,[x,y],[1,2,3],[4,5,6])","[4.0,10.0,18.0]");
		valueTest("Map(if(x>0,x,0),x,[-2,-1,0,1,2])","[0.0,0.0,0.0,1.0,2.0]");
		valueTest("Map(abs(x),x,[[-2,-1],[1,2]])","[[2.0,1.0],[1.0,2.0]]");
	}

	public void testDotInName() throws ParseException, Exception {
	}
	
	public void testVecCmp() throws Exception {
		valueTest("[1,2,3]==[1,2,3]",1);
		valueTest("[1,2,3]==[1,2,4]",0);
	}

	public void testVectorSum() throws Exception {
		valueTest("Sum([x,x^2],x,1,10)","[55.0,385.0]");
	}
	
	public void testTgtDev() throws Exception
	{
		parsePreprocSimp("v=[x,x^2.0,x^3.0]");
		parsePreprocSimp("l=7");
		parsePreprocSimp("v/l");
		parsePreprocSimp("dv=diff(v,x)");
		parsePreprocSimp("dotprod=dv.dv");
		parsePreprocSimp("length=sqrt(dotprod)");
		parsePreprocSimp("v+y*dv/length");
	}

	public void testComplexMatricies() throws Exception {
		valueTest("v=[1+i,1-2i]","[(1.0, 1.0),(1.0, -2.0)]");
		valueTest("vsum(v)","(2.0, -1.0)");
		valueTest("m=[[1+i,-1+i],[1-i,-1-i]]","[[(1.0, 1.0),(-1.0, 1.0)],[(1.0, -1.0),(-1.0, -1.0)]]");
		valueTest("vsum(m)","(0.0, 0.0)");
		valueTest("trace(m)","(0.0, 0.0)");
		valueTest("m*v","[(1.0, 5.0),(-1.0, 1.0)]");
		valueTest("v*m","[(-1.0, -1.0),(-5.0, 1.0)]");
		valueTest("trans(m)","[[(1.0, 1.0),(1.0, -1.0)],[(-1.0, 1.0),(-1.0, -1.0)]]");
		valueTest("det(m)","(0.0, -4.0)");
	}

/* TODO GenMat Not jet implemented for MatrixJep (can it be done?)
	public void testGenMatEle() throws Exception
	{
	    System.out.println("The following caused a problem as ele only acepted Double arguments");
	    valueTest("m=[1,2,3]","[1.0,2.0,3.0]");
	    valueTest("GenMat(3,ele(m,n)*10,n)","[10.0,20.0,30.0]");
	}
*/
}
