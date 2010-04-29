package inference;

import java.lang.Math;

public class BinClassificationInference extends BinBoundariesComputation implements InferenceCaller {
	public BinClassificationInference() {
		caller=this;
	}

	/* the number of the bin */
	public int m_prime;
	/* the position we are currently evaluating */
	public int curx;

	/* the output */
	int[] boundaries;
	public void doit(int M) {
		super.initCount();
		m_prime=M+2; // ignore m_prime
		super.doit(M);
		double normal=logEvidences[M];

		boundaries=new int[K()];
		double totp,maxp,curp;
		int maxm;
		for(curx=0;curx<K();curx++) {
			totp=0.0;
			maxp=curp=0.0;
			maxm=0;
			for(m_prime=0;m_prime<=M;m_prime++) {
				super.doit(M);
				curp=Math.exp(logEvidences[M]-normal);
				if(curp>maxp) {
					maxp=curp;
					maxm=m_prime;
				}
				totp+=curp;
			}
			// totp only for testing. has to be 1 here
			boundaries[curx]=maxm;
			System.err.println("BCI "+curx+": "+maxm+" totp :"+totp);
		}
	}

	/* after doit(), this function calculates the new bin index */
	public /*data*/int rebin(/*data*/int originalBin) {
		return boundaries[originalBin];
	}

	public double logExpectationFactor(int m,
			int lower_bound,int upper_bound) {
		if(m!=m_prime) return 0.0;
		if(curx>lower_bound && curx<=upper_bound) return 0.0;
		return -1e300;
	}


	public double logPrior(int m) {
		return defaultLogPrior(m);
	}
}

