package org.jfree.data.statistics;

/**
 * A {@link HistogramDataset} that returns the log of the count in each bin (plus one),
 * so as to have a logarithmic plot.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Dec 28, 2010
 *
 */
public class LogHistogramDataset extends HistogramDataset {
	
	private static final long serialVersionUID = 6012084169414194555L;

	@Override
	public Number getY(int series, int item) {
		Number val = super.getY(series, item);
		return Math.log(1+val.doubleValue());
	}

}
