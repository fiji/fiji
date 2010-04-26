package org.lsmp.djepJUnit;

import junit.framework.*;
import org.nfunk.jep.*;
import org.nfunk.jep.type.*;

/* @author rich
 * Created on 19-Nov-2003
 */

/**
 * @author Rich Morris
 * Created on 19-Nov-2003
 */
public class JepTest extends TestCase {
	JEP j;
	public static final boolean SHOW_BAD=false;
	
	public JepTest(String name) {
		super(name);
	}

	/**
	 * Create a test suite.
	 * @return the TestSuite
	 */
	public static Test suite() {
		return new TestSuite(JepTest.class);
	}

	/**
	 * Main entry point.
	 * @param args
	 */
	public static void main(String args[]) {
		// Create an instance of this class and analyse the file

		TestSuite suite= new TestSuite(JepTest.class);
		suite.run(new TestResult());
	}	
	/**
	 * Run before each test.
	 */
	protected void setUp() {
		j = new JEP();
		j.addStandardConstants();
		j.addStandardFunctions();
		j.addComplex();
		//j.setTraverse(true);
		j.setAllowAssignment(true);
		j.setAllowUndeclared(true);
		j.setImplicitMul(true);
	}

	/**
	 * Assertion with message on command line.
	 * 
	 * @param msg message to display
	 * @param expected expected result
	 * @param actual actual result
	 */
	public void myAssertEquals(String msg,Object expected,Object actual)
	{
		if(!actual.equals(expected))
			System.out.println("Error: '"+msg+"' is '"+actual+"' should be '"+expected+"'");
		assertEquals("<"+msg+">",expected,actual);
		System.out.println("Success: Value of \""+msg+"\" is "+actual+"");
	}
	
	public void myAssertNaN(String msg,Object actual)
	{
		if(actual instanceof Double) {
			if(Double.isNaN( ((Double) actual).doubleValue()) ) {
				System.out.println("Success: Value of \""+msg+"\" is "+actual+"");
			}
			else {
				System.out.println("Error: \""+msg+"\" is '"+actual+"' should be NaN");
				assertTrue("<"+msg+"> is "+actual+" should be NaN",false);
			}
		}
		else {
			System.out.println("Error: '"+msg+"' is '"+actual+"' should be 'NaN'");
			assertTrue("<"+msg+">",false);
		}
	}


	/** Parse and evaluate an expression.
	 * 
	 * @param expr string to parse
	 * @return value after evaluate.
	 * @throws ParseException
	 */
	public Object calcValue(String expr) throws ParseException
	{
		Node n = j.parse(expr);
		return calcValue(n);
	}

	public Object calcValue(Node expr) throws ParseException
	{
		Object val = j.evaluate(expr);
		return val;
	}
	
	/**
	 * Test result j.evaluate(j.parse(expr))
	 * @param expr the expression to parse and evaluate
	 * @param expected result expected
	 * @throws Exception
	 */
	public void valueTest(String expr,Object expected) throws Exception
	{
		Object res = calcValue(expr);
		myAssertEquals(expr,expected,res);
	}

	public void valueTest(String expr,String expected) throws Exception
	{
		Object res = calcValue(expr);
		myAssertEquals(expr,expected,res.toString());
	}

	public void valueTestNaN(String expr) throws Exception
	{
		Object res = calcValue(expr);
		myAssertNaN(expr,res);
	}
	/**
	 * Test parse and evaluate which should give the result Integer(a).
	 * @param expr
	 * @param a expected value will be converted to an Integer.
	 * @throws Exception
	 */
	public void valueTestInt(String expr,int a) throws Exception
	{
		valueTest(expr,new Integer(a));
	}
	
	/** Test parse and evaluate with with a Double result.
	 * 
	 * @param expr
	 * @param a expected value will be converted to a Double.
	 * @throws Exception
	 */
	public void valueTest(String expr,double a) throws Exception
	{
		valueTest(expr,new Double(a));
	}

	public void valueTest(String expr,double a,double tol) throws Exception
	{
		Object res = calcValue(expr);
		if(res instanceof Double) {
			double val = ((Double) res).doubleValue();
			if(Math.abs(val-a)<tol) {
				System.out.println("Success value of \""+expr+"\" is "+res);
			}
			else {
				System.out.println("Error value of \""+expr+"\" is "+res+" should be "+a);
				assertEquals(expr,a,val,tol);
			}
		}
		else {
			System.out.println("Error value of \""+expr+"\" is "+res+" should be "+a);
			assertTrue("<"+expr+"> expected: <"+a+"> but was <"+res+">",false);
		}
	}

