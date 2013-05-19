// DifferentialEvolution.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


//  Price, K., and R. Storn.  1997.  Differential evolution: a simple
//  strategy for fast optimization.  Dr. Dobb's Journal 264(April), pp. 18-24.
//  Strategy used here:  DE/rand-to-best/1/bin
//  http://www.icsi.berkeley.edu/~storn/code.html


package pal.math;


/**
 * <b>global</b> minimization of a real-valued function of several
 * variables without using derivatives using a genetic algorithm
 * (Differential Evolution)
 * @author Korbinian Strimmer
 */
public class DifferentialEvolution extends MultivariateMinimum
{
	//
	// Public stuff
	//
        
	// Variables that control aspects of the inner workings of the
        // minimization algorithm. Setting them is optional, they      
        // are all set to some reasonable default values given below.        

	/** weight factor (default 0.7) */
	public double F = 0.7 /* 0.5*/;
	
	/** Crossing over factor (default 0.9) */
	public double CR = 0.9 /*1.0*/;
		
	/**
	 * variable controlling print out, default value = 0
	 * (0 -> no output, 1 -> print final value, 
	 * 2 -> detailed map of optimization process)
	 */
	public int prin = 0; 


	/**
	 * construct DE optimization modul (population size is
	 * selected automatically)
	 *
	 * <p><em>DE web page:</em> 
	 * <a href="http://www.icsi.berkeley.edu/~storn/code.html"
	 * >http://www.icsi.berkeley.edu/~storn/code.html</a> 
         *
	 * @param dim dimension of optimization vector 
	 */ 
	public DifferentialEvolution (int dim)
	{
		this(dim, 5*dim);
	}

	/**
	 * construct optimization modul
	 *
	 * @param dim dimension of optimization vector
	 * @param popSize population size 
	 */ 	
	public DifferentialEvolution (int dim, int popSize) 
	{
		// random number generator
		rng = new MersenneTwisterFast();
		
		// Dimension and Population size
		dimension = dim;
		populationSize = popSize;
		
		numFun = 0;		
		
		// Allocate memory
		currentPopulation = new double[populationSize][dimension];
		nextPopulation = new double[populationSize][dimension];
		costs = new double[populationSize];
		trialVector = new double[dimension];
				
		// helper variable
		//numr = 5; // for strategy DE/best/2/bin
		numr = 3; // for stragey DE/rand-to-best/1/bin
		r = new int[numr]; 
	}
	
	
	// implementation of abstract method 
	
	public void optimize(MultivariateFunction func, double[] xvec, double tolfx, double tolx)
	{
		f = func;
		x = xvec;
	
		// Create first generation
		firstGeneration ();
		
		stopCondition(fx, x, tolfx, tolx, true);
			
		while (true)
		{
			boolean xHasChanged;
			do
			{
				xHasChanged = nextGeneration ();
				
				if (maxFun > 0 && numFun > maxFun)
				{
					break;
				}

				if (prin > 1 && currGen % 20 == 0)
				{
					printStatistics();
				}
			}
			while (!xHasChanged);
			
			
			if (stopCondition(fx, x, tolfx, tolx, false) || 
				(maxFun > 0 && numFun > maxFun))
			{
				break;		
			}
		}
		
		if (prin > 0) printStatistics();
	}
	
	//
	// Private stuff
	//

	private MultivariateFunction f;
	private int currGen;
	private double fx;
	private double[] x;
	
	// Dimension
	private int dimension;
	
	// Population size
	private int populationSize; 
			
	// Population data
	private double trialCost;
	private double[] costs;
	private double[] trialVector;
	private double[][] currentPopulation;
	private double[][] nextPopulation;

	// Random number generator
	private MersenneTwisterFast rng;
	
	// Helper variable
	private int numr;
	private int[] r;
	
	private void printStatistics()
	{
		// Compute mean
		double meanCost = 0.0;
		for (int i = 0; i < populationSize; i++)
		{
			meanCost += costs[i];
		}
		meanCost = meanCost/populationSize;
				
		// Compute variance
		double varCost = 0.0;
		for (int i = 0; i < populationSize; i++)
		{
			double tmp = (costs[i]-meanCost);
			varCost += tmp*tmp;
		}
		varCost = varCost/(populationSize-1);
			
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("Smallest value: " + fx);
		System.out.println();
		for (int k = 0; k < dimension; k++)
		{
			System.out.println("x[" + k + "] = " + x[k]);
		}
		System.out.println();
		System.out.println("Current Generation: " + currGen);
		System.out.println("Function evaluations: " + numFun);
		System.out.println("Populations size (populationSize): " + populationSize);
		System.out.println("Average value: " + meanCost);
		System.out.println("Variance: " + varCost);

		System.out.println("Weight factor (F): " + F);
		System.out.println("Crossing-over (CR): " + CR);
		System.out.println();
	}
	
