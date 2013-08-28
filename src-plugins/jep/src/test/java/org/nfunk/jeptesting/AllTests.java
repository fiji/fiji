/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jeptesting;

import junit.framework.*;

public class AllTests {
	
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite("All JUnit Tests");
		suite.addTest(new JEPTest("testParseExpression"));
		suite.addTestSuite(LogarithmTest.class);
		suite.addTestSuite(NaturalLogarithmTest.class);
		suite.addTestSuite(BugsTest.class);
		suite.addTestSuite(ComplexTest.class);		
		return suite;
	}
}
