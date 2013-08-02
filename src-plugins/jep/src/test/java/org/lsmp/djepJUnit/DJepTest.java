/* @author rich
 * Created on 22-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepJUnit;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.lsmp.djep.djep.DJep;
import org.lsmp.djep.djep.DSymbolTable;
import org.nfunk.jep.Node;
import org.nfunk.jep.ParseException;

/**
 * @author Rich Morris
 * Created on 22-Apr-2005
 */
public class DJepTest extends XJepTest {

	public DJepTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(DJepTest.class);
	}

	public static void main(String[] args) {
		TestSuite suite= new TestSuite(DJepTest.class);
		suite.run(new TestResult());
	}
	
	protected void setUp() {
		j = new DJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		//j.setTraverse(true);
		j.setAllowAssignment(true);
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
		((DJep) j).addStandardDiffRules();
	}

	public void testDiff() throws ParseException
	{
		System.out.println("\nTesting assignment");

		simplifyTest("diff(x^2,x)","2 x");
		simplifyTest("diff(x^3,x)","3 x^2");
		simplifyTest("diff(x,x)","1");
		simplifyTest("diff(1,x)","0");
		simplifyTest("diff(x^2+x+1,x)","2 x+1");
		simplifyTest("diff((x+x^2)*(x+x^3),x)","(1+2*x)*(x+x^3)+(x+x^2)*(1+3*x^2)");
		simplifyTest("diff((x+x^2)/(x+x^3),x)","((1+2*x)*(x+x^3)-(x+x^2)*(1+3*x^2))/((x+x^3)*(x+x^3))");

		simplifyTest("diff(y^x,x)","y^x*ln(y)");
		simplifyTest("diff(e^x,x)","e^x*ln(e)");

		simplifyTest("diff(sin(x),x)","cos(x)");

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
		simplifyTest("diff(ln(x),x)","1/x");
		simplifyTest("diff(log(x),x)","(1/ln(10)) /x");
		simplifyTest("diff(abs(x),x)","abs(x)/x");
		simplifyTest("diff(atan2(y,x),x)","y/(y^2+x^2)");
		simplifyTest("diff(atan2(y,x),y)","-x/(y^2+x^2)");
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

	public void testAssignDiff() throws Exception
	{
		System.out.println("\nTesting assignment and diff");
		simplifyTestString("y=x^5","y=x^5.0");
		simplifyTestString("z=diff(y,x)","z=5.0*x^4.0");
		Node n1 = ((DSymbolTable) j.getSymbolTable()).getPartialDeriv("y",new String[]{"x"}).getEquation();
		myAssertEquals("dy/dx",((DJep) j).toString(n1),"5.0*x^4.0");
		simplifyTestString("w=diff(z,x)","w=20.0*x^3.0");
		Node n2 = ((DSymbolTable) j.getSymbolTable()).getPartialDeriv("y",new String[]{"x","x"}).getEquation();
		myAssertEquals("d^2y/dxdx",((DJep) j).toString(n2),"20.0*x^3.0");
		valueTest("x=2",2);
		valueTest("y",32); // x^5
		valueTest("z",80); // 5 x^4 
		valueTest("w",160); // 20 x^3
		simplifyTestString("diff(ln(y),x)","(1.0/y)*5.0*x^4.0");

	}

	public void testChainedVaraibles() throws Exception
	{
		simplifyTestString("x=5","x=5.0");
		simplifyTestString("y=x","y=x");
		simplifyTestString("z=y","z=y");
		simplifyTestString("w=diff(z,x)","w=1.0");
	}
}
