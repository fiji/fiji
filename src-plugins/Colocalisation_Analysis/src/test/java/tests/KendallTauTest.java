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
		final int[] values1 = { 4, 10, 3, 1, 9, 2, 6, 7, 8, 5 };
		final int[] values2 = { 5, 8, 6, 2, 10, 3, 9, 4, 7, 1 };
		final PairIterator<DoubleType> iter = new PairIterator<DoubleType>() {
			private int i = -1;
			private DoubleType ch1 = new DoubleType();
			private DoubleType ch2 = new DoubleType();

			@Override
			public boolean hasNext() {
				return i + 1 < values1.length;
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

		assertEquals(KendallTauRankCorrelation.calculateNaive(iter), 23.0 / 45.0, 1e-10);
	}
}
