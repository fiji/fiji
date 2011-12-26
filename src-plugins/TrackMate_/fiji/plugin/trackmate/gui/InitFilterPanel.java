package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Spot;

/**
 * 
 */
public class InitFilterPanel extends ActionListenablePanel {

	private static final long serialVersionUID = -5067695740285574761L;
	private static final String EXPLANATION_TEXT = "<html><p align=\"justify\">" +
			"Set here a threshold on the quality feature to restrict the number of spots " +
			"before calculating other features and rendering. This step can help save " +
			"time in the case of a very large number of spots. " +
			"<br/> " +
			"Warning: the spot filtered here will be discarded: they will not be saved " +
			"and cannot be retrieved by any other means than re-doing the segmentation " +
			"step." +
			"</html>";
	private static final String SELECTED_SPOT_STRING = "Selected spots: %d out of %d";

	private Map<String, double[]> features;
	private FilterPanel jPanelThreshold;
	private JPanel jPanelFields;
	private JLabel jLabelInitialThreshold;
	private JLabel jLabelExplanation;
	private JLabel jLabelSelectedSpots;
	private JPanel jPanelText;

	public InitFilterPanel(Map<String, double[]> featureValues) {
		this(featureValues, null);
	}


	public InitFilterPanel(Map<String, double[]> featureValues, Float initialFilterValue) {
		super();
		this.features = featureValues;
		initGUI(initialFilterValue);
		thresholdChanged();
	}

	/*
	 * PUBLIC METHOD
	 */

	/**
	 * Return the feature threshold on quality set by this panel. 
	 */
	public FeatureFilter getFeatureThreshold() {
		return new FeatureFilter(jPanelThreshold.getKey(), new Float(jPanelThreshold.getThreshold()), jPanelThreshold.isAboveThreshold());
	}

	/*
	 * PRIVATE METHODS
	 */

	private void thresholdChanged() {
		double threshold  = jPanelThreshold.getThreshold();
		boolean isAbove = jPanelThreshold.isAboveThreshold();
		double[] values = features.get(Spot.QUALITY);
		if (null == values)
			return;
		int nspots = values.length;
		int nselected = 0;
		if (isAbove) {
			for (double val : values) 
				if (val >= threshold)
					nselected++;
		} else {
			for (double val : values) 
				if (val <= threshold)
					nselected++;
		}
		jLabelSelectedSpots.setText(String.format(SELECTED_SPOT_STRING, nselected, nspots));
	}


	private void initGUI(Float initialThreshold) { 
		try {
			BorderLayout thisLayout = new BorderLayout();
			this.setLayout(thisLayout);
			this.setPreferredSize(new java.awt.Dimension(300, 500));
			{

				ArrayList<String> keys = new ArrayList<String>(1);
				keys.add(Spot.QUALITY);
				HashMap<String, String> keyNames = new HashMap<String, String>(1);
				keyNames.put(Spot.QUALITY, Spot.FEATURE_NAMES.get(Spot.QUALITY));
				jPanelThreshold = new FilterPanel(features, keys, keyNames);
				if (null != initialThreshold)
					jPanelThreshold.setThreshold(initialThreshold);
				else
					jPanelThreshold.setThreshold(0);
				jPanelThreshold.jComboBoxFeature.setEnabled(false);
				jPanelThreshold.jRadioButtonAbove.setEnabled(false);
				jPanelThreshold.jRadioButtonBelow.setEnabled(false);
				this.add(jPanelThreshold, BorderLayout.CENTER);
				jPanelThreshold.setPreferredSize(new java.awt.Dimension(300, 200));
				jPanelThreshold.addChangeListener(new ChangeListener() {
					public void stateChanged(ChangeEvent e) {
						thresholdChanged();
					}
				});
			}
			{
				jPanelFields = new JPanel();
				this.add(jPanelFields, BorderLayout.SOUTH);
				jPanelFields.setPreferredSize(new java.awt.Dimension(300, 100));
				jPanelFields.setLayout(null);
				{
					jLabelSelectedSpots = new JLabel();
					jPanelFields.add(jLabelSelectedSpots);
					jLabelSelectedSpots.setText("Selected spots: <n1> out of <n2>");
					jLabelSelectedSpots.setBounds(12, 12, 276, 15);
					jLabelSelectedSpots.setFont(FONT);
				}
			}
			{
				jPanelText = new JPanel();
				this.add(jPanelText, BorderLayout.NORTH);
				jPanelText.setPreferredSize(new Dimension(300, 200));
				jPanelText.setLayout(null);
				{
					jLabelInitialThreshold = new JLabel();
					jPanelText.add(jLabelInitialThreshold);
					jLabelInitialThreshold.setText("Initial thresholding");
					jLabelInitialThreshold.setFont(BIG_FONT);
					jLabelInitialThreshold.setBounds(12, 12, 276, 15);
				}
				{
					jLabelExplanation = new JLabel();
					jPanelText.add(jLabelExplanation);
					jLabelExplanation.setText(EXPLANATION_TEXT);
					jLabelExplanation.setBounds(12, 39, 276, 100);
					jLabelExplanation.setFont(FONT.deriveFont(Font.ITALIC));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/*
	 * MAIN METHOD
	 */


	/**
	 * Auto-generated main method to display this 
	 * JPanel inside a new JFrame.
	 */
	public static void main(String[] args) {
		// Prepare fake data
		final int N_ITEMS = 100;
		final Random ran = new Random();
		double mean;

		Map<String, double[]> fv = new HashMap<String, double[]>();
		double[] val = new double[N_ITEMS];
		mean = ran.nextDouble() * 10;
		for (int j = 0; j < val.length; j++) 
			val[j] = ran.nextGaussian() + 5 + mean;
		fv.put(Spot.QUALITY, val);

		JFrame frame = new JFrame();
		frame.getContentPane().add(new InitFilterPanel(fv));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
