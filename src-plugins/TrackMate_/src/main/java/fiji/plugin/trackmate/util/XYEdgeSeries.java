package fiji.plugin.trackmate.util;

import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

public class XYEdgeSeries extends Series {

	/*
	 * FIELDS
	 */
	
	private static final long serialVersionUID = -3716934680176727207L;
	private XYSeries startSeries = new XYSeries("StartPoints", false, true);
	private XYSeries endSeries = new XYSeries("EndPoints", false, true);
	
	/*
	 * CONSTRUCTOR
	 */
	
	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(Comparable key) {
		super(key);
	}
	
	@SuppressWarnings("rawtypes")
	public XYEdgeSeries(Comparable key, String description) {
		super(key, description);
	}
	
	/*
	 * PUBLIC METHODS
	 */

	@Override
	public int getItemCount() {
		return startSeries.getItemCount();
	}
	
	public void addEdge(double x0, double y0, double x1, double y1) {
		startSeries.add(x0, y0, false);
		endSeries.add(x1, y1, false);
	}
	
	public Number getEdgeXStart(int index) {
		return startSeries.getX(index);
	}
	
	public Number getEdgeYStart(int index) {
		return startSeries.getY(index);
	}
	
	public Number getEdgeXEnd(int index) {
		return endSeries.getX(index);
	}
	
	public Number getEdgeYEnd(int index) {
		return endSeries.getY(index);
	}
	
	
	
	
	

}
