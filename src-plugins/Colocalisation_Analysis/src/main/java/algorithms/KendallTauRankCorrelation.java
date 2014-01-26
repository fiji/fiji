package algorithms;

import java.util.Arrays;


import ij.IJ;
import results.ResultHandler;
import gadgets.DataContainer;
import net.imglib2.PairIterator;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This algorithm calculates Kendall's Tau-b rank correlation coefficient
 * <p>
 * According to
 * http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient, Tau-b
 * (appropriate if multiple pairs share the same first, or second, value), the
 * rank correlation of a set of pairs <tt>(x_1, y_1), ..., (x_n, y_n)</tt>:
 * 
 * <pre>
 * Tau_B = (n_c - n_d) / sqrt( (n_0 - n_1) (n_0 - n_2) )
 * </pre>
 * 
 * where
 * 
 * <pre>
 * n_0 = n (n - 1) / 2
 * n_1 = sum_i t_i (t_i - 1) / 2
 * n_2 = sum_j u_j (u_j - 1) / 2
 * n_c = #{ i, j; i != j && (x_i - x_j) * (y_i - y_j) > 0 },
 * &nbsp; i.e. the number of pairs of pairs agreeing on the order of x and y, respectively
 * n_d = #{ i, j: i != j && (x_i - x_j) * (y_i - y_j) < 0 },
 * &nbsp; i.e. the number of pairs of pairs where x and y are ordered opposite of each other
 * t_i = number of tied values in the i-th group of ties for the first quantity
 * u_j = number of tied values in the j-th group of ties for the second quantity
 * </pre>
 * 
 * </p>
 * 
 * @author Johannes Schindelin
 * @param <T>
 */
public class KendallTauRankCorrelation<T extends RealType< T >> extends Algorithm<T> {

	public KendallTauRankCorrelation() {
		super("Kendall's Tau-b Rank Correlation");
	}

	private double tau;

	@Override
	public void execute(DataContainer<T> container)
		throws MissingPreconditionException
	{
		RandomAccessible<T> img1 = container.getSourceImage1();
		RandomAccessible<T> img2 = container.getSourceImage2();
		RandomAccessibleInterval<BitType> mask = container.getMask();

		TwinCursor<T> cursor = new TwinCursor<T>(img1.randomAccess(),
				img2.randomAccess(), Views.iterable(mask).localizingCursor());

		tau = calculateNaive(cursor);
	}

	public static<T extends RealType<T>> double calculateNaive(final PairIterator<T> iterator) {
		if (!iterator.hasNext()) {
			return Double.NaN;
		}

		// See http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient
		int n = 0, max1 = 0, max2 = 0, max = 255;
		int[][] histogram = new int[max + 1][max + 1];
		while (iterator.hasNext()) {
			iterator.fwd();
			T type1 = iterator.getFirst();
			T type2 = iterator.getSecond();
			double ch1 = type1.getRealDouble();
			double ch2 = type2.getRealDouble();
			if (ch1 < 0 || ch2 < 0 || ch1 > max || ch2 > max) {
				IJ.log("Error: The current Kendall Tau implementation is limited to 8-bit data");
				return Double.NaN;
			}
			n++;
			int ch1Int = (int)Math.round(ch1);
			int ch2Int = (int)Math.round(ch2);
			histogram[ch1Int][ch2Int]++;
			if (max1 < ch1Int) {
				max1 = ch1Int;
			}
			if (max2 < ch2Int) {
				max2 = ch2Int;
			}
		}
		long n0 = n * (n - 1) / 2, n1 = 0, n2 = 0, nc = 0, nd = 0;
		for (int i1 = 0; i1 <= max1; i1++) {
			IJ.log("" + i1 + "/" + max1);
			int ch1 = 0;
			for (int i2 = 0; i2 <= max2; i2++) {
				ch1 += histogram[i1][i2];

				int count = histogram[i1][i2];
				for (int j1 = 0; j1 < i1; j1++) {
					for (int j2 = 0; j2 < i2; j2++) {
						nc += count * histogram[j1][j2];
					}
				}
				for (int j1 = 0; j1 < i1; j1++) {
					for (int j2 = i2 + 1; j2 <= max2; j2++) {
						nd += count * histogram[j1][j2];
					}
				}
			}
			n1 += ch1 * (long)(ch1 - 1) / 2;
		}
		for (int i2 = 0; i2 <= max2; i2++) {
			int ch2 = 0;
			for (int i1 = 0; i1 <= max1; i1++) {
				ch2 += histogram[i1][i2];
			}
			n2 += ch2 * (long)(ch2 - 1) / 2;
		}

		return (nc - nd) / Math.sqrt((n0 - n1) * (double)(n0 - n2));
	}

	private static<T extends RealType<T>> double[][] getPairs(final PairIterator<T> iterator) {
		// TODO: it is ridiculous that this has to be counted all the time (i.e. in most if not all measurements!).
		// We only need an upper bound to begin with, so even the number of pixels in the first channel would be enough!
		int capacity = 0;
		while (iterator.hasNext()) {
			iterator.fwd();
			capacity++;
		}

		double[] values1 = new double[capacity];
		double[] values2 = new double[capacity];
		iterator.reset();
		int count = 0;
		while (iterator.hasNext()) {
			iterator.fwd();
			values1[count] = iterator.getFirst().getRealDouble();
			values2[count] = iterator.getSecond().getRealDouble();
			count++;
		}

		if (count < capacity) {
			values1 = Arrays.copyOf(values1, count);
			values2 = Arrays.copyOf(values2, count);
		}
		return new double[][] { values1, values2 };
	}

