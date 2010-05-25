/*****************************************************************************

 JEP 2.4.1, Extensions 1.1.1
      April 30 2007
      (c) Copyright 2007, Nathan Funk and Richard Morris
      See LICENSE-*.txt for license information.

*****************************************************************************/

package org.nfunk.jeptesting;

import java.io.*;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.nfunk.jep.*;
import org.nfunk.jep.type.Complex;

/**
 * This class is designed for testing the validity of JEP evaluations.
 * Expressions from a text file are evaluated with JEP in pairs of two, and
 * the results are compared. If they do not match, the two expressions are 
 * printed to standard output.<p>
 * Take for example an input text file containing the two lines
 * <pre>1+2
 *3.</pre>
 * The expressions '1+2' and '3' are evaluated with JEP and the results compared.
 */
public class JEPTest extends TestCase {

	/** The parser */
	JEP myParser;
	
	/** Current line position */
	int lineCount;

	/**
	 * Constructor
	 *
	public JEPTester() {
		// Set up the parser
		myParser = new JEP();
		myParser.setImplicitMul(true);
		myParser.addStandardFunctions();
		myParser.addStandardConstants();
		myParser.addComplex();
		myParser.setTraverse(false);
		lineCount = 0;
	}*/
	
	/**
	 * Creates a new JEPTest instance
	 */
	public JEPTest(String name) {
		super(name);
	}
	
	/**
	 * Sets up the parser.
	 */
	public void setUp() {
		// Set up the parser
		myParser = new JEP();
		myParser.setImplicitMul(true);
		myParser.addStandardFunctions();
		myParser.addStandardConstants();
		myParser.addComplex();
		myParser.setTraverse(false);
		lineCount = 0;
	}
	
	/**
	 * Runs the test.
	 */
	public void runTest() {
		String fileName = "JEPTestExpressions.txt";
		testWithFile(fileName);
		testGetValue();
		testGetComplexValue();
	}
	
	/**
	 * The main method checks the arguments and creates an instance
	 * and calls it's run method.
	 */
	public static void main(String args[]) {
		String fileName;
		
		// get filename from argument, or use default
		if (args!=null && args.length>0) {
			fileName = args[0];
		} else {
			fileName = "JEPTestExpressions.txt";
			println("Using default input file: " + fileName);
			println("Start with \"java org.nfunk.jepexamples."+
			"JEPTest <filename>\" to load a different input file.");
		}
		
		// Create an instance of this class and analyse the file
		JEPTest jt = new JEPTest("JEP Test");
		jt.setUp();
		jt.testWithFile(fileName);
	}

	public void testWithFile() {
		testWithFile("JEPTestExpressions.txt");
	}
	
	/**
	 * Loads the file specified in fileName. Evaluates the expressions listed
	 * in it and compares the expressions with the results.
	 */
	public void testWithFile(String fileName) {
		BufferedReader reader;
		Object v1, v2;
		boolean hasError = false;

		// Load the input file
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (Exception e) {
			Assert.assertTrue(false);
			println("File \""+fileName+"\" not found");
			return;
		}
		
		// reset the line count
		lineCount = 0;
		
		// cycle through the expressions in pairs of two
		println("Evaluating and comparing expressions...");
		while (true) {
			// get values of a pair of two lines
			try {
				v1 = parseNextLine(reader); //returns null when end of file is reached
				v2 = parseNextLine(reader);
			} catch (Exception e) {
				println(e.getMessage());
				hasError = true;
				break;
			}

			// v1 or v2 is null when end of file is reached
			if (v1 == null || v2 == null) {
				println("Reached end of file.");
				break;
			}
	
			// compare the results
			if (!equal(v1, v2)) {
				hasError = true;
				print("Line: " + lineCount + ": ");
				println(v1.toString() + " != " + v2.toString());
				Assert.fail();
			}
		}
		
		// Closing remarks
		print("\n" + lineCount + " lines processed. ");
		if (hasError) {
			print("Errors were found.\n\n");
		} else {
			print("No errors were found.\n\n");
		}
	}
	
