package org.lsmp.djepJUnit;

import junit.framework.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;
import org.lsmp.djep.xjep.*;
import org.lsmp.djep.djep.*;
import java.text.NumberFormat;
import org.lsmp.djep.sjep.*;
/* @author rich
 * Created on 19-Nov-2003
 */

/**
 * @author Rich Morris
 * Created on 19-Nov-2003
 */
public class SJepTest extends TestCase {
	DJep j;
	PolynomialCreator pc;
	
	public static final boolean SHOW_BAD=false;
	
	public SJepTest(String name) {
		super(name);
	}

	public static void main(String args[]) {
		// Create an instance of this class and analyse the file

		TestSuite suite= new TestSuite(SJepTest.class);
//		DJepTest jt = new DJepTest("DJepTest");
//		jt.setUp();
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
		j.addStandardDiffRules();
		pc = new PolynomialCreator(j);
	}

	public static Test suite() {
		return new TestSuite(SJepTest.class);
	}

	public void myAssertEquals(String msg,String actual,String expected)
	{
		if(!actual.equals(expected))
			System.out.println("Error \""+msg+"\" is \""+actual+" should be "+expected+"\"");
		assertEquals("<"+msg+">",expected,actual);
		System.out.println("Success: Value of \""+msg+"\" is \""+actual+"\"");
	}
	
	public void assertPolynomialEquals(String s1,String s2) throws ParseException
	{
		Node n1= j.parse(s1);
		PNodeI p1 = pc.createPoly(n1);
		PNodeI e1 = p1.expand();
		Node n2 = j.parse(s2);
		PNodeI p2 = pc.createPoly(n2);
		PNodeI e2 = p2.expand();
		if(e1.equals(e2)){
			System.out.println("Sucess: \""+s1+"\" equals \""+s2+"\"");
		}else{
			System.out.println("Error: \""+s1+"\" is not equal to \""+s2+"\"");
			assertTrue("<"+s1+"> should be equal to <"+s2+"> it is not",false);
		}
	}
	/** just test JUnit working OK */
	public void testGood()
	{
		assertEquals(1,1);
	}

	public void valueTest(String expr,double dub) throws Exception
	{
		valueTest(expr,new Double(dub));
	}
	public void valueTest(String expr,Object expected) throws Exception
	{
		Node node = j.parse(expr);
		Node n2 = j.preprocess(node);
		Object res = j.evaluate(n2);
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Success value of <"+expr+"> is "+res);
	}
	public void complexValueTest(String expr,Complex expected,double tol) throws Exception
	{
		Node node = j.parse(expr);
		Node n2 = j.preprocess(node);
		Object res = j.evaluate(n2);
		assertTrue("<"+expr+"> expected: <"+expected+"> but was <"+res+">",
			expected.equals((Complex) res,tol));
		System.out.println("Success value of <"+expr+"> is "+res);
	}

	public Object calcValue(String expr) throws ParseException
	{
		Node node = j.parse(expr);
		Node n2 = j.preprocess(node);
		Object res = j.evaluate(n2);
		return res;
	}
	
	public void simplifyTest(String expr,String expected) throws ParseException
	{
		Node node = j.parse(expr);
		Node processed = j.preprocess(node);
		PNodeI poly = pc.createPoly(processed);
		String res = poly.toString();
		
		Node node2 = j.parse(expected);
		Node processed2 = j.preprocess(node2);
		PNodeI poly2 = pc.createPoly(processed2);
		String res2 = poly2.toString();

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
		Node node = j.parse(expr);
		Node processed = j.preprocess(node);
		PNodeI poly = pc.createPoly(processed);
		String res = poly.toString();
		
		if(!expected.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+expected+"\"");
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Sucess: Value of \""+expr+"\" is \""+res+"\"");
			
//		System.out.print("Full Brackets:\t");
//		j.pv.setFullBrackets(true);
//		j.pv.println(simp);
//		j.pv.setFullBrackets(false);

	}

	public void expandTestString(String expr,String expected) throws ParseException
	{
		Node node = j.parse(expr);
		Node processed = j.preprocess(node);
		PNodeI poly = pc.createPoly(processed);
		PNodeI expand = poly.expand();
		String res = expand.toString();
		
		if(!expected.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+expected+"\"");
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Sucess: Value of \""+expr+"\" is \""+res+"\"");
			
