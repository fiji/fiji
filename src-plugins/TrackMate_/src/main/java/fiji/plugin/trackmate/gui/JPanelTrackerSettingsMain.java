package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_GAP_CLOSING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_MERGING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_ALLOW_TRACK_SPLITTING;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_MERGING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_FEATURE_PENALTIES;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.util.NumberParser;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class JPanelTrackerSettingsMain extends javax.swing.JPanel {

	private static final long serialVersionUID = -1L;

//	private JLabel jLabelTrackDescription;
	private JLabel jLabelSplittingMaxDistanceUnit;
	private JNumericTextField jTextFieldSplittingMaxDistance;
	private JCheckBox jCheckBoxAllowSplitting;
	private JLabel jLabelTrackerName;
	private JPanelFeatureSelectionGui jPanelGapClosing;
	private JPanelFeatureSelectionGui jPanelMergingFeatures;
	private JPanelFeatureSelectionGui jPanelLinkingFeatures;
	private JPanelFeatureSelectionGui jPanelSplittingFeatures;
	private JScrollPane jScrollPaneMergingFeatures;
	private JLabel jLabelMergingMaxDistanceUnit;
	private JNumericTextField jTextFieldMergingMaxDistance;
	private JCheckBox jCheckBoxAllowMerging;
	private JScrollPane jScrollPaneSplittingFeatures;
	private JScrollPane jScrollPaneGapClosingFeatures;
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
	private JLabel jLabel15;
	private JLabel jLabel13;
	private JLabel jLabel16;

	private final String trackerName;
	private final List<String> features;
	private final Map<String, String> featureNames;
	private final String spaceUnits;


	public JPanelTrackerSettingsMain(final String trackerName, final String spaceUnits, final List<String> features, final Map<String, String> featureNames) {
		this.trackerName = trackerName;
		this.spaceUnits = spaceUnits;
		this.features = features;
		this.featureNames = featureNames;
		initGUI();
	}

	/*
	 * PUBLIC METHODS
	 */


	@SuppressWarnings("unchecked")
	void echoSettings(final Map<String, Object> settings) {

		jTextFieldLinkingMaxDistance.setText(String.format("%.1f", (Double) settings.get(KEY_LINKING_MAX_DISTANCE)));
		jPanelLinkingFeatures.setSelectedFeaturePenalties((Map<String, Double>) settings.get(KEY_LINKING_FEATURE_PENALTIES));

		jCheckBoxAllowGapClosing.setSelected((Boolean) settings.get(KEY_ALLOW_GAP_CLOSING));
		jTextFieldGapClosingMaxDistance.setText(String.format("%.1f", (Double) settings.get(KEY_GAP_CLOSING_MAX_DISTANCE)));
		jTextFieldGapClosingMaxFrameInterval.setText(String.format("%d", (Integer) settings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP)));
		jPanelGapClosing.setSelectedFeaturePenalties((Map<String, Double>) settings.get(KEY_GAP_CLOSING_FEATURE_PENALTIES));

		jCheckBoxAllowSplitting.setSelected((Boolean) settings.get(KEY_ALLOW_TRACK_SPLITTING));
		jTextFieldSplittingMaxDistance.setText(String.format("%.1f", (Double) settings.get(KEY_SPLITTING_MAX_DISTANCE)));
		jPanelSplittingFeatures.setSelectedFeaturePenalties((Map<String, Double>) settings.get(KEY_SPLITTING_FEATURE_PENALTIES));

		jCheckBoxAllowMerging.setSelected((Boolean) settings.get(KEY_ALLOW_TRACK_MERGING));
		jTextFieldMergingMaxDistance.setText(String.format("%.1f", (Double) settings.get(KEY_SPLITTING_MAX_DISTANCE)));
		jPanelMergingFeatures.setSelectedFeaturePenalties((Map<String, Double>) settings.get(KEY_MERGING_FEATURE_PENALTIES));

		setEnabled(
				new Component[] {jLabel6, jTextFieldGapClosingMaxDistance, jLabelGapClosingMaxDistanceUnit, 
						jLabel7, jTextFieldGapClosingMaxFrameInterval, jTextFieldGapClosingMaxFrameInterval,
						jLabel8, jScrollPaneGapClosingFeatures, jPanelGapClosing}, 
						jCheckBoxAllowGapClosing.isSelected());

		setEnabled(
				new Component[] {jLabel10, jTextFieldSplittingMaxDistance, jLabelSplittingMaxDistanceUnit, 
						jLabel15, jScrollPaneSplittingFeatures, jPanelSplittingFeatures}, 
						jCheckBoxAllowSplitting.isSelected());

		setEnabled(
				new Component[] {jLabel13, jTextFieldMergingMaxDistance, jLabelMergingMaxDistanceUnit, 
						jLabel16, jScrollPaneMergingFeatures, jPanelMergingFeatures}, 
						jCheckBoxAllowMerging.isSelected());
	}

	/**
	 * @return a new settings {@link Map} with values taken from this panel.
	 */
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = LAPUtils.getDefaultLAPSettingsMap();

		settings.put(KEY_LINKING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldLinkingMaxDistance.getText()));
		settings.put(KEY_LINKING_FEATURE_PENALTIES, jPanelLinkingFeatures.getFeaturePenalties());

		settings.put(KEY_ALLOW_GAP_CLOSING, jCheckBoxAllowGapClosing.isSelected());
		settings.put(KEY_GAP_CLOSING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldGapClosingMaxDistance.getText()));
		settings.put(KEY_GAP_CLOSING_MAX_FRAME_GAP, NumberParser.parseInteger(jTextFieldGapClosingMaxFrameInterval.getText()));
		settings.put(KEY_GAP_CLOSING_FEATURE_PENALTIES, jPanelGapClosing.getFeaturePenalties());

		settings.put(KEY_ALLOW_TRACK_SPLITTING, jCheckBoxAllowSplitting.isSelected());
		settings.put(KEY_SPLITTING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldSplittingMaxDistance.getText()));
		settings.put(KEY_SPLITTING_FEATURE_PENALTIES, jPanelSplittingFeatures.getFeaturePenalties());

		settings.put(KEY_ALLOW_TRACK_MERGING, jCheckBoxAllowMerging.isSelected());
		settings.put(KEY_MERGING_MAX_DISTANCE, NumberParser.parseDouble(jTextFieldMergingMaxDistance.getText()));
		settings.put(KEY_MERGING_FEATURE_PENALTIES, jPanelMergingFeatures.getFeaturePenalties());

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
//			thisLayout.rowHeights = new int[] {15, 20, 60, 15, 10, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95};
			thisLayout.rowHeights = new int[] {15, 20, 0, 15, 10, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95, 15, 15, 15, 15, 15, 95};
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
				jLabelTrackerName.setText(trackerName);
			}
