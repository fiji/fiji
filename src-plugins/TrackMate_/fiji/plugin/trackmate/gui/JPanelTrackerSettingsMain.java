package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class JPanelTrackerSettingsMain extends javax.swing.JPanel {
	
	private static final long serialVersionUID = -3775536792625326253L;
	
	private JLabel jLabelTrackDescription;
	private JLabel jLabelSplittingMaxDistanceUnit;
	private JNumericTextField jTextFieldSplittingMaxDistance;
	private JCheckBox jCheckBoxAllowSplitting;
	private JLabel jLabelTrackerName;
	private JPanelFeatureSelectionGui jPanelGapClosing;
	private JPanelFeatureSelectionGui jPanelMergingFeatures;
	private JPanelFeatureSelectionGui jPanelLinkingFeatures;
	private JPanelFeatureSelectionGui jPanelSplittingFeatures;
	private JScrollPane jScrollPaneMergingFeatures;
	private JLabel jLabelMergingMaxFrameIntervalUnit;
	private JNumericTextField jTextFieldMergingFrameInterval;
	private JLabel jLabelMergingMaxDistanceUnit;
	private JNumericTextField jTextFieldMergingMaxDistance;
	private JCheckBox jCheckBoxAllowMerging;
	private JScrollPane jScrollPaneSplittingFeatures;
	private JLabel jLabelSplittingMaxFrameIntervalUnit;
	private JNumericTextField jTextFieldSplittingMaxFrameInterval;
	private JScrollPane jScrollPaneGapClosingFeatures;
	private JLabel jLabelGapClosingMaxFrameIntervalUnit;
	private JNumericTextField jTextFieldGapClosingMaxFrameInterval;
	private JLabel jLabelGapClosingMaxDistanceUnit;
	private JNumericTextField jTextFieldGapClosingMaxDistance;
	private JCheckBox jCheckBoxAllowGapClosing;
	private JLabel jLabelLinkingMaxDistanceUnits;
	private JNumericTextField jTextFieldLinkingMaxDistance;
	private JLabel jLabel6;
	private JLabel jLabel7;
	private JLabel jLabel8;
	private JLabel jLabel10;
	private JLabel jLabel11;
	private JLabel jLabel15;
	private JLabel jLabel13;
	private JLabel jLabel14;
	private JLabel jLabel16;

	
	public JPanelTrackerSettingsMain() {
		initGUI();
	}
	
	/*
	 * PUBLIC METHODS
	 */
	
	
	void echoSettings(TrackMateModel model) {
		
		LAPTrackerSettings settings = (LAPTrackerSettings) model.getSettings().trackerSettings;
		
		if (null != model.getSettings().tracker) {
			jLabelTrackerName.setText(model.getSettings().tracker.toString());
			jLabelTrackDescription.setText(model.getSettings().tracker.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
		}

		jLabelLinkingMaxDistanceUnits.setText(model.getSettings().spaceUnits);
		jTextFieldLinkingMaxDistance.setText(String.format("%.1f", settings.linkingDistanceCutOff));
		jPanelLinkingFeatures.setDisplayFeatures(model.getFeatureModel().getSpotFeatures(), model.getFeatureModel().getSpotFeatureNames());
		jPanelLinkingFeatures.setSelectedFeaturePenalties(settings.linkingFeaturePenalties);
		
		jCheckBoxAllowGapClosing.setSelected(settings.allowGapClosing);
		jLabelGapClosingMaxDistanceUnit.setText(model.getSettings().spaceUnits);
		jLabelGapClosingMaxFrameIntervalUnit.setText(model.getSettings().timeUnits);
		jTextFieldGapClosingMaxDistance.setText(String.format("%.1f", settings.gapClosingDistanceCutoff));
		jTextFieldGapClosingMaxFrameInterval.setText(String.format("%.1f", settings.gapClosingTimeCutoff));
		jPanelGapClosing.setDisplayFeatures(model.getFeatureModel().getSpotFeatures(), model.getFeatureModel().getSpotFeatureNames());
		jPanelGapClosing.setSelectedFeaturePenalties(settings.gapClosingFeaturePenalties);
		
		jCheckBoxAllowSplitting.setSelected(settings.allowSplitting);
		jLabelSplittingMaxDistanceUnit.setText(model.getSettings().spaceUnits);
		jLabelSplittingMaxFrameIntervalUnit.setText(model.getSettings().timeUnits);
		jTextFieldSplittingMaxDistance.setText(String.format("%.1f", settings.splittingDistanceCutoff));
		jTextFieldSplittingMaxFrameInterval.setText(String.format("%.1f", settings.splittingTimeCutoff));
		jPanelSplittingFeatures.setDisplayFeatures(model.getFeatureModel().getSpotFeatures(), model.getFeatureModel().getSpotFeatureNames());
		jPanelSplittingFeatures.setSelectedFeaturePenalties(settings.splittingFeaturePenalties);
		
		jCheckBoxAllowMerging.setSelected(settings.allowMerging);
		jLabelMergingMaxDistanceUnit.setText(model.getSettings().spaceUnits);
		jLabelMergingMaxFrameIntervalUnit.setText(model.getSettings().timeUnits);
		jTextFieldMergingMaxDistance.setText(String.format("%.1f", settings.splittingDistanceCutoff));
		jTextFieldMergingFrameInterval.setText(String.format("%.1f", settings.mergingTimeCutoff));
		jPanelMergingFeatures.setDisplayFeatures(model.getFeatureModel().getSpotFeatures(), model.getFeatureModel().getSpotFeatureNames());
		jPanelMergingFeatures.setSelectedFeaturePenalties(settings.mergingFeaturePenalties);

		setEnabled(
				new Component[] {jLabel6, jTextFieldGapClosingMaxDistance, jLabelGapClosingMaxDistanceUnit, 
						jLabel7, jTextFieldGapClosingMaxFrameInterval, jTextFieldGapClosingMaxFrameInterval, jLabelGapClosingMaxFrameIntervalUnit,
						jLabel8, jScrollPaneGapClosingFeatures, jPanelGapClosing}, 
				jCheckBoxAllowGapClosing.isSelected());
		
		setEnabled(
				new Component[] {jLabel10, jTextFieldSplittingMaxDistance, jLabelSplittingMaxDistanceUnit, 
						jLabel11, jTextFieldSplittingMaxFrameInterval, jLabelSplittingMaxFrameIntervalUnit, jTextFieldSplittingMaxFrameInterval,
						jLabel15, jScrollPaneSplittingFeatures, jPanelSplittingFeatures}, 
				jCheckBoxAllowSplitting.isSelected());
		
		setEnabled(
				new Component[] {jLabel13, jTextFieldMergingMaxDistance, jLabelMergingMaxDistanceUnit, 
						jLabel14, jTextFieldMergingFrameInterval, jLabelMergingMaxFrameIntervalUnit,
						jLabel16, jScrollPaneMergingFeatures, jPanelMergingFeatures}, 
				jCheckBoxAllowMerging.isSelected());
	}
	
	/**
	 * Update the {@link Settings} object given at the creation of this panel with the
	 * settings entered by the user on this panel. Only the {@link Settings#trackerSettings} field
	 * and sub-fields will be updated here.
	 */
	public TrackerSettings getSettings() {
		LAPTrackerSettings settings = new LAPTrackerSettings();
		
		settings.linkingDistanceCutOff = Double.parseDouble(jTextFieldLinkingMaxDistance.getText());
		settings.linkingFeaturePenalties = jPanelLinkingFeatures.getFeatureWeights();
		
		settings.allowGapClosing 			= jCheckBoxAllowGapClosing.isSelected();
		settings.gapClosingDistanceCutoff	= Double.parseDouble(jTextFieldGapClosingMaxDistance.getText());
		settings.gapClosingTimeCutoff		= Double.parseDouble(jTextFieldGapClosingMaxFrameInterval.getText());
		settings.gapClosingFeaturePenalties	= jPanelGapClosing.getFeatureWeights();
		
		settings.allowSplitting				= jCheckBoxAllowSplitting.isSelected();
		settings.splittingDistanceCutoff	= Double.parseDouble(jTextFieldSplittingMaxDistance.getText());
		settings.splittingTimeCutoff		= Double.parseDouble(jTextFieldSplittingMaxFrameInterval.getText());
		settings.splittingFeaturePenalties	= jPanelSplittingFeatures.getFeatureWeights();
		
		settings.allowMerging				= jCheckBoxAllowMerging.isSelected();
		settings.mergingDistanceCutoff		= Double.parseDouble(jTextFieldMergingMaxDistance.getText());
		settings.mergingTimeCutoff			= Double.parseDouble(jTextFieldMergingFrameInterval.getText());
		settings.mergingFeaturePenalties	= jPanelMergingFeatures.getFeatureWeights();
		
		
		return settings;
	}
	
	
	
	/*
	 * PRIVATE METHODS
	 */
	
	
	private void setEnabled(final Component[] components, boolean enable) {
		for(Component component : components)
			component.setEnabled(enable);
	}
	
	
	
	private void initGUI() {

		try {
			this.setPreferredSize(new Dimension(280, 1000));
			GridBagLayout thisLayout = new GridBagLayout();
			thisLayout.columnWidths = new int[] {180, 50, 50};
			thisLayout.columnWeights = new double[] {0.1, 0.8, 0.1};
			thisLayout.rowHeights = new int[] {15, 20, 60, 15, 10, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95};
			thisLayout.rowWeights = new double[] {0.0, 0.1, 0.25, 0.1, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.25, 0.1, 0.0, 0.0, 0.0, 0.0, 0.0};
			this.setLayout(thisLayout);
			{
				JLabel jLabel1 = new JLabel();
				this.add(jLabel1, new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 0, 10), 0, 0));
				jLabel1.setText("Settings for tracker:");
				jLabel1.setFont(FONT);
			}
			{
				jLabelTrackerName = new JLabel();
				this.add(jLabelTrackerName, new GridBagConstraints(0, 1, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(10, 20, 0, 0), 0, 0));
				jLabelTrackerName.setHorizontalTextPosition(SwingConstants.CENTER);
				jLabelTrackerName.setHorizontalAlignment(SwingConstants.CENTER);
				jLabelTrackerName.setFont(BIG_FONT);
			}
			{
				jLabelTrackDescription = new JLabel();
				this.add(jLabelTrackDescription, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
				jLabelTrackDescription.setFont(SMALL_FONT.deriveFont(Font.ITALIC));
			}
			{
				JLabel jLabel2 = new JLabel();
				this.add(jLabel2, new GridBagConstraints(0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel2.setText("Frame to frame linking:");
				jLabel2.setFont(BIG_FONT.deriveFont(Font.BOLD));
			}
			{
				JLabel jLabel3 = new JLabel();
				this.add(jLabel3, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel3.setText("Max distance:");
				jLabel3.setFont(SMALL_FONT);
			}
			{
				jTextFieldLinkingMaxDistance = new JNumericTextField();
				this.add(jTextFieldLinkingMaxDistance, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldLinkingMaxDistance.setFont(SMALL_FONT);
				jTextFieldLinkingMaxDistance.setSize(TEXTFIELD_DIMENSION);
			}
			{
				jLabelLinkingMaxDistanceUnits = new JLabel();
				this.add(jLabelLinkingMaxDistanceUnits, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelLinkingMaxDistanceUnits.setFont(SMALL_FONT);
			}
			{
				JLabel jLabel4 = new JLabel();
				this.add(jLabel4, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel4.setText("Feature ratio thresholds");
				jLabel4.setFont(SMALL_FONT);
			}
			{
				JScrollPane jScrollPaneLinkingFeatures = new JScrollPane();
				MouseWheelListener[] l = jScrollPaneLinkingFeatures.getMouseWheelListeners();
				jScrollPaneLinkingFeatures.removeMouseWheelListener(l[0]);
				this.add(jScrollPaneLinkingFeatures, new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jScrollPaneLinkingFeatures.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneLinkingFeatures.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jPanelLinkingFeatures = new JPanelFeatureSelectionGui();
				jScrollPaneLinkingFeatures.setViewportView(jPanelLinkingFeatures);
			}

			// Gap closing
			
			{
				JLabel jLabel5 = new JLabel();
				this.add(jLabel5, new GridBagConstraints(0, 7, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 10, 0, 10), 0, 0));
				jLabel5.setText("Track segment gap closing:");
				jLabel5.setFont(BIG_FONT.deriveFont(Font.BOLD));
			}
			{
				jCheckBoxAllowGapClosing = new JCheckBox();
				this.add(jCheckBoxAllowGapClosing, new GridBagConstraints(0, 8, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jCheckBoxAllowGapClosing.setText("Allow gap closing");
				jCheckBoxAllowGapClosing.setFont(SMALL_FONT);
				jCheckBoxAllowGapClosing.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setEnabled(
								new Component[] {jLabel6, jTextFieldGapClosingMaxDistance, jLabelGapClosingMaxDistanceUnit, 
										jLabel7, jTextFieldGapClosingMaxFrameInterval, jTextFieldGapClosingMaxFrameInterval, jLabelGapClosingMaxFrameIntervalUnit,
										jLabel8, jScrollPaneGapClosingFeatures, jPanelGapClosing}, 
								jCheckBoxAllowGapClosing.isSelected());
					}
				});
			}
			{
				jLabel6 = new JLabel();
				this.add(jLabel6, new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel6.setText("Max distance:");
				jLabel6.setFont(SMALL_FONT);
			}
			{
				jTextFieldGapClosingMaxDistance = new JNumericTextField();
				this.add(jTextFieldGapClosingMaxDistance, new GridBagConstraints(1, 9, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingMaxDistance.setSize(TEXTFIELD_DIMENSION);
				jTextFieldGapClosingMaxDistance.setFont(SMALL_FONT);
			}
			{
				jLabelGapClosingMaxDistanceUnit = new JLabel();
				this.add(jLabelGapClosingMaxDistanceUnit, new GridBagConstraints(2, 9, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelGapClosingMaxDistanceUnit.setFont(SMALL_FONT);
			}
			{
				jLabel7 = new JLabel();
				this.add(jLabel7, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel7.setText("Max frame interval:");
				jLabel7.setFont(SMALL_FONT);
			}
			{
				jTextFieldGapClosingMaxFrameInterval = new JNumericTextField();
				this.add(jTextFieldGapClosingMaxFrameInterval, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingMaxFrameInterval.setSize(TEXTFIELD_DIMENSION);
				jTextFieldGapClosingMaxFrameInterval.setFont(SMALL_FONT);
			}
			{
				jLabelGapClosingMaxFrameIntervalUnit = new JLabel();
				this.add(jLabelGapClosingMaxFrameIntervalUnit, new GridBagConstraints(2, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelGapClosingMaxFrameIntervalUnit.setFont(SMALL_FONT);
			}
			{
				jLabel8 = new JLabel();
				this.add(jLabel8, new GridBagConstraints(0, 11, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel8.setText("Feature weights:");
				jLabel8.setFont(SMALL_FONT);
			}
			{
				jScrollPaneGapClosingFeatures = new JScrollPane();
				MouseWheelListener[] l = jScrollPaneGapClosingFeatures.getMouseWheelListeners();
				jScrollPaneGapClosingFeatures.removeMouseWheelListener(l[0]);
				this.add(jScrollPaneGapClosingFeatures, new GridBagConstraints(0, 12, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jScrollPaneGapClosingFeatures.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneGapClosingFeatures.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jPanelGapClosing = new JPanelFeatureSelectionGui();
				jScrollPaneGapClosingFeatures.setViewportView(jPanelGapClosing);
			}
			
			// Splitting
			
			{
				JLabel jLabel9 = new JLabel();
				this.add(jLabel9, new GridBagConstraints(0, 13, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 10, 0, 10), 0, 0));
				jLabel9.setText("Track segment splitting:");
				jLabel9.setFont(BIG_FONT.deriveFont(Font.BOLD));
			}
			{
				jCheckBoxAllowSplitting = new JCheckBox();
				this.add(jCheckBoxAllowSplitting, new GridBagConstraints(0, 14, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jCheckBoxAllowSplitting.setText("Allow track segment splitting");
				jCheckBoxAllowSplitting.setFont(SMALL_FONT);
				jCheckBoxAllowSplitting.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setEnabled(
								new Component[] {jLabel10, jTextFieldSplittingMaxDistance, jLabelSplittingMaxDistanceUnit, 
										jLabel11, jTextFieldSplittingMaxFrameInterval, jLabelSplittingMaxFrameIntervalUnit, jTextFieldSplittingMaxFrameInterval,
										jLabel15, jScrollPaneSplittingFeatures, jPanelSplittingFeatures}, 
								jCheckBoxAllowSplitting.isSelected());;
					}
				});
			}
			{
				jLabel10 = new JLabel();
				this.add(jLabel10, new GridBagConstraints(0, 15, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel10.setText("Max distance:");
				jLabel10.setFont(SMALL_FONT);
			}
			{
				jTextFieldSplittingMaxDistance = new JNumericTextField();
				this.add(jTextFieldSplittingMaxDistance, new GridBagConstraints(1, 15, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldSplittingMaxDistance.setSize(TEXTFIELD_DIMENSION);
				jTextFieldSplittingMaxDistance.setFont(SMALL_FONT);
			}
			{
				jLabelSplittingMaxDistanceUnit = new JLabel();
				this.add(jLabelSplittingMaxDistanceUnit, new GridBagConstraints(2, 15, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelSplittingMaxDistanceUnit.setFont(SMALL_FONT);
			}
			{
				jLabel11 = new JLabel();
				this.add(jLabel11, new GridBagConstraints(0, 16, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel11.setText("Max frame interval:");
				jLabel11.setFont(SMALL_FONT);
			}
			{
				jTextFieldSplittingMaxFrameInterval = new JNumericTextField();
				this.add(jTextFieldSplittingMaxFrameInterval, new GridBagConstraints(1, 16, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldSplittingMaxFrameInterval.setSize(TEXTFIELD_DIMENSION);
				jTextFieldSplittingMaxFrameInterval.setFont(SMALL_FONT);
			}
			{
				jLabelSplittingMaxFrameIntervalUnit = new JLabel();
				this.add(jLabelSplittingMaxFrameIntervalUnit, new GridBagConstraints(2, 16, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelSplittingMaxFrameIntervalUnit.setFont(SMALL_FONT);
			}
			{
				jLabel15 = new JLabel();
				this.add(jLabel15, new GridBagConstraints(0, 17, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel15.setText("Feature ratio thresholds:");
				jLabel15.setFont(SMALL_FONT);
			}
			{
				jScrollPaneSplittingFeatures = new JScrollPane();
				MouseWheelListener[] l = jScrollPaneSplittingFeatures.getMouseWheelListeners();
				jScrollPaneSplittingFeatures.removeMouseWheelListener(l[0]);
				this.add(jScrollPaneSplittingFeatures, new GridBagConstraints(0, 18, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jScrollPaneSplittingFeatures.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneSplittingFeatures.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jPanelSplittingFeatures = new JPanelFeatureSelectionGui();
				jScrollPaneSplittingFeatures.setViewportView(jPanelSplittingFeatures);
			}
			
			// Merging
			
			{
				JLabel jLabel12 = new JLabel();
				this.add(jLabel12, new GridBagConstraints(0, 19, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(20, 10, 0, 10), 0, 0));
				jLabel12.setText("Track segment merging:");
				jLabel12.setFont(BIG_FONT.deriveFont(Font.BOLD));
			}
			{
				jCheckBoxAllowMerging = new JCheckBox();
				this.add(jCheckBoxAllowMerging, new GridBagConstraints(0, 20, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jCheckBoxAllowMerging.setText("Allow track segment merging");
				jCheckBoxAllowMerging.setFont(SMALL_FONT);
				jCheckBoxAllowMerging.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setEnabled(
								new Component[] {jLabel13, jTextFieldMergingMaxDistance, jLabelMergingMaxDistanceUnit, 
										jLabel14, jTextFieldMergingFrameInterval, jLabelMergingMaxFrameIntervalUnit,
										jLabel16, jScrollPaneMergingFeatures, jPanelMergingFeatures}, 
										jCheckBoxAllowMerging.isSelected());
					}
				});
			}
			{
				jLabel13 = new JLabel();
				this.add(jLabel13, new GridBagConstraints(0, 21, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel13.setText("Max distance:");
				jLabel13.setFont(SMALL_FONT);
			}
			{
				jTextFieldMergingMaxDistance = new JNumericTextField();
				this.add(jTextFieldMergingMaxDistance, new GridBagConstraints(1, 21, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldMergingMaxDistance.setSize(TEXTFIELD_DIMENSION);
				jTextFieldMergingMaxDistance.setFont(SMALL_FONT);
			}
			{
				jLabelMergingMaxDistanceUnit = new JLabel();
				this.add(jLabelMergingMaxDistanceUnit, new GridBagConstraints(2, 21, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelMergingMaxDistanceUnit.setFont(SMALL_FONT);
			}
			{
				jLabel14 = new JLabel();
				this.add(jLabel14, new GridBagConstraints(0, 22, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 0), 0, 0));
				jLabel14.setText("Max frame interval:");
				jLabel14.setFont(SMALL_FONT);
			}
			{
				jTextFieldMergingFrameInterval = new JNumericTextField();
				this.add(jTextFieldMergingFrameInterval, new GridBagConstraints(1, 22, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldMergingFrameInterval.setSize(TEXTFIELD_DIMENSION);
				jTextFieldMergingFrameInterval.setFont(SMALL_FONT);
			}
			{
				jLabelMergingMaxFrameIntervalUnit = new JLabel();
				this.add(jLabelMergingMaxFrameIntervalUnit, new GridBagConstraints(2, 22, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0));
				jLabelMergingMaxFrameIntervalUnit.setFont(SMALL_FONT);
			}
			{
				jLabel16 = new JLabel();
				this.add(jLabel16, new GridBagConstraints(0, 23, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel16.setText("Feature ratio thresholds:");
				jLabel16.setFont(SMALL_FONT);
			}
			{
				jScrollPaneMergingFeatures = new JScrollPane();
				MouseWheelListener[] l = jScrollPaneMergingFeatures.getMouseWheelListeners();
				jScrollPaneMergingFeatures.removeMouseWheelListener(l[0]);
				this.add(jScrollPaneMergingFeatures, new GridBagConstraints(0, 24, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				jScrollPaneMergingFeatures.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				jScrollPaneMergingFeatures.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				jPanelMergingFeatures = new JPanelFeatureSelectionGui();
				jScrollPaneMergingFeatures.setViewportView(jPanelMergingFeatures);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


}
