package org.nfunk.jeptesting;

import java.util.Enumeration;
import java.util.Stack;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.nfunk.jep.JEP;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.SymbolTable;
import org.nfunk.jep.Variable;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.type.Complex;

/**
 * This class is intended to contain all tests related to reported bugs.
 * 
 * @author Nathan Funk
 */
public class BugsTest extends TestCase {
	private JEP jep;

	/**
	 * Creates a new BugsTest instance
	 */
	public BugsTest(String name) {
		super(name);
	}
	
	public void setUp() {
		// Set up the parser
		jep = new JEP();
		jep.setImplicitMul(true);
		jep.addStandardFunctions();
		jep.addStandardConstants();
		jep.addComplex();
		jep.setTraverse(false);
	}
	
	/**
	 * Tests the uninitialized OperatorSet bug 1061200
	 */
	public void testOpSetBug() {
		JEP j = new JEP(false, true, true, null);
		Assert.assertNotNull(j.getOperatorSet());
	}
	
	/**
	 * Tests [ 1562371 ] ParseException not sets jep.hasError() flag.
	 * 
	 * This bug turned out to actually not be a bug. The user reported that 
	 * no error occured from a custom function during parsing, only after
	 * evaluation. This is expected behaviour since the run() method is
	 * not called during parsing - so even if there is a type compatibility
	 * issue, it will not be determined while parsing.
	 */
	public void testHasError() {		
		
		jep.addFunction("custFunc", new CustFunc());
		jep.parseExpression("custFunc(-1)");
		Assert.assertTrue(!jep.hasError());
		jep.getValue();
		Assert.assertTrue(jep.hasError());		
		
		// additional tests
		// test too many arguments
		jep.parseExpression("custFunc(1, 1)");
		Assert.assertTrue(jep.hasError());
		jep.getValue();
		Assert.assertTrue(jep.hasError());
		
		// test for empty expression causing error (should have error after parsing)
		jep.parseExpression("");
		Assert.assertTrue(jep.hasError());
		jep.getValue();
		Assert.assertTrue(jep.hasError());
		
		// test syntax error (should have error after parsing)
		jep.parseExpression("1+");
		Assert.assertTrue(jep.hasError());
		jep.getValue();
		Assert.assertTrue(jep.hasError());

		// test type error (should have error after evaluation)
		jep.parseExpression("sin([1, 1])");
		Assert.assertTrue(!jep.hasError());
		jep.getValue();
		Assert.assertTrue(jep.hasError());
	}

	/**
	 * Inner class for testing bug 1562371
	 * This custom function returns the parameter if it is a regular number 
	 * greater than zero. It throws an exception otherwise.
	 * @author singularsys
	 */
	private class CustFunc extends PostfixMathCommand
	{
		public CustFunc() { numberOfParameters = 1; }
		
		public void run(Stack inStack) throws ParseException 
		{
			checkStack(inStack);// check the stack
			Object param = inStack.pop();
			if (param instanceof Number && ((Number)param).doubleValue() > 0) {
				inStack.push(param);
			} else {
				System.out.println("Throwing exception");
				throw new ParseException("Parameter is not a Number or not >0");
			}
			return;
		}
	}
	
	/**
	 * Tests bug [ 1585128 ] setAllowUndeclared does not work!!!
	 * setAllowedUndeclared should add variables to the symbol table.
	 * 
	 * This test parses the expression "x" and checks whether only the
	 * variable x is in the symboltable (no more no less)
	 */
	public void testSetAllowUndeclared() {
		jep.initSymTab();				// clear the Symbol Table
		jep.setAllowUndeclared(true);
		jep.parseExpression("x");
		SymbolTable st = jep.getSymbolTable();
		
		int i = 0;
		// should only contain a single variable x
		for (Enumeration e = st.elements(); e.hasMoreElements(); ) 
		{
			Variable var = (Variable) e.nextElement();
			Assert.assertTrue(var.getName().equals("x"));
			i++;
		}
		Assert.assertTrue(i==1);
	}
	
	/**
	 * Tests [ 1589277 ] Power function and "third root".
	 * 
	 * Simple test for (-8)^(1/3) == -2.
	 *
	public void testComplexPower() {
		jep.initSymTab();
		jep.parseExpression("(-8)^(1/3)");
		Complex result = jep.getComplexValue();
		Assert.assertTrue(result.equals(new Complex(-2, 0)));
	}*/
	
	/**
	 * Tests [ 1563324 ] getValueAsObject always return null after an error
	 * 
	 * JEP 2.4.0 checks the <code>errorList</code> variable before evaluating 
	 * an expression if there is an error in the list, null is returned. This
	 * behaviour is bad because errors are added to the list by
	 * getValueAsObject. If the first evaluation fails (after a successful parse)
	 * then an error is added to the list. Subsequent calls to getValueAsObject
	 * fail because there is an error in the list.
	 */
	public void testBug1563324() {
		jep.initSymTab();
		jep.setAllowUndeclared(true);
		// parse a valid expression
		jep.parseExpression("abs(x)");
		// add a variable with a value that causes evaluation to fail
		// (the Random type is not supported by the abs function)
		jep.addVariable("x", new java.util.Random()); 
		Object result = jep.getValueAsObject();
		// evaluation should have failed
		Assert.assertTrue(jep.hasError());
		
		// change the variable value to a value that should be evaluated
		jep.addVariable("x", -1);
		// ensure that it is evaluated correctly
		result = jep.getValueAsObject();
		Assert.assertTrue((result instanceof Double) && ((Double)result).doubleValue() == 1.0);
	}
}
