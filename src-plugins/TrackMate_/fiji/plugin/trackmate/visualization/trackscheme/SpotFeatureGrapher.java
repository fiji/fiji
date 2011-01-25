package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class SpotFeatureGrapher extends JFrame {

	/*
	 * CONSTRUCTOR
	 */
	
	private Feature xFeature;
	private Set<Feature> yFeatures;
	private List<Spot> spots;
	private Graph<Spot, DefaultWeightedEdge> graph;

	public SpotFeatureGrapher(final Feature xFeature, final Set<Feature> yFeatures, final List<Spot> spots, final Graph<Spot, DefaultWeightedEdge> graph) {
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.spots = spots;
		this.graph = graph;
		initGUI();
		
	}
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		
		// Title
		String title = "";
		{
			Iterator<Feature> it = yFeatures.iterator();
			title = "Plot of "+it.next().shortName();
			while(it.hasNext())
				title += ", "+it.next().shortName();
			title += " vs "+xFeature.shortName()+".";
		}
		
		// X label
		String xAxisLabel = xFeature.shortName();
		
		// Y label
		String yAxisLabel = "";
		
		// Data-set for points (easy)
		XYSeriesCollection dataset = new XYSeriesCollection();
		{
			for(Feature feature : yFeatures) {
				XYSeries series = new XYSeries(feature.shortName());
				for(Spot spot : spots)
					series.add(spot.getFeature(xFeature), spot.getFeature(feature));
				dataset.addSeries(series);
			}
		}
		
		// Point renderer
		XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();
		pointRenderer.setSeriesLinesVisible(0, false);
		pointRenderer.setSeriesShape(0, XYLineAndShapeRenderer.DEFAULT_SHAPE, false);

		// Data-set for edges
		XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		XYEdgeSeries edgeSeries = new XYEdgeSeries("TEST");
		edgeSeries.addEdge(10, 10, 20, 20);
		edgeSeries.addEdge(20, 10, 20, 10);
		
		// Edge renderer
		XYEdgeRenderer edgeRenderer = new XYEdgeRenderer();
		
		// The chart
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, edgeDataset, PlotOrientation.VERTICAL, true, true, false);
		
		// The plot
		XYPlot plot = (XYPlot) chart.getPlot();
//		plot.setRenderer(0, pointRenderer);
//		plot.setDataset(1, edgeDataset);
		plot.setRenderer(edgeRenderer);
		
		// The panel
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		
		// The frame
		add(chartPanel);
        validate();
        setSize(new java.awt.Dimension(500, 270));
	}
	
}
