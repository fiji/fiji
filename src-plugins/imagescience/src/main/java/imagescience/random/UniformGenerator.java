package imagescience.random;

/** Uniform random number generator. Implementation of the so-called <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html" target="newbrowser">Mersenne Twister</a> described by M. Matsumoto and T. Nishimura, "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform Pseudo-Random Number Generator", ACM Transactions on Modeling and Computer Simulation, vol. 8, no. 1, January 1998, pp. 3-30. It is one of the strongest uniform pseudo-random number generators known to date, with a period of 2^19937 - 1. This Java implementation is based on the 26 January 2002 C-version provided on the linked website and has been validated by comparing the output for different (positive and negative) integer test seeds. */
public class UniformGenerator implements RandomGenerator {
	
	private final static int N = 624;
	private final static int M = 397;
	private final static int UPPER_MASK = 0x80000000;
	private final static int LOWER_MASK = 0x7fffffff;
	private final static int[] mag01 = { 0x0, 0x9908b0df };
	
	private final int[] mt = new int[N];
	private int mti;
	
	private final double min;
	private final double max;
	
	/** Constructs a generator of random numbers uniformly distributed in the open interval (0,1). The seed used for initialization is derived from the system's current time in milliseconds. */
	public UniformGenerator() { this(0.0,1.0,(int)System.currentTimeMillis()); }
	
	/** Constructs a generator of random numbers uniformly distributed in the open interval (0,1) and initialized with the given seed.
		
		@param seed the seed used for initialization of the generator.
	*/
	public UniformGenerator(final int seed) { this(0.0,1.0,seed); }
	
	/** Constructs a generator of random numbers uniformly distributed in the open interval ({@code min},{@code max}). The seed used for initialization is derived from the system's current time in milliseconds.
		
		@param min {@code max} - the interval parameters. Random numbers are generated in the open interval ({@code min},{@code max}). If {@code min > max}, the meaning of the parameters is automatically reversed.
	*/
	public UniformGenerator(final double min, final double max) { this(min,max,(int)System.currentTimeMillis()); }
	
	/** Constructs a generator of random numbers uniformly distributed in the open interval ({@code min},{@code max}) and initialized with the given seed.
		
		@param min {@code max} - the interval parameters. Random numbers are generated in the open interval ({@code min},{@code max}). If {@code min > max}, the meaning of the parameters is automatically reversed.
		
		@param seed the seed used for initialization of the generator.
	*/
	public UniformGenerator(final double min, final double max, final int seed) {
		
		this.min = min; this.max = max;
		
		initialize(seed);
	}
	
	private void initialize(final int seed) {
		
		mt[0] = seed;
		for (mti=1; mti<N; ++mti)
			mt[mti] = (1812433253 * (mt[mti-1] ^ (mt[mti-1] >>> 30)) + mti);
	}
	
	/** Returns a uniform random number in the interval specified at construction.
		
		@return a uniform random number in the interval specified at construction.
	*/
	public double next() { return next(min,max); }
	
	/** Returns a uniform random number in the open interval ({@code min},{@code max}).
		
		@param min {@code max} - the interval parameters. Random numbers are generated in the open interval ({@code min},{@code max}). If {@code min > max}, the meaning of the parameters is automatically reversed.
	*/
	public double next(final double min, final double max) {
		
		int y;
		
		if (mti >= N) {
			for (int kk=0; kk<N-M; ++kk) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			for (int kk=N-M; kk<N-1; ++kk) {
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
			}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];
			mti = 0;
		}
		
		y = mt[mti++];
		
		y ^= (y >>> 11);
		y ^= (y << 7) & 0x9d2c5680;
		y ^= (y << 15) & 0xefc60000;
		y ^= (y >>> 18);
		
		return min + (max-min)*((y & 0xffffffffL) + 0.5)/4294967296.0;
	}
	
}
