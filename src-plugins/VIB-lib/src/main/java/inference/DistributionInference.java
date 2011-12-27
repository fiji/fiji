package inference;

import java.lang.Math;

public class DistributionInference extends Inference implements InferenceCaller {
	public DistributionInference() {
		caller=this;
	}

	boolean evidOnly;
	double curDiriDenom;
	int curBin;
	/* the distribution */
	double maxProb,minProb;
	double[] distribution;
	public void doit(int M) {
		super.initCount();
		evidOnly=true;
		super.doit(M);
		double normal=logEvidences[M];
		evidOnly=false;
		distribution=new double[K()];
		int k;
		curDiriDenom=N()+M+1;
		double totp=0;
		maxProb=-1.0;
		minProb=1e300;
		for(curBin=0;curBin<K();curBin++) {
			super.doit(M);
			distribution[curBin]=Math.exp(logEvidences[M]-normal);
			System.err.println("Dist "+curBin+": "+distribution[curBin]);
			totp+=distribution[curBin];
			if(maxProb<distribution[curBin]) maxProb=distribution[curBin];
			if(minProb>distribution[curBin]) minProb=distribution[curBin];
		}
		System.err.println("Total probability "+totp);
	}

	/* after doit(), this function calculates the new bin index */
	public /*data*/double getProbability(/*data*/int originalBin) {
		/* TODO: binary search */
		if(originalBin<0 || originalBin>=K()) return 0.0;
		return distribution[originalBin];
	}

	public double getMaxProb() {return maxProb;};
	public double getMinProb() {return minProb;};

	public double logExpectationFactor(int m,
			int lower_bound,int upper_bound) {
		if(evidOnly || curBin<=lower_bound || curBin>upper_bound) return 0.0;
		return Math.log((getCount(lower_bound,upper_bound)+1)/curDiriDenom/(upper_bound-lower_bound));
	}

	public double logPrior(int m) {
		return defaultLogPrior(m);
	}
}

