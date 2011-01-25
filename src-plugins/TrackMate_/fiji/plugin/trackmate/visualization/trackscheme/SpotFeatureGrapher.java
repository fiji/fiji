package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.PaintList;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class SpotFeatureGrapher extends JFrame {

	/*
	 * CONSTRUCTOR
	 */
	
	private static final Shape DEFAULT_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);
	private InterpolatePaintScale paints = InterpolatePaintScale.Jet; 
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
		XYSeriesCollection pointDataset = new XYSeriesCollection();
		{
			for(Feature feature : yFeatures) {
				XYSeries series = new XYSeries(feature.shortName());
				for(Spot spot : spots)
					series.add(spot.getFeature(xFeature), spot.getFeature(feature));
				pointDataset.addSeries(series);
			}
		}
		
		// Point renderer
		XYLineAndShapeRenderer pointRenderer = new XYLineAndShapeRenderer();

		// Collect edges
		ArrayList<Spot[]> edges = new ArrayList<Spot[]>();
		{
			int nspots = spots.size();
			Spot source, target;
			for (int i = 0; i < nspots; i++) {
				source = spots.get(i);
				for (int j = i+1; j < nspots; j++) {
					target = spots.get(j);

					if (graph.containsEdge(source, target)) 
						edges.add(new Spot[] {source, target});

				}
			}
		}
		
		// Edge renderer
		XYEdgeRenderer edgeRenderer = new XYEdgeRenderer();

		// Data-set for edges
		XYEdgeSeriesCollection edgeDataset = new XYEdgeSeriesCollection();
		{
			double x0, x1, y0, y1;
			XYEdgeSeries edgeSeries;
			Spot source, target;
			for(Feature yFeature : yFeatures) {
				edgeSeries = new XYEdgeSeries(yFeature.shortName());
				for(Spot[]	edge : edges) {
					source = edge[0];
					target = edge[1];
					x0 = source.getFeature(xFeature);
					y0 = source.getFeature(yFeature);
					x1 = target.getFeature(xFeature);
					y1 = target.getFeature(yFeature);
					edgeSeries.addEdge(x0, y0, x1, y1);
				}
				edgeDataset.addSeries(edgeSeries);
			}
		}
		
		
		// The chart
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxisLabel, yAxisLabel, pointDataset, PlotOrientation.VERTICAL, true, true, false);

		// The plot
		XYPlot plot = chart.getXYPlot();
		plot.setDataset(1, edgeDataset);
		plot.setRenderer(1, edgeRenderer);
		plot.setRenderer(0, pointRenderer);

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
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
		
		// The frame
		add(chartPanel);
        validate();
        setSize(new java.awt.Dimension(500, 270));
	}
	
}