	// Generate starting population
	private void firstGeneration()
	{
		currGen = 1;
		
		// Construct populationSize random start vectors
		for (int i = 0; i < populationSize; i++)
		{
			for (int j = 0; j < dimension; j++ )
			{
				double min = f.getLowerBound(j);
				double max = f.getUpperBound(j);
				
				double diff = max - min;
				
				// Uniformly distributed sample points
				currentPopulation[i][j] = min + diff*rng.nextDouble();
			}
			costs[i] = f.evaluate(currentPopulation[i]);
		}
		numFun += populationSize;
		
		findSmallestCost ();
	}
	
	// check whether a parameter is out of range
	private double checkBounds(double param, int numParam)
	{
		if (param < f.getLowerBound(numParam))
		{
			return f.getLowerBound(numParam);
		}
		else if (param > f.getUpperBound(numParam))
		{
			return f.getUpperBound(numParam);
		}
		else
		{
			return param;
		}
	}
	
	// Generate next generation
	private boolean nextGeneration()
	{
		boolean updateFlag = false;
		int best = 0; // to avoid compiler complaints
		double[][] swap;
		
		currGen++;
		
		// Loop through all population vectors
		for (int r0 = 0; r0 < populationSize; r0++)
		{
			// Choose ri so that r0 != r[1] != r[2] != r[3] != r[4] ...
		
			r[0] = r0;			
			for (int k = 1; k < numr; k++)
			{
				r[k] = randomInteger (populationSize-k);
				for (int l = 0; l < k; l++)
				{
					if (r[k] >= r[l])
					{
						r[k]++;
					}
				}
			}
			
			copy(trialVector, currentPopulation[r0]);
			int n = randomInteger (dimension); 
			for (int i = 0; i < dimension; i++) // perform binomial trials
			{
				// change at least one parameter
				if (rng.nextDouble() < CR || i == dimension - 1)
				{                       
					// DE/rand-to-best/1/bin
					// (change to 'numr=3' in constructor when using this strategy)
					trialVector[n] = trialVector[n] +
						F*(x[n] - trialVector[n]) +
						F*(currentPopulation[r[1]][n] - currentPopulation[r[2]][n]);

					//DE/rand-to-best/2/bin
					//double K = rng.nextDouble();
					//trialVector[n] = trialVector[n] +
					//	K*(x[n] - trialVector[n]) +
					//	F*(currentPopulation[r[1]][n] - currentPopulation[r[2]][n]);
					
							     
	       				// DE/best/2/bin
					// (change to 'numr=5' in constructor when using this strategy)
	       				//trialVector[n] = x[n] + 
		     			//	 (currentPopulation[r[1]][n]+currentPopulation[r[2]][n]
					//	 -currentPopulation[r[3]][n]-currentPopulation[r[4]][n])*F;
				}
				n = (n+1) % dimension;
			}

			// make sure that trial vector obeys boundaries
			for (int i = 0; i < dimension; i++)
			{
				trialVector[i] = checkBounds(trialVector[i], i);
			}
			
			
			// Test this choice
			trialCost = f.evaluate(trialVector);
			if (trialCost < costs[r0])
			{
				// Better than old vector
				costs[r0] = trialCost;
				copy(nextPopulation[r0], trialVector);
				
				// Check for new best vector
				if (trialCost < fx)
				{
					fx = trialCost;
					best = r0;
					updateFlag = true;
				}
			}
			else
			{
				// Keep old vector
				copy(nextPopulation[r0], currentPopulation[r0]);
			}			
		}
		numFun += populationSize;
		
		// Update best vector
		if (updateFlag)
		{
			copy(x, nextPopulation[best]);
		}
		
		// Switch pointers
		swap = currentPopulation;
		currentPopulation = nextPopulation;
		nextPopulation = swap;
		
		return updateFlag;
	}
	
	// Determine vector with smallest cost in current population
	private void findSmallestCost()
	{
		int best = 0;
		fx = costs[0];
		
		for (int i = 1; i < populationSize; i++)
		{
			if (costs[i] < fx)
			{
				fx = costs[i];
				best = i;
			}
		}
		copy(x, currentPopulation[best]);
	}
	
	// draw random integer in the range from 0 to n-1
	private int randomInteger(int n)
	{
		return rng.nextInt(n);
	}
}

