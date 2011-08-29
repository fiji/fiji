package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * A simplified configuration panel for the {@link LAPTracker}.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - 2010-2011
 */
public class SimpleLAPTrackerSettingsPanel extends TrackerSettingsPanel {
	
	private static final long serialVersionUID = 7869519363287710721L;

	private JLabel jLabel1;
	private JLabel jLabel2;
	private JLabel jLabel4;
	private JLabel jLabelLinkingMaxDistanceUnit;
	private JLabel jLabelTrackerName;
	private JLabel jLabelGapClosingTimeCutoffUnit;
	private JLabel jLabelGapClosingMaxDistanceUnit;
	private JNumericTextField jTextFieldGapClosingTimeCutoff;
	private JNumericTextField jTextFieldGapClosingDistanceCutoff;
	private JNumericTextField jTextFieldLinkingDistance;
	private JLabel jLabel3;
	private JLabel jLabelTrackerDescription;
	
	private TrackerSettings settings;

	/*
	 * CONSTRUCTOR
	 */
	
	
	public SimpleLAPTrackerSettingsPanel(TrackerSettings settings) {
		super();
		this.settings = settings;
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public TrackerSettings getSettings() {
		// Default ones
		settings.allowMerging 		= false;
		settings.allowSplitting 	= false;
		settings.linkingFeaturePenalties.clear();
		settings.gapClosingFeaturePenalties.clear();
		// Panel ones
		settings.linkingDistanceCutOff 		= Double.parseDouble(jTextFieldLinkingDistance.getText());
		settings.gapClosingDistanceCutoff	= Double.parseDouble(jTextFieldGapClosingDistanceCutoff.getText());
		settings.gapClosingTimeCutoff		= Double.parseDouble(jTextFieldGapClosingTimeCutoff.getText());
		// Hop!
		return settings;
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private void initGUI() {
		try {
			this.setPreferredSize(new java.awt.Dimension(300, 500));
			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
			thisLayout.rowHeights = new int[] {31, 50, 119, 7, 50, 50, 50, 50};
			thisLayout.columnWeights = new double[] {0.0, 0.0, 0.1};
			thisLayout.columnWidths = new int[] {203, 42, 7};
			this.setLayout(thisLayout);
			{
				jLabel1 = new JLabel();
				this.add(jLabel1, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel1.setFont(FONT);
				jLabel1.setText("Settings for tracker:");
			}
			{
				jLabelTrackerName = new JLabel();
				this.add(jLabelTrackerName, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
				jLabelTrackerName.setHorizontalTextPosition(SwingConstants.CENTER);
				jLabelTrackerName.setHorizontalAlignment(SwingConstants.CENTER);
				jLabelTrackerName.setFont(BIG_FONT);
				jLabelTrackerName.setText(settings.trackerType.toString());
			}
			{
				jLabelTrackerDescription = new JLabel();
				this.add(jLabelTrackerDescription, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
				jLabelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelTrackerDescription.setText(settings.trackerType.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
			}
			{
				jLabel2 = new JLabel();
				this.add(jLabel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel2.setFont(FONT);
				jLabel2.setText("Linking distance cutoff:");
			}
			{
				jLabel3 = new JLabel();
				this.add(jLabel3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel3.setFont(FONT);
				jLabel3.setText("Gap-closing distance cutoff:");
			}
			{
				jLabel4 = new JLabel();
				this.add(jLabel4, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel4.setFont(FONT);
				jLabel4.setText("Gap-closing time cutoff:");
			}
			{
				jTextFieldLinkingDistance = new JNumericTextField();
				jTextFieldLinkingDistance.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldLinkingDistance, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldLinkingDistance.setFont(FONT);
				jTextFieldLinkingDistance.setText(String.format("%.1f", settings.linkingDistanceCutOff));
			}
			{
				jTextFieldGapClosingDistanceCutoff = new JNumericTextField();
				jTextFieldGapClosingDistanceCutoff.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldGapClosingDistanceCutoff, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingDistanceCutoff.setFont(FONT);
				jTextFieldGapClosingDistanceCutoff.setText(String.format("%.1f", settings.gapClosingDistanceCutoff));
			}
			{
				jTextFieldGapClosingTimeCutoff = new JNumericTextField();
				jTextFieldGapClosingTimeCutoff.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldGapClosingTimeCutoff, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingTimeCutoff.setFont(FONT);
				jTextFieldGapClosingTimeCutoff.setText(String.format("%.1f", settings.gapClosingTimeCutoff));
			}
			{
				jLabelLinkingMaxDistanceUnit = new JLabel();
				this.add(jLabelLinkingMaxDistanceUnit, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelLinkingMaxDistanceUnit.setFont(FONT);
				jLabelLinkingMaxDistanceUnit.setText(settings.spaceUnits);
			}
			{
				jLabelGapClosingMaxDistanceUnit = new JLabel();
				this.add(jLabelGapClosingMaxDistanceUnit, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelGapClosingMaxDistanceUnit.setFont(FONT);
				jLabelGapClosingMaxDistanceUnit.setText(settings.spaceUnits);
			}
			{
				jLabelGapClosingTimeCutoffUnit = new JLabel();
				this.add(jLabelGapClosingTimeCutoffUnit, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelGapClosingTimeCutoffUnit.setFont(FONT);
				jLabelGapClosingTimeCutoffUnit.setText(settings.timeUnits);
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
		JFrame frame = new JFrame();
		TrackerSettings ts = new TrackerSettings();
		ts.trackerType = fiji.plugin.trackmate.tracking.TrackerType.SIMPLE_LAP_TRACKER;
		frame.getContentPane().add(new SimpleLAPTrackerSettingsPanel(ts));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
