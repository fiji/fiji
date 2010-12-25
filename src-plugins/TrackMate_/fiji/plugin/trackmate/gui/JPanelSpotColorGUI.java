package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.renderer.InterpolatePaintScale;

import fiji.plugin.trackmate.Feature;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class JPanelSpotColorGUI extends ActionListenablePanel {

	/*
	 * FIELDS
	 */
	
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
	
	private String[] featureStringList;
	protected InterpolatePaintScale colorMap = InterpolatePaintScale.Jet;

	/*
	 * DEFAULT VISIBILITY
	 */
	
	EnumMap<Feature, double[]> featureValues = new EnumMap<Feature, double[]>(Feature.class);
	Feature setColorByFeature;


	private ActionListenablePanel caller;

	/*
	 * CONSTRUCTOR
	 */
	
	public JPanelSpotColorGUI(ActionListenablePanel caller) {
		super();
		this.caller = caller;
		initGUI();
		
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void setColorByFeature() {
		int selection = jComboBoxSetColorBy.getSelectedIndex();
		if (selection == 0) 
			setColorByFeature = null;
		else
			setColorByFeature = Feature.values()[selection-1];
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
			this.setPreferredSize(new java.awt.Dimension(270, 55));

			jPanelByFeature = new JPanel();
			BoxLayout jPanelByFeatureLayout = new BoxLayout(jPanelByFeature, javax.swing.BoxLayout.X_AXIS);
			jPanelByFeature.setLayout(jPanelByFeatureLayout);
			add(jPanelByFeature, BorderLayout.CENTER);
			jPanelByFeature.setPreferredSize(new java.awt.Dimension(270, 35));
			jPanelByFeature.setMaximumSize(new java.awt.Dimension(32767, 50));
			jPanelByFeature.setSize(270, 40);
			{
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jLabelSetColorBy = new JLabel();
				jPanelByFeature.add(jLabelSetColorBy);
				jLabelSetColorBy.setText("Set color by");
				jLabelSetColorBy.setFont(SMALL_FONT);
			}
			{
				Feature[] allFeatures = Feature.values();
				featureStringList = new String[allFeatures.length+1];
				featureStringList[0] = "Default";
				for (int i = 0; i < allFeatures.length; i++) 
					featureStringList[i+1] = allFeatures[i].toString();
				ComboBoxModel jComboBoxSetColorByModel = new DefaultComboBoxModel(featureStringList);
				jComboBoxSetColorBy = new JComboBox();
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(Box.createHorizontalStrut(5));
				jPanelByFeature.add(jComboBoxSetColorBy);
				jComboBoxSetColorBy.setModel(jComboBoxSetColorByModel);
				jComboBoxSetColorBy.setFont(SMALL_FONT);
				jComboBoxSetColorBy.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setColorByFeature();
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
