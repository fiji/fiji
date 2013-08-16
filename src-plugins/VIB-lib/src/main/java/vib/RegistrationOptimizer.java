/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package vib;

import pal.math.*;
import math3d.*;
import ij.IJ;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import util.StupidLog;

public abstract class RegistrationOptimizer {

	protected boolean verbose = false;
	/*
	 * The extended euler parameters (9 values):
	 * First the rotations, then the translation (which is applied after
	 * the rotation), and then the rotation center.
	 * If eulerParameters == null, this class will try all 24 orientations.
	 */
	double[] eulerParameters;

	double[][] cachedInitialGuesses;

	/*
	 * If no initial transformation is given, the center of rotation and
	 * the initial translation have to be provided by setting the centers
	 * in the orig (the template) and trans (the dataset to be transformed).
	 */
	Point3d origC, transC;
	public abstract void getInitialCenters();

	/*
	 * This is the meat of it all: given a matrix representing the rigid
	 * transformation, tell how bad it is. This function will be minimized.
	 */
	public abstract double calculateBadness(FastMatrix matrix);

	/*
	 * refines the given registration (or tries extensively when
	 * matrix == null)
	 */
	public FastMatrix doRegister(double tol) {
		if(verbose) VIB.println("tol: " + tol);
		//ConjugateGradientSearch CG = new ConjugateGradientSearch();
		ConjugateDirectionSearch CG = new ConjugateDirectionSearch();
		//DifferentialEvolution CG = new DifferentialEvolution(6);
		//OrthogonalSearch CG = new OrthogonalSearch();
		//StochasticOSearch CG = new StochasticOSearch();
		//GridSearch CG = new GridSearch();
		//CG.conjugateGradientStyle = 1;
		if (IJ.getInstance() != null && verbose)
			CG.prin = 1; // debug
		//CG.step = 1;
		//CG.step = 0.1;
		//CG.defaultStep = 0.001;


		if (eulerParameters == null) {
			eulerParameters = searchInitialEulerParams()[0];
		}

		/*
		System.out.println("Registering starting with Euler parameters:");
		for( int i = 0; i < eulerParameters.length; ++i ) {
			System.out.println("  "+eulerParameters[i]);
		}
		*/

		/* MHL added ... */
		double [] originalEulerParameters = (double [])eulerParameters.clone();		
		double[] lastx = new double[6];
                for(int i=0; i<6; ++i)
                    lastx[i] = Double.MAX_VALUE;
		boolean gotStuck = false;
		/* ... end */
		
		Refinement refinement = new Refinement(eulerParameters);
		refinement.showStatus = true;
		double[] x = new double[6];
		do {
			//CG.step = 8.1;
			CG.optimize(refinement, x, tol,  tol);
			x = refinement.best;
			/* MHL added ... */
			if( Arrays.equals(x, lastx) ) {
				// Just for the moment, log exactly what went on when we got stuck:
				StupidLog.log("       Got stuck: x is (and was): "+x[0]+", "+x[1]+", "+x[2]+", "+x[3]+", "+x[4]+", "+x[5]);
				StupidLog.log("                   maxAdjust was: "+refinement.maxAdjust);
				StupidLog.log("                translateMax was: "+translateMax);
				StupidLog.log("                          tol is: "+tol);
				double [] e = originalEulerParameters;
				StupidLog.log("    originalEulerParameters were: "+e[0]+", "+e[1]+", "+e[2]+", "+e[3]+", "+e[4]+", "+e[5]+", "+e[6]+", "+e[7]+", "+e[8]);
				gotStuck = true;
			    break;
			}
                        System.arraycopy(x, 0, lastx, 0, 6);
			/* ... end */
			
                        // As a sideffect, this resets x to all zeros and
                        // changes 'initial' in refinement.
			eulerParameters = refinement.adjustInitial(x);

			//CG.step = 1;
			//CG.optimize(refinement, x, 0.001 / tol, 0.001 / tol);
			//eulerParameters = refinement.adjustInitial(x);
			if(verbose) VIB.println("eulerParameters: " + eulerParameters[0] + ", " + eulerParameters[1] + ", " + eulerParameters[2] + "; " + eulerParameters[3] + ", " + eulerParameters[4] + ", " + eulerParameters[5] + "; " + eulerParameters[6] + ", " + eulerParameters[7] + ", " + eulerParameters[8]);
		} while(!false && refinement.maxAdjust > translateMax / 8);

		if( gotStuck ) {
			Refinement originalRefinement = new Refinement(originalEulerParameters);
			for( int i = 0; i < x.length; ++i )
				x[i] = 0;
			return originalRefinement.getMatrix(x);
		} else {
			return refinement.getMatrix(x);
		}
	}
		