	/** Test parse-evaluate with complex number and given tollerence.
	 * 
	 * @param expr
	 * @param expected
	 * @param tol
	 * @throws Exception
	 */
	public void complexValueTest(String expr,Complex expected,double tol) throws Exception
	{
		Object res = calcValue(expr);
		if(expected.equals((Complex) res,tol))
			System.out.println("Success value of \""+expr+"\" is "+res);
		else {
			System.out.println("Error value of \""+expr+"\" is "+res+" should be "+expected);
			assertTrue("<"+expr+"> expected: <"+expected+"> but was <"+res+">",false);
		}
	}

	/////////////////// Tests ////////////////
	
	/** just test JUnit working OK */
	public void testGood()
	{
		myAssertEquals("1",new Double(1),new Double(1));
		myAssertNaN("NaN",new Double(Double.NaN));
	}

	public void testSimpleSum() throws Exception
	{
		valueTest("1+2",3);		
		valueTest("2*6+3",15);		
		valueTest("2*(6+3)",18);
	}
	
	public void testLogical() throws Exception
	{
		System.out.println("\nTesting logical operations");

		valueTest("T=1",1);
		valueTest("F=0",0);
		valueTest("!T",0);
		valueTest("!F",1);
		valueTest("!5",0);
		valueTest("-0==0",1);
		valueTest("!-5",0);
		valueTest("-!5==0",1);
		valueTest("-!0",-1);
		valueTest("-0",-0.0);
		valueTest("T&&T",1);
		valueTest("T&&F",0);
		valueTest("F&&T",0);
		valueTest("F&&F",0);
		valueTest("T||T",1);
		valueTest("T||F",1);
		valueTest("F||T",1);
		valueTest("F||F",0);
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
		
		j.addVariable("true",new Boolean(true));
		j.addVariable("false",new Boolean(false));
		valueTest("true==true",1);
		valueTest("false==false",1);
		valueTest("true==false",0);
		valueTest("true==true&&false==false",1);
		valueTest("if(true==true&&false==false,6,7)",6);
		valueTest("if(false&&true,6,7)",7);
		valueTest("if(true&&false==false,6,7)",6);
		valueTest("if((true&&true)==true,6,7)",6);
		valueTest("if((!false)==true,6,7)",6);
	}
	
	public void testFunction() throws Exception
	{
		System.out.println("\nTesting real functions");
		valueTest("abs(2.5)",2.5);
		valueTest("abs(-2.5)",2.5);
		valueTest("acos(1/sqrt(2))",Math.PI/4,0.00000001);
		valueTest("cos(pi/3)",0.5,0.00000001);
		
	}
	public void testComplex() throws Exception
	{
		System.out.println("\nTesting complex values");
		double tol = 0.00000001;

		complexValueTest("z=complex(3,2)",new Complex(3,2),tol);
		complexValueTest("z*z-z",new Complex(2,10),tol);
		complexValueTest("z^3",new Complex(-9,46),tol);
		complexValueTest("(z*z-z)/z",new Complex(2,2),tol);
		complexValueTest("w=polar(2,pi/2)",new Complex(0,2),tol);
		
		complexValueTest("ln(-1)",new Complex(0,Math.PI),tol);
		complexValueTest("sqrt(-1)",new Complex(0,1),tol);
		complexValueTest("pow(-1,0.5)",new Complex(0,1),tol);
		valueTest("arg(w)",Math.PI/2);
		valueTest("cmod(w)",2);
		valueTest("re(z)",3);
		valueTest("im(z)",2);
		complexValueTest("conj(z)",new Complex(3,-2),tol);
		complexValueTest("exp(pi i/2)",new Complex(0,1),tol);
		//complexValueTest("cos(z)",new Complex(3,-2),tol);
	}


