package fiji.plugin.trackmate.gui;
import java.awt.BorderLayout;
import java.awt.Font;

import java.awt.Dimension;
import java.util.EnumMap;
import java.util.Random;

import javax.swing.WindowConstants;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT; 

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.SpotFeature;

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
public class InitThresholdPanel extends ActionListenablePanel {
	
	private static final long serialVersionUID = -5067695740285574761L;
	private static final String EXPLANATION_TEXT = "<html><p align=\"justify\">" +
			"Set here a threshold on the quality feature to restrict the number of spots " +
			"before calculating other features and rendering. " +
			"</html>";
	private static final String SELECTED_SPOT_STRING = "Selected spots: %d out of %d";
	
	private EnumMap<SpotFeature, double[]> features;
	private FilterPanel<SpotFeature> jPanelThreshold;
	private JPanel jPanelFields;
	private JLabel jLabelInitialThreshold;
	private JLabel jLabelExplanation;
	private JLabel jLabelSelectedSpots;
	private JPanel jPanelText;

	public InitThresholdPanel(EnumMap<SpotFeature, double[]> featureValues) {
		this(featureValues, null);
	}
	
	
	public InitThresholdPanel(EnumMap<SpotFeature, double[]> featureValues,	Float initialThreshold) {
		super();
		this.features = featureValues;
		initGUI(initialThreshold);
		thresholdChanged();
	}

	/*
	 * PUBLIC METHOD
	 */

	/**
	 * Return the feature threshold on quality set by this panel. 
	 */
	public FeatureFilter<SpotFeature> getFeatureThreshold() {
		return new FeatureFilter<SpotFeature>(jPanelThreshold.getKey(), new Float(jPanelThreshold.getThreshold()), jPanelThreshold.isAboveThreshold());
	}
	
	/*
	 * PRIVATE METHODS
	 */
	
	private void thresholdChanged() {
		double threshold  = jPanelThreshold.getThreshold();
		boolean isAbove = jPanelThreshold.isAboveThreshold();
		double[] values = features.get(SpotFeature.QUALITY);
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
					
				jPanelThreshold = new FilterPanel<SpotFeature>(features, SpotFeature.QUALITY);
				if (null != initialThreshold)
					jPanelThreshold.setThreshold(initialThreshold);
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
				jPanelFields.setPreferredSize(new java.awt.Dimension(300, 200));
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
				jPanelText.setPreferredSize(new Dimension(300, 100));
				jPanelText.setLayout(null);
				{
					jLabelInitialThreshold = new JLabel();
					jPanelText.add(jLabelInitialThreshold);
					jLabelInitialThreshold.setText("Initial thresholding");
					jLabelInitialThreshold.setFont(FONT.deriveFont(Font.BOLD));
					jLabelInitialThreshold.setBounds(12, 12, 276, 15);
				}
				{
					jLabelExplanation = new JLabel();
					jPanelText.add(jLabelExplanation);
					jLabelExplanation.setText(EXPLANATION_TEXT);
					jLabelExplanation.setBounds(12, 39, 276, 49);
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
		fiji.plugin.trackmate.SpotFeature[] features = new fiji.plugin.trackmate.SpotFeature[] { 
				fiji.plugin.trackmate.SpotFeature.QUALITY, 
				fiji.plugin.trackmate.SpotFeature.ELLIPSOIDFIT_AXISPHI_A, 
				fiji.plugin.trackmate.SpotFeature.MEAN_INTENSITY };
		EnumMap<fiji.plugin.trackmate.SpotFeature, double[]> fv = new EnumMap<fiji.plugin.trackmate.SpotFeature, double[]>(fiji.plugin.trackmate.SpotFeature.class);
		for (fiji.plugin.trackmate.SpotFeature feature : features) {
			double[] val = new double[N_ITEMS];
			mean = ran.nextDouble() * 10;
			for (int j = 0; j < val.length; j++) 
				val[j] = ran.nextGaussian() + 5 + mean;
			fv.put(feature, val);
		}
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(new InitThresholdPanel(fv));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
