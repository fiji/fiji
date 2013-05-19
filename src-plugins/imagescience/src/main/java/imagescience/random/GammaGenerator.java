package imagescience.random;

import java.lang.Math;

/** Gamma random number generator of positive integer order. This implementation is based on the algorithm described by W. H. Press, S. A. Teukolsky, W. T. Vetterling, B. P. Flannery, <a href="http://www.nr.com/" target="newbrowser">Numerical Recipes in C: The Art of Scientific Computing</a> (2nd edition), Cambridge University Press, Cambridge, 1992, Section 7.3, and uses {@link UniformGenerator} as a source of uniform random numbers. */
public class GammaGenerator implements RandomGenerator {
	
	private final int order;
	
	private final UniformGenerator unigen;
	
	/** Constructs a generator of random numbers from the first-order gamma distribution. The seed used for initialization is derived from the system's current time in milliseconds. */
	public GammaGenerator() { this(1); }
	
	/** Constructs a generator of random numbers from the gamma distribution with given positive integer order. The seed used for initialization is derived from the system's current time in milliseconds.
		
		@param order the order of the gamma distribution. Must be larger than {@code 0}.
		
		@exception IllegalArgumentException if {@code order} is less than or equal to {@code 0}.
	*/
	public GammaGenerator(final int order) {
		
		if (order <= 0) throw new IllegalArgumentException("Order less than or equal to 0");
		
		this.order = order;
		
		unigen = new UniformGenerator(0,1);
	}
	
	/** Constructs a generator of random numbers from the gamma distribution with given positive integer order and initialized with the given seed.
		
		@param order the order of the gamma distribution. Must be larger than {@code 0}.
		
		@param seed the seed used for initialization of the generator.
		
		@exception IllegalArgumentException if {@code order} is less than or equal to {@code 0}.
	*/
	public GammaGenerator(final int order, final int seed) {
		
		if (order <= 0) throw new IllegalArgumentException("Order less than or equal to 0");
		
		this.order = order;
		
		unigen = new UniformGenerator(0,1,seed);
	}
	
	/** Returns a random number from the gamma distribution with integer order specified at construction.
		
		@return a random number from the gamma distribution with integer order specified at construction.
	*/
	public double next() {
		
		if (order < 6) { // Direct method
			double x = 1.0;
			for (int j=0; j<order; ++j) x *= unigen.next();
			return -Math.log(x);
		} else { // Rejection method
			double om1, sq, v1, v2, x, y;
			do {
				do {
					do {
						v1 = unigen.next();
						v2 = 2.0*unigen.next() - 1.0;
					} while (v1*v1 + v2*v2 > 1.0);
					y = v2/v1;
					om1 = order - 1;
					sq = Math.sqrt(2.0*om1 + 1.0);
					x = sq*y + om1;
				} while (x <= 0.0);
			} while (unigen.next() > (1.0 + y*y)*Math.exp(om1*Math.log(x/om1) - sq*y));
			return x;
		}
	}
	
}