	public void testIf()  throws Exception
	{
		System.out.println("\nTesting if statement");
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

	public void testAssign()  throws Exception
	{
		System.out.println("\nTesting assignment of variables");
		valueTest("x=3",3);
		valueTest("y=3+4",7);
		valueTest("z=x+y",10);
		valueTest("a=b=c=z",10);
		valueTest("b",10);
		valueTest("d=f=a-b",0);
		valueTest("x=2",2);
		valueTest("(x*x)*x*(x*x)",32.0); // Works fine with Multiply
		new org.lsmp.djep.vectorJep.VectorJep();
		valueTest("(x*x)*x*(x*x)",32.0);
		// this created an error in 2.3.0b
		// as creating a VectorJep changed the operator set
		// and hence the broken MMultiply was used.								
	}

	public void testDotInName() throws ParseException,Exception
	{
		System.out.println("\nTesting names with dot in them");
		valueTest("x.x=3",3);
		valueTest("x.x+1",4);
	}


	public void testBinom() throws ParseException,Exception
	{
		System.out.println("\nTesting binomial coeffs");
		valueTestInt("binom(0,0)",1);
		valueTestInt("binom(1,0)",1);
		valueTestInt("binom(1,1)",1);
		valueTestInt("binom(2,0)",1);
		valueTestInt("binom(2,1)",2);
		valueTestInt("binom(2,2)",1);
		valueTestInt("binom(3,0)",1);
		valueTestInt("binom(3,1)",3);
		valueTestInt("binom(3,2)",3);
		valueTestInt("binom(3,3)",1);
		valueTestInt("binom(4,0)",1);
		valueTestInt("binom(4,1)",4);
		valueTestInt("binom(4,2)",6);
		valueTestInt("binom(4,3)",4);
		valueTestInt("binom(4,4)",1);
		valueTestInt("binom(5,0)",1);
		valueTestInt("binom(5,1)",5);
		valueTestInt("binom(5,2)",10);
		valueTestInt("binom(5,3)",10);
		valueTestInt("binom(5,4)",5);
		valueTestInt("binom(5,5)",1);

		valueTestInt("binom(6,0)",1);
		valueTestInt("binom(6,1)",6);
		valueTestInt("binom(6,2)",15);
		valueTestInt("binom(6,3)",20);
		valueTestInt("binom(6,4)",15);
		valueTestInt("binom(6,5)",6);
		valueTestInt("binom(6,6)",1);
		
		valueTestInt("binom(10,1)",10);
		valueTestInt("binom(10,5)",252);
	}
	
	public void testNaN() throws Exception
	{
		System.out.println("\nTesting for NaN");
		j.addVariable("x",new Double(Double.NaN));
		System.out.println("x=NaN");
		valueTestNaN("ln(x)");
		valueTestNaN("log(x)");
		valueTestNaN("sin(x)");
		valueTestNaN("x+x");
		valueTest("x!=x",1);
		valueTest("x==x",0);

		j.addVariable("y",new Double(Double.NaN));
		Node n = j.parse("x+5");
		System.out.println(calcValue(n));
		Node n2 = j.parse("y");
		System.out.println(calcValue(n2));
		valueTest("x == x+5",0);
		valueTest("x == 0/0",0);
		valueTest("x == x",0);
		valueTest("x == 0 * x",0);
		valueTest("x == 5",0);
		valueTest("x == y",0);
		valueTest("y == y",0);
		System.out.println("Set x to Double(5)");
		j.setVarValue("x",new Double(5));
		valueTest("x == x+5",0);
		valueTest("x == x",1);
	}
	
	public void testAssign2()
	{
		System.out.println("\nTesting for assignment using parseExpression and getValue");

		JEP parser = new JEP();

		parser.addVariable("AB",12);
		parser.addVariable("graph",new Object());
		parser.addVariable("graph1",(Double) null);
		parser.setAllowAssignment(true);
		parser.parseExpression("AB=3"); // AB = 8
		System.out.println("AB=3"+parser.getValue());
		parser.parseExpression("AB+2");
		double result= parser.getValue(); // Result = 17
		assertEquals("<AB+2>",5.0,result,0.0);
	}

	 boolean isExpressionValid(String expression) 
	 { 
		 JEP jep = j; 
		 try{ 
			 Node n = jep.parse(expression); 
			 System.out.println("expression " + expression + " \n Parsed value " + jep.hasError()); 
			 if(jep.hasError())
			 { 
				 System.out.println("jep.getErrorInfo " + jep.getErrorInfo()); 
				 return false; 
			 } 
			 System.out.println("jep.getSymbolTable " + jep.getSymbolTable());
			 System.out.println("Eval: " +jep.evaluate(n).toString());
			 return true; 
		 }
		 catch(Error e)
		 { 
			 System.out.println(e.getMessage()); 
			 if(jep.hasError()) 
			 System.out.println("Error is : " + jep.getErrorInfo()); 
			 return false; 
		 }
		 catch(Exception e1)
		 { 
			 System.out.println(e1.getMessage());  
			 if(jep.hasError()) 
			 System.out.println("Error is : " + jep.getErrorInfo()); 
			 return false; 
		 } 
	  
	 } 

	public void testNumParam() throws Exception
	{
		j.parse("if(3,1,2)");
		j.parse("if(4,1,2,3)");
		try
		{
			j.parse("if(5,1,2,3,4)");
			fail("Did not trap illegal number of arguments");
		}
		catch(ParseException e) {}
		j.parse("a1=1234");
		j.parse("a2=5678");
		j.parse("ApportionmentAmt=4321");
		j.parse("a4 = 2000 + (3000 /2000) + (3.45787 * 33544 - (212.223 /2000)) + + 1200");
		j.parse("a3 = if(a1 > 0 && ApportionmentAmt < 1000, if(a2 < 2000, if(a2 < 1000, 200, 0), if(a1 > 1000, if((2000 + (3000 /2000) + (3.45787 * 33544 - (212.223 /2000)) + 1200 + ApportionmentAmt / 2000 + ApportionmentAmt * ApportionmentAmt + 2000) > 0, 100, 200),200)), if(a1/a2 < 1000, a1/a2, 1, a1 * a2 + a1))");
		try
		{
		 j.parse("a3 = if(a1 > 0 && ApportionmentAmt < 1000, if(a2 < 2000, if(a2 < 1000, 200, 0), if(a1 > 1000, if((2000 + (3000 /2000) + (3.45787 * 33544 - (212.223 /2000)) + 1200 + ApportionmentAmt / 2000 + ApportionmentAmt * ApportionmentAmt + 2000) > 0, 100, 200)),200), if(a1/a2 < 1000, a1/a2, 1, a1 * a2 + a1))");
			fail("Did not trap illegal number of arguments");
		}
		catch(ParseException e) {}
/*		 double a1=0,a2=0,ApportionmentAmt=0;
		 double a3 = 
			 myif(
					 a1 > 0 && ApportionmentAmt < 1000, 
					 myif(
							 a2 < 2000, 
							 myif(a2 < 1000, 200, 0), 
							 myif(
									 a1 > 1000, 
									 myif(
											 (2000 + (3000 /2000) + (3.45787 * 33544 - (212.223 /2000)) + 1200 + ApportionmentAmt / 2000 + ApportionmentAmt * ApportionmentAmt + 2000) > 0,
											 100,
											 200
									 ),
									 333)), 
				myif(a1/a2 < 1000, a1/a2,  a1 * a2 + a1));


a3 = if(a1 > 0 && ApportionmentAmt < 1000, 
		if(a2 < 2000, 
			if(a2 < 1000, 200, 0), 
			if(a1 > 1000, 
				if(
					(2000 + (3000 /2000) + (3.45787 * 33544 - (212.223 /2000)) + 1200 + ApportionmentAmt / 2000 + ApportionmentAmt * ApportionmentAmt + 2000) > 0,
					100,
					200
				)
			),
			200
		),
		if(a1/a2 < 1000, a1/a2, 1, a1 * a2 + a1)
	)
*/
	}
	public void testUndeclared() throws Exception
	{
	    j.setAllowUndeclared(false);
	    try {
	        j.parse("zap * wow");
	        fail("Should have found undeclared error");
	    } catch(ParseException e) {
	        System.out.println("Error caught: "+e.getMessage());
	    }
	    try {
	        j.setVarValue("foo",new Double(5.0));
	        fail("Should have found a null pointer exception");
	    } catch(NullPointerException e) {
	        System.out.println("Error caught: "+e.getClass().getName()+": "+e.getMessage());
	    }
	}
	
	public void testBad() throws Exception
	{
		if(SHOW_BAD)
		{
			valueTest("recurse = recurse+1",null);
		}
	}
}
