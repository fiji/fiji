/*
 * #%L
 * ImgLib: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2013 Stephan Preibisch, Tobias Pietzsch, Barry DeZonia,
 * Stephan Saalfeld, Albert Cardona, Curtis Rueden, Christian Dietz, Jean-Yves
 * Tinevez, Johannes Schindelin, Lee Kamentsky, Larry Lindsey, Grant Harris,
 * Mark Hiner, Aivar Grislis, Martin Horn, Nick Perry, Michael Zinsmaier,
 * Steffen Jaensch, Jan Funke, Mark Longair, and Dimiter Prodanov.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package script.imglib.analysis;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Collection;
import java.util.Map;

import javax.swing.JFrame;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RGBALegacyType;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * TODO
 *
 */
public class BarChart extends JFrame
{
	private static final long serialVersionUID = 7347559630675867104L;

	private final JFreeChart chart;

	public BarChart(final Collection<? extends Number> data) {
		this(data, "Bar chart", "", "");
	}

	public BarChart(final Collection<? extends Number> data, final String title,
			final String xLabel, final String yLabel) {
		super(title);
		this.chart = createChart(data, title, xLabel, yLabel);
		this.getContentPane().add(new ChartPanel(chart));
		this.pack();
		this.setVisible(true);
	}

	public BarChart(final Map<? extends Number, ? extends Number> data) {
		this(data, "Bar chart", "", "");
	}

	public BarChart(final Map<? extends Number, ? extends Number> data, final String title,
			final String xLabel, final String yLabel) {
		super(title);
		this.chart = createChart(data, title, xLabel, yLabel);
		this.getContentPane().add(new ChartPanel(chart));
		this.pack();
		this.setVisible(true);
	}

	public JFreeChart getChart() {
		return chart;
	}

	static private final JFreeChart createChart(final Collection<? extends Number> data, final String title,
			final String xLabel, final String yLabel) {
		DefaultCategoryDataset dcd = new DefaultCategoryDataset();
		int k = 1;
		for (final Number value : data) {
			dcd.addValue(value, "", k++);
		}
		boolean legend = false;
		boolean tooltips = true;
		boolean urls = false;
		JFreeChart chart = ChartFactory.createBarChart(title, xLabel, yLabel, dcd,
				PlotOrientation.VERTICAL, legend, tooltips, urls);
		setBarTheme(chart);
		return chart;
	}
	
	@SuppressWarnings("unchecked")
	static private final JFreeChart createChart(final Map<? extends Number, ? extends Number> data, final String title,
			final String xLabel, final String yLabel) {
		DefaultCategoryDataset dcd = new DefaultCategoryDataset();
		for (final Map.Entry<? extends Number, ? extends Number> e : data.entrySet()) {
			dcd.addValue(e.getValue(), "", (Comparable) e.getKey());
		}
		boolean legend = false;
		boolean tooltips = true;
		boolean urls = false;
		JFreeChart chart = ChartFactory.createBarChart(title, xLabel, yLabel, dcd,
				PlotOrientation.VERTICAL, legend, tooltips, urls);
		setBarTheme(chart);
		return chart;
	}
	

	static private final void setBarTheme(final JFreeChart chart) {
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.setSeriesOutlinePaint(0, Color.lightGray);
		renderer.setShadowVisible(false);
		renderer.setDrawBarOutline(true);
		setBackgroundDefault(chart);
	}

 	static private void setBackgroundDefault(final JFreeChart chart) {
		BasicStroke gridStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{2.0f, 1.0f}, 0.0f);
		CategoryPlot plot = (CategoryPlot) chart.getPlot();
		plot.setRangeGridlineStroke(gridStroke);
		plot.setDomainGridlineStroke(gridStroke);
		plot.setBackgroundPaint(new Color(235, 235, 235));
		plot.setRangeGridlinePaint(Color.white);
		plot.setDomainGridlinePaint(Color.white);
		plot.setOutlineVisible(false);
		plot.getDomainAxis().setAxisLineVisible(false);
		plot.getRangeAxis().setAxisLineVisible(false);
		plot.getDomainAxis().setLabelPaint(Color.gray);
		plot.getRangeAxis().setLabelPaint(Color.gray);
		plot.getDomainAxis().setTickLabelPaint(Color.gray);
		plot.getRangeAxis().setTickLabelPaint(Color.gray);
		chart.getTitle().setPaint(Color.gray);
	}
 
 	public Image<RGBALegacyType> asImage() {
		return ChartUtils.asImage(chart);
	}
}
