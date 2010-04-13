/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Some basic unit tests for the various Eigenvalue decomposition classes. */

package math3d;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import Jama.Matrix;
import Jama.EigenvalueDecomposition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Random;

public class TestEigenvalueDecompositions {

	boolean printTimings = false;

	ArrayList<TestMatrixAndResultDouble> realSymmetricTestCases2x2Double;
	ArrayList<TestMatrixAndResultFloat> realSymmetricTestCases2x2Float;

	ArrayList<TestMatrixAndResultDouble> realSymmetricTestCases3x3Double;
	ArrayList<TestMatrixAndResultFloat> realSymmetricTestCases3x3Float;

	static class TestMatrixAndResultDouble {
		int side;
		TestMatrixAndResultDouble( int side,
					   double [] matrixEntries,
					   double [] evaluesResult ) {
			this.side = side;
			this.m = new double[side][side];
			this.evaluesResult = new double[side];
			this.evectorsResult = new double[side][side];
			for(int i=0; i<(side*side); ++i ) {
				m[i/side][i%side] = matrixEntries[i];
			}
			System.arraycopy(evaluesResult,0,this.evaluesResult,0,side);
		}
		TestMatrixAndResultDouble( int side,
					   double [][] matrixEntries,
					   double [] evaluesResult ) {
			this.side = side;
			this.m = new double[side][side];
			this.evaluesResult = new double[side];
			this.evectorsResult = new double[side][side];
			for(int i=0; i<(side*side); ++i ) {
				m[i/side][i%side] = matrixEntries[i/side][i%side];
			}
			System.arraycopy(evaluesResult,0,this.evaluesResult,0,side);
		}
		double [][] m;
		double [] evaluesResult;
		double [][] evectorsResult;
		void checkEvalues(double [] calculatedEvalues) {
			assertTrue(calculatedEvalues.length == side);
			double [] copiedCalculatedEvalues = new double[side];
			double [] copiedGroundEvalues = new double[side];
			System.arraycopy(calculatedEvalues,0,copiedCalculatedEvalues,0,side);
			System.arraycopy(evaluesResult,0,copiedGroundEvalues,0,side);
			Arrays.sort(copiedCalculatedEvalues);
			Arrays.sort(copiedGroundEvalues);
			for( int i = 0; i < side; ++i ) {
				assertEquals(copiedGroundEvalues[i],copiedCalculatedEvalues[i],0.000001);
			}
		}
		float [][] getMatrixFloats() {
			float [][] result = new float[side][side];
			for(int i=0; i<(side*side); ++i ) {
				result[i/side][i%side] = (float)m[i/side][i%side];
			}
			return result;
		}
		float [] getEvaluesFloats() {
			float [] result = new float[side];
			for(int i=0; i<side; ++i ) {
				result[i] = (float)evaluesResult[i];
			}
			return result;
		}
	}


	/* Silly to repeat this code for the float version, really: */

	static class TestMatrixAndResultFloat {
		int side;
		TestMatrixAndResultFloat( int side,
					  float [] matrixEntries,
					  float [] evaluesResult ) {
			this.side = side;
			this.m = new float[side][side];
			this.evaluesResult = new float[side];
			this.evectorsResult = new float[side][side];
			for(int i=0; i<(side*side); ++i ) {
				m[i/side][i%side] = matrixEntries[i];
			}
			System.arraycopy(evaluesResult,0,this.evaluesResult,0,side);
		}
		TestMatrixAndResultFloat( int side,
					  float [][] matrixEntries,
					  float [] evaluesResult ) {
			this.side = side;
			this.m = new float[side][side];
			this.evaluesResult = new float[side];
			this.evectorsResult = new float[side][side];
			for(int i=0; i<(side*side); ++i ) {
				m[i/side][i%side] = matrixEntries[i/side][i%side];
			}
			System.arraycopy(evaluesResult,0,this.evaluesResult,0,side);
		}
		float [][] m;
		float [] evaluesResult;
		float [][] evectorsResult;
		void checkEvalues(float [] calculatedEvalues) {
			assertTrue(calculatedEvalues.length == side);
			float [] copiedCalculatedEvalues = new float[side];
			float [] copiedGroundEvalues = new float[side];
			System.arraycopy(calculatedEvalues,0,copiedCalculatedEvalues,0,side);
			System.arraycopy(evaluesResult,0,copiedGroundEvalues,0,side);
			Arrays.sort(copiedCalculatedEvalues);
			Arrays.sort(copiedGroundEvalues);
			for( int i = 0; i < side; ++i ) {
				assertEquals(copiedGroundEvalues[i],copiedCalculatedEvalues[i],0.000001);
			}
		}
	}


