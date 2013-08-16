package inference;

/* The abstract class (how do I make it abstract?) from which rebinning methods are to be derived */

public class BinBoundariesComputation extends Inference implements InferenceCaller {
	public BinBoundariesComputation() {
		caller=this;
	}


	/* after doit(), this function calculates the new bin index */
	public /*data*/int rebin(/*data*/int originalBin) {
		return 0;
	}

	public double logExpectationFactor(int m,
			int lower_bound,int upper_bound) {
		return 0.0;
	}

	public int getBoundary(int m) {
		return 0;
	}

	public double logPrior(int m) {
		return defaultLogPrior(m);
	}
}
