package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.DomainOrder;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.general.AbstractSeriesDataset;
import org.jfree.data.xy.XYDataset;

public class XYEdgeSeriesCollection extends AbstractSeriesDataset implements XYDataset{

	/*
	 * FIELDS
	 */
	
	private ArrayList<XYEdgeSeries> seriesList = new ArrayList<XYEdgeSeries>();
	
	
	/*
	 * PUBLIC METHODS
	 */

	public double getXStartValue(int series, int item) {
		return seriesList.get(series).getEdgeXStart(item).doubleValue();
	}
	
	public double getYStartValue(int series, int item) {
		return seriesList.get(series).getEdgeYStart(item).doubleValue();
	}
	

	public double getXEndValue(int series, int item) {
		return seriesList.get(series).getEdgeXEnd(item).doubleValue();
	}
	
	public double getYEndValue(int series, int item) {
		return seriesList.get(series).getEdgeYEnd(item).doubleValue();
	}
	
	@Override
	public int getSeriesCount() {
		return seriesList.size();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Comparable getSeriesKey(int seriesIndex) {
		return seriesList.get(seriesIndex).getKey();
	}
	
	public XYEdgeSeries getSeries(int seriesIndex) {
		return seriesList.get(seriesIndex);
	}

	@SuppressWarnings("rawtypes")
	public XYEdgeSeries getSeries(Comparable key) {
		for(XYEdgeSeries s : seriesList) 
			if (s.getKey().equals(key)) 
				return s;
        throw new UnknownKeyException("Key not found: " + key);
	}
	
	public List<XYEdgeSeries> getSeries() {
		return seriesList;
	}
	
	/*
	 * XYDATASET METHODS
	 */

	@Override
	public DomainOrder getDomainOrder() {
		return DomainOrder.ASCENDING;
	}

	@Override
	public int getItemCount(int series) {
		return seriesList.get(series).getItemCount();
	}

	@Override
	public Number getX(int series, int item) {
		return null;
	}

	@Override
	public double getXValue(int series, int item) {
		return 0;
	}

	@Override
	public Number getY(int series, int item) {
		return null;
	}

	@Override
	public double getYValue(int series, int item) {
		return 0;
	}
	
}
