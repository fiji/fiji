package fiji.plugin.trackmate.gui.panels.components;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.ModelChangeEvent;
import fiji.plugin.trackmate.ModelChangeListener;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.track.TrackIndexAnalyzer;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;

public class TrackEdgeColorByFeatureGUI extends ActionListenablePanel {

	/*
	 * FIELDS
	 */

	public static final String EDGE_CATEGORY_KEY = "Edges";
	public static final String TRACK_CATEGORY_KEY = "Tracks";
	private static final long serialVersionUID = 1L;
	/**
	 * This action is fired when the feature to color in the "Set color by feature"
	 * JComboBox is changed.
	 */
	public final ActionEvent TRACK_COLOR_FEATURE_CHANGED = new ActionEvent(this, 1, "TrackColorFeatureChanged");
	private JLabel jLabelSetColorBy;
	private CategoryJComboBox<String, String> jComboBoxSetColorBy;
	private JPanel jPanelByFeature;
	private Canvas canvasColor;
	private JPanel jPanelColor;

	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	protected final TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */
	
	public TrackEdgeColorByFeatureGUI(TrackMateModel model) {
		super();
		this.model = model;
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
	 * Returns a key to the color generator category selected in the combo box. 
	 * Can be {@link #TRACK_CATEGORY_KEY} or {@link #EDGE_CATEGORY_KEY}.
	 * @return the selected category.
	 * @see #getColorFeature() 
	 */
	public String getColorGeneratorCategory() {
		return jComboBoxSetColorBy.getSelectedCategory();
	}
	
	/**
	 * Returns the selected feature in the combo box.
	 * @return the selected feature.
	 * @see #getColorGeneratorCategory()
	 */
	public String getColorFeature() {
		return jComboBoxSetColorBy.getSelectedItem();
	}
	
	/*
	 * PRIVATE METHODS
	 */
	

	/**
	 * Forward the 'color by feature' action to the caller of this GUI.
	 */
	private void colorByFeatureChanged() {
		super.fireAction(TRACK_COLOR_FEATURE_CHANGED);
	}

	private void repaintColorCanvas(Graphics g) {
		if (null == jComboBoxSetColorBy.getSelectedItem()) {
			g.clearRect(0, 0, canvasColor.getWidth(), canvasColor.getHeight());
			return;
		}

		final double[] values = getValues(jComboBoxSetColorBy);
		
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
				jComboBoxSetColorBy = createComboBoxSelector();
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
			{
				model.addTrackMateModelChangeListener(new ModelChangeListener() {
					@Override
					public void modelChanged(ModelChangeEvent event) {
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

	/**
	 * Return the {@link CategoryJComboBox} that configures this selector. 
	 * Subclasses can override this method to decide what items are in the combo box list.
	 * @return a new {@link CategoryJComboBox}.
	 */
	protected CategoryJComboBox<String, String> createComboBoxSelector() {
		LinkedHashMap<String, Collection<String>> features = new LinkedHashMap<String, Collection<String>>(2);
		Collection<String> trackFeatures = model.getFeatureModel().getTrackFeatures();
		features.put(TRACK_CATEGORY_KEY, trackFeatures);
		Collection<String> edgeFeatures = model.getFeatureModel().getEdgeFeatures();
		features.put(EDGE_CATEGORY_KEY, edgeFeatures);
		// Build feature names
		HashMap<String, String> featureNames = new HashMap<String, String>(trackFeatures.size() + edgeFeatures.size());
		featureNames.putAll(model.getFeatureModel().getTrackFeatureNames());
		featureNames.putAll(model.getFeatureModel().getEdgeFeatureNames());
		// Build category names
		HashMap<String, String> categoryNames = new HashMap<String, String>(2);
		categoryNames.put(TRACK_CATEGORY_KEY, "Track features:");
		categoryNames.put(EDGE_CATEGORY_KEY, "Edge features:");
		return new CategoryJComboBox<String, String>(features, featureNames, categoryNames);
	}
	

	/**
	 * Returns the feature values for the item currently selected in the combo box.
	 * @param cb  the {@link CategoryJComboBox} to interrogate.
	 * @return a new double array containing the feature values.
	 */
	protected double[] getValues(CategoryJComboBox<String, String> cb) {
		double[] values;
		String category = cb.getSelectedCategory();
		if (category.equals(TRACK_CATEGORY_KEY)) {
			values = model.getFeatureModel().getTrackFeatureValues(cb.getSelectedItem(), true);
		} else if (category.equals(EDGE_CATEGORY_KEY)) {
			values = model.getFeatureModel().getEdgeFeatureValues(cb.getSelectedItem(), true);
		} else {
			throw new IllegalArgumentException("Unknown category: " + category);
		}
		return values;
	}

}
