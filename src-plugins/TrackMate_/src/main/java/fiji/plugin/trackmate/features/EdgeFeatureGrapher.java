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
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.util.XYEdgeRenderer;
import fiji.plugin.trackmate.util.XYEdgeSeries;
import fiji.plugin.trackmate.util.XYEdgeSeriesCollection;

public class EdgeFeatureGrapher extends AbstractFeatureGrapher {

	private final List<DefaultWeightedEdge> edges;
	private final Dimension xDimension;
	private final Map<String, Dimension> yDimensions;
	private final Map<String, String> featureNames;

	public EdgeFeatureGrapher(String xFeature, Set<String> yFeatures, List<DefaultWeightedEdge> edges, TrackMateModel model) {
		super(xFeature, yFeatures, model);
		this.edges = edges;
		this.xDimension = model.getFeatureModel().getEdgeFeatureDimensions().get(xFeature);
		this.yDimensions = model.getFeatureModel().getEdgeFeatureDimensions();
		this.featureNames = model.getFeatureModel().getEdgeFeatureNames();
	}

	@Override
	public void render() {

		final Settings settings = model.getSettings();

		// Check x units
		String xdim= TMUtils.getUnitsFor(xDimension, settings);
		if (null == xdim) { // not a number feature
			return; 
		}

		// X label
		String xAxisLabel = xFeature + " (" + xdim +")";

		// Find how many different dimensions
		Set<Dimension> dimensions = getUniqueValues(yFeatures, yDimensions);

		// Generate one panel per different dimension
		ArrayList<ExportableChartPanel> chartPanels = new ArrayList<ExportableChartPanel>(dimensions.size());
		for (Dimension dimension : dimensions) {

			// Y label
			String yAxisLabel = TMUtils.getUnitsFor(dimension, settings);
			
			// Check y units
			if (null == yAxisLabel) { // not a number feature
				continue; 
			}

			// Collect suitable feature for this dimension
			List<String> featuresThisDimension = getCommonKeys(dimension, yFeatures, yDimensions);

			// Title
			String title = buildPlotTitle(featuresThisDimension, featureNames);
			
			// Data-set for points (easy)
			XYSeriesCollection pointDataset = buildEdgeDataSet(featuresThisDimension, edges);

			// Point renderer
			XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();

			// Edge renderer
			XYEdgeRenderer edgeRenderer = new XYEdgeRenderer();

			// Data-set for edges
			XYEdgeSeriesCollection edgeDataset = buildConnectionDataSet(featuresThisDimension, edges);

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



	private XYEdgeSeriesCollection buildConnectionDataSet(List<String> targetYFeatures, List<DefaultWeightedEdge> edges) {
		XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		// First create series per y features. At this stage, we assume that they are all numeric 
		for(String yFeature : targetYFeatures) {
			XYEdgeSeries edgeSeries = new XYEdgeSeries(featureNames.get(yFeature));
			edgeDataset.addSeries(edgeSeries);
		}

		// Build dataset. We look for edges that have a spot in common, one for the target one for the source
		final FeatureModel fm = model.getFeatureModel();
		for(DefaultWeightedEdge	edge0 : edges) {
			for(DefaultWeightedEdge	edge1 : edges) {

				if (model.getTrackModel().getEdgeSource(edge0).equals(model.getTrackModel().getEdgeTarget(edge1))) {
					for(String yFeature : targetYFeatures) {
						XYEdgeSeries edgeSeries = edgeDataset.getSeries(featureNames.get(yFeature));
						Number x0 = (Number) fm.getEdgeFeature(edge0, xFeature);
						Number y0 = (Number) fm.getEdgeFeature(edge0, yFeature);
						Number x1 = (Number) fm.getEdgeFeature(edge1, xFeature);;
						Number y1 = (Number) fm.getEdgeFeature(edge1, yFeature);
						edgeSeries.addEdge(x0.doubleValue(), y0.doubleValue(), x1.doubleValue(), y1.doubleValue());
					}
				}
			}
		}
		return edgeDataset;
	}

	/**
	 * @return a new dataset that contains the values, specified from the given feature, and  extracted from all
	 * the given edges.
	 */
	private XYSeriesCollection buildEdgeDataSet(final Iterable<String> targetYFeatures, final Iterable<DefaultWeightedEdge> edges) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		final FeatureModel fm = model.getFeatureModel();
		for(String feature : targetYFeatures) {
			XYSeries series = new XYSeries(featureNames.get(feature));
			for(DefaultWeightedEdge edge : edges) {
				Number x = (Number) fm.getEdgeFeature(edge, xFeature);
				Number y = (Number) fm.getEdgeFeature(edge, feature);
				if (null == x || null == y) {
					continue;
				}
				series.add(x.doubleValue(), y.doubleValue());
			}
			dataset.addSeries(series);
		}
		return dataset;
	}
}