//		System.out.print("Full Brackets:\t");
//		j.pv.setFullBrackets(true);
//		j.pv.println(simp);
//		j.pv.setFullBrackets(false);

	}
/*	
	public Node parseProcSimpEval(String expr,Object expected) throws ParseException,Exception
	{
		Node node = j.parse(expr);
		Node processed = j.preprocess(node);
		Node simp = j.simplify(processed);
		Object res = j.evaluate(simp);
		
		if(!expected.equals(res))		
			System.out.println("Error: Value of \""+expr+"\" is \""+res+"\" should be \""+expected+"\"");
		assertEquals("<"+expr+">",expected,res);
		System.out.println("Sucess: Value of \""+expr+"\" is \""+res+"\"");
		return simp;
	}
*/

	public void testSimpleSum() throws Exception
	{
		valueTest("1+2",3);		
		valueTest("2*6+3",15);		
		valueTest("2*(6+3)",18);
	}
	
	public void testOperators() throws Exception
	{
		OperatorSet opSet = j.getOperatorSet();
		if(!((XOperator) opSet.getMultiply()).isDistributiveOver(opSet.getAdd()))
			fail("* should be distrib over +");
		if(((XOperator) opSet.getMultiply()).isDistributiveOver(opSet.getDivide()))
			fail("* should not be distrib over /");
		if(((XOperator) opSet.getMultiply()).getPrecedence() > ((XOperator) opSet.getAdd()).getPrecedence())
			fail("* should have a lower precedence than +");

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
	
	public void testPrint() throws ParseException
	{
		simplifyTestString("(a+b)+c","a+b+c");
		simplifyTestString("(a-b)+c","a-b+c");
		simplifyTestString("(a+b)-c","a+b-c"); 
		simplifyTestString("(a-b)-c","a-b-c");

		simplifyTestString("a+(b+c)","a+b+c");
		simplifyTestString("a-(b+c)","a-b-c"); //
		simplifyTestString("a+(b-c)","a+b-c");   
		simplifyTestString("a-(b-c)","a-b+c");

		simplifyTestString("(a*b)*c","a*b*c");
		simplifyTestString("(a/b)*c","a*c/b");
		simplifyTestString("(a*b)/c","a*b/c"); 
		simplifyTestString("(a/b)/c","a/(b*c)");

		simplifyTestString("a*(b*c)","a*b*c");
		simplifyTestString("a/(b*c)","a/(b*c)");
		simplifyTestString("a*(b/c)","a*b/c");
		simplifyTestString("a/(b/c)","a*c/b");

		//simplifyTestString("a=(b=c)","a=b=c");
		//simplifyTestString("(a=b)=c","a/(b/c)");

		simplifyTestString("(a*b)+c","a*b+c");
		simplifyTestString("(a+b)*c","(a+b)*c");
		simplifyTestString("a*(b+c)","a*(b+c)"); 
		simplifyTestString("a+(b*c)","a+b*c");

//		simplifyTestString("(a||b)||c","a||b||c");
//		simplifyTestString("(a&&b)||c","a&&b||c");
//		simplifyTestString("(a||b)&&c","(a||b)&&c"); 
//		simplifyTestString("(a&&b)&&c","a&&b&&c");

//		simplifyTestString("a||(b||c)","a||b||c");
//		simplifyTestString("a&&(b||c)","a&&(b||c)");
//		simplifyTestString("a||(b&&c)","a||b&&c");   
//		simplifyTestString("a&&(b&&c)","a&&b&&c");
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
//		simplifyTest("3*(x+2)","6+3*x");
//		simplifyTest("3*(2+x)","6+3*x");
//		simplifyTest("(3+2)*x","5*x");
//		simplifyTest("(3+x)*2","6+2*x");
//		simplifyTest("(x+3)*2","6+x*2");

		simplifyTest("x*(3-2)","x");
//		simplifyTest("3*(x-2)","-6+3*x");
//		simplifyTest("3*(2-x)","6-3*x");
		simplifyTest("(3-2)*x","x");
//		simplifyTest("(3-x)*2","6-2*x");
//		simplifyTest("(x-3)*2","-6+2*x");

//		simplifyTest("3+(x/4)","3+x/4");
//		simplifyTest("2*(x/4)","0.5*x");
//		simplifyTest("(2*(3+(x/4)))","6+0.5*x");
//		simplifyTest("1+(2*(3+(x/4)))","7+0.5*x");
//		simplifyTest("((3+(x/4))*2)+1","7+0.5*x");

		simplifyTest("x*x","x^2");
		simplifyTest("x*x*x","x^3");
		simplifyTest("(x^3)*(x^4)","x^7");
		simplifyTest("(x^4)/(x^3)","x");
		simplifyTest("(x^3)/(x^4)","1/x");
		simplifyTest("(x^2)/(x^4)","1/x^2");
		simplifyTestString("1/x","1/x");
		simplifyTestString("-1/x","-1/x");
		simplifyTestString("2/x","2/x");
		simplifyTestString("-2/x","-2/x");
		simplifyTestString("(1+x)*(1+x)","(1+x)^2");
		simplifyTestString("(1+x)/(1+x)","1");
		simplifyTest("2*x+x","3*x");
		simplifyTest("2*x+3*x","5*x");
		simplifyTest("5*x-3*x","2*x");
		simplifyTest("3*x-5*x","-2*x");
		simplifyTest("3*x-x","2*x");
		simplifyTest("(2*x+x)^3","27*x^3");
	}

	public void testPolySimp() throws ParseException,Exception
	{
		Node n1 = j.parse("(1.0+2.0*x+x^2.0)*(1.0+2.0*x+x^2.0)");
		pc.createPoly(n1);		

		expandTestString("(a+b)*(c+d)","a*c+a*d+b*c+b*d");
		expandTestString("a*c+a*d+b*c+b*d","a*c+a*d+b*c+b*d");
		expandTestString("(a+b)*(a+b)","2*a*b+a^2+b^2");
		expandTestString("(a-b)*(a-b)","-2*a*b+a^2+b^2");
		expandTestString("(x+7.6)*(x+5.8832)*(x-55.12)","-2464.5430784-698.4816639999999*x-41.636799999999994*x^2+x^3");
		simplifyTestString("(a+b)^0","1");
		simplifyTestString("(a-b)^0","1");
		simplifyTestString("(a+b)^1","a+b");
		simplifyTestString("(a-b)^1","a-b");
		expandTestString("(a+b)^2","2*a*b+a^2+b^2");
		expandTestString("(a-b)^2","-2*a*b+a^2+b^2");
		expandTestString("(a+b)^3","3*a*b^2+3*a^2*b+a^3+b^3");
		expandTestString("(a-b)^3","3*a*b^2-3*a^2*b+a^3-b^3");
		expandTestString("1+x+x^2+x*y+y^2","1+x+x*y+x^2+y^2");
		expandTestString("(5*x+3*y)^2","30*x*y+25*x^2+9*y^2");

		j.getPrintVisitor().setMaxLen(80);
		Node Q8node = j.parse("(xx^2+yy^2+zz^2+ww^2)^4");
		Node Q8expand = pc.expand(Q8node);
		j.println(Q8expand);
		expandTestString("(xx^2+yy^2+zz^2+ww^2)^4",
		"24*ww^2*xx^2*yy^2*zz^2+12*ww^2*xx^2*yy^4+12*ww^2*xx^2*"+
		"zz^4+12*ww^2*xx^4*yy^2+12*ww^2*xx^4*zz^2+4*ww^2*xx^6+"+
		"12*ww^2*yy^2*zz^4+12*ww^2*yy^4*zz^2+4*ww^2*yy^6+4*"+
		"ww^2*zz^6+12*ww^4*xx^2*yy^2+12*ww^4*xx^2*zz^2+6*ww^4*"+
		"xx^4+12*ww^4*yy^2*zz^2+6*ww^4*yy^4+6*ww^4*zz^4+4*"+
		"ww^6*xx^2+4*ww^6*yy^2+4*ww^6*zz^2+ww^8+12*xx^2*yy^2*"+
		"zz^4+12*xx^2*yy^4*zz^2+4*xx^2*yy^6+4*xx^2*zz^6+12*"+
		"xx^4*yy^2*zz^2+6*xx^4*yy^4+6*xx^4*zz^4+4*xx^6*yy^2+4*"+
		"xx^6*zz^2+xx^8+4*yy^2*zz^6+6*yy^4*zz^4+4*yy^6*zz^2+"+
		"yy^8+zz^8");

		Node n = j.parse("ln(x+1)");
		Node diff = j.differentiate(n,"x");
		Node simp = pc.simplify(diff);
		myAssertEquals("diff(ln(x+1))",j.toString(simp),"1.0/(1.0+x)");
		diff = j.differentiate(simp,"x");
		j.println(diff);
		simp = pc.simplify(diff);
		myAssertEquals("d^2(ln(x+1))",j.toString(simp),"-1.0/(1.0+x)^2.0");
		diff = j.differentiate(simp,"x");
		j.println(diff);
		simp = pc.simplify(diff);
		myAssertEquals("d^3(ln(x+1))",j.toString(simp),"2.0/(1.0+x)^3.0");
		diff = j.differentiate(simp,"x");
		j.println(diff);
		simp = pc.simplify(diff);
		System.out.println("D^4\t"+j.toString(simp));

		j.getPrintVisitor().setMaxLen(80);

		diff = j.differentiate(simp,"x");
//		j.println(diff);
		simp = pc.simplify(diff);
		System.out.print("D^5\t");
		j.println(simp);

		diff = j.differentiate(simp,"x");
//		j.println(diff);
		simp = pc.simplify(diff);
		System.out.print("D^6\t");
		j.println(simp);

		diff = j.differentiate(simp,"x");
//		j.println(diff);
		simp = pc.simplify(diff);
		System.out.print("D^7\t");
		j.println(simp);

		diff = j.differentiate(simp,"x");
//		j.println(diff);
		simp = pc.simplify(diff);
		System.out.print("D^8\t");
		j.println(simp);
	}

	public void testTotalOrder() throws ParseException,Exception
	{
		expandTestString("y+x","x+y");
		expandTestString("x^2+x","x+x^2");
		expandTestString("x^3+x^2","x^2+x^3");
		expandTestString("x*y+x","x+x*y");
		expandTestString("x^2+x*y","x*y+x^2");
		expandTestString("x+1/x","1/x+x");
		expandTestString("1/x^2+1/x","1/x^2+1/x");

		simplifyTestString("y+x","x+y");
		simplifyTestString("x^2+x","x+x^2");
		simplifyTestString("x^3+x^2","x^2+x^3");
		simplifyTestString("x*y+x","x+x*y");
		simplifyTestString("x^2+x*y","x*y+x^2");
		simplifyTestString("x+1/x","1/x+x");
		simplifyTestString("1/x^2+1/x","1/x^2+1/x");
	}
	
	public void testPolySimp2() throws ParseException,Exception
	{
		expandTestString("1+2*(1+x)","3+2*x");
		expandTestString("6x+3y+4x+3(15x+7y)+40","40+55*x+24*y");
		expandTestString("x*y+2*x","2*x+x*y");
		expandTestString("(1+x+y)^2","1+2*x+2*x*y+x^2+2*y+y^2");
	}
	
	public void testFormat() throws ParseException
	{
		NumberFormat format = NumberFormat.getInstance();
		j.getPrintVisitor().setNumberFormat(format);
		format.setMaximumFractionDigits(3);
		format.setMinimumFractionDigits(0);
		
		String s1 = "[10,0,0.1,0.11,0.111,0.1111]";
		String r1 = j.toString(j.parse(s1));
		String s2 = "[0.9,0.99,0.999,0.9999]";
		String r2 = j.toString(j.parse(s2));
		this.myAssertEquals(s1,r1,"[10,0,0.1,0.11,0.111,0.111]");
		this.myAssertEquals(s2,r2,"[0.9,0.99,0.999,1]");
		
		//j.addComplex();
		j.println(j.parse("[0,1,i,1+i]"));
		j.getPrintVisitor().setMode(PrintVisitor.COMPLEX_I,true);
		j.println(j.simplify(j.parse("(2+i)+(1+i)")));
		j.parseExpression("(2+i)+(1+i)");
		Complex c = j.getComplexValue();
		System.out.println(c.toString(format,true));
	}

	public void testSimpleEquals() throws ParseException
	{
		assertPolynomialEquals("a+b-(c+d)","(a+b)-c-d");
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
}
