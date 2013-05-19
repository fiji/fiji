/*****************************************************************************

  JEP 2.4.1, Extensions 1.1.1
       April 30 2007
       (c) Copyright 2007, Nathan Funk and Richard Morris
       See LICENSE-*.txt for license information.

 *****************************************************************************/

package org.nfunk.jepexamples;

/**
 * This class tests the thread safety of the JEP package with a brute force
 * approach. 1000 threads are started, and each one invokes the evaluate method.
 * The evaluate method creates 10 JEP instances. Note that running this class
 * successfully does not necessarily ensure that no errors will ever occur.
 * <p>
 * Thanks to Matthew Baird and Daniel Teng for this code.
 */
public class ThreadTest {

	static long time = 0;

	/**
	 * Main method. Launches many threads.
	 */
	public static void main(String[] args) {
		int n = 1000;
		System.out.println("Starting " + n + " threads...");
		ThreadTest test = new ThreadTest();

		for (int i = 0; i < n; i++) {
			ThreadTestThread t = new ThreadTestThread(test);
			t.start();
		}
		System.out.println("Returned from starting threads. Threads may still need to terminate.");
		// TODO: check why application appears to end before all thread exit
	}

	public ThreadTest() {

	}

	/**
	 * Perform a simple evaluation using a new JEP instance. This method is
	 * called by all ThreadTestThreads at very much the same time.
	 */
	public void evaluate() {
		for (int i = 0; i < 10; i++) {
			org.nfunk.jep.JEP myParser = new org.nfunk.jep.JEP();
			String fooValue = null;
			Math.random();

			if (Math.random() > 0.5) {
				fooValue = "NLS";
			} else {
				fooValue = "NLT";
			}

			// TODO: add more involved calculations so the execution time of the
			// evaluation is longer (leading to more possible thread conflicts)
			myParser.addVariable("foo", fooValue);
			myParser.parseExpression("foo == \"" + fooValue + "\"");

			if (myParser.getValue() != 1.0)
				System.out.println("Wrong value returned");
			
			if (myParser.hasError())
				System.out.println(myParser.getErrorInfo());
		}
	}
}
