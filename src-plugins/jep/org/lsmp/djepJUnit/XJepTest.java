/* @author rich
 * Created on 17-Apr-2005
 *
 * See LICENSE.txt for license information.
 */
package org.lsmp.djepJUnit;

import java.text.NumberFormat;
import java.util.Vector;

import org.nfunk.jep.*;
import org.nfunk.jep.type.Complex;
import org.lsmp.djep.xjep.*;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * @author Rich Morris
 * Created on 17-Apr-2005
 */
public class XJepTest extends JepTest {

	public XJepTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(XJepTest.class);
	}

	public static void main(String[] args) {
		TestSuite suite= new TestSuite(XJepTest.class);
		suite.run(new TestResult());
	}
	
	protected void setUp() {
		j = new XJep();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		//j.setTraverse(true);
		j.setAllowAssignment(true);
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
	}
	
	public String parsePreprocSimp(String expr) throws ParseException
	{
		XJep xj = (XJep) j;
		Node node = xj.parse(expr);
		Node matEqn = xj.preprocess(node);
		Node simp = xj.simplify(matEqn);
		String res = xj.toString(simp);
		return res;
	}

	public void simplifyTestString(String expr,String expected) throws ParseException
	{
		XJep xj = (XJep) j;

		Node node = xj.parse(expr);
		Node processed = xj.preprocess(node);
		Node simp = xj.simplify(processed);
		String res = xj.toString(simp);
		
		if(!expected.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+expected+"\"");
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Success: Value of \""+expr+"\" is \""+res+"\"");
	}

	public void simplifyTest(String expr,String expected) throws ParseException
	{
		XJep xj = (XJep) j;
		
		Node node2 = xj.parse(expected);
		Node processed2 = xj.preprocess(node2);
		Node simp2 = xj.simplify(processed2);
		String res2 = xj.toString(simp2);

		simplifyTestString(expr,res2);
	}

	public Node parseProcSimpEval(String expr,Object expected) throws ParseException,Exception
	{
		XJep xj = (XJep) j;

		Node node = xj.parse(expr);
		Node processed = xj.preprocess(node);
		Node simp = xj.simplify(processed);
		Object res = xj.evaluate(simp);
		
		myAssertEquals(expr,expected,res);
		return simp;
	}

	public void testLogical() throws Exception {
		super.testLogical();
		OperatorSet opSet = j.getOperatorSet();
		if(!((XOperator) opSet.getMultiply()).isDistributiveOver(opSet.getAdd()))
			fail("* should be distrib over +");
		if(((XOperator) opSet.getMultiply()).isDistributiveOver(opSet.getDivide()))
			fail("* should not be distrib over /");
		if(((XOperator) opSet.getMultiply()).getPrecedence() > ((XOperator) opSet.getAdd()).getPrecedence())
			fail("* should have a lower precedence than +");

	}
	
	public void testPrint() throws ParseException
	{
		simplifyTestString("(a+b)+c","a+b+c");
		simplifyTestString("(a-b)+c","a-b+c");
		simplifyTestString("(a+b)-c","a+b-c"); 
		simplifyTestString("(a-b)-c","a-b-c");

		simplifyTestString("a+(b+c)","a+b+c");
		simplifyTestString("a-(b+c)","a-(b+c)");
		simplifyTestString("a+(b-c)","a+b-c");   
		simplifyTestString("a-(b-c)","a-(b-c)");

		simplifyTestString("(a*b)*c","a*b*c");
		simplifyTestString("(a/b)*c","(a/b)*c");
		simplifyTestString("(a*b)/c","a*b/c"); 
		simplifyTestString("(a/b)/c","(a/b)/c");

		simplifyTestString("a*(b*c)","a*b*c");
		simplifyTestString("a/(b*c)","a/(b*c)");
		simplifyTestString("a*(b/c)","a*b/c");
		simplifyTestString("a/(b/c)","a/(b/c)");

		simplifyTestString("a=(b=c)","a=b=c");
		//simplifyTestString("(a=b)=c","a/(b/c)");

		simplifyTestString("(a*b)+c","a*b+c");
		simplifyTestString("(a+b)*c","(a+b)*c");
		simplifyTestString("a*(b+c)","a*(b+c)"); 
		simplifyTestString("a+(b*c)","a+b*c");

		simplifyTestString("(a||b)||c","a||b||c");
		simplifyTestString("(a&&b)||c","a&&b||c");
		simplifyTestString("(a||b)&&c","(a||b)&&c"); 
		simplifyTestString("(a&&b)&&c","a&&b&&c");

		simplifyTestString("a||(b||c)","a||b||c");
		simplifyTestString("a&&(b||c)","a&&(b||c)");
		simplifyTestString("a||(b&&c)","a||b&&c");   
		simplifyTestString("a&&(b&&c)","a&&b&&c");
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

		simplifyTest("(x/2)*3","x*1.5");

	}

	public void testMacroFun() throws Exception
	{
		j.addFunction("zap",new MacroFunction("zap",1,"x*(x-1)/2",(XJep) j));
		valueTest("zap(10)",45);
	}

	public void testVariableReuse() throws Exception
	{
		XJep xj = (XJep) j;
		System.out.println("\nTesting variable reuse");
		parseProcSimpEval("x=3",new Double(3));
		Node node13 = parseProcSimpEval("y=x^2",new Double(9));
		Node node15 = parseProcSimpEval("z=y+x",new Double(12));
			
		j.setVarValue("x",new Double(4));
		System.out.println("j.setVarValue(\"x\",new Double(4));");
		System.out.println("j.getVarValue(y): "+j.getVarValue("y"));
		myAssertEquals("eval y eqn","16.0",j.evaluate(node13).toString());
		System.out.println("j.getVarValue(y): "+j.getVarValue("y"));
		myAssertEquals("eval z eqn","20.0",j.evaluate(node15).toString());

//		j.getSymbolTable().clearValues();
		j.setVarValue("x",new Double(5));
		System.out.println("j.setVarValue(\"x\",new Double(5));");
		myAssertEquals("j.findVarValue(y)","25.0",xj.calcVarValue("y").toString());
		myAssertEquals("j.findVarValue(z)","30.0",xj.calcVarValue("z").toString());

		j.getSymbolTable().clearValues();
		j.setVarValue("x",new Double(6));
		System.out.println("j.setVarValue(\"x\",new Double(6));");
		myAssertEquals("j.findVarValue(z)","42.0",xj.calcVarValue("z").toString());
		myAssertEquals("j.findVarValue(y)","36.0",xj.calcVarValue("y").toString());

		parseProcSimpEval("x=7",new Double(7));
		myAssertEquals("eval y eqn","49.0",j.evaluate(node13).toString());
		myAssertEquals("eval z eqn","56.0",j.evaluate(node15).toString());
	}

	public void testReentrant() throws ParseException,Exception
	{
		XJep xj = (XJep) j;

		xj.restartParser("x=1; // semi-colon; in comment\n y=2; z=x+y;");
		Node node = xj.continueParsing();
		myAssertEquals("x=1; ...","1.0",calcValue(node).toString());
		node = xj.continueParsing();
		myAssertEquals("..., y=2; ...","2.0",calcValue(node).toString());
		node = xj.continueParsing();
		myAssertEquals("..., z=x+y;","3.0",calcValue(node).toString());
		node = xj.continueParsing();
		assertNull("empty string ",node);
	}
	
	
	public void testFormat() throws ParseException
	{
		XJep xj = (XJep) j;

		NumberFormat format = NumberFormat.getInstance();
		xj.getPrintVisitor().setNumberFormat(format);
		format.setMaximumFractionDigits(3);
		format.setMinimumFractionDigits(0);
		
		String s1 = "[10,0,0.1,0.11,0.111,0.1111]";
		String r1 = xj.toString(j.parse(s1));
		String s2 = "[0.9,0.99,0.999,0.9999]";
		String r2 = xj.toString(j.parse(s2));
		this.myAssertEquals(s1,r1,"[10,0,0.1,0.11,0.111,0.111]");
		this.myAssertEquals(s2,r2,"[0.9,0.99,0.999,1]");
		
		//j.addComplex();
		xj.println(j.parse("[0,1,i,1+i]"));
		xj.getPrintVisitor().setMode(PrintVisitor.COMPLEX_I,true);
		xj.println(xj.simplify(j.parse("(2+i)+(1+i)")));
		//j.parseExpression("(2+i)+(1+i)");
		Complex c = (Complex) calcValue("(2+i)+(1+i)");
		System.out.println(c.toString(format,true));
	}

	public void testVarInEqn() throws Exception
	{
		XJep xj = (XJep) j;

		Node n1 = j.parse("a+b+c+d");
		Vector v = xj.getVarsInEquation(n1,new Vector());
		assertTrue("Does not contain a",v.contains(j.getSymbolTable().getVar("a")));
		assertTrue("Does not contain b",v.contains(j.getSymbolTable().getVar("b")));
		assertTrue("Does not contain c",v.contains(j.getSymbolTable().getVar("c")));
		assertTrue("Does not contain d",v.contains(j.getSymbolTable().getVar("d")));

		xj.preprocess(j.parse("x=a+b t"));
		xj.preprocess(j.parse("y=c+d t"));
		xj.preprocess(j.parse("f=x*y"));
		xj.preprocess(j.parse("g=x+y"));
		Node n2 = xj.preprocess(j.parse("f+g"));

		Vector v2 = xj.recursiveGetVarsInEquation(n2,new Vector());
		Vector v3 = new Vector();
		v3.add(j.getVar("a"));
		v3.add(j.getVar("b"));
		v3.add(j.getVar("t"));
		v3.add(j.getVar("x"));
		v3.add(j.getVar("c"));
		v3.add(j.getVar("d"));
		v3.add(j.getVar("y"));
		v3.add(j.getVar("f"));
		v3.add(j.getVar("g"));

		System.out.println(v2.toString());
		assertEquals("Bad element seq",v3,v2);
	}

	public void testUndecVar() throws ParseException {
		j.setAllowUndeclared(true);
		Node node1 = ((XJep) j).parse("zap * biff * gosh");
		((XJep) j).preprocess(node1);
	}

	public void testSum() throws Exception
	{
		valueTest("Sum(x,x,1,10)",55);
		valueTest("Sum(x^2,x,1,5)",55);
		valueTest("Product(x,x,1,5)",120);
		valueTest("Min(x^2,x,1,5)",1);
		valueTest("Max(x^2,x,1,5)",25);
		valueTest("MinArg(x^2,x,1,5)",1);
		valueTest("MaxArg(x^2,x,1,5)",5);
	}

	public void testHex() throws Exception
	{
	    valueTest("toHex(0)","0x0");
	    valueTest("toHex(0,1)","0x0.0");
	    valueTest("toHex(0,2)","0x0.00");

	    valueTest("toHex(1)","0x1");
	    valueTest("toHex(1,1)","0x1.0");
	    valueTest("toHex(1,2)","0x1.00");

	    valueTest("toHex(-1)","-0x1");
	    valueTest("toHex(-1,1)","-0x1.0");
	    valueTest("toHex(-1,2)","-0x1.00");
	    
	    valueTest("toHex(7)","0x7");
	    valueTest("toHex(7,1)","0x7.0");
	    valueTest("toHex(7,2)","0x7.00");

	    valueTest("toHex(-7)","-0x7");
	    valueTest("toHex(-7,1)","-0x7.0");
	    valueTest("toHex(-7,2)","-0x7.00");

	    valueTest("toHex(8)","0x8");
	    valueTest("toHex(8,1)","0x8.0");
	    valueTest("toHex(8,2)","0x8.00");

	    valueTest("toHex(-8)","-0x8");
	    valueTest("toHex(-8,1)","-0x8.0");
	    valueTest("toHex(-8,2)","-0x8.00");

	    valueTest("toHex(10)","0xa");
	    valueTest("toHex(10,1)","0xa.0");
	    valueTest("toHex(10,2)","0xa.00");

	    valueTest("toHex(-10)","-0xa");
	    valueTest("toHex(-10,1)","-0xa.0");
	    valueTest("toHex(-10,2)","-0xa.00");

	    valueTest("toHex(15)","0xf");
	    valueTest("toHex(15,1)","0xf.0");
	    valueTest("toHex(15,2)","0xf.00");

	    valueTest("toHex(-15)","-0xf");
	    valueTest("toHex(-15,1)","-0xf.0");
	    valueTest("toHex(-15,2)","-0xf.00");

	    valueTest("toHex(16)","0x10");
	    valueTest("toHex(16,1)","0x10.0");
	    valueTest("toHex(16,2)","0x10.00");

	    valueTest("toHex(-16)","-0x10");
	    valueTest("toHex(-16,1)","-0x10.0");
	    valueTest("toHex(-16,2)","-0x10.00");

	    valueTest("toHex(17)","0x11");
	    valueTest("toHex(17,1)","0x11.0");
	    valueTest("toHex(17,2)","0x11.00");

	    valueTest("toHex(-17)","-0x11");
	    valueTest("toHex(-17,1)","-0x11.0");
	    valueTest("toHex(-17,2)","-0x11.00");

	    valueTest("toHex(256)","0x100");
	    valueTest("toHex(256,1)","0x100.0");
	    valueTest("toHex(256,2)","0x100.00");

	    valueTest("toHex(-256)","-0x100");
	    valueTest("toHex(-256,1)","-0x100.0");
	    valueTest("toHex(-256,2)","-0x100.00");

	    valueTest("toHex(1/16)","0x0");
	    valueTest("toHex(1/16,1)","0x0.1");
	    valueTest("toHex(1/16,2)","0x0.10");

	    valueTest("toHex(-1/16)","-0x0");
	    valueTest("toHex(-1/16,1)","-0x0.1");
	    valueTest("toHex(-1/16,2)","-0x0.10");

	    valueTest("toHex(7/16)","0x0");
	    valueTest("toHex(7/16,1)","0x0.7");
	    valueTest("toHex(7/16,2)","0x0.70");

	    valueTest("toHex(-7/16)","-0x0");
	    valueTest("toHex(-7/16,1)","-0x0.7");
	    valueTest("toHex(-7/16,2)","-0x0.70");

	    valueTest("toHex(8/16)","0x1");
	    valueTest("toHex(8/16,1)","0x0.8");
	    valueTest("toHex(8/16,2)","0x0.80");

	    valueTest("toHex(-8/16)","-0x1");
	    valueTest("toHex(-8/16,1)","-0x0.8");
	    valueTest("toHex(-8/16,2)","-0x0.80");

	    valueTest("toHex(10/16)","0x1");
	    valueTest("toHex(10/16,1)","0x0.a");
	    valueTest("toHex(10/16,2)","0x0.a0");

	    valueTest("toHex(-10/16)","-0x1");
	    valueTest("toHex(-10/16,1)","-0x0.a");
	    valueTest("toHex(-10/16,2)","-0x0.a0");

	    valueTest("toHex(15/16)","0x1");
	    valueTest("toHex(15/16,1)","0x0.f");
	    valueTest("toHex(15/16,2)","0x0.f0");

	    valueTest("toHex(-15/16)","-0x1");
	    valueTest("toHex(-15/16,1)","-0x0.f");
	    valueTest("toHex(-15/16,2)","-0x0.f0");
	    
	    valueTest("toHex(17/16)","0x1");
	    valueTest("toHex(17/16,1)","0x1.1");
	    valueTest("toHex(17/16,2)","0x1.10");

	    valueTest("toHex(-17/16)","-0x1");
	    valueTest("toHex(-17/16,1)","-0x1.1");
	    valueTest("toHex(-17/16,2)","-0x1.10");
	    
	    valueTest("toHex(31/16)","0x2");
	    valueTest("toHex(31/16,1)","0x1.f");
	    valueTest("toHex(31/16,2)","0x1.f0");

	    valueTest("toHex(-31/16)","-0x2");
	    valueTest("toHex(-31/16,1)","-0x1.f");
	    valueTest("toHex(-31/16,2)","-0x1.f0");
	    
	    valueTest("toHex(1/256)","0x0");
	    valueTest("toHex(1/256,1)","0x0.0");
	    valueTest("toHex(1/256,2)","0x0.01");

	    valueTest("toHex(-1/256)","-0x0");
	    valueTest("toHex(-1/256,1)","-0x0.0");
	    valueTest("toHex(-1/256,2)","-0x0.01");

	    valueTest("toHex(15/256)","0x0");
	    valueTest("toHex(15/256,1)","0x0.1");
	    valueTest("toHex(15/256,2)","0x0.0f");

	    valueTest("toHex(-15/256)","-0x0");
	    valueTest("toHex(-15/256,1)","-0x0.1");
	    valueTest("toHex(-15/256,2)","-0x0.0f");

	    valueTest("toHex(17/256)","0x0");
	    valueTest("toHex(17/256,1)","0x0.1");
	    valueTest("toHex(17/256,2)","0x0.11");

	    valueTest("toHex(-17/256)","-0x0");
	    valueTest("toHex(-17/256,1)","-0x0.1");
	    valueTest("toHex(-17/256,2)","-0x0.11");

	    valueTest("toHex(127/256)","0x0");
	    valueTest("toHex(127/256,1)","0x0.8");
	    valueTest("toHex(127/256,2)","0x0.7f");

	    valueTest("toHex(-127/256)","-0x0");
	    valueTest("toHex(-127/256,1)","-0x0.8");
	    valueTest("toHex(-127/256,2)","-0x0.7f");

	    valueTest("toHex(128/256)","0x1");
	    valueTest("toHex(128/256,1)","0x0.8");
	    valueTest("toHex(128/256,2)","0x0.80");

	    valueTest("toHex(-128/256)","-0x1");
	    valueTest("toHex(-128/256,1)","-0x0.8");
	    valueTest("toHex(-128/256,2)","-0x0.80");

	    valueTest("toHex(240/256)","0x1");
	    valueTest("toHex(240/256,1)","0x0.f");
	    valueTest("toHex(240/256,2)","0x0.f0");

	    valueTest("toHex(-240/256)","-0x1");
	    valueTest("toHex(-240/256,1)","-0x0.f");
	    valueTest("toHex(-240/256,2)","-0x0.f0");

	    valueTest("toHex(248/256)","0x1");
	    valueTest("toHex(248/256,1)","0x1.0");
	    valueTest("toHex(248/256,2)","0x0.f8");

	    valueTest("toHex(-248/256)","-0x1");
	    valueTest("toHex(-248/256,1)","-0x1.0");
	    valueTest("toHex(-248/256,2)","-0x0.f8");
	    
	    valueTest("toHex(1/4096)","0x0");
	    valueTest("toHex(1/4096,1)","0x0.0");
	    valueTest("toHex(1/4096,2)","0x0.00");
	    valueTest("toHex(1/4096,3)","0x0.001");
	    valueTest("toHex(1/4096,4)","0x0.0010");

	    valueTest("toHex(1+1/4096)","0x1");
	    valueTest("toHex(1+1/4096,1)","0x1.0");
	    valueTest("toHex(1+1/4096,2)","0x1.00");
	    valueTest("toHex(1+1/4096,3)","0x1.001");
	    valueTest("toHex(1+1/4096,4)","0x1.0010");
	    
	    XJep xj = (XJep) j;
	    BaseFormat bf = new BaseFormat(16,"0x");
	    bf.setMaximumFractionDigits(0);
	    xj.getPrintVisitor().setNumberFormat(bf);
	    String st = "10 x+15 x^2 - 16 x^3 + 32 x^4 - 256 x^5";
	    Node n = xj.parse(st);
	    String res = xj.toString(n);
	    myAssertEquals(st,"0xa*x+0xf*x^0x2-0x10*x^0x3+0x20*x^0x4-0x100*x^0x5",res);
}
	public void testDefine() throws Exception
	{
	    XJep xj = (XJep) j;
	    //Node n = xj.parse("Define(\"sumToX\",1,\"x*(x+1)/2\")");
	    //xj.preprocess(n);
	    //valueTest("sumToX(4)",10);
	}
	public void testBad() throws Exception
	{
		if(SHOW_BAD)
		{
			valueTest("recurse = recurse+1",null);
			simplifyTest("1&&(1||x)","1");
			simplifyTest("diff(sgn(x),x)","0");	// sgn not implemented
			simplifyTest("diff(re(x+i y),x)","1"); // not smart enough to work out re(i) = 1
			simplifyTest("diff(re(x+i y),y)","0");
			simplifyTest("diff(im(x+i y),x)","0");
			simplifyTest("diff(im(x+i y),y)","1");
		}
	}

}
