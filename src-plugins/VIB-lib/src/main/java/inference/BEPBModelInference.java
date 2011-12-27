package inference;

/* this class calculates the expectation for M (step 4(a)ii in the paper).
 * it illustrates how to write adaptation for the inference of other
 * functions (for example bin boundaries, or the variance of P_m). */
public class BEPBModelInference extends BEPBInference implements InferenceCaller {
	public BEPBModelInference() {
		caller=this;
	}

	/* the suggested M is between lower and upper */
	public int lower,upper,m_winner;
	/* probability is the minimal probability of M to be between
	 * lower and upper. */
	public void doit(int M,double probability) {
		super.initCount();
		super.doit(M);
		/* find k with maximum evidence */
		m_winner=0;
		double total=-1e300 /*double_max*/;
		for(int m=0;m<=M;m++) {
			System.err.println("BEPB Evidence "+m+": "+logEvidences[m]);
			total=LogFuncs.LogAddLogLog(total,logEvidences[m]);
			if(logEvidences[m]>logEvidences[m_winner])
				m_winner=m;
		}
		/* now expand the interval (greedy algorithm) */
		lower=upper=m_winner;
		double current_probability=Math.exp(logEvidences[m_winner]-total);
		while(current_probability<probability) {
			if(upper==M || (lower!=0 && logEvidences[lower-1]>logEvidences[upper+1])) {
				lower--;
				current_probability+=Math.exp(logEvidences[lower]-total);
			} else {
				upper++;
				current_probability+=Math.exp(logEvidences[upper]-total);
			}
		}
	}

	public double logExpectationFactor(int m,
			int lower_bound,int upper_bound) {
		return 0;
	}

	public double logPrior(int m) {
		return defaultLogPrior(m);
	}
}

