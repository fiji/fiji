package imagescience.random;

import imagescience.utility.FMath;
import java.lang.Math;

/** Poisson random number generator. This implementation is based on the algorithm described by W. H. Press, S. A. Teukolsky, W. T. Vetterling, B. P. Flannery, <a href="http://www.nr.com/" target="newbrowser">Numerical Recipes in C: The Art of Scientific Computing</a> (2nd edition), Cambridge University Press, Cambridge, 1992, Section 7.3, and uses {@link UniformGenerator} as a source of uniform random numbers. */
public class PoissonGenerator implements RandomGenerator {
	
	private final double mean;
	
	private final UniformGenerator unigen;
	
	/** Constructs a generator of random numbers from the Poisson distribution with unit mean. The seed used for initialization is derived from the system's current time in milliseconds. */
	public PoissonGenerator() { this(1.0); }
	
	/** Constructs a generator of random numbers from the Poisson distribution with unit mean and initialized with the given seed.
		
		@param seed the seed used for initialization of the generator.
	*/
	public PoissonGenerator(final int seed) { this(1.0,seed); }
	
	/** Constructs a generator of random numbers from the Poisson distribution with given mean. The seed used for initialization is derived from the system's current time in milliseconds.
		
		@param mean the mean of the distribution. Must be larger than or equal to {@code 0}.
		
		@exception IllegalArgumentException if {@code mean} is less than {@code 0}.
	*/
	public PoissonGenerator(final double mean) {
		
		if (mean < 0.0) throw new IllegalArgumentException("Mean less than 0");
		
		this.mean = mean;
		
		unigen = new UniformGenerator();
	}
	
	/** Constructs a generator of random numbers from the Poisson distribution with given mean and initialized with the given seed.
		
		@param mean the mean of the distribution. Must be larger than or equal to {@code 0}.
		
		@param seed the seed used for initialization of the generator.
		
		@exception IllegalArgumentException if {@code mean} is less than {@code 0}.
	*/
	public PoissonGenerator(final double mean, final int seed) {
		
		if (mean < 0.0) throw new IllegalArgumentException("Mean less than 0");
		
		this.mean = mean;
		
		unigen = new UniformGenerator(seed);
	}
	
	/** Returns a random number from the Poisson distribution with mean specified at construction.
		
		@return a random number from the Poisson distribution with mean specified at construction.
	*/
	public double next() { return next(this.mean); }
	
	private double prevmean = -1.0;
	private double sqrt2mean, logmean, comp;
	
	/** Returns a random number from the Poisson distribution with given mean.
		
		@param mean the mean of the distribution. Must be larger than or equal to {@code 0}.
		
		@exception IllegalArgumentException if {@code mean} is less than {@code 0}.
	*/
	public double next(final double mean) {
		
		if (mean < 0.0) {
			throw new IllegalArgumentException("Mean less than 0");
		} else if (mean < 12.0) { // Direct method
			if (mean != prevmean) { prevmean = mean; comp = Math.exp(-mean); }
			double em = -1; double t = 1.0;
			do { ++em; t *= unigen.next(); } while (t > comp);
			return em;
		} else { // Rejection method
			if (mean != prevmean) {
				prevmean = mean;
				sqrt2mean = Math.sqrt(2.0*mean);
				logmean = Math.log(mean);
				comp = mean*logmean - FMath.lngamma(mean + 1.0);
			}
			double loren, em;
			do {
				do {
					loren = Math.tan(Math.PI*unigen.next());
					em = sqrt2mean*loren + mean;
				} while (em < 0.0);
				em = Math.floor(em);
			} while (unigen.next() > 0.9*(1.0 + loren*loren)*Math.exp(em*logmean - FMath.lngamma(em + 1.0) - comp));
			return em;
		}
	}
	
}
