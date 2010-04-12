// GeneralizedDEOptimizer.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

package pal.math;


/**
 * Provides an general interface to the DifferentialEvolution class that is not
 * tied to a certain number of parameters (as DifferentialEvolution is). Works but
 * creating a new DiffentialEvolution engine when presented with a new number of
 * parameters. All the actual optimisation work is handled by DifferentialEvolution.,
 * @author Matthew Goode
 * @version $Id: GeneralizedDEOptimizer.java,v 1.1 2006/06/01 15:27:32 gene099 Exp $
 */


public class GeneralizedDEOptimizer extends MultivariateMinimum {

	private DifferentialEvolution optimiser_;

  private int currentNumberOfParameters_ = 0;

	public GeneralizedDEOptimizer() {
	}



	/**
	 * The actual optimization routine
	 * It finds a minimum close to vector x when the
	 * absolute tolerance for each parameter is specified.
         *
	 * @param f multivariate function
	 * @param xvec initial guesses for the minimum
	 *         (contains the location of the minimum on return)
	 * @param tolfx absolute tolerance of function value
	 * @param tolx absolute tolerance of each parameter
 	 */
	public void optimize(MultivariateFunction f, double[] xvec, double tolfx, double tolx) {
   	if(optimiser_==null||xvec.length!=currentNumberOfParameters_) {
     	optimiser_ = new DifferentialEvolution(xvec.length);
      this.currentNumberOfParameters_= xvec.length;
    }
    optimiser_.optimize(f,xvec,tolfx, tolx);
  }

}
