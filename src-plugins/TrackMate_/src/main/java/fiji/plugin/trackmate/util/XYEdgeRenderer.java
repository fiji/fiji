package fiji.plugin.trackmate.util;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

public class XYEdgeRenderer extends AbstractXYItemRenderer {
	
	private static final long serialVersionUID = -4565389588020243812L;
	
	@Override
	public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
			ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
		
		// get the data point...
		XYEdgeSeriesCollection edgeDataset = (XYEdgeSeriesCollection) dataset;
		XYEdgeSeries s = edgeDataset.getSeries(series);
		
		double x0 = s.getEdgeXStart(item).doubleValue();
		double y0 = s.getEdgeYStart(item).doubleValue();
		double x1 = s.getEdgeXEnd(item).doubleValue();
		double y1 = s.getEdgeYEnd(item).doubleValue();
		
        if (Double.isNaN(y1) || Double.isNaN(x1) || Double.isNaN(y0) || Double.isNaN(x0))
            return;
        
        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        double transX0 = domainAxis.valueToJava2D(x0, dataArea, xAxisLocation);
        double transY0 = rangeAxis.valueToJava2D(y0, dataArea, yAxisLocation);

        double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        double transY1 = rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation);
        
        // only draw if we have good values
        if (Double.isNaN(transX0) || Double.isNaN(transY0) || Double.isNaN(transX1) || Double.isNaN(transY1)) 
            return;

        PlotOrientation orientation = plot.getOrientation();
        if (orientation == PlotOrientation.HORIZONTAL) {
            state.workingLine.setLine(transY0, transX0, transY1, transX1);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            state.workingLine.setLine(transX0, transY0, transX1, transY1);
        }

        if (state.workingLine.intersects(dataArea)) {
            g2.setStroke(getItemStroke(series, item));
            g2.setPaint(getItemPaint(series, item));
            g2.draw(state.workingLine);
        }
		
		
	}

}
