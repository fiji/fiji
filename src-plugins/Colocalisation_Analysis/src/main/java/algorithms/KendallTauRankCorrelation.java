package algorithms;

import ij.IJ;
import results.ResultHandler;
import gadgets.DataContainer;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This algorithm calculates Kendall's Tau-b rank correlation coefficient
 *
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

		if (!cursor.hasNext()) {
			return;
		}

		// See http://en.wikipedia.org/wiki/Kendall_tau_rank_correlation_coefficient
		int n = 0, max1 = 0, max2 = 0, max = 255;
		int[][] histogram = new int[max + 1][max + 1];
		while (cursor.hasNext()) {
			cursor.fwd();
			T type1 = cursor.getChannel1();
			T type2 = cursor.getChannel2();
			double ch1 = type1.getRealDouble();
			double ch2 = type2.getRealDouble();
			if (ch1 < 0 || ch2 < 0 || ch1 > max || ch2 > max) {
				IJ.log("Error: The current Kendall Tau implementation is limited to 8-bit data");
				return;
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

		tau = (nc - nd) / Math.sqrt((n0 - n1) * (double)(n0 - n2));
	}

	@Override
	public void processResults(ResultHandler<T> handler) {
		super.processResults(handler);
		handler.handleValue("Kendall's Tau-b rank correlation value", tau, 4);
	}
}
