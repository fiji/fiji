package inference;

/* PRECOMPUTE : parts changed for precomputing evidence-sub-parts. now, initCount() has to be called once before doit() !!! */

// implements MAP inference for bin boundaries

public class MAPInference {
	/* INIT section */
	/* count[k] = sum_{i=0}^{k-1} data[i] */
	public /*data*/int[] data;
	public /*data*/int[] count;
	//position of the maxima.
	public short[][] maxpos;
	public short[] boundaries;
	public int Mmax;
	/*PRECOMPUTE*/
	// evidence sub-parts, i.e. LogFuncs.LogFactorial(n_m)-LogFuncs.LogPow(k-kk,n_m)
	public double[] subEvidences;


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
				subEvidences[j+(i+1)*K()]=LogFuncs.LogFactorial(n)-LogFuncs.LogPow(j-i,n);
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
		maxpos=new short[M][K()];
		boundaries=new short[M];
		for(int k=0;k<K();k++) {
			///*data*/int n=getCount(-1,k);
			//logA[k]=LogFuncs.LogFactorial(n)-LogFuncs.LogPow(k+1,n)
			//

			/*PRECOMPUTE*/
			logA[k]=subEvidences[k];
		}
		logEvidences[0]=substep4b(0);
	}


	/* calculate the inner most loop (step 4(a)ii in the paper).
	 * it is reused for step6.
	 * here, k is k_{\tilde M} and kk is k_{\tilde M-1}.
	 * furthermore, begin is m-1. */
	private double substep4aii(int k,int begin) {
		double a=-1e300/*double_max*/;
		double maxval=-1e300,curval;
		for(int kk=begin;kk<k;kk++) {
			///*data*/int n_m=getCount(kk,k);
			//a=LogFuncs.LogAddLogLog(a,logA[kk]
			//	+LogFuncs.LogFactorial(n_m)-LogFuncs.LogPow(k-kk,n_m)
			//  );

			/*PRECOMPUTE*/
			curval=logA[kk]+subEvidences[k+(kk+1)*K()];
			a=LogFuncs.LogAddLogLog(a,curval);
			if(curval>maxval) {
				maxval=curval;
				maxpos[begin][k]=(short)kk;
			}
		}
		return a;
	}

	/* calculate the evidence (step 4(b) in the paper).
	 * it is reused for step7. */
	private double substep4b(int m) {
		return logA[K()-1]+defaultLogPrior(m);
	}

	/* calculate evidences smaller than M (step 4 in the paper) */
	private int step4(int M, boolean stopAtMax) {
		for(int m=1;m<M;m++) {
			for(int k=K()-1;k>=m;k--) {
				logA[k]=substep4aii(k,m-1);
			}
			logEvidences[m]=substep4b(m);
			// stop at posterior maximum
			if(stopAtMax && (logEvidences[m]-logEvidences[m-1]<0)) return m-1;
		}
		return M;
	}

	/* calculate steps 5-7 of paper */
	void step6(int M) {
		logA[K()-1]=substep4aii(K()-1,M-1);
		logEvidences[M]=substep4b(M);
	}

	/* this is the main function, implementing the algorithm */
	public void doit(int M,boolean stopAtMax) {
		//initCount();
		init(M);
		Mmax=step4(M,stopAtMax);
		if(M==Mmax) step6(Mmax);
		computeBoundaries(Mmax);
	}

	public double defaultLogPrior(int m) {
		return 	LogFuncs.LogFactorial(K()-1-m)+2*LogFuncs.LogFactorial(m)
			-LogFuncs.LogFactorial(K()-1)-LogFuncs.LogFactorial(N()+m);
	}


	private void computeBoundaries(int M) {
		/* just for testing: print maxpos array;
		for(int j=0;j<M;j++) {
			for(int k=0;k<K();k++) System.err.print(maxpos[j][k]+" ");
			System.err.println();
		}
		*/
		boundaries[M-1]=maxpos[M-1][K()-1];
		System.err.println("MAPBB "+(M-1)+": "+boundaries[M-1]);
		for(int i=M-2;i>=0;i--) {
			boundaries[i]=maxpos[i][boundaries[i+1]];
			System.err.println("MAPBB "+i+": "+boundaries[i]);
		}
	}

	// return posterior probability of the MAP model
	public double getPosteriorProb() {
		int n=getCount(-1,boundaries[0]);
		double retval=defaultLogPrior(Mmax);
		retval+=LogFuncs.LogFactorial(n)-LogFuncs.LogPow(boundaries[0]+1,n);
		for(int i=1;i<Mmax;i++) {
			n=getCount(boundaries[i-1],boundaries[i]);
			retval+=LogFuncs.LogFactorial(n)-LogFuncs.LogPow(boundaries[i]-boundaries[i-1],n);
		}
		n=getCount(boundaries[Mmax-1],K()-1);
		retval+=LogFuncs.LogFactorial(n)-LogFuncs.LogPow(K()-1-boundaries[Mmax-1],n);
		retval-=logEvidences[Mmax];
		return Math.exp(retval);
	}


	public short getBoundary(int m) {
		if(m>=0 && m<boundaries.length) return boundaries[m];
		return -1;
	}

	public /*data*/int rebin(/*data*/int originalBin) {
		int i;
		for(i=0;i<boundaries.length && boundaries[i]<originalBin;i++);
		return i;
	}


	public static void main(String[] args) {
		MAPInference m=new MAPInference();
		for(int i=1;i<10000;i*=10) {
			int[] data={i,i,3*i,i,10*i,10*i,10*i,8*i};
			m.setData(data);
			m.doit(7,true);
			System.err.println("N: "+(44*i));
			System.err.println("MAP Prob "+m.getPosteriorProb());
			System.err.println();
		}
	}
}

