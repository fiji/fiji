package fiji.plugin.trackmate.features;

import static fiji.plugin.trackmate.visualization.trackscheme.TrackScheme.TRACK_SCHEME_ICON;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jfree.chart.renderer.InterpolatePaintScale;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.ExportableChartPanel;

public abstract class AbstractFeatureGrapher {
	
	protected static final Shape DEFAULT_SHAPE = new Ellipse2D.Double(-3, -3, 6, 6);

	protected final InterpolatePaintScale paints = InterpolatePaintScale.Jet; 
	protected final String xFeature;
	protected final Set<String> yFeatures;
	protected final TrackMateModel model;

	public AbstractFeatureGrapher(final String xFeature, final Set<String> yFeatures,final TrackMateModel model) {
		this.xFeature = xFeature;
		this.yFeatures = yFeatures;
		this.model = model;
	}
	
	/**
	 * Draw and render the graph.
	 */
	public abstract void render();

	/*
	 * UTILS
	 */
	
	/**
	 * Render and display a frame containing all the char panels, grouped by dimension
	 */
	protected final void renderCharts(final List<ExportableChartPanel> chartPanels) {
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
		JFrame frame = new JFrame();
		frame.setTitle("Feature plot for Track scheme");
		frame.setIconImage(TRACK_SCHEME_ICON.getImage());
		frame.getContentPane().add(scrollPane);
		frame.validate();
		frame.setSize(new java.awt.Dimension(520, 320));
		frame.setVisible(true);
	}


	/**
	 * @return the unique mapped values in the given map, for the collection of keys given.
	 */
	protected final <K, V> Set<V> getUniqueValues(Iterable<K> keys, Map<K,V> map) {
		HashSet<V> mapping = new HashSet<V>();
		for (K key : keys) {
			mapping.add(map.get(key));
		}
		return mapping;
	}

	/**
	 * @return  the collection of keys amongst the given ones, 
	 * that point to the target value in the given map.
	 * @param targetValue the common value to search
	 * @param keys the keys to inspect
	 * @param map the map to search in
	 */
	protected final <K, V> List<K> getCommonKeys(final V targetValue, final Iterable<K> keys, final Map<K,V> map) {
		ArrayList<K> foundKeys = new ArrayList<K>();
		for (K key : keys) {
			if (map.get(key).equals(targetValue)) {
				foundKeys.add(key);
			}
		}
		return foundKeys;
	}
	
	/**
	 * @return a suitable plot title built from the given target features
	 */
	
	protected final String buildPlotTitle(final Iterable<String> yFeatures, final Map<String, String> featureNames) {
		StringBuilder sb = new StringBuilder("Plot of ");
		Iterator<String> it = yFeatures.iterator();
		sb.append(featureNames.get(it.next()) );
		while(it.hasNext()) {
			sb.append(", ");
			sb.append(featureNames.get(it.next()));
		}
		sb.append(" vs ");
		sb.append(featureNames.get(xFeature));
		sb.append(".");
		return sb.toString();
	}

	/**
	 * @return the list of links that have their source and target in the given spot list.
	 */
	protected final List<DefaultWeightedEdge> getInsideEdges(final List<Spot> spots) {
		ArrayList<DefaultWeightedEdge> edges = new ArrayList<DefaultWeightedEdge>();
		int nspots = spots.size();
		Spot source, target;
		for (int i = 0; i < nspots; i++) {
			source = spots.get(i);
			for (int j = i+1; j < nspots; j++) {
				target = spots.get(j);

				if (model.getTrackModel().containsEdge(source, target)) {
					DefaultWeightedEdge edge = model.getTrackModel().getEdge(source, target);
					edges.add(edge);
				}
				if(model.getTrackModel().containsEdge(target, source)) { // careful for directed edge
					DefaultWeightedEdge edge = model.getTrackModel().getEdge(target, source);
					edges.add(edge);
				}

			}
		}
		return edges;
	}
	
	

}