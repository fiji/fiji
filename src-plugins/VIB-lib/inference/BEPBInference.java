package inference;

/* PRECOMPUTE : parts changed for precomputing evidence-sub-parts. now, initCount() has to be called once before doit() !!! */

// Modificartion of Inference for equi-probable binning


public class BEPBInference {
	/* INIT section */
	/* count[k] = sum_{i=0}^{k-1} data[i] */
	public /*data*/int[] data;
	public /*data*/int[] count;
	/*PRECOMPUTE*/
	// evidence sub-parts, i.e. LogFuncs.LogFactorial(n_m)-LogFuncs.LogPow(k-kk,n_m)
	public double[] subEvidences;

	public InferenceCaller caller;

	public void setData(int[] data_) {
		data=data_;
		initCount();
	}

	public void setData(java.util.Vector data_) {
		int size=data_.size();
		data=new int[size];
		for(int i=0;i<size;i++)
			data[i]=((Integer)data_.get(i)).intValue();
		initCount();
	}

	/* initialise the variable "count" for the current "data" */
	public void initCount() {
		int i,j,n;
	        count=new /*data*/int[K()+1];
		subEvidences=new double[K()*K()];
	        for(i=1,count[0]=0;i<=K();i++)
	                count[i]=count[i-1]+data[i-1];

		/*PRECOMPUTE*/
		for(i=-1;i<K()-1;i++)
			for(j=i+1;j<K();j++) {
				n=getCount(i,j);
				subEvidences[j+(i+1)*K()]=-LogFuncs.LogPow(j-i,n);
			}

	}

	/* getCount(k1,k2) returns the number of data points in bins
	 * greater than k1 and smaller or equal to k2 (see paper) */
	public /*data*/int getCount(int k1,int k2) /*const*/ {
		return count[k2+1]-count[k1+1];
	}

	/* K is the original number of bins */
	public int K() /*const*/ {
		return data.length;
	}

	/* N is the total count of data points. */
	public /*data*/int N() /*const*/ {
		return count[K()];
	}


	/* CALCULATE section */
	/* logA and logEvidences contain the logarithms of the temporary
	 * array a and the array evidences from the paper */
	double[] logA,logEvidences;

	/* initialise the arrays (steps 1-3 in the paper) */
	private void init(int M) {
	        logA=new double[K()];
	        logEvidences=new double[M+1];
		for(int k=0;k<K();k++) {
			///*data*/int n=getCount(-1,k);
			//logA[k]=-LogFuncs.LogPow(k+1,n)
			//	+caller.logExpectationFactor(0,0,k);

			/*PRECOMPUTE*/
			logA[k]=subEvidences[k]+caller.logExpectationFactor(0,-1,k);
		}
		logEvidences[0]=substep4b(0);
	}


	/* calculate the inner most loop (step 4(a)ii in the paper).
	 * it is reused for step6.
	 * here, k is k_{\tilde M} and kk is k_{\tilde M-1}.
	 * furthermore, begin is m-1. */
	private double substep4aii(int k,int begin) {
		double a=-1e300/*double_max*/;
		for(int kk=begin;kk<k;kk++) {
			///*data*/int n_m=getCount(kk,k);
			//a=LogFuncs.LogAddLogLog(a,logA[kk]
			//	+LogFuncs.LogFactorial(n_m)-LogFuncs.LogPow(k-kk,n_m)
			//		+caller.logExpectationFactor(begin+1,kk,k));

			/*PRECOMPUTE*/
			a=LogFuncs.LogAddLogLog(a,logA[kk]+subEvidences[k+(kk+1)*K()]
							+caller.logExpectationFactor(begin+1,kk,k));
		}
		return a;
	}

	/* calculate the evidence (step 4(b) in the paper).
	 * it is reused for step7. */
	private double substep4b(int m) {
		return logA[K()-1]+caller.logPrior(m);
	}

	/* calculate evidences smaller than M (step 4 in the paper) */
	private void step4(int M) {
		for(int m=1;m<M;m++) {
			for(int k=K()-1;k>=m;k--) {
				logA[k]=substep4aii(k,m-1);
			}
			logEvidences[m]=substep4b(m);
		}
	}

	/* calculate steps 5-7 of paper */
	void step6(int M) {
		logA[K()-1]=substep4aii(K()-1,M-1);
		logEvidences[M]=substep4b(M);
	}

	/* this is the main function, implementing the algorithm */
	public void doit(int M) {
		//initCount();
		init(M);
		if(M>0) {
			step4(M);
			step6(M);
		}
	}

	public double defaultLogPrior(int m) {
		return 	LogFuncs.LogFactorial(K()-1-m)+LogFuncs.LogFactorial(m)
			-LogFuncs.LogFactorial(K()-1)-N()*Math.log(m+1);
			//-LogFuncs.LogFactorial(N()+m) What was I thinking?

	}

	public static void main(String[] args) {
		BEPBBinBoundariesInference m=new BEPBBinBoundariesInference();
		BEPBModelInference moi=new BEPBModelInference();
		int[] data={0,0,0,0,100,0,0,0};
		moi.setData(data);
		moi.doit(7,0.03);
		System.err.println("best M "+moi.m_winner);

		m.setData(data);
		m.doit(moi.m_winner);
		for(int k=0;k<data.length;k++) System.err.println(k+" -> "+m.rebin(k));

	}
}

