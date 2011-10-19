package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.TEXTFIELD_DIMENSION;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;

/**
 * A simplified configuration panel for the {@link LAPTracker}.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> - 2010-2011
 */
public class SimpleLAPTrackerSettingsPanel extends TrackerSettingsPanel {
	
	private static final long serialVersionUID = 4099537392544048109L;
	private JLabel jLabelLinkingMaxDistanceUnit;
	private JLabel jLabelTrackerName;
	private JLabel jLabelGapClosingTimeCutoffUnit;
	private JLabel jLabelGapClosingMaxDistanceUnit;
	private JNumericTextField jTextFieldGapClosingTimeCutoff;
	private JNumericTextField jTextFieldGapClosingDistanceCutoff;
	private JNumericTextField jTextFieldLinkingDistance;
	private JLabel jLabelTrackerDescription;
	
	private LAPTrackerSettings settings;

	/*
	 * CONSTRUCTOR
	 */
	
	
	public SimpleLAPTrackerSettingsPanel() {
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	@Override
	public void setTrackerSettings(TrackMateModel model) {
		this.settings = (LAPTrackerSettings) model.getSettings().trackerSettings;
		echoSettings(model);
		
	};
	
	@Override
	public TrackerSettings getTrackerSettings() {
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
	
	private void echoSettings(TrackMateModel model) {
		jLabelTrackerName.setText(model.getSettings().tracker.toString());
		jLabelTrackerDescription.setText(model.getSettings().tracker.getInfoText()
				.replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
		jTextFieldLinkingDistance.setText(String.format("%.1f", settings.linkingDistanceCutOff));
		jTextFieldGapClosingDistanceCutoff.setText(String.format("%.1f", settings.gapClosingDistanceCutoff));
		jTextFieldGapClosingTimeCutoff.setText(String.format("%.1f", settings.gapClosingTimeCutoff));
		jLabelGapClosingMaxDistanceUnit.setText(model.getSettings().spaceUnits);
		jLabelGapClosingTimeCutoffUnit.setText(model.getSettings().timeUnits);
		jLabelLinkingMaxDistanceUnit.setText(model.getSettings().spaceUnits);
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
			}
			{
				jLabelTrackerDescription = new JLabel();
				this.add(jLabelTrackerDescription, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
				jLabelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
			}
			{
				JLabel jLabel2 = new JLabel();
				this.add(jLabel2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel2.setFont(FONT);
				jLabel2.setText("Linking distance cutoff:");
			}
			{
				JLabel jLabel3 = new JLabel();
				this.add(jLabel3, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel3.setFont(FONT);
				jLabel3.setText("Gap-closing distance cutoff:");
			}
			{
				JLabel jLabel4 = new JLabel();
				this.add(jLabel4, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel4.setFont(FONT);
				jLabel4.setText("Gap-closing time cutoff:");
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
			}
			{
				jLabelGapClosingMaxDistanceUnit = new JLabel();
				this.add(jLabelGapClosingMaxDistanceUnit, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 10), 0, 0));
				jLabelGapClosingMaxDistanceUnit.setFont(FONT);
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
