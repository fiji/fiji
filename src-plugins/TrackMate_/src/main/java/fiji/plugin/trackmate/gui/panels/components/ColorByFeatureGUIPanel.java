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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.gui.panels.ActionListenablePanel;

public class ColorByFeatureGUIPanel extends ActionListenablePanel {

	/** The key for the default, uniform painting style. */
	public static final String UNIFORM_KEY = "UNIFORM";
	/** The name of the default, uniform painting style. */
	public static final String UNIFORM_NAME = "Uniform color";


	/*
	 * ENUM
	 */

	public static enum Category {
		SPOTS("spots"),
		EDGES("edges"),
		TRACKS("tracks"),
		DEFAULT("Default");

		private String name;

		private Category(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}

	}

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 1L;
	/** This action is fired when the feature to color in the "Set color by feature"
	 * JComboBox is changed. */
	public final ActionEvent COLOR_FEATURE_CHANGED = new ActionEvent(this, 1, "ColorFeatureChanged");
	private JLabel jLabelSetColorBy;
	private CategoryJComboBox<Category, String> jComboBoxSetColorBy;
	private JPanel jPanelByFeature;
	private Canvas canvasColor;
	private JPanel jPanelColor;

	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;
	protected final Model model;
	private final List<Category> categories;

	/*
	 * CONSTRUCTOR
	 */

	public ColorByFeatureGUIPanel(final Model model, final List<Category> categories) {
		super();
		this.model = model;
		this.categories = categories;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Forward the enabled flag to all components off this panel.
	 */
	@Override
	public void setEnabled(final boolean enabled) {
		jLabelSetColorBy.setEnabled(enabled);
		jComboBoxSetColorBy.setEnabled(enabled);
		canvasColor.setEnabled(enabled);
	}

	/**
	 * Returns a key to the color generator category selected in the combo box.
	 * Will be a {@link Category} enum type, as set in constructor.
	 * @return the selected category.
	 * @see #getColorFeature()
	 */
	public Category getColorGeneratorCategory() {
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

	public void setColorFeature(final String feature) {
		if (null == feature) {
			jComboBoxSetColorBy.setSelectedItem(UNIFORM_KEY);
		} else {
			jComboBoxSetColorBy.setSelectedItem(feature);
		}
	}

	/*
	 * PRIVATE METHODS
	 */


	/**
	 * Forward the 'color by feature' action to the caller of this GUI.
	 */
	private void colorByFeatureChanged() {
		super.fireAction(COLOR_FEATURE_CHANGED);
	}

	private void repaintColorCanvas(final Graphics g) {
		if (null == jComboBoxSetColorBy.getSelectedItem() || getColorGeneratorCategory().equals(Category.DEFAULT)) {
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
		final FontMetrics fm = g.getFontMetrics();
		final String minStr = String.format("%.1f", min);
		final String maxStr = String.format("%.1f", max);
		g.drawString(minStr, 1, height/2 + fm.getHeight()/2);
		g.drawString(maxStr, width - fm.stringWidth(maxStr)-1, height/2 + fm.getHeight()/2);
	}


	private void initGUI() {

		{
			final BorderLayout layout = new BorderLayout();
			setLayout(layout);
			this.setPreferredSize(new java.awt.Dimension(270, 45));

			jPanelByFeature = new JPanel();
			final BoxLayout jPanelByFeatureLayout = new BoxLayout(jPanelByFeature, javax.swing.BoxLayout.X_AXIS);
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
				jComboBoxSetColorBy = createComboBoxSelector(categories);
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(jComboBoxSetColorBy);
				jComboBoxSetColorBy.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						colorByFeatureChanged();
						canvasColor.repaint();
					}
				});
			}
		}
		{
			jPanelColor = new JPanel();
			final BorderLayout jPanelColorLayout = new BorderLayout();
			add(jPanelColor, BorderLayout.SOUTH);
			jPanelColor.setLayout(jPanelColorLayout);
			jPanelColor.setPreferredSize(new java.awt.Dimension(10, 20));
			{
				canvasColor = new Canvas() {
					private static final long serialVersionUID = -2174317490066575040L;
					@Override
					public void paint(final Graphics g) {
						repaintColorCanvas(g);
					}
				};
				jPanelColor.add(canvasColor, BorderLayout.CENTER);
				canvasColor.setPreferredSize(new java.awt.Dimension(270, 20));
				canvasColor.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(final MouseEvent e) {
						canvasColor.repaint();
					}
				});
			}
		}
		{
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					canvasColor.repaint();
				}
			});
		}
	}

	/**
	 * Return the {@link CategoryJComboBox} that configures this selector.
	 * Subclasses can override this method to decide what items are in the combo box list.
	 * @return a new {@link CategoryJComboBox}.
	 */
	protected CategoryJComboBox<Category, String> createComboBoxSelector(final List<Category> categories) {
		final LinkedHashMap<Category, Collection<String>> features = new LinkedHashMap<Category, Collection<String>>(categories.size());
		final HashMap<Category, String> categoryNames = new HashMap<Category, String>(categories.size());
		final HashMap<String, String> featureNames = new HashMap<String, String>();

		for (final Category category : categories) {
			switch (category) {
			case SPOTS:
				categoryNames.put(Category.SPOTS, "Spot features:");
				final Collection<String> spotFeatures = model.getFeatureModel().getSpotFeatures();
				features.put(Category.SPOTS, spotFeatures);
				featureNames.putAll(model.getFeatureModel().getSpotFeatureNames());
				break;

			case EDGES:
				categoryNames.put(Category.EDGES, "Edge features:");
				final Collection<String> edgeFeatures = model.getFeatureModel().getEdgeFeatures();
				features.put(Category.EDGES, edgeFeatures);
				featureNames.putAll(model.getFeatureModel().getEdgeFeatureNames());
				break;

			case TRACKS:
				categoryNames.put(Category.TRACKS, "Track features:");
				final Collection<String> trackFeatures = model.getFeatureModel().getTrackFeatures();
				features.put(Category.TRACKS, trackFeatures);
				featureNames.putAll(model.getFeatureModel().getTrackFeatureNames());
				break;

			case DEFAULT:
				categoryNames.put(Category.DEFAULT, "Default:");
				final Collection<String> defaultOptions = new ArrayList<String>();
				defaultOptions.add(UNIFORM_KEY);
				features.put(Category.DEFAULT, defaultOptions );
				featureNames.put(UNIFORM_KEY, UNIFORM_NAME);
				break;

			default:
				throw new IllegalArgumentException("Unknown category: " + category);
			}
		}
		return new CategoryJComboBox<Category, String>(features, featureNames, categoryNames);
	}


	/**
	 * Returns the feature values for the item currently selected in the combo box.
	 * @param cb  the {@link CategoryJComboBox} to interrogate.
	 * @return a new double array containing the feature values.
	 */
	protected double[] getValues(final CategoryJComboBox<Category, String> cb) {

		double[] values;
		final Category category = cb.getSelectedCategory();
		final String feature = cb.getSelectedItem();
		switch (category) {
		case TRACKS:
			values = model.getFeatureModel().getTrackFeatureValues(feature, true);
			break;
		case EDGES:
			values = model.getFeatureModel().getEdgeFeatureValues(feature, true);
			break;
		case SPOTS:
			final SpotCollection spots = model.getSpots();
			values = spots.collectValues(feature, false);
			break;
		case DEFAULT:
			throw new IllegalArgumentException("Cannot return values for " + category);
		default:
			throw new IllegalArgumentException("Unknown category: " + category);
		}
		return values;
	}

}
