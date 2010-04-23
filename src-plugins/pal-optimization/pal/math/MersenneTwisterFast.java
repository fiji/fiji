// MersenneTwisterFast.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)



// Original file MersenneTwisterFast.java
// (c) 1999-2000 by Michael Lecuyer and Sean Luke
// see also http://www.cs.umd.edu/users/seanl/


package pal.math;

import java.io.*;
import java.util.*;


/**
 * MersenneTwisterFast:
 *
 * A simulation quality fast random number generator (MT19937)
 * with the  same public methods as java.util.Random.

 * <p>About the Mersenne Twister.
 * This is a Java version of the C-program for MT19937: Integer version.
 * next(32) generates one pseudorandom unsigned integer (32bit)
 * which is uniformly distributed among 0 to 2^32-1  for each
 * call.  next(int bits) >>>'s by (32-bits) to get a value ranging
 * between 0 and 2^bits-1 long inclusive; hope that's correct.
 * setSeed(seed) set initial values to the working area
 * of 624 words. For setSeed(seed), seed is any 32-bit integer
 * except for 0.
 *<p>
 * Reference.
 * M. Matsumoto and T. Nishimura,
 * "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform
 * Pseudo-Random Number Generator",
 * <i>ACM Transactions on Modeling and Computer Simulation,</i>
 * Vol. 8, No. 1, January 1998, pp 3--30.
 *
 * <p>Bug Fixes. This implementation implements the bug fixes made
 * in Java 1.2's version of Random, which means it can be used with
 * earlier versions of Java.  See
 * <a href="http://www.javasoft.com/products/jdk/1.2/docs/api/java/util/Random.html">
 * the JDK 1.2 java.util.Random documentation</a> for further documentation
 * on the random-number generation contracts made.  Additionally, there's
 * an undocumented bug in the JDK java.util.Random.nextBytes() method,
 * which this code fixes.
 *
 * <p> Important Note.  Just like java.util.Random, this
 * generator accepts a long seed but doesn't use all of it.  java.util.Random
 * uses 48 bits.  The Mersenne Twister instead uses 32 bits (int size).
 * So it's best if your seed does not exceed the int range.
 *
 * <p><a href="http://www.cs.umd.edu/users/seanl/">Sean Luke's web page</a>
 *
 * <P>
 * - added shuffling method (Alexei Drummond)
 */
