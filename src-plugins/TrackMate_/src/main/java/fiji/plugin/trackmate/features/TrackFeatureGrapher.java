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

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.ExportableChartPanel;
import fiji.plugin.trackmate.util.TMUtils;

public class TrackFeatureGrapher extends AbstractFeatureGrapher {

	private final Dimension xDimension;
	private final Map<String, Dimension> yDimensions;
	private final Map<String, String> featureNames;

	public TrackFeatureGrapher(String xFeature, Set<String> yFeatures, TrackMateModel model) {
		super(xFeature, yFeatures, model);
		this.xDimension = model.getFeatureModel().getTrackFeatureDimensions().get(xFeature);
		this.yDimensions = model.getFeatureModel().getTrackFeatureDimensions();
		this.featureNames = model.getFeatureModel().getTrackFeatureNames();
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
			XYSeriesCollection pointDataset = buildTrackDataSet(featuresThisDimension);

			// Point renderer
			XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();

			// The chart
			JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, pointDataset, PlotOrientation.VERTICAL, true, true, false);
			chart.getTitle().setFont(FONT);
			chart.getLegend().setItemFont(SMALL_FONT);

			// The plot
			XYPlot plot = chart.getXYPlot();
			plot.setRenderer(0, pointRenderer);
			plot.getRangeAxis().setLabelFont(FONT);
			plot.getRangeAxis().setTickLabelFont(SMALL_FONT);
			plot.getDomainAxis().setLabelFont(FONT);
			plot.getDomainAxis().setTickLabelFont(SMALL_FONT);

			// Paint
			pointRenderer.setUseOutlinePaint(true);
			int nseries = pointDataset.getSeriesCount();
			for (int i = 0; i < nseries; i++) {
				pointRenderer.setSeriesOutlinePaint(i, Color.black);
				pointRenderer.setSeriesLinesVisible(i, false);
				pointRenderer.setSeriesShape(i, DEFAULT_SHAPE, false);
				pointRenderer.setSeriesPaint(i, paints.getPaint((double)i/nseries), false);
			}

			// The panel
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
			chartPanels.add(chartPanel);
		}

		renderCharts(chartPanels);
	}



	/**
	 * @return a new dataset that contains the values, specified from the given feature, 
	 * and  extracted from all the visible tracks in the model.
	 */
	private XYSeriesCollection buildTrackDataSet(final Iterable<String> targetYFeatures) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		final FeatureModel fm = model.getFeatureModel();
		for(String feature : targetYFeatures) {
			XYSeries series = new XYSeries(featureNames.get(feature));
			for(Integer trackID : model.getTrackModel().getFilteredTrackIDs()) {
				Double x = fm.getTrackFeature(trackID, xFeature);
				Double y = fm.getTrackFeature(trackID, feature);
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