	/**
	 * Parses a single line from the reader, and returns the
	 * evaluation of that line.
	 * @return evaluated line. Returns null when the end of the file
	 *         is reached.
	 * @throws Exception when IOException occurs, parsing fails, or when
	 *         evaluation fails
	 */
	private Object parseNextLine(BufferedReader reader) throws Exception {
		Object value;
		String line, errorStr;
		
		// cycle till a valid line is found
		do {
			line = reader.readLine(); // returns null on end of file
			if (line == null) return null;
			lineCount++;
		} while (line.length() == 0 || line.trim().charAt(0) == '#');

		// parse the expression
		myParser.parseExpression(line);
		// did an error occur while parsing?
		if (myParser.hasError()) {
			errorStr = myParser.getErrorInfo();
			throw new Exception("Error while parsing line " + lineCount + ": " + errorStr);
		}
		
		// evaluate the expression
		value = myParser.getValueAsObject();
		// did an error occur while evaluating?
		if (value == null || myParser.hasError()) {
			errorStr = myParser.getErrorInfo();
			throw new Exception("Error while evaluating line " + lineCount + ": " + errorStr);
		}
			
		return value;
	}

	/**
	 * Compares o1 and o2. Copied from Comparative.java.
	 * @return true if o1 and o2 are equal. false otherwise.
	 */
	private boolean equal(Object param1, Object param2)
	{
		double tolerance = 1e-15;
		if ((param1 instanceof Complex) && (param2 instanceof Complex)) {
			return ((Complex)param1).equals((Complex)param2, tolerance);
		}
		if ((param1 instanceof Complex) && (param2 instanceof Number)) {
			return ((Complex)param1).equals(new Complex((Number) param2), tolerance);
		}
		if ((param2 instanceof Complex) && (param1 instanceof Number)) {
			return ((Complex)param2).equals(new Complex((Number) param1), tolerance);
		}
		if ((param1 instanceof Number) && (param2 instanceof Number)) {
			return Math.abs(((Number)param1).doubleValue()-((Number)param2).doubleValue())
					< tolerance;
		}
		// test any other types here
		return param1.equals(param2);
	}

	/**
	 * Test the getValue() method.
	 */
	public void testGetValue() {
		// Test whether a normal double value is returned correctly
		myParser.parseExpression("2.1345");
		Assert.assertEquals(myParser.getValue(), 2.1345, 0);
		
		// Test whether NaN is returned for Somplex numbers
		myParser.parseExpression("i");
		Assert.assertTrue(Double.isNaN(myParser.getValue()));
		
		// Test whether NaN is returned for String results
		myParser.parseExpression("\"asdf\"");
		Assert.assertTrue(Double.isNaN(myParser.getValue()));
	}
	
	/**
	 * Test the getComplexValue() method.
	 */
	public void testGetComplexValue() {
		// Test whether a normal double value is returned as a Complex
		myParser.parseExpression("2.1345");
		Assert.assertTrue(new Complex(2.1345, 0).equals(
							myParser.getComplexValue(), 0));
		
		// Test whether (0, 1) is returned for i
		myParser.parseExpression("i");
		Complex z = myParser.getComplexValue();
		Assert.assertTrue(z != null);
		Assert.assertTrue(z.re() == 0);
		Assert.assertTrue(z.im() == 1);
		
		// Test whether NaN is returned for String results
		myParser.parseExpression("\"asdf\"");
		Assert.assertTrue(Double.isNaN(myParser.getValue()));
	}


	/**
	 * Helper function for printing.
	 */
	private static void print(String str) {
		System.out.print(str);
	}

	/**
	 * Helper function for printing lines.
	 */
	private static void println(String str) {
		System.out.println(str);
	}
}
