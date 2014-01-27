package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import net.imglib2.PairIterator;
import net.imglib2.type.numeric.real.DoubleType;

import org.junit.Test;

import algorithms.KendallTauRankCorrelation;
import algorithms.MissingPreconditionException;

/**
 * Tests the Kendall Tau implementation.
 */
public class KendallTauTest {

	private boolean exhaustive = false;

	@Test
	public void testSimple() throws MissingPreconditionException {
		assumeTrue(!exhaustive);
		// From Armitage P, Berry G. Statistical Methods in Medical Research (3rd edition). Blackwell 1994, p. 466.
		assertTau(23.0 / 45.0, new int[] { 4, 10, 3, 1, 9, 2, 6, 7, 8, 5 }, new int[] { 5, 8, 6, 2, 10, 3, 9, 4, 7, 1 });
	}

	@Test
	public void testPathological() throws MissingPreconditionException {
		assumeTrue(!exhaustive);
		assertTau(Double.NaN, new int[] { 1, 1, 1, 1 }, new int[] { 2, 2, 2, 2 });
	}

	@Test
	public void testSomeDuplicates() throws MissingPreconditionException {
		assumeTrue(!exhaustive);
		// for pairs (1, 3), (1, 2), (2, 1), (3, 1),
		// n = 4,
		// n0 = n * (n - 1) / 2 = 4 * 3 / 2 = 6
		// n1 = 1 + 0 + 0 + 0 = 1
		// n2 = 1 + 0 + 0 + 0 = 1
		// nc = #{ } = 0
		// nd = #{ (1, 3)x(2, 1), (1, 3)x(3, 1), (1, 2)x(2, 1), (1, 2)x(3, 1) } = 4
		// therefore Tau_b = -4 / sqrt(5 * 5) = -0.8
		assertTau(-0.8, new int[] { 1, 1, 2, 3 }, new int[] { 3, 2, 1, 1 });
	}

	private PairIterator<DoubleType> pairIterator(final int[] values1, final int[] values2) {
		assertEquals(values1.length, values2.length);
		return new PairIterator<DoubleType>() {
			private int i = -1;
			private DoubleType ch1 = new DoubleType();
			private DoubleType ch2 = new DoubleType();

			@Override
			public boolean hasNext() {
				return i + 1 < values1.length;
			}

			@Override
			public void reset() {
				i = -1;
			}

			@Override
			public void fwd() {
				i++;
			}

			@Override
			public DoubleType getFirst() {
				ch1.set(values1[i]);
				return ch1;
			}

			@Override
			public DoubleType getSecond() {
				ch2.set(values2[i]);
				return ch2;
			}
		};
	}

	private void assertTau(final double expected, final int[] values1, final int[] values2) throws MissingPreconditionException {
		final PairIterator<DoubleType> iter = pairIterator(values1, values2);
		assertEquals(expected, KendallTauRankCorrelation.calculateMergeSort(iter), 1e-10);
	}

	private int seed;

	private int pseudoRandom()
	{
		return seed = 3170425 * seed + 132102;
	}

	@Test
	public void exhaustiveTesting() throws Exception {
		assumeTrue(exhaustive);
		final int n = 5, m = 10;
		final int[] values1 = new int[n], values2 = new int[n];
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < n; j++) {
				values1[j] = Math.abs(pseudoRandom()) % m;
				values2[j] = Math.abs(pseudoRandom()) % m;
			}
			final PairIterator<DoubleType> iter = pairIterator(values1, values2);
			double value1 = KendallTauRankCorrelation.calculateNaive(iter);
			iter.reset();
			double value2 = KendallTauRankCorrelation.calculateMergeSort(iter);
			if (Double.isNaN(value1)) {
				assertTrue("i: " + i + ", value2: " + value2, Double.isInfinite(value2) || Double.isNaN(value2));
			} else {
				assertEquals("i: " + i, value1, value2, 1e-10);
			}
		}
	}
}