	//returns an ordered list of the 24 principle orientations tried
	//results[0] is a 9 dim matrix representing the best EulerParameter
	//result[1] is the 2nd best etc, upto [23]
	protected double[][] searchInitialEulerParams() {
		if (cachedInitialGuesses != null)
			return cachedInitialGuesses;
		double[][] p = new double[24][9];

		angleMax = Math.PI / 4 ;
		translateMax = 20.0 ;

		ConjugateDirectionSearch CG = new ConjugateDirectionSearch();
		CG.step = 10;

		double [][] orderedEulerParams = new double[24][9];

		getInitialCenters();

		// translation
		p[0][3] = origC.x - transC.x;
		p[0][4] = origC.y - transC.y;
		p[0][5] = origC.z - transC.z;
		// center of rotation (not to be optimized)
		p[0][6] = transC.x;
		p[0][7] = transC.y;
		p[0][8] = transC.z;

		/*
		 * The 24 principal orientations correspond to
		 * the Euler angles (0..3, 0..3, 0) * PI/2
		 * and (0..3, 1, 1) * PI/2 and (0..3, 3, 1) * PI/2
		 */
		for (int i = 1; i < 24; i++)
			System.arraycopy(p[0], 0, p[i], 0, 9);

		double pi2 = Math.PI / 2.0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				p[i * 6 + j][0] = i * pi2;
				p[i * 6 + j][1] = j * pi2;
			}
			for (int j = 0; j < 2; j++) {
				p[i * 6 + 4 + j][0] = i * pi2;
				p[i * 6 + 4 + j][1] = (j * 2 + 1) * pi2;
				p[i * 6 + 4 + j][2] = pi2;
			}
		}

		ArrayList refinements = new ArrayList();
		double[][] x = new double[24][6];
		for (int i = 0; i < 24; i++) {
			if(verbose) 
			VIB.showStatus("Trying orientation " + 
						(i + 1) + " of 24...");
			Refinement refinement = new Refinement(p[i]);
			CG.optimize(refinement, x[i], 5, 5);
			refinements.add(refinement);

			if(verbose) VIB.showProgress(i + 1, 24);
		}

		Collections.sort(refinements);

		for (int i = 0; i < refinements.size(); i++) {
			Refinement refinement = (Refinement)refinements.get(i);
			x[i] = refinement.best;
			orderedEulerParams[i] = refinement.adjustInitial(x[i]);
			if(verbose) 
				VIB.println((i+1) + " eulerParameters (" + refinement.min + "): " + orderedEulerParams[i][0] + ", " + orderedEulerParams[i][1] + ", " + orderedEulerParams[i][2]+ "; " + orderedEulerParams[i][3] + ", " + orderedEulerParams[i][4]+ ", " + orderedEulerParams[i][5] + "; " + orderedEulerParams[i][6] + ", " + orderedEulerParams[i][7] + ", " + orderedEulerParams[i][8]);
		}

		cachedInitialGuesses = orderedEulerParams;
		return orderedEulerParams;
	}

	public final static FastMatrix getEulerMatrix(double[] x) {
		return getEulerMatrix(x[0], x[1], x[2],
				x[3], x[4], x[5], x[6], x[7], x[8]);
	}

	public final static FastMatrix getEulerMatrix(
			double a1, double a2, double a3,
			double tX, double tY, double tZ,
			double cX, double cY, double cZ) {
		FastMatrix trans = FastMatrix.translate(tX, tY, tZ);
		FastMatrix rot = FastMatrix.rotateEulerAt(a1, a2, a3,
				cX, cY, cZ);
		return trans.times(rot);
	}

	double translateMax, angleMax;

	class Refinement implements MultivariateFunction, Comparable {
		// TODO: static class Refinement implements MFWithGradient {
		public boolean showStatus = false;
		double min;
		double[] best;
		double[] initial;

		double angleFactor;

		public Refinement(double[] start) {
			if(verbose)
				VIB.println("translateMax: " + 
				translateMax + ", angleMax: " + angleMax);
			min = Double.MAX_VALUE;
			initial = start;
			evaluate(new double[9]);
			angleFactor = angleMax / translateMax;
		}

		/*
		 * The vector x contains 6 parameters: (x,y,z) -- the
		 * translation -- and angleX, angleY, angleZ -- the rotations
		 * around the x, y and z axis.
		 *
		 * However, the angles are normalized to the same range as
		 * the coordinates of the translation, so that the precision
		 * of the optimization can be the same for all parameters of x.
		 *
		 * These transformations are applied in this order:
		 * first z, y and x rotations, and then translation
		 */
		public FastMatrix getMatrix(double[] x) {
			return getEulerMatrix(
					x[0] * angleFactor + initial[0],
					x[1] * angleFactor + initial[1],
					x[2] * angleFactor + initial[2],
					x[3] + initial[3],
					x[4] + initial[4],
					x[5] + initial[5],
					initial[6],
					initial[7],
					initial[8]);
		}

		public double evaluate(double[] x) {
			double result = calculateBadness(getMatrix(x));
			if (result < min) {
				best = (double[])x.clone();
				min = result;
				if (showStatus)
					VIB.showStatus("difference: " + min);
			}
			return result;
		}

		public int getNumArguments() {
			return 6;
		}

		public double getLowerBound(int n) {
			return n < 3 ? -angleMax / angleFactor : -translateMax;
		}

		public double getUpperBound(int n) {
			return n < 3 ? +angleMax / angleFactor : +translateMax;
		}

		public double evaluate(double[] x, double[] gradient) {
			double result = evaluate(x);
			computeGradient(x, gradient);
			return result;
		}

		public void computeGradient(double[] x, double[] gradient) {
			for (int i = 0; i < 6; i++) {
				double bup = x[i];
				x[i] = bup + 1;
				double r1 = evaluate(x);
				x[i] = bup - 1;
				double r2 = evaluate(x);
				x[i] = bup;
				gradient[i] = (r1 - r2) / 2.0;
			}
		}

		double maxAdjust;

		public double[] adjustInitial(double[] x) {
			maxAdjust = 0;
			for (int i = 0; i < 6; i++) {
				if (i < 3)
					initial[i] += x[i] * angleFactor;
				else
					initial[i] += x[i];
				if (Math.abs(x[i]) > maxAdjust)
					maxAdjust = Math.abs(x[i]);
				x[i] = 0;
			}
			return initial;
		}

		public int compareTo(Object other) {
			Refinement o = (Refinement)other;
			if (min < o.min)
				return -1;
			else if (min > o.min)
				return +1;
			else
				return 0;
		}
	}
}