//			{
//				jLabelTrackDescription = new JLabel();
//				this.add(jLabelTrackDescription, new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 10), 0, 0));
//				jLabelTrackDescription.setFont(SMALL_FONT.deriveFont(Font.ITALIC));
//			}
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
				jLabelLinkingMaxDistanceUnits.setText(spaceUnits);
			}
			{
				JLabel jLabel4 = new JLabel();
				this.add(jLabel4, new GridBagConstraints(0, 5, 3, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel4.setText("Feature penalties");
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
				jPanelLinkingFeatures.setDisplayFeatures(features, featureNames);
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
										jLabel7, jTextFieldGapClosingMaxFrameInterval, jTextFieldGapClosingMaxFrameInterval, 
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
				jLabelGapClosingMaxDistanceUnit.setText(spaceUnits);
			}
			{
				jLabel7 = new JLabel();
				this.add(jLabel7, new GridBagConstraints(0, 10, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel7.setText("Max frame gap:");
				jLabel7.setFont(SMALL_FONT);
			}
			{
				jTextFieldGapClosingMaxFrameInterval = new JNumericTextField();
				this.add(jTextFieldGapClosingMaxFrameInterval, new GridBagConstraints(1, 10, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
				jTextFieldGapClosingMaxFrameInterval.setSize(TEXTFIELD_DIMENSION);
				jTextFieldGapClosingMaxFrameInterval.setFont(SMALL_FONT);
			}
			{
				jLabel8 = new JLabel();
				this.add(jLabel8, new GridBagConstraints(0, 11, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel8.setText("Feature penalties:");
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
				jPanelGapClosing.setDisplayFeatures(features, featureNames);
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
				jLabelSplittingMaxDistanceUnit.setText(spaceUnits);
			}
			{
				jLabel15 = new JLabel();
				this.add(jLabel15, new GridBagConstraints(0, 17, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel15.setText("Feature penalties:");
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
				jPanelSplittingFeatures.setDisplayFeatures(features, featureNames);
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
				jLabelMergingMaxDistanceUnit.setText(spaceUnits);
			}
			{
				jLabel16 = new JLabel();
				this.add(jLabel16, new GridBagConstraints(0, 23, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
				jLabel16.setText("Feature penalties:");
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
				jPanelMergingFeatures.setDisplayFeatures(features, featureNames);
				jScrollPaneMergingFeatures.setViewportView(jPanelMergingFeatures);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}


}
