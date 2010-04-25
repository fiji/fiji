// ConjugateGradientSearch.java
//
// (c) 2000-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the GNU Lesser General Public License (LGPL)


package pal.math;


/**
 * minimization of a real-valued function of
 * several variables using a the nonlinear
 * conjugate gradient method where several variants of the direction
 * update are available (Fletcher-Reeves, Polak-Ribiere,
 * Beale-Sorenson, Hestenes-Stiefel) and bounds are respected.
 * Gradients are computed numerically if they are not supplied by the
 * user.  The line search is entirely based on derivative
 * evaluation, similar to the strategy used in macopt (Mackay).
 *
 *
 * @author Korbinian Strimmer
 */
public class ConjugateGradientSearch extends MultivariateMinimum
{
	//
	// Public stuff
	//

	public final static int FLETCHER_REEVES_UPDATE = 0;
	public final static int POLAK_RIBIERE_UPDATE = 1;
	public final static int BEALE_SORENSON_HESTENES_STIEFEL_UPDATE = 2;

	// Variables that control aspects of the inner workings of the
	// minimization algorithm. Setting them is optional, they      
	// are all set to some reasonable default values given below.

	/**
	 *  controls the printed output from the routine        
	 *  (0 -> no output, 1 -> print only starting and final values,            
	 *   2 -> detailed map of the minimisation process),
	 *  the default value is 0
	 */
	public int prin = 0;
	
	/**
	 * defaultStep is a steplength parameter and should be set equal     
	 * to the expected distance from the solution (in a line search)          
	 * exceptionally small or large values of defaultStep lead to   
	 * slower convergence on the first few iterations (the step length
	 * itself is adapted during search), the default value is 1.0                     
	 */
	public double defaultStep = 1.0;
		

	/**
	 * conjugateGradientStyle determines the method for the
	 * conjugate gradient direction update
	 * update (0 -> Fletcher-Reeves, 1 -> Polak-Ribiere,
	 * 2 -> Beale-Sorenson, Hestenes-Stiefel), the default is 2.
	 */
	public int conjugateGradientStyle = BEALE_SORENSON_HESTENES_STIEFEL_UPDATE;


	public ConjugateGradientSearch() {
	}

	public ConjugateGradientSearch(int conGradStyle) {
		this.conjugateGradientStyle = conGradStyle;
	}


	// implementation of abstract method