	@Before public void setUp() {

		// ------------------------------------------------------------------------
		// First the 3x3 cases:

		realSymmetricTestCases3x3Double = new ArrayList<TestMatrixAndResultDouble>();

		// An example that seems to fail sometimes:
		double [] m3 =
			{ 1.0038581,    0.11780524,  -0.020147324,
			  0.11780524,   0.82471085,  0.040020466,
			  -0.020147324, 0.040020466, 1.0540304 };
		double [] evalues3 =
			{ 0.7594775408789509,
			  1.0607974405512743,
			  1.0623243685697759 };
		realSymmetricTestCases3x3Double.add( new TestMatrixAndResultDouble(3,m3,evalues3) );

		// An example that should trigger the unusual discriminant == 0 case:
		double [] m4 =
			{ 3, 0, 0,
			  0, 5, 0,
                          0, 0, 5 };
		double [] evalues4 =
			{ 3, 5, 5 };
		realSymmetricTestCases3x3Double.add( new TestMatrixAndResultDouble(3,m4,evalues4) );

		// An example I solved in axiom:
		double [] m2 =
			{ 1, 2, 3,
			  2, 4, 5,
			  3, 5, 6 };
		double [] evalues2 =
			{ -0.51572947158925714026, 11.344814282762077688, 0.17091518882717945217 };
		realSymmetricTestCases3x3Double.add( new TestMatrixAndResultDouble(3,m2,evalues2) );

		// An example from http://en.wikipedia.org/wiki/Eigenvalue_algorithm
		double [] m1 =
			{ 0,  1, -1,
			  1,  1,  0,
			  -1, 0,  1 };
		double [] evalues1 =
			{ 2, 1, -1 };
		realSymmetricTestCases3x3Double.add( new TestMatrixAndResultDouble(3,m1,evalues1) );

		// Now generate lots of random real symmetric matrices
		// (and assume that the Jama version is correct!)

		Random r = new Random(123456789);
		for( int i = 0; i < 2000; ++i ) {
			double [] m = new double[9];
			m[0] = r.nextDouble();
			m[1] = m[3] = r.nextDouble();
			m[2] = m[6] = r.nextDouble();
			m[4] = r.nextDouble();
			m[5] = m[7] = r.nextDouble();
			m[8] = r.nextDouble();

			double [][] mm = new double[3][3];
			for( int d = 0; d < 9; ++d )
				mm[d/3][d%3] = m[d];

			// Find the eigenvalues with Jama:
			Matrix M = new Matrix(mm);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);
			double [] eigenValues = E.getRealEigenvalues();

			realSymmetricTestCases3x3Double.add( new TestMatrixAndResultDouble(3,m,eigenValues) );
		}

		realSymmetricTestCases3x3Float = new ArrayList<TestMatrixAndResultFloat>();

		// Now generate the float test cases from these:

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases3x3Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble trd = i.next();

			TestMatrixAndResultFloat trf =
				new TestMatrixAndResultFloat(3,trd.getMatrixFloats(),trd.getEvaluesFloats());