	/**
	 * Calculate Tau-b efficiently.
	 * <p>
	 * This implementation is based on this description of the merge sort based
	 * way to calculate Tau-b:
	 * http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient
	 * #Algorithms. This is supposed to be the method described in:
	 * <blockquote>Knight, W. (1966).
	 * "A Computer Method for Calculating Kendall's Tau with Ungrouped Data".
	 * Journal of the American Statistical Association 61 (314): 436–439.
	 * doi:10.2307/2282833.</blockquote> but since that article is not available
	 * as Open Access, it is unnecessarily hard to verify.
	 * </p>
	 * 
	 * @param iterator the iterator of the pairs
	 * @return Tau-b
	 */
	public static<T extends RealType<T>> double calculateMergeSort(final PairIterator<T> iterator) {
		final double[][] pairs = getPairs(iterator);
		final double[] x = pairs[0];
		final double[] y = pairs[1];
		final int n = x.length;

		int[] index = new int[n];
		for (int i = 0; i < n; i++) {
			index[i] = i;
		}

		// First sort by x as primary key, y as secondary one.
		// We use IntroSort here because it is fast and in-place.
		IntArraySorter.sort(index, new IntComparator() {

			@Override
			public int compare(int a, int b) {
				double xa = x[a], ya = y[a];
				double xb = x[b], yb = y[b];
				int result = Double.compare(xa, xb);
				return result != 0 ? result : Double.compare(ya, yb);
			}

		});

		// The trick is to count the ties of x (n1) and the joint ties of x and y (n3) now, while
		// index is sorted with regards to x.
		long n0 = n * (n - 1) / 2;
		long n1 = 0, n3 = 0;

		for (int i = 1; i < n; i++) {
			double x0 = x[index[i - 1]];
			if (x[index[i]] != x0) {
				continue;
			}
			double y0 = y[index[i - 1]];
			int i1 = i;
			do {
				double y1 = y[index[i1++]];
				if (y1 == y0) {
					int i2 = i1;
					while (i1 < n && x[index[i1]] == x0 && y[index[i1]] == y0) {
						i1++;
					}
					n3 += (i1 - i2 + 2) * (long)(i1 - i2 + 1) / 2;
				}
				y0 = y1;
			} while (i1 < n && x[index[i1]] == x0);
			n1 += (i1 - i + 1) * (long)(i1 - i) / 2;
			i = i1;
		}

		// Now, let's perform that merge sort that also counts S, the number of
		// swaps a Bubble Sort would require (and which therefore is half the number
		// by which we have to adjust n_0 - n_1 - n_2 + n_3 to obtain n_c - n_d)
		final MergeSort mergeSort = new MergeSort(index, new IntComparator() {

			@Override
			public int compare(int a, int b) {
				double ya = y[a];
				double yb = y[b];
				return Double.compare(ya, yb);
			}
		});
		long S = mergeSort.sort();
		index = mergeSort.getSorted();
		long n2 = 0;

		for (int i = 1; i < n; i++) {
			double y0 = y[index[i - 1]];
			if (y[index[i]] != y0) {
				continue;
			}
			int i1 = i + 1;
			while (i1 < n && y[index[i1]] == y0) {
				i1++;
			}
			n2 += (i1 - i + 1) * (long)(i1 - i) / 2;
			i = i1;
		}

		return (n0 - n1 - n2 + n3 - 2 * S) / Math.sqrt((n0 - n1) * (double)(n0 - n2));
	}

	private final static class MergeSort {

		private int[] index;
		private final IntComparator comparator;

		public MergeSort(int[] index, IntComparator comparator) {
			this.index = index;
			this.comparator = comparator;
		}

		public int[] getSorted() {
			return index;
		}

		/**
		 * Sorts the {@link #index} array.
		 * <p>
		 * This implements a non-recursive merge sort.
		 * </p>
		 * @param begin
		 * @param end
		 * @return the equivalent number of BubbleSort swaps
		 */
		public long sort() {
			long swaps = 0;
			int n = index.length;
			// There are merge sorts which perform in-place, but their runtime is worse than O(n log n)
			int[] index2 = new int[n];
			for (int step = 1; step < n; step <<= 1) {
				int begin = 0, k = 0;
				for (;;) {
					int begin2 = begin + step, end = begin2 + step;
					if (end >= n) {
						if (begin2 >= n) {
							break;
						}
						end = n;
					}

					// calculate the equivalent number of BubbleSort swaps
					// and perform merge, too
					int i = begin, j = begin2;
					while (i < begin2 && j < end) {
						int compare = comparator.compare(index[i], index[j]);
						if (compare > 0) {
							swaps += (begin2 - i);
							index2[k++] = index[j++];
						} else {
							index2[k++] = index[i++];
						}
					}
					if (i < begin2) {
						do {
							index2[k++] = index[i++];
						} while (i < begin2);
					} else {
						while (j < end) {
							index2[k++] = index[j++];
						}
					}
					begin = end;
				}
				if (k < n) {
					System.arraycopy(index, k, index2, k, n - k);
				}
				int[] swapIndex = index2;
				index2 = index;
				index = swapIndex;
			}

			return swaps;
		}

	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Kendall's Tau-b rank correlation value", tau, 4);
	}
}
