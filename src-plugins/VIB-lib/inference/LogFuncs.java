package inference;

import java.lang.Math;

public class LogFuncs {
	private static final LogFuncs _theInstance=new LogFuncs();

	private final static int maxFactorial=10000;

	private LogFuncs() {
		factorials=new double[maxFactorial];
		factorials[0]=0;
		for(int i=1;i<maxFactorial;i++)
			factorials[i]=factorials[i-1]+Math.log(i);
	}

	private double[] factorials;

	public static double LogFactorial(int n) {
		if(n>=0 && n<maxFactorial)
			return _theInstance.factorials[n];
		return LogGamma(n-1);
	}

	public static double LogPow(double base,double exponent) {
		return exponent*Math.log(base);
	}

	/* log(A+B)
	 * = log(exp(log(A))+exp(log(B)))
	 * = log(exp(log(A))*(1+exp(log(B)-log(A)))
	 * = log(A)+log(1+exp(log(B)-log(A))) */
	public static double LogAddLogLog(double loga,double logb) {
		if(loga>logb)
			// TODO: return loga+Math.log1p(Math.exp(logb-loga));
			return loga+Math.log(1+Math.exp(logb-loga));
		else
			// TODO: return logb+Math.log1p(Math.exp(loga-logb));
			return logb+Math.log(1+Math.exp(loga-logb));
	}

	public final static double pi=3.1415926535897932384626433832795029;
	final static double[] p={1.000000000190015, 76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 1.208650973866179e-3, -5.395239384953e-6};
	// use Lanczos' approximation
	public static double LogGamma(double z) {
		double sum=p[0];
		for(int i=1;i<7;i++)
			sum+=p[i]/(z+i);
		return 0.5*Math.log(2*pi)-Math.log(z)+Math.log(sum)+(z+0.5)*Math.log(z+5.5)-(z+5.5);
	}

	public static double LogPoisson(double lambda,double x)
	{
		return -lambda+x*Math.log(lambda)-LogGamma(x+1);
	}

}

