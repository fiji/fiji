// OrthogonalSearch.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.math;


/**
 * minimization of a real-valued function of
 * several variables without using derivatives, using the simple
 * strategy of optimizing variables one by one.
 *
 * @author Korbinian Strimmer
 */
public class OrthogonalSearch extends MultivariateMinimum
{
	//
	// Public stuff
	//

	/**
	 * Initialization
	 */
	public OrthogonalSearch()
	{
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

		while (true)
		{
			for (int i = 0; i < numArgs; i++)
			{
				olf.selectArgument(i);

				// Note that we don't use xvec as starting point
				// in the 1d line minimization.
				xvec[i] = um.optimize(olf, tolx);
				olf.setArgument(xvec[i]);

				numFun += um.numFun;
			}
			fx = um.fminx;
			if (stopCondition(fx, xvec, tolfx, tolx, false) ||
				(maxFun > 0 && numFun > maxFun) ||
				 numArgs == 1)
			{
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
