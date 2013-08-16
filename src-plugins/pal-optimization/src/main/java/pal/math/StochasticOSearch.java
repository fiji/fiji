// StochasticOSearch.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * minimization of a real-valued function of
 * several variables without using derivatives, using the simple
 * strategy of optimizing variables one by one. Variable selection is stochastic
 *
 * @author Matthew Goode Korbinian Strimmer
 */
public class StochasticOSearch extends MultivariateMinimum {
	//
	// Public stuff
	//

	//
	// Private stuff
	//
	private static MersenneTwisterFast random = new MersenneTwisterFast();
	/**
	 * Initialization
	 */
	public StochasticOSearch() {
		um = new UnivariateMinimum();
	}


	// implementation of abstract method

	public void optimize(MultivariateFunction f, double[] xvec, double tolfx, double tolx)
	{
		numArgs = f.getNumArguments();

		numFun = 1;
		double fx = f.evaluate(xvec);

		stopCondition(fx, xvec, tolfx, tolx, true);

		OrthogonalLineFunction olf = new OrthogonalLineFunction(f);
		olf.setAllArguments(xvec);
		int[] ordering = new int[numArgs];
		for(int i = 0 ; i < ordering.length ; i++) {
			ordering[i] = i;
		}
		while (true) {
			random.shuffle(ordering);

			for (int i = 0; i < numArgs; i++)	{
				int argument = ordering[i];
				//System.out.println("i:"+argument);
				olf.selectArgument(argument);
				// Note that we don't use xvec as starting point
				// in the 1d line minimization.
				xvec[argument] = um.optimize(olf, tolx);
				//If we actually found a better minimum...
				if(um.fminx<fx) {
					olf.setArgument(xvec[argument]);
					fx = um.fminx;
				}
				numFun += um.numFun;

			}
			//fx = um.fminx;
			if (stopCondition(fx, xvec, tolfx, tolx, false) ||
				(maxFun > 0 && numFun > maxFun) ||
				 numArgs == 1) {
				break;
			}
		}
	}


	//
	// Private stuff
	//

	private UnivariateMinimum um;
	private int numArgs;
}