public class MersenneTwisterFast implements Serializable
		{
		// Period parameters
		private static final int N = 624;
		private static final int M = 397;
		private static final int MATRIX_A = 0x9908b0df;   //  private static final * constant vector a
		private static final int UPPER_MASK = 0x80000000; // most significant w-r bits
		private static final int LOWER_MASK = 0x7fffffff; // least significant r bits


		// Tempering parameters
		private static final int TEMPERING_MASK_B = 0x9d2c5680;
		private static final int TEMPERING_MASK_C = 0xefc60000;


		// #define TEMPERING_SHIFT_U(y)  (y >>> 11)
		// #define TEMPERING_SHIFT_S(y)  (y << 7)
		// #define TEMPERING_SHIFT_T(y)  (y << 15)
		// #define TEMPERING_SHIFT_L(y)  (y >>> 18)

		private int mt[]; // the array for the state vector
		private int mti; // mti==N+1 means mt[N] is not initialized
		private int mag01[];

		// a good initial seed (of int size, though stored in a long)
		private static final long GOOD_SEED = 4357;

		private double nextNextGaussian;
		private boolean haveNextNextGaussian;

		// The following can be accessed externally by the static accessor methods which
		// inforce synchronization
		private static final MersenneTwisterFast DEFAULT_INSTANCE = new MersenneTwisterFast();

		// Added to curernt time in default constructor, and then adjust to allow for programs that construct
		// multiple MersenneTwisterFast in a short amount of time.
		private static long seedAdditive_ = 0;

		/**
		 * Constructor using the time of day as default seed.
		 */
		public MersenneTwisterFast() {
			this(System.currentTimeMillis()+seedAdditive_);
			seedAdditive_+=nextInt();
		}

		/**
		 * Constructor using a given seed.  Though you pass this seed in
		 * as a long, it's best to make sure it's actually an integer.
		 *
		 * @param seed generator starting number, often the time of day.
		 */
		public MersenneTwisterFast(long seed)
	{
		if (seed == 0)
		{
			setSeed(GOOD_SEED);
		}
		else
		{
			setSeed(seed);
		}
				}

	/**
	 * Shuffles an array.
	 */
	public final void shuffle(int[] array) {
		int l = array.length;
		for (int i = 0; i < l; i++) {
			int index = nextInt(l-i) + i;
			int temp = array[index];
			array[index] = array[i];
			array[i] = temp;
		}
	}
	/**
	 * Shuffles an array. Shuffles numberOfShuffles times
	 */
	public final void shuffle(int[] array, int numberOfShuffles) {
		int i, j, temp, l = array.length;
		for (int shuffle = 0; shuffle < numberOfShuffles; shuffle++) {
			do {
				i = nextInt(l);
				j = nextInt(l);
			} while(i!=j);
			temp = array[j];
			array[j] = array[i];
			array[i] = temp;
		}
	}
	/**
	 * Returns an array of shuffled indices of length l.
	 * @param l length of the array required.
	 */
	 public int[] shuffled(int l) {

		int[] array = new int[l];

		// initialize array
		for (int i = 0; i < l; i++) {
			array[i] = i;
		}
		shuffle(array);

		return array;
	}

		/**
		 * Initalize the pseudo random number generator.
		 * The Mersenne Twister only uses an integer for its seed;
		 * It's best that you don't pass in a long that's bigger
		 * than an int.
		 *
		 * @param seed from constructor
		 *
		 */
		public final void setSeed(long seed)
				{
	haveNextNextGaussian = false;

	mt = new int[N];

	// setting initial seeds to mt[N] using
	// the generator Line 25 of Table 1 in
	// [KNUTH 1981, The Art of Computer Programming
	//    Vol. 2 (2nd Ed.), pp102]

	// the 0xffffffff is commented out because in Java
	// ints are always 32 bits; hence i & 0xffffffff == i

	mt[0]= ((int)seed); // & 0xffffffff;

	for (mti = 1; mti < N; mti++)
			mt[mti] = (69069 * mt[mti-1]); //& 0xffffffff;

	// mag01[x] = x * MATRIX_A  for x=0,1
	mag01 = new int[2];
	mag01[0] = 0x0;
	mag01[1] = MATRIX_A;
				}

		public final int nextInt()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return y;
	}



		public final short nextShort()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return (short)(y >>> 16);
	}



		public final char nextChar()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return (char)(y >>> 16);
	}


		public final boolean nextBoolean()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return (boolean)((y >>> 31) != 0);
	}


		public final byte nextByte()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return (byte)(y >>> 24);
	}


		public final void nextBytes(byte[] bytes)
	{
	int y;

	for (int x=0;x<bytes.length;x++)
			{
			if (mti >= N)   // generate N words at one time
		{
		int kk;

		for (kk = 0; kk < N - M; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		for (; kk < N-1; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
		mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

		mti = 0;
		}

			y = mt[mti++];
			y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

			bytes[x] = (byte)(y >>> 24);
			}
	}


		public final long nextLong()
	{
	int y;
	int z;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (z >>> 1) ^ mag01[z & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (z >>> 1) ^ mag01[z & 0x1];
		}
			z = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (z >>> 1) ^ mag01[z & 0x1];

			mti = 0;
			}

	z = mt[mti++];
	z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
	z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
	z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
	z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

	return (((long)y) << 32) + (long)z;
	}


		public final double nextDouble()
	{
	int y;
	int z;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (z >>> 1) ^ mag01[z & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (z >>> 1) ^ mag01[z & 0x1];
		}
			z = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (z >>> 1) ^ mag01[z & 0x1];

			mti = 0;
			}

	z = mt[mti++];
	z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
	z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
	z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
	z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

	/* derived from nextDouble documentation in jdk 1.2 docs, see top */
	return ((((long)(y >>> 6)) << 27) + (z >>> 5)) / (double)(1L << 53);
	}





		public final double nextGaussian()
	{
	if (haveNextNextGaussian)
			{
			haveNextNextGaussian = false;
			return nextNextGaussian;
			}
	else
			{
			double v1, v2, s;
			do
		{
		int y;
		int z;
		int a;
		int b;

				if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
					{
					y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
					mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
					}
			for (; kk < N-1; kk++)
					{
					y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
					mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
					}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

		y = mt[mti++];
		y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
		y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
		y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
		y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

		if (mti >= N)   // generate N words at one time
				{
				int kk;

				for (kk = 0; kk < N - M; kk++)
			{
			z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+M] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
				for (; kk < N-1; kk++)
			{
			z = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+(M-N)] ^ (z >>> 1) ^ mag01[z & 0x1];
			}
				z = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N-1] = mt[M-1] ^ (z >>> 1) ^ mag01[z & 0x1];

				mti = 0;
				}

		z = mt[mti++];
		z ^= z >>> 11;                          // TEMPERING_SHIFT_U(z)
		z ^= (z << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(z)
		z ^= (z << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(z)
		z ^= (z >>> 18);                        // TEMPERING_SHIFT_L(z)

		if (mti >= N)   // generate N words at one time
				{
				int kk;

				for (kk = 0; kk < N - M; kk++)
			{
			a = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+M] ^ (a >>> 1) ^ mag01[a & 0x1];
			}
				for (; kk < N-1; kk++)
			{
			a = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+(M-N)] ^ (a >>> 1) ^ mag01[a & 0x1];
			}
				a = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N-1] = mt[M-1] ^ (a >>> 1) ^ mag01[a & 0x1];

				mti = 0;
				}

		a = mt[mti++];
		a ^= a >>> 11;                          // TEMPERING_SHIFT_U(a)
		a ^= (a << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(a)
		a ^= (a << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(a)
		a ^= (a >>> 18);                        // TEMPERING_SHIFT_L(a)

		if (mti >= N)   // generate N words at one time
				{
				int kk;

				for (kk = 0; kk < N - M; kk++)
			{
			b = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+M] ^ (b >>> 1) ^ mag01[b & 0x1];
			}
				for (; kk < N-1; kk++)
			{
			b = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
			mt[kk] = mt[kk+(M-N)] ^ (b >>> 1) ^ mag01[b & 0x1];
			}
				b = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
				mt[N-1] = mt[M-1] ^ (b >>> 1) ^ mag01[b & 0x1];

				mti = 0;
				}

		b = mt[mti++];
		b ^= b >>> 11;                          // TEMPERING_SHIFT_U(b)
		b ^= (b << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(b)
		b ^= (b << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(b)
		b ^= (b >>> 18);                        // TEMPERING_SHIFT_L(b)

		/* derived from nextDouble documentation in jdk 1.2 docs, see top */
		v1 = 2 *
				(((((long)(y >>> 6)) << 27) + (z >>> 5)) / (double)(1L << 53))
				- 1;
		v2 = 2 * (((((long)(a >>> 6)) << 27) + (b >>> 5)) / (double)(1L << 53))
				- 1;
		s = v1 * v1 + v2 * v2;
		} while (s >= 1);
			double multiplier = Math.sqrt(-2 * Math.log(s)/s);
			nextNextGaussian = v2 * multiplier;
			haveNextNextGaussian = true;
			return v1 * multiplier;
			}
	}












		public final float nextFloat()
	{
	int y;

	if (mti >= N)   // generate N words at one time
			{
			int kk;

			for (kk = 0; kk < N - M; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			for (; kk < N-1; kk++)
		{
		y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
		mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
		}
			y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
			mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

			mti = 0;
			}

	y = mt[mti++];
	y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
	y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
	y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
	y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

	return (y >>> 8) / ((float)(1 << 24));
	}



		/** Returns an integer drawn uniformly from 0 to n-1.  Suffice it to say,
	n must be > 0, or an IllegalArgumentException is raised. */
		public int nextInt(int n)
	{
	if (n<=0)
			throw new IllegalArgumentException("n must be positive");

	if ((n & -n) == n)  // i.e., n is a power of 2
			{
			int y;

			if (mti >= N)   // generate N words at one time
		{
		int kk;

		for (kk = 0; kk < N - M; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		for (; kk < N-1; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
		mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

		mti = 0;
		}

			y = mt[mti++];
			y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

			return (int)((n * (long) (y >>> 1) ) >> 31);
			}

	int bits, val;
	do
			{
			int y;

			if (mti >= N)   // generate N words at one time
		{
		int kk;

		for (kk = 0; kk < N - M; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+M] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		for (; kk < N-1; kk++)
				{
				y = (mt[kk] & UPPER_MASK) | (mt[kk+1] & LOWER_MASK);
				mt[kk] = mt[kk+(M-N)] ^ (y >>> 1) ^ mag01[y & 0x1];
				}
		y = (mt[N-1] & UPPER_MASK) | (mt[0] & LOWER_MASK);
		mt[N-1] = mt[M-1] ^ (y >>> 1) ^ mag01[y & 0x1];

		mti = 0;
		}

			y = mt[mti++];
			y ^= y >>> 11;                          // TEMPERING_SHIFT_U(y)
			y ^= (y << 7) & TEMPERING_MASK_B;       // TEMPERING_SHIFT_S(y)
			y ^= (y << 15) & TEMPERING_MASK_C;      // TEMPERING_SHIFT_T(y)
			y ^= (y >>> 18);                        // TEMPERING_SHIFT_L(y)

			bits = (y >>> 1);
			val = bits % n;
			} while(bits - val + (n-1) < 0);
	return val;
	}


	// ===================== Static access methods to the private DEFAULT_INSTANCE ===========
	/** Access a default instance of this class, access is synchronized */
	public static final byte getNextByte() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextByte();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final boolean getNextBoolean() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextBoolean();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final void getNextBytes(byte[] bs) {
		synchronized(DEFAULT_INSTANCE) {
			DEFAULT_INSTANCE.nextBytes(bs);
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final char getNextChar() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextChar();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final double getNextDouble() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextDouble();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final float getNextFloat() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextFloat();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final long getNextLong() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextLong();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final short getNextShort() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextShort();
		}
	}
	/** Access a default instance of this class, access is synchronized */
	public static final int getNextInt() {
		synchronized(DEFAULT_INSTANCE) {
			return DEFAULT_INSTANCE.nextInt();
		}
	}
}
