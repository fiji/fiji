package inference;

public interface InferenceCaller {
	/* these two functions are used to adapt the inference.
	 * In order to adapt the inference, inherit from this class,
	 * override these functions and call doit(this,M). */
	
	/* logExpectationFactor is called when step 4(a)ii is calculated. */
	double logExpectationFactor(int m,
			int lower_bound,int upper_bound);
	
	/* logPrior is called when step 4(b) is calculated. */
	double logPrior(int m);
}