			realSymmetricTestCases3x3Float.add(trf);
		}

		// ------------------------------------------------------------------------
		// Now the 2x2 cases:

		realSymmetricTestCases2x2Double = new ArrayList<TestMatrixAndResultDouble>();

		// An example that seems to fail sometimes:
		double [] mb3 =
			{ 1.0038581,    0.11780524,
			  0.11780524,   0.82471085 };
		double [] bEvalues3 =
			{ 0.7662928912288334,1.0622760587711666 };
		realSymmetricTestCases2x2Double.add( new TestMatrixAndResultDouble(2,mb3,bEvalues3) );

		// An example that should trigger the unusual discriminant == 0 case:
		double [] mb4 =
			{ 3, 0,
			  0, 5 };
		double [] bEvalues4 =
			{ 3, 5 };
		realSymmetricTestCases2x2Double.add( new TestMatrixAndResultDouble(2,mb4,bEvalues4) );

		// An example I solved in axiom:
		double [] mb2 =
			{ 1, 2,
			  2, 4 };
		double [] bEvalues2 =
			{ 0, 5 };
		realSymmetricTestCases2x2Double.add( new TestMatrixAndResultDouble(2,mb2,bEvalues2) );

		double [] mb1 =
			{ 0,  -1,
			  -1,  1 };
		double [] bEvalues1 =
			{ -0.61803388595581054688, 1.61803388595581054688 };
		realSymmetricTestCases2x2Double.add( new TestMatrixAndResultDouble(2,mb1,bEvalues1) );
		
		// Now generate lots of random real symmetric matrices
		// (and assume that the Jama version is correct!)

		r = new Random(123456789);
		for( int i = 0; i < 2000; ++i ) {
			double [] m = new double[4];
			m[0] = r.nextDouble();
			m[1] = m[2] = r.nextDouble();
			m[3] = r.nextDouble();

			double [][] mm = new double[2][2];
			for( int d = 0; d < 4; ++d )
				mm[d/2][d%2] = m[d];

			// Find the eigenvalues with Jama:
			Matrix M = new Matrix(mm);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);
			double [] eigenValues = E.getRealEigenvalues();

			realSymmetricTestCases2x2Double.add( new TestMatrixAndResultDouble(2,m,eigenValues) );
		}

		realSymmetricTestCases2x2Float = new ArrayList<TestMatrixAndResultFloat>();

		// Now generate the float test cases from these:

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases2x2Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble trd = i.next();

			TestMatrixAndResultFloat trf =
				new TestMatrixAndResultFloat(2,trd.getMatrixFloats(),trd.getEvaluesFloats());

			realSymmetricTestCases2x2Float.add(trf);
		}


	}

	@After
	public void tearDown() {

		realSymmetricTestCases3x3Float = null;
		realSymmetricTestCases3x3Double = null;

		realSymmetricTestCases2x2Float = null;
		realSymmetricTestCases2x2Double = null;

		System.gc();
	}

	@Test
	public void test3x3Jama() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases3x3Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			Matrix M = new Matrix(mr.m);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);

			double[] result = E.getImagEigenvalues();

			boolean foundImaginaryEigenvalues = false;
			for (int e = 0; e < result.length; ++e) {
				// There should be no complex eigenvalues:
				assertTrue(result[e] == 0);
			}

			result = E.getRealEigenvalues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJama: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test3x3JacobiDouble() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases3x3Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			JacobiDouble jc=new JacobiDouble(mr.m,50);
			double [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiDouble: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test3x3JacobiFloat() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultFloat> i = realSymmetricTestCases3x3Float.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultFloat mr = i.next();

			JacobiFloat jc=new JacobiFloat(mr.m,50);
			float [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiFloat: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test3x3JacobiDoubleAgain() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases3x3Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			JacobiDouble jc=new JacobiDouble(mr.m,50);
			double [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiDoubleAgain: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test3x3RootFindingDouble() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases3x3Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			Eigensystem3x3Double e = new Eigensystem3x3Double(mr.m);
			boolean result = e.findEvalues();
			assert(result);
			if(result)
				mr.checkEvalues(e.eigenValues);
		}

		if (printTimings) System.out.println("testRootFinding: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test3x3RootFindingFloat() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultFloat> i = realSymmetricTestCases3x3Float.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultFloat mr = i.next();

			Eigensystem3x3Float e = new Eigensystem3x3Float(mr.m);
			boolean result = e.findEvalues();
			assert(result);
			if(result)
				mr.checkEvalues(e.eigenValues);
		}

		if (printTimings) System.out.println("testRootFinding: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	// ========================================================================

	@Test
	public void test2x2Jama() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases2x2Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			Matrix M = new Matrix(mr.m);
			EigenvalueDecomposition E = new EigenvalueDecomposition(M);

			double[] result = E.getImagEigenvalues();

			boolean foundImaginaryEigenvalues = false;
			for (int e = 0; e < result.length; ++e) {
				// There should be no complex eigenvalues:
				assertTrue(result[e] == 0);
			}

			result = E.getRealEigenvalues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJama: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test2x2JacobiDouble() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases2x2Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			JacobiDouble jc=new JacobiDouble(mr.m,50);
			double [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiDouble: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test2x2JacobiFloat() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultFloat> i = realSymmetricTestCases2x2Float.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultFloat mr = i.next();

			JacobiFloat jc=new JacobiFloat(mr.m,50);
			float [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiFloat: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test2x2JacobiDoubleAgain() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases2x2Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			JacobiDouble jc=new JacobiDouble(mr.m,50);
			double [] result = jc.getEigenValues();
			mr.checkEvalues(result);
		}

		if (printTimings) System.out.println("testJacobiDoubleAgain: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test2x2RootFindingDouble() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultDouble> i = realSymmetricTestCases2x2Double.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultDouble mr = i.next();

			Eigensystem2x2Double e = new Eigensystem2x2Double(mr.m);
			boolean result = e.findEvalues();
			assert(result);
			if(result)
				mr.checkEvalues(e.eigenValues);
		}

		if (printTimings) System.out.println("testRootFinding: "+(System.currentTimeMillis()-startTime)/1000.0);
	}

	@Test
	public void test2x2RootFindingFloat() {

		long startTime = System.currentTimeMillis();

		for( Iterator<TestMatrixAndResultFloat> i = realSymmetricTestCases2x2Float.iterator();
		     i.hasNext(); ) {

			TestMatrixAndResultFloat mr = i.next();

			Eigensystem2x2Float e = new Eigensystem2x2Float(mr.m);
			boolean result = e.findEvalues();
			assert(result);
			if(result)
				mr.checkEvalues(e.eigenValues);
		}

		if (printTimings) System.out.println("testRootFinding: "+(System.currentTimeMillis()-startTime)/1000.0);
	}



}
