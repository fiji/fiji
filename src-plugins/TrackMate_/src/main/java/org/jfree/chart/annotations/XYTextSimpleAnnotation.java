package org.jfree.chart.annotations;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AnnotationChangeEvent;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

public class XYTextSimpleAnnotation extends AbstractXYAnnotation {
	
	private static final long serialVersionUID = 1L;
	private float x, y;
	private String text;
	private Font font;
	private Color color;
	private ChartPanel chartPanel;
	
	
	public XYTextSimpleAnnotation(ChartPanel chartPanel) {
		this.chartPanel = chartPanel;
	}
	
	/*
	 * PUBLIC METHOD
	 */
	
	
	@Override
	public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea,
			ValueAxis domainAxis, ValueAxis rangeAxis, int rendererIndex,
			PlotRenderingInfo info) {
		
		Rectangle2D box = chartPanel.getScreenDataArea();
		float sx = (float) plot.getDomainAxis().valueToJava2D(x, box, plot.getDomainAxisEdge());
		float maxXLim = (float) box.getWidth() - g2.getFontMetrics().stringWidth(text); 
		if (sx > maxXLim) {
			sx = maxXLim;
		}
		if (sx < box.getMinX()) {
			sx = (float) box.getMinX();
		}
		
		float sy = (float) plot.getRangeAxis().valueToJava2D(y, chartPanel.getScreenDataArea(), plot.getRangeAxisEdge());
		g2.setTransform(new AffineTransform());
		g2.setColor(color);
		g2.setFont(font);
		g2.drawString(text, sx, sy);
	}
	
	public void setLocation(float x, float y) {
		this.x = x;
		this.y = y;
		notifyListeners(new AnnotationChangeEvent(this, this));
	}
	
	public void setText(String text) { this.text = text; }
	public void setFont(Font font) { this.font = font;	}
	public void setColor(Color color) {
		this.color = color; 
		notifyListeners(new AnnotationChangeEvent(this, this));
	}

}
