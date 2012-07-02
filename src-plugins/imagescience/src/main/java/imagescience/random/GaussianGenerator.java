package imagescience.random;

import java.lang.Math;

/** Gaussian random number generator. This implementation is based on the Box-Muller transformation applied to uniform random numbers, the latter of which are obtained from class {@link UniformGenerator}. For more details, see for example W. H. Press, S. A. Teukolsky, W. T. Vetterling, B. P. Flannery, <a href="http://www.nr.com/" target="newbrowser">Numerical Recipes in C: The Art of Scientific Computing</a> (2nd edition), Cambridge University Press, Cambridge, 1992, Section 7.2. */
public class GaussianGenerator implements RandomGenerator {
	
	private final double mean;
	private final double stdev;
	
	private final UniformGenerator unigen;
	
	private boolean cached = false;
	private double cache;
	
	/** Constructs a generator of random numbers from the Gaussian distribution with zero mean and unit standard deviation. The seed used for initialization is derived from the system's current time in milliseconds. */
	public GaussianGenerator() { this(0.0,1.0); }
	
	/** Constructs a generator of random numbers from the Gaussian distribution with zero mean and unit standard deviation and initialized with the given seed.
		
		@param seed the seed used for initialization of the generator.
	*/
	public GaussianGenerator(final int seed) { this(0.0,1.0,seed); }
	
	/** Constructs a generator of random numbers from the Gaussian distribution with given mean and standard deviation. The seed used for initialization is derived from the system's current time in milliseconds.
		
		@param mean the mean of the distribution.
		
		@param stdev the standard deviation from the mean of the distribution. Must be larger than or equal to {@code 0}.
		
		@exception IllegalArgumentException if {@code stdev} is less than {@code 0}.
	*/
	public GaussianGenerator(final double mean, final double stdev) {
		
		if (stdev < 0.0) throw new IllegalArgumentException("Standard deviation less than 0");
		
		this.mean = mean; this.stdev = stdev;
		
		unigen = new UniformGenerator(-1,1);
	}
	
	/** Constructs a generator of random numbers from the Gaussian distribution with given mean and standard deviation and initialized with the given seed.
		
		@param mean the mean of the distribution.
		
		@param stdev the standard deviation from the mean of the distribution. Must be larger than or equal to {@code 0}.
		
		@param seed the seed used for initialization of the generator.
		
		@exception IllegalArgumentException if {@code stdev} is less than {@code 0}.
	*/
	public GaussianGenerator(final double mean, final double stdev, final int seed) {
		
		if (stdev < 0.0) throw new IllegalArgumentException("Standard deviation less than 0");
		
		this.mean = mean; this.stdev = stdev;
		
		unigen = new UniformGenerator(-1,1,seed);
	}
	
	/** Returns a random number from the Gaussian distribution with mean and standard deviation specified at construction.
		
		@return a random number from the Gaussian distribution with mean and standard deviation specified at construction.
	*/
	public double next() {
		
		if (cached) { cached = false; return cache; }
		
		double v1, v2, R2;
		do {
			v1 = unigen.next();
			v2 = unigen.next();
			R2 = v1*v1 + v2*v2;
		} while (R2 >= 1.0);
		
		final double fac = Math.sqrt(-2.0*Math.log(R2)/R2);
		cache = mean + stdev*v1*fac; cached = true;
		
		return mean + stdev*v2*fac;
	}
	
}
