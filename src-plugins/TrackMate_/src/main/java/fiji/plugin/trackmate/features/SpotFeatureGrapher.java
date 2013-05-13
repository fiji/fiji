package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.XYEdgeRenderer;
import fiji.plugin.trackmate.util.XYEdgeSeries;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class SpotFeatureGrapher extends AbstractFeatureGrapher  {

	private final List<Spot> spots;
	private final Dimension xDimension;
	private final Map<String, Dimension> yDimensions;
	private final Map<String, String> featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public SpotFeatureGrapher(final String xFeature, final Set<String> yFeatures, final List<Spot> spots, final TrackMateModel model) {
		super(xFeature, yFeatures, model);
		this.spots = spots;
		this.xDimension = model.getFeatureModel().getSpotFeatureDimensions().get(xFeature);
		this.yDimensions = model.getFeatureModel().getSpotFeatureDimensions();
		this.featureNames = model.getFeatureModel().getSpotFeatureNames();

	}
	
	/*
	 * PRIVATE METHODS
	 */

	
	@Override
	public void render() {
				
		final Settings settings = model.getSettings();
		
		// X label
		String xAxisLabel = xFeature + " (" + TMUtils.getUnitsFor(xDimension, settings)+")";
		
		// Find how many different dimensions
		Set<Dimension> dimensions = getUniqueValues(yFeatures, yDimensions);
		
		// Generate one panel per different dimension
		ArrayList<ExportableChartPanel> chartPanels = new ArrayList<ExportableChartPanel>(dimensions.size());
		for (Dimension dimension : dimensions) {
			
			// Y label
			String yAxisLabel = TMUtils.getUnitsFor(dimension, settings);
			
			// Collect suitable feature for this dimension
			List<String> featuresThisDimension = getCommonKeys(dimension, yFeatures, yDimensions);

			// Title
			String title = buildPlotTitle(featuresThisDimension, featureNames);

			// Data-set for points (easy)
			XYSeriesCollection pointDataset = buildSpotDataSet(featuresThisDimension, spots);

			// Point renderer
			XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();

			// Edge renderer
			XYEdgeRenderer edgeRenderer = new XYEdgeRenderer();

			// Data-set for edges
			XYEdgeSeriesCollection edgeDataset = buildEdgeDataSet(featuresThisDimension, spots);
			
			// The chart
			JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, pointDataset, PlotOrientation.VERTICAL, true, true, false);
			chart.getTitle().setFont(FONT);
			chart.getLegend().setItemFont(SMALL_FONT);

			// The plot
			XYPlot plot = chart.getXYPlot();
			plot.setDataset(1, edgeDataset);
			plot.setRenderer(1, edgeRenderer);
			plot.setRenderer(0, pointRenderer);
			plot.getRangeAxis().setLabelFont(FONT);
			plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
			plot.getDomainAxis().setLabelFont(FONT);
			plot.getDomainAxis().setTickLabelFont(SMALL_FONT);
			
			// Paint
			pointRenderer.setUseOutlinePaint(true);
			int nseries = edgeDataset.getSeriesCount();
			for (int i = 0; i < nseries; i++) {
				pointRenderer.setSeriesOutlinePaint(i, Color.black);
				pointRenderer.setSeriesLinesVisible(i, false);
				pointRenderer.setSeriesShape(i, DEFAULT_SHAPE, false);
				pointRenderer.setSeriesPaint(i, paints.getPaint((double)i/nseries), false);
				edgeRenderer.setSeriesPaint(i, paints.getPaint((double)i/nseries), false);
			}

			// The panel
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
			chartPanels.add(chartPanel);
		}
		
		renderCharts(chartPanels);
	}
	
	/**
	 * @return a new dataset that contains the values, specified from the given feature, and  extracted from all
	 * the given spots.
	 */
	private XYSeriesCollection buildSpotDataSet(final Iterable<String> targetYFeatures, final Iterable<Spot> spots) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		for(String feature : targetYFeatures) {
			XYSeries series = new XYSeries(featureNames.get(feature));
			for(Spot spot : spots) {
				Double x = spot.getFeature(xFeature);
				Double y = spot.getFeature(feature);
				if (null == x || null == y) {
					continue;
				}
				series.add(x.doubleValue(), y.doubleValue());
			}
			dataset.addSeries(series);
		}
		return dataset;
	}
	
	/**
	 * @return a new dataset that contains the values, specified from the given feature, and  extracted from all
	 * the given spots. The dataset returned is a {@link XYEdgeSeriesCollection}, made to plot the lines
	 * between 2 points representing 2 spot. We therefore retrieve 
	 */
	private XYEdgeSeriesCollection buildEdgeDataSet(final Iterable<String> targetYFeatures, final List<Spot> spots) {
		// Collect edges
		List<DefaultWeightedEdge> edges = getInsideEdges(spots);
		
		// Build dataset
		XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		Double x0, x1, y0, y1;
		XYEdgeSeries edgeSeries;
		Spot source, target;
		for(String yFeature : targetYFeatures) {
			edgeSeries = new XYEdgeSeries(featureNames.get(yFeature));
			for(DefaultWeightedEdge	edge : edges) {
				source = model.getTrackModel().getEdgeSource(edge);
				target = model.getTrackModel().getEdgeTarget(edge);
				x0 = source.getFeature(xFeature);
				y0 = source.getFeature(yFeature);
				x1 = target.getFeature(xFeature);
				y1 = target.getFeature(yFeature);
				if (null == x0 || null == y0 || null == x1 || null == y1) {
					continue;
				}
				edgeSeries.addEdge(x0.doubleValue(), y0.doubleValue(), x1.doubleValue(), y1.doubleValue());
			}
			edgeDataset.addSeries(edgeSeries);
		}
		return edgeDataset;
	}
}
