package tests;

import static org.junit.Assert.assertEquals;
import net.imglib2.PairIterator;
import net.imglib2.type.numeric.real.DoubleType;

import org.junit.Test;

import algorithms.KendallTauRankCorrelation;
import algorithms.MissingPreconditionException;

/**
 * Tests the Kendall Tau implementation.
 */
public class KendallTauTest {
	@Test
	public void testSimple() throws MissingPreconditionException {
		// From Armitage P, Berry G. Statistical Methods in Medical Research (3rd edition). Blackwell 1994, p. 466.
		assertTau(23.0 / 45.0, new int[] { 4, 10, 3, 1, 9, 2, 6, 7, 8, 5 }, new int[] { 5, 8, 6, 2, 10, 3, 9, 4, 7, 1 });
	}

	@Test
	public void testPathological() throws MissingPreconditionException {
		assertTau(Double.NaN, new int[] { 1, 1, 1, 1 }, new int[] { 2, 2, 2, 2 });
	}

	@Test
	public void testSomeDuplicates() throws MissingPreconditionException {
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

	private void assertTau(final double expected, final int[] values1, final int[] values2) throws MissingPreconditionException {
		assertEquals(values1.length, values2.length);
		final PairIterator<DoubleType> iter = new PairIterator<DoubleType>() {
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

		assertEquals(KendallTauRankCorrelation.calculateNaive(iter), expected, 1e-10);
	}
}
