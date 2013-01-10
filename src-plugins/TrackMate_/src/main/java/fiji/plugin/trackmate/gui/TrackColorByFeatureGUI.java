package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.edges.EdgeVelocityAnalyzer;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.TrackColorGenerator;

public class TrackColorByFeatureGUI extends ActionListenablePanel {

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;
	/**
	 * This action is fired when the feature to color in the "Set color by feature"
	 * JComboBox is changed.
	 */
	public final ActionEvent TRACK_COLOR_FEATURE_CHANGED = new ActionEvent(this, 1, "TrackColorFeatureChanged");
	private JLabel jLabelSetColorBy;
	private CategoryJComboBox<TrackColorGenerator, String> jComboBoxSetColorBy;
	private JPanel jPanelByFeature;
	private Canvas canvasColor;
	private JPanel jPanelColor;

	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;

	/*
	 * DEFAULT VISIBILITY
	 */

	private final Map<TrackColorGenerator, List<String>> features;
	private final Map<String, String> featureNames;
	private final Map<TrackColorGenerator, String> categoryNames;

	private ActionListenablePanel caller;
	private final PerTrackFeatureColorGenerator trackColorGenerator;
	private final PerEdgeFeatureColorGenerator edgeColorGenerator;
	private final TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	public TrackColorByFeatureGUI(TrackMateModel model, ActionListenablePanel caller) {
		super();
		// Build features map
		features = new LinkedHashMap<TrackColorGenerator, List<String>>(2);
		trackColorGenerator = new PerTrackFeatureColorGenerator(model, TrackIndexAnalyzer.TRACK_INDEX);
		List<String> trackFeatures = model.getFeatureModel().getTrackFeatures();
		features.put(trackColorGenerator, trackFeatures);
		edgeColorGenerator = new PerEdgeFeatureColorGenerator(model, EdgeVelocityAnalyzer.VELOCITY);
		List<String> edgeFeatures = model.getFeatureModel().getEdgeFeatures();
		features.put(edgeColorGenerator, edgeFeatures);
		// Build feature names
		featureNames = new HashMap<String, String>(trackFeatures.size() + edgeFeatures.size());
		featureNames.putAll(model.getFeatureModel().getTrackFeatureNames());
		featureNames.putAll(model.getFeatureModel().getEdgeFeatureNames());
		// Build category names
		categoryNames = new HashMap<TrackColorGenerator, String>(2);
		categoryNames.put(trackColorGenerator, "Track features:");
		categoryNames.put(edgeColorGenerator, "Edge features:");
		// The rest
		this.model = model;
		this.caller = caller;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Forward the enabled flag to all components off this panel.
	 */
	@Override
	public void setEnabled(boolean enabled) {
		jLabelSetColorBy.setEnabled(enabled);
		jComboBoxSetColorBy.setEnabled(enabled);
		canvasColor.setEnabled(enabled);
	}

	/**
	 * @return a configured {@link TrackColorGenerator} matching the user choice on this GUI component.
	 */
	public TrackColorGenerator getColorGenerator() {
		return jComboBoxSetColorBy.getSelectedCategory();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	

	/**
	 * Forward the 'color by feature' action to the caller of this GUI.
	 */
	private void colorByFeatureChanged() {
		// Configure color generator
		if (jComboBoxSetColorBy.getSelectedCategory() == trackColorGenerator) {
			trackColorGenerator.setFeature(jComboBoxSetColorBy.getSelectedItem());
		} else {
			edgeColorGenerator.setFeature(jComboBoxSetColorBy.getSelectedItem());
		}
		// pass event
		caller.fireAction(TRACK_COLOR_FEATURE_CHANGED);
	}

	private void repaintColorCanvas(Graphics g) {
		if (null == jComboBoxSetColorBy.getSelectedItem()) {
			g.clearRect(0, 0, canvasColor.getWidth(), canvasColor.getHeight());
			return;
		}

		final double[] values;
		if (jComboBoxSetColorBy.getSelectedCategory() == trackColorGenerator) {
			values = model.getFeatureModel().getTrackFeatureValues(jComboBoxSetColorBy.getSelectedItem(), true);
		} else {
			values = model.getFeatureModel().getEdgeFeatureValues(jComboBoxSetColorBy.getSelectedItem(), true);
		}
		
		if (null == values) {
			g.clearRect(0, 0, canvasColor.getWidth(), canvasColor.getHeight());
			return;
		}
		double max = Float.NEGATIVE_INFINITY;
		double min = Float.POSITIVE_INFINITY;
		double val;
		for (int i = 0; i < values.length; i++) {
			val = values[i];
			if (val > max) max = val;
			if (val < min) min = val;
		}

		final int width = canvasColor.getWidth();
		final int height = canvasColor.getHeight();
		float alpha;
		for (int i = 0; i < width; i++) {
			alpha = (float) i / (width-1);
			g.setColor(colorMap.getPaint(alpha));
			g.drawLine(i, 0, i, height);
		}
		g.setColor(Color.WHITE);
		g.setFont(SMALL_FONT.deriveFont(Font.BOLD));
		FontMetrics fm = g.getFontMetrics();
		String minStr = String.format("%.1f", min);
		String maxStr = String.format("%.1f", max);
		g.drawString(minStr, 1, height/2 + fm.getHeight()/2);
		g.drawString(maxStr, width - fm.stringWidth(maxStr)-1, height/2 + fm.getHeight()/2);
	}


	private void initGUI() {

		{
			BorderLayout layout = new BorderLayout();
			setLayout(layout);
			this.setPreferredSize(new java.awt.Dimension(270, 45));

			jPanelByFeature = new JPanel();
			BoxLayout jPanelByFeatureLayout = new BoxLayout(jPanelByFeature, javax.swing.BoxLayout.X_AXIS);
			jPanelByFeature.setLayout(jPanelByFeatureLayout);
			add(jPanelByFeature, BorderLayout.CENTER);
			jPanelByFeature.setPreferredSize(new java.awt.Dimension(270, 25));
			jPanelByFeature.setMaximumSize(new java.awt.Dimension(32767, 25));
			jPanelByFeature.setSize(270, 25);
			{
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jLabelSetColorBy = new JLabel();
				jPanelByFeature.add(jLabelSetColorBy);
				jLabelSetColorBy.setText("Set color by");
				jLabelSetColorBy.setFont(SMALL_FONT);
			}
			{
				jComboBoxSetColorBy = new CategoryJComboBox<TrackColorGenerator, String>(features, featureNames, categoryNames);
				jComboBoxSetColorBy.setSelectedItem(TrackIndexAnalyzer.TRACK_INDEX);
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(jComboBoxSetColorBy);
				jComboBoxSetColorBy.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						colorByFeatureChanged();
						canvasColor.repaint();
					}
				});
			}
		}
		{
			jPanelColor = new JPanel();
			BorderLayout jPanelColorLayout = new BorderLayout();
			add(jPanelColor, BorderLayout.SOUTH);
			jPanelColor.setLayout(jPanelColorLayout);
			jPanelColor.setPreferredSize(new java.awt.Dimension(10, 20));
			{
				canvasColor = new Canvas() {
					private static final long serialVersionUID = -2174317490066575040L;
					@Override
					public void paint(Graphics g) {
						repaintColorCanvas(g);
					}
				};
				jPanelColor.add(canvasColor, BorderLayout.CENTER);
				canvasColor.setPreferredSize(new java.awt.Dimension(270, 20));
			}
		}
	}
}
