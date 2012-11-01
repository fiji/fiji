package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTrackerSettings;

public class NearestNeighborTrackerSettingsPanel extends TrackerConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private JNumericTextField maxDistField;
	private JLabel labelTrackerDescription;
	private JLabel labelUnits;
	private JLabel labelTracker;


	public NearestNeighborTrackerSettingsPanel() {
		initGUI();
	}

	@Override
	public TrackerSettings getTrackerSettings() {
		NearestNeighborTrackerSettings settings = new NearestNeighborTrackerSettings();
		settings.maxLinkingDistance = maxDistField.getValue();
		return settings;
	}

	@Override
	public void setTrackerSettings(TrackMateModel model) {
		NearestNeighborTrackerSettings settings = (NearestNeighborTrackerSettings) model.getSettings().trackerSettings;
		maxDistField.setText(""+settings.maxLinkingDistance);
		labelTracker.setText(model.getSettings().tracker.toString());
		labelTrackerDescription.setText(model.getSettings().tracker.getInfoText()
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">"));
		labelUnits.setText(model.getSettings().spaceUnits);
	}


	private void initGUI() {

		setLayout(null);

		JLabel lblSettingsForTracker = new JLabel("Settings for tracker:");
		lblSettingsForTracker.setBounds(10, 11, 280, 20);
		lblSettingsForTracker.setFont(FONT);
		add(lblSettingsForTracker);


		labelTracker = new JLabel("<tracker name>");
		labelTracker.setFont(BIG_FONT);
		labelTracker.setHorizontalAlignment(SwingConstants.CENTER);
		labelTracker.setBounds(10, 42, 280, 20);
		add(labelTracker);

		labelTrackerDescription = new JLabel("<tracker description>");
		labelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
		labelTrackerDescription.setBounds(10, 67, 280, 175);
		add(labelTrackerDescription);

		JLabel lblMaximalLinkingDistance = new JLabel("Maximal linking distance: ");
		lblMaximalLinkingDistance.setFont(FONT);
		lblMaximalLinkingDistance.setBounds(10, 314, 164, 20);
		add(lblMaximalLinkingDistance);

		maxDistField = new JNumericTextField();
		maxDistField.setFont(FONT);
		maxDistField.setBounds(184, 316, 62, 16);
		maxDistField.setSize(TEXTFIELD_DIMENSION);
		add(maxDistField);

		labelUnits = new JLabel("<unit>");
		labelUnits.setFont(FONT);
		labelUnits.setBounds(236, 314, 34, 20);
		add(labelUnits);
	}
}
