package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.DEFAULT_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALTERNATIVE_LINKING_COST_FACTOR;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_BLOCKING_VALUE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_CUTOFF_PERCENTILE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.util.NumberParser;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A simplified configuration panel for the {@link LAPTracker}.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - 2010-2011
 */
public class SimpleLAPTrackerSettingsPanel extends ConfigurationPanel {

	private static final long serialVersionUID = -1L;
	private JLabel jLabelLinkingMaxDistanceUnit;
	private JLabel jLabelTrackerName;
	private JLabel jLabelGapClosingTimeCutoffUnit;
	private JLabel jLabelGapClosingMaxDistanceUnit;
	private JNumericTextField jTextFieldGapClosingTimeCutoff;
	private JNumericTextField jTextFieldGapClosingDistanceCutoff;
	private JNumericTextField jTextFieldLinkingDistance;
	private JLabel jLabelTrackerDescription;

	private String infoText;
	private String trackerName;
	private String spaceUnits;

	/*
	 * CONSTRUCTOR
	 */


	public SimpleLAPTrackerSettingsPanel(String trackerName, String infoText, String spaceUnits) {
		this.trackerName = trackerName;
		this.infoText = infoText;
		this.spaceUnits = spaceUnits;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void setSettings(final Map<String, Object> settings) {
		echoSettings(settings);
	};

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();
		// Linking
		settings.put(KEY_LINKING_FEATURE_PENALTIES, DEFAULT_LINKING_FEATURE_PENALTIES);
		// Gap closing
		settings.put(KEY_ALLOW_GAP_CLOSING, true);
		settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, DEFAULT_GAP_CLOSING_FEATURE_PENALTIES);
		// Track splitting
		settings.put(KEY_ALLOW_TRACK_SPLITTING, false);
		settings.put(KEY_SPLITTING_MAX_DISTANCE, DEFAULT_SPLITTING_MAX_DISTANCE);
		settings.put(KEY_SPLITTING_FEATURE_PENALTIES, DEFAULT_SPLITTING_FEATURE_PENALTIES);
		// Track merging
		settings.put(KEY_ALLOW_TRACK_MERGING, false);
		settings.put(KEY_MERGING_MAX_DISTANCE, DEFAULT_MERGING_MAX_DISTANCE);
		settings.put(KEY_MERGING_FEATURE_PENALTIES, DEFAULT_MERGING_FEATURE_PENALTIES);
		// Others
		settings.put(KEY_BLOCKING_VALUE, DEFAULT_BLOCKING_VALUE);
		settings.put(KEY_ALTERNATIVE_LINKING_COST_FACTOR, DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR);
		settings.put(KEY_CUTOFF_PERCENTILE, DEFAULT_CUTOFF_PERCENTILE);
		// Panel ones
		settings.put(KEY_LINKING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldLinkingDistance.getText()));
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldGapClosingDistanceCutoff.getText()));
		settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, NumberParser.parseInteger(jTextFieldGapClosingTimeCutoff.getText()));
		// Hop!
		return settings;
	}

	/*
	 * PRIVATE METHODS
	 */

	private void echoSettings(final Map<String, Object> settings) {
		jTextFieldLinkingDistance.setText(String.format("%.1f", (Double) settings.get(KEY_LINKING_MAX_DISTANCE)));
		jTextFieldGapClosingDistanceCutoff.setText(String.format("%.1f", (Double) settings.get(KEY_GAP_CLOSING_MAX_DISTANCE)));
		jTextFieldGapClosingTimeCutoff.setText(String.format("%d", (Integer) settings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP)));
	}

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
				JLabel jLabel1 = new JLabel();
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
				jLabelTrackerName.setText(trackerName);
			}
			{
				jLabelTrackerDescription = new JLabel();
				this.add(jLabelTrackerDescription, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
				jLabelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
				jLabelTrackerDescription.setText(infoText
						.replace("<br>", "")
						.replace("<p>", "<p align=\"justify\">")
						.replace("<html>", "<html><p align=\"justify\">"));
			}
			{
				JLabel jLabel2 = new JLabel();
				this.add(jLabel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel2.setFont(FONT);
				jLabel2.setText("Linking max distance:");
			}
			{
				JLabel jLabel3 = new JLabel();
				this.add(jLabel3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel3.setFont(FONT);
				jLabel3.setText("Gap-closing max distance:");
			}
			{
				JLabel jLabel4 = new JLabel();
				this.add(jLabel4, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel4.setFont(FONT);
				jLabel4.setText("Gap-closing max frame gap:");
			}
			{
				jTextFieldLinkingDistance = new JNumericTextField();
				jTextFieldLinkingDistance.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldLinkingDistance, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldLinkingDistance.setFont(FONT);
			}
			{
				jTextFieldGapClosingDistanceCutoff = new JNumericTextField();
				jTextFieldGapClosingDistanceCutoff.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldGapClosingDistanceCutoff, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingDistanceCutoff.setFont(FONT);
			}
			{
				jTextFieldGapClosingTimeCutoff = new JNumericTextField();
				jTextFieldGapClosingTimeCutoff.setMinimumSize(TEXTFIELD_DIMENSION);
				this.add(jTextFieldGapClosingTimeCutoff, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingTimeCutoff.setFont(FONT);
			}
			{
				jLabelLinkingMaxDistanceUnit = new JLabel();
				this.add(jLabelLinkingMaxDistanceUnit, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelLinkingMaxDistanceUnit.setFont(FONT);
				jLabelLinkingMaxDistanceUnit.setText(spaceUnits);
			}
			{
				jLabelGapClosingMaxDistanceUnit = new JLabel();
				this.add(jLabelGapClosingMaxDistanceUnit, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelGapClosingMaxDistanceUnit.setFont(FONT);
				jLabelGapClosingMaxDistanceUnit.setText(spaceUnits);
			}
			{
				jLabelGapClosingTimeCutoffUnit = new JLabel();
				this.add(jLabelGapClosingTimeCutoffUnit, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelGapClosingTimeCutoffUnit.setFont(FONT);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
