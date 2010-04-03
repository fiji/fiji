/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.lsmp.djepJUnit;

import junit.framework.*;

public class AllTests {
	
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite("All JUnit Tests");
		suite.addTestSuite(DJepTest.class);
		suite.addTestSuite(GroupJepTest.class);
		suite.addTestSuite(JepTest.class);
		suite.addTestSuite(MatrixJepTest.class);
		suite.addTestSuite(MRpTest.class);
		suite.addTestSuite(RewriteTest.class);
		suite.addTestSuite(SJepTest.class);
		suite.addTestSuite(VectorJepTest.class);
		suite.addTestSuite(XJepTest.class);
		return suite;
	}
}