	public void optimize(MultivariateFunction f, double[] x, double tolfx, double tolx)
	{
		xvec = x;
		numArgs = f.getNumArguments();
		
		boolean numericalGradient;
		if (f instanceof MFWithGradient)
		{
			numericalGradient = false;
			fgrad = (MFWithGradient) f;
		}
		else
		{
			numericalGradient = true;
			fgrad = null;
		}
		
		
		// line function
		LineFunction lf = new LineFunction(f);
		
		// xvec contains current guess for minimum
		lf.checkPoint(xvec);
		double[] xold = new double[numArgs];
		copy(xold, xvec);
		
		// function value and gradient at current guess
		numFun = 0;
		double fx;
		numGrad = 0;
		gvec = new double[numArgs];
		if (numericalGradient)
		{
			fx = f.evaluate(xvec);
			numFun++;
			NumericalDerivative.gradient(f, xvec, gvec);
			numFun += 2*numArgs;
		}
		else
		{
			fx = fgrad.evaluate(xvec, gvec);
			numFun++;
			numGrad++;
		}
		double[] gold = new double[numArgs];
		copy(gold, gvec);

		// init stop condition
		stopCondition(fx, xvec, tolfx, tolx, true);

		// currently active variables
		boolean[] active = new boolean[numArgs];
		double numActive = lf.checkVariables(xvec, gvec, active);
		
		// if no variables are active return
		if (numActive == 0)
		{
			return;
		}

		// initial search direction (steepest descent)
		sdir = new double[numArgs];
		steepestDescentDirection(sdir, gvec, active);
		lf.update(xvec, sdir);

		// slope at start point in initial direction
		double slope = gradientProjection(sdir, gvec);

		
		if (prin > 0)
		{
			System.out.println("--- starting minimization ---");
			System.out.println("... current parameter settings ...");
			System.out.println("...   tolx   ... " + tolx);
			System.out.println("...   tolfx   ... " + tolfx);
			System.out.println("... maxFun  ... " + maxFun);
			System.out.println();
			printVec("... start vector ...", xvec);
			System.out.println();
			printVec("... start direction ...", sdir);	
		}
		
		
		int numLin = 0;
		lastStep = defaultStep;
		while(true)
		{
			// determine an appropriate step length
			double step = findStep(lf, fx, slope, numericalGradient);
			lastStep = step;
			numLin++;
			
			// update xvec
			lf.getPoint(step, xvec);
			lf.checkPoint(xvec);

			
			// function value at current guess
			if (numericalGradient)
			{
				fx = f.evaluate(xvec);
				numFun++;
			}
			else
			{
				// compute gradient as well
				fx = fgrad.evaluate(xvec, gvec);
				numFun++;
				numGrad++;
			}

			// test for for convergence
			if (stopCondition(fx, xvec, tolfx, tolx, false)
				|| (maxFun > 0 && numFun > maxFun))
			{
				break;
			}
	
			// Compute numerical gradient
			if (numericalGradient)
			{
				NumericalDerivative.gradient(f, xvec, gvec);
				numFun += 2*numArgs;
			}
			
			
			numActive = lf.checkVariables(xvec, gvec, active);
			
			// if all variables are inactive return
			if (numActive == 0)
			{
				break;
			}
					
			// determine new search direction
			conjugateGradientDirection(sdir, gvec, gold, active);
			lf.checkDirection(xvec, sdir);
			
			// compute slope in new direction
			slope = gradientProjection(sdir, gvec);
			
			if (slope >= 0)
			{
				//reset to steepest descent direction
				steepestDescentDirection(sdir, gvec, active);

				// compute slope in new direction
				slope = gradientProjection(sdir, gvec);
				
				// reset to default step length
				lastStep = defaultStep;
			}
				
			
			// other updates
			lf.update(xvec, sdir);
			copy(xold, xvec);
			copy(gold, gvec);
			
			if (prin > 1)
			{
				System.out.println();
				System.out.println("Function value: " +
				f.evaluate(xvec));
				System.out.println();
				printVec("... new vector ...", xvec);
				System.out.println();
				printVec("... new direction ...", sdir);	
				System.out.println("... numFun  ... " + numFun);
				if (!numericalGradient)
				{
					System.out.println("... numGrad  ... " + numGrad);
				}
				System.out.println("... numLin  ... " + numLin);
				System.out.println();
			}		
		}
		
		if (prin > 0)
		{
			System.out.println();
			printVec("... final vector ...", xvec);	
			System.out.println("... numFun  ... " + numFun);
			System.out.println("... numLin  ... " + numLin);
			System.out.println();
			System.out.println("--- end of minimization ---");
		}
	}
                                                                	

	//
	// Private stuff
	//
	
	private int numArgs, numGrad;
	private double lastStep;
	private double[] xvec, gvec, sdir;
	private MFWithGradient fgrad;

	private double findStep(LineFunction lf, double f0, double s0, boolean numericalGradient)
	{
		// f0 function value at step = 0
		// s0 slope at step = 0
	
		double step;
		double maxStep = lf.getUpperBound();
		if (maxStep <= 0 || s0 == 0)
		{
			return 0.0;
		}
		
		//step = Math.abs(lf.findMinimum());


		// growing/shrinking factors for bracketing
				
		double g1 = 2.0;
		double g2 = 1.25;
		double g3 = 0.5;
		
		// x1 and x2 try to bracket the minimum

		double x1 = 0;
		double s1 = s0;
		double x2 = lastStep*g2;
		if(x2 > maxStep)
		{
			x2 = maxStep*g3;
		}
		double s2 = computeDerivative(lf, x2, numericalGradient);
				
		// we need to go further to bracket minimum
		boolean boundReached = false;
		while (s2 <= 0 && !boundReached) 
		{
			x1 = x2;
			s1 = s2;
			x2 = x2*g1;
			if (x2 > maxStep)
			{
				x2 = maxStep;
				boundReached = true;
			}
			s2 = computeDerivative(lf, x2, numericalGradient);
		}
		
		// determine step length by quadratic interpolation
		// for minimum in interval [x1,x2]
		
		if (s2 <= 0)
		{
			// true local minimum could NOT be bracketed
			// instead we have a local minimum on a boundary 
			
			step = x2;
		}
		else
		{
			// minimum is bracketed
			
			step = (x1*s2-x2*s1)/(s2-s1);
			// note that nominator is always positive
		}			
		
		// just to be safe - should not be necessary
		if (step >= maxStep)
		{
			step = maxStep;
		}
		if (step < 0)
		{
			step = 0;
		}
						
		return step;
	}

