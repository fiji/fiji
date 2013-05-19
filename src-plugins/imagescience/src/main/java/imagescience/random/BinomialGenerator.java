package imagescience.random;

import imagescience.utility.FMath;
import java.lang.Math;

/** Binomial random number generator. This implementation is based on the algorithm described by W. H. Press, S. A. Teukolsky, W. T. Vetterling, B. P. Flannery, <a href="http://www.nr.com/" target="newbrowser">Numerical Recipes in C: The Art of Scientific Computing</a> (2nd edition), Cambridge University Press, Cambridge, 1992, Section 7.3, and uses {@link UniformGenerator} as a source of uniform random numbers. */
public class BinomialGenerator implements RandomGenerator {
	
	private final int trials;
	private final double probability;
	
	private final UniformGenerator unigen;
	
	/** Constructs a generator of random numbers from the binomial distribution with one trial of probability {@code 0.5}. The seed used for initialization is derived from the system's current time in milliseconds. */
	public BinomialGenerator() { this(1,0.5); }
	
	/** Constructs a generator of random numbers from the binomial distribution with one trial of probability {@code 0.5} and initialized with the given seed.
		
		@param seed the seed used for initialization of the generator.
	*/
	public BinomialGenerator(final int seed) { this(1,0.5,seed); }
	
	/** Constructs a generator of random numbers from the binomial distribution with given number of trials of given probability. The seed used for initialization is derived from the system's current time in milliseconds.
		
		@param trials the number of trials. Must be larger than or equal to {@code 0}.
		
		@param probability the probability for each trial. Must be in the range {@code [0,1]}.
		
		@exception IllegalArgumentException if {@code trials} is less than {@code 0}, or if {@code probability} is outside the range {@code [0,1]}.
	*/
	public BinomialGenerator(final int trials, final double probability) {
		
		if (trials < 0) throw new IllegalArgumentException("Number of trials less than 0");
		
		if (probability < 0.0 || probability > 1.0) throw new IllegalArgumentException("Probability outside range [0,1]");
		
		this.trials = trials;
		this.probability = probability;
		
		unigen = new UniformGenerator();
	}
	
	/** Constructs a generator of random numbers from the binomial distribution with given number of trials of given probability and initialized with the given seed.
		
		@param trials the number of trials. Must be larger than or equal to {@code 0}.
		
		@param probability the probability for each trial. Must be in the range {@code [0,1]}.
		
		@param seed the seed used for initialization of the generator.
		
		@exception IllegalArgumentException if {@code trials} is less than {@code 0}, or if {@code probability} is outside the range {@code [0,1]}.
	*/
	public BinomialGenerator(final int trials, final double probability, final int seed) {
		
		if (trials < 0) throw new IllegalArgumentException("Number of trials less than 0");
		
		if (probability < 0.0 || probability > 1.0) throw new IllegalArgumentException("Probability outside range [0,1]");
		
		this.trials = trials;
		this.probability = probability;
		
		unigen = new UniformGenerator(seed);
	}
	
	/** Returns a random number from the binomial distribution with number of trials and probability specified at construction.
		
		@return a random number from the binomial distribution with number of trials and probability specified at construction.
	*/
	public double next() { return next(this.trials,this.probability); }
	
	private int prevtrials = -1;
	private double prevprob = -1.0;
	private double prevcomp, prevlogprob, prevlog1mprob;
	
	/** Returns a random number from the binomial distribution with given number of trials of given probability.
		
		@param trials the number of trials. Must be larger than or equal to {@code 0}.
		
		@param probability the probability for each trial. Must be in the range {@code [0,1]}.
		
		@return a random number from the binomial distribution with given number of trials of given probability.
		
		@exception IllegalArgumentException if {@code trials} is less than {@code 0}, or if {@code probability} is outside the range {@code [0,1]}.
	*/
	public double next(final int trials, final double probability) {
		
		if (trials < 0) throw new IllegalArgumentException("Number of trials less than 0");
		
		if (probability < 0.0 || probability > 1.0) throw new IllegalArgumentException("Probability outside range [0,1]");
		
		final double prob = (probability <= 0.5) ? probability : (1.0 - probability);
		final double mean = trials*prob;
		double bnl = 0.0;
		
		if (trials < 25) { // Direct method
			for (int j=0; j<trials; ++j)
				if (unigen.next() < prob) ++bnl;
		} else if (mean < 1.0) { // Direct Poisson method
			final double comp = Math.exp(-mean);
			double t = 1.0;
			for (int j=0; j<=trials; ++j) {
				t *= unigen.next();
				if (t < comp) { bnl = (j <= trials) ? j : trials; break; }
			}
		} else { // Rejection method
			if (trials != prevtrials) {
				prevcomp = FMath.lngamma(trials + 1.0);
				prevtrials = trials;
			}
			if (prob != prevprob) {
				prevlogprob = Math.log(prob);
				prevlog1mprob = Math.log(1.0 - prob);
				prevprob = prob;
			}
			final double sq = Math.sqrt(2.0*mean*(1.0-prob));
			double y, em;
			do {
				do {
					y = Math.tan(Math.PI*unigen.next());
					em = sq*y + mean;
				} while (em < 0.0 || em >= (trials + 1.0));
				em = Math.floor(em);
			} while (unigen.next() > 
				1.2*sq*(1.0 + y*y)*Math.exp(prevcomp + em*prevlogprob +
				(trials - em)*prevlog1mprob -
				FMath.lngamma(em + 1.0) -
				FMath.lngamma(trials - em + 1.0)));
			bnl = em;
		}
		
		if (prob != probability) bnl = trials - bnl;
		return bnl;
	}
	
}
