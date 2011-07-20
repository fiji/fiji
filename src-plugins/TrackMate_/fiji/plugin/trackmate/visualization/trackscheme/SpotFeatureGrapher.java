package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;
import static fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame.TRACK_SCHEME_ICON;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ExportableChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TMUtils;

public class SpotFeatureGrapher extends JFrame {

	private static final long serialVersionUID = 5983064022212100254L;
	private static final Shape DEFAULT_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);
	private InterpolatePaintScale paints = InterpolatePaintScale.Jet; 
	private SpotFeature xFeature;
	private Set<SpotFeature> yFeatures;
	private List<Spot> spots;
	private TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	public SpotFeatureGrapher(final SpotFeature xFeature, final Set<SpotFeature> yFeatures, final List<Spot> spots, final TrackMateModel model) {
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.spots = spots;
		this.model = model;
		initGUI();
	}
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
				
		final Settings settings = model.getSettings();
		
		// X label
		String xAxisLabel = xFeature.shortName() + " (" + TMUtils.getUnitsFor(xFeature.getDimension(), settings)+")";
		
		// Find how many different dimensions
		HashSet<Dimension> dimensions = new HashSet<Dimension>();
		{
			for (SpotFeature yFeature : yFeatures) 
				dimensions.add(yFeature.getDimension());
		}
		
		// Generate one panel per different dimension
		ArrayList<ExportableChartPanel> chartPanels = new ArrayList<ExportableChartPanel>(dimensions.size());
		for (Dimension dimension : dimensions) {
			
			// Y label
			String yAxisLabel = TMUtils.getUnitsFor(dimension, settings);
			
			// Collect suitable feature for this dimension
			ArrayList<SpotFeature> featuresThisDimension = new ArrayList<SpotFeature>();
			for (SpotFeature yFeature : yFeatures)
				if (yFeature.getDimension().equals(dimension))
					featuresThisDimension.add(yFeature);
			

			// Title
			String title = "";
			{
				Iterator<SpotFeature> it = featuresThisDimension.iterator();
				title = "Plot of "+it.next().shortName();
				while(it.hasNext())
					title += ", "+it.next().shortName();
				title += " vs "+xFeature.shortName()+".";
			}

			// Data-set for points (easy)
			XYSeriesCollection pointDataset = new XYSeriesCollection();
			{
				for(SpotFeature feature : featuresThisDimension) {
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

						if (model.containsEdge(source, target)) 
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
				for(SpotFeature yFeature : featuresThisDimension) {
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

		// The Panel
		JPanel panel = new JPanel();
		BoxLayout panelLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(panelLayout);
		for(ExportableChartPanel chartPanel : chartPanels)  {
			panel.add(chartPanel);
			panel.add(Box.createVerticalStrut(5));
		}
		
		// Scroll pane
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportView(panel);

		// The frame
		setTitle("Feature plot for Track scheme");
		setIconImage(TRACK_SCHEME_ICON.getImage());
		getContentPane().add(scrollPane);
		validate();
		setSize(new java.awt.Dimension(500, 300));
	}

}
