package org.nfunk.jeptesting;

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.NaturalLogarithm;

import junit.framework.Assert;
import junit.framework.TestCase;

public class NaturalLogarithmTest extends TestCase {

	public NaturalLogarithmTest(String name) {
		super(name);
	}

	/*
	 * Test method for 'org.nfunk.jep.function.Logarithm.run(Stack)'
	 * Tests the return value of log(NaN). This is a test for bug #1177557
	 */
	public void testNaturalLogarithm() {
		NaturalLogarithm logFunction = new NaturalLogarithm();
		java.util.Stack stack = new java.util.Stack();
		stack.push(new Double(Double.NaN));
		try {
			logFunction.run(stack);
		} catch (ParseException e) {
			Assert.fail();
		}
		Object returnValue = stack.pop();

		if (returnValue instanceof Double) {
			Assert.assertTrue(Double.isNaN(((Double)returnValue).doubleValue()));
		} else {
			Assert.fail();
		}
	}

}