	private double computeDerivative(LineFunction lf, double lambda, boolean numericalGradient)
	{
		if (numericalGradient)
		{
			numFun += 2;
			return NumericalDerivative.firstDerivative(lf, lambda);
		}
		else
		{
			/* lf.getPoint(lambda, xvec);
			lf.checkPoint(xvec);
			fgrad.computeGradient(xvec, gvec);
			numGrad++;
			return gradientProjection(sdir, gvec);
			*/
			
			// the following code prevents overstepping
			// and is due to Jesse Stone <jessestone@yahoo.com>
			
			double[] xtmp = new double[numArgs];
			copy(xtmp, xvec);
			lf.getPoint(lambda, xtmp);
			lf.checkPoint(xtmp);
			fgrad.computeGradient(xtmp, gvec);
			numGrad++;
			return gradientProjection(sdir, gvec);
		
		}
	}

	private void testStep(double f0, double s0, double f1, double s1, double step)
	{
		// f0  function value at x=0
		// s0  slope at x=0
		
		// f1 function value at x=step
		// f2 function value at x=step
		
		double mue = 0.0001;
		double eta = 0.9;
		
		// sufficent decrease in function value
		if (f1 <= mue*s0*step + f0)
		{
			System.out.println("<<< Sufficient decrease in function value");
		}
		else
		{
			System.out.println("<<< NO sufficient decrease in function value");
		}
		
		// sufficient decrease in slope
		if (Math.abs(s1) <= eta*Math.abs(s0))
		{
			System.out.println("<<< Sufficient decrease in slope");
		}
		else
		{
			System.out.println("<<< NO sufficient decrease in slope");
		}
	}


	private void conjugateGradientDirection(double[] sdir, double[] gvec, double[] gold,
		boolean[] active)
	{
		double gg = 0;
		double dgg = 0;
		for (int i = 0; i < numArgs; i++)
		{
			if (active[i])
			{
				switch (conjugateGradientStyle)
				{
					case 0:
						// Fletcher-Reeves
						dgg += gvec[i]*gvec[i];
						gg += gold[i]*gold[i];
						break;
	
					case 1:
						// Polak-Ribiere
						dgg += gvec[i]*(gvec[i]-gold[i]);
						gg += gold[i]*gold[i];
						break;
					
					case 2:
						// Beale-Sorenson
						// Hestenes-Stiefel
						dgg += gvec[i] * (gvec[i] - gold[i]);
						gg += sdir[i] * (gvec[i] - gold[i]);
						break;
				}
			}
		}
		double beta = dgg/gg;
		if (beta < 0 || gg == 0)
		{
			// better convergence (Gilbert and Nocedal)
			beta = 0;
		}
		for (int i = 0; i < numArgs; i++)
		{
			if (active[i])
			{
				sdir[i] = -gvec[i] + beta*sdir[i];
			}
			else
			{
				sdir[i] = 0;
			}
		}
	}

	private void steepestDescentDirection(double[] sdir, double[] gvec, boolean[] active)
	{
		for (int i = 0; i < numArgs; i++)
		{
			if (active[i])
			{
				sdir[i] = -gvec[i];
			}
			else
			{
				sdir[i] = 0;
			}
		}
	}

	private double gradientProjection(double[] sdir, double[] gvec)
	{
		double s = 0;
		double n = gvec.length;
		for (int i = 0; i < n; i++)
		{
			s += gvec[i]*sdir[i];
		}
		
		return s;
	}
	
	private void printVec(String s, double[] x)
	{
		System.out.println(s);
		for (int i=0; i < x.length; i++)
		{
			System.out.print(x[i] + "  ");
		}
		System.out.println();
	}
}
