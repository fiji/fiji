package inference;

import java.lang.Math;

public class BinBoundariesInference extends BinBoundariesComputation implements InferenceCaller {
	public BinBoundariesInference() {
		caller=this;
	}

	/* the number of the bin, the upper boundary of which we are interested in */
	public int m_prime;

	/* the output */
	double[] boundaries;
	public void doit(int M) {
		super.initCount();
		m_prime=M+2; // ignore m_prime
		super.doit(M);
		double normal=logEvidences[M];

		boundaries=new double[M];
		for(m_prime=0;m_prime<M;m_prime++) {
			super.doit(M);
			boundaries[m_prime]=Math.exp(logEvidences[M]-normal);
			// rounding seems to make sense. but this is only an approximation, anyway
			boundaries[m_prime]=Math.round(boundaries[m_prime]);
			System.err.println("BBI "+m_prime+": "+boundaries[m_prime]);
		}
	}

	public int getBoundary(int m) {
		if(m>=0 && m<boundaries.length) return (int)boundaries[m];
		return -1;
	}

	/* after doit(), this function calculates the new bin index */
	public /*data*/int rebin(/*data*/int originalBin) {
		/* TODO: binary search */
		int i;
		for(i=0;i<boundaries.length && boundaries[i]<originalBin;i++);
		return i;
	}

	public double logExpectationFactor(int m,
			int lower_bound,int upper_bound) {
		return (m==m_prime?Math.log((double)upper_bound):0.0);
	}

	public double logPrior(int m) {
		return defaultLogPrior(m);
	}
}

