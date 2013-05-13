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
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

public class JPanelColorByFeatureGUI extends ActionListenablePanel {

	/*
	 * FIELDS
	 */

	private static final long serialVersionUID = 498572562002300656L;
	/**
	 * This action is fired when the feature to color in the "Set color by feature"
	 * JComboBox is changed.
	 */
	public final ActionEvent COLOR_FEATURE_CHANGED = new ActionEvent(this, 1, "ColorFeatureChanged");
	private JLabel jLabelSetColorBy;
	private JComboBox jComboBoxSetColorBy;
	private JPanel jPanelByFeature;
	private Canvas canvasColor;
	private JPanel jPanelColor;

	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;

	/*
	 * DEFAULT VISIBILITY
	 */

	String setColorByFeature;
	
	private Map<String, double[]> featureValues;
	private Map<String, String> featureNames;
	private List<String> features;

	private ActionListenablePanel caller;

	/*
	 * CONSTRUCTOR
	 */

	public JPanelColorByFeatureGUI(final List<String> features, final Map<String, String> featureNames, final ActionListenablePanel caller) {
		super();
		this.features = features;
		this.featureNames = featureNames;
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

	public String getSelectedFeature() {
		return setColorByFeature;
	}
	
	public void setColorByFeature(String feature) {
		if (null == feature) {
			jComboBoxSetColorBy.setSelectedIndex(0);
		} else {
			jComboBoxSetColorBy.setSelectedItem(featureNames.get(feature));
		}
	}

	/*
	 * PRIVATE METHODS
	 */
	

	/**
	 * Forward the 'color by feature' action to the caller of this GUI.
	 */
	private void colorByFeatureChanged() {
		int selection = jComboBoxSetColorBy.getSelectedIndex();
		if (selection == 0) 
			setColorByFeature = null;
		else
			setColorByFeature = features.get(selection-1);
		caller.fireAction(COLOR_FEATURE_CHANGED);
		
		
	}

	private void repaintColorCanvas(Graphics g) {
		if (null == setColorByFeature) {
			g.clearRect(0, 0, canvasColor.getWidth(), canvasColor.getHeight());
			return;
		}

		final double[] values = featureValues.get(setColorByFeature);
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
				String[] featureStringList = new String[features.size()+1];
				featureStringList[0] = "Default";
				for (int i = 0; i < features.size(); i++) 
					featureStringList[i+1] = featureNames.get(features.get(i));
				ComboBoxModel jComboBoxSetColorByModel = new DefaultComboBoxModel(featureStringList);
				jComboBoxSetColorBy = new JComboBox();
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(jComboBoxSetColorBy);
				jComboBoxSetColorBy.setModel(jComboBoxSetColorByModel);
				jComboBoxSetColorBy.setFont(SMALL_FONT);
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

	public Map<String, double[]> getFeatureValues() {
		return featureValues;
	}

	public void setFeatureValues(Map<String, double[]> featureValues) {
		this.featureValues = featureValues;
	}
}
