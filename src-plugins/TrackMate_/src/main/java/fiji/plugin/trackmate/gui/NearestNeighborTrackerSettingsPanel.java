package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.TEXTFIELD_DIMENSION;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_LINKING_MAX_DISTANCE;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class NearestNeighborTrackerSettingsPanel extends ConfigurationPanel {

	private static final long serialVersionUID = 1L;
	private JNumericTextField maxDistField;
	private JLabel labelTrackerDescription;
	private JLabel labelUnits;
	private JLabel labelTracker;
	private final String infoText;
	private final String trackerName;
	private final String spaceUnits;


	public NearestNeighborTrackerSettingsPanel(String trackerName, String infoText, String spaceUnits) {
		this.trackerName = trackerName;
		this.infoText = infoText;
		this.spaceUnits = spaceUnits;
		initGUI();
	}

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();
		settings.put(KEY_LINKING_MAX_DISTANCE, maxDistField.getValue());
		return settings;
	}

	@Override
	public void setSettings(final Map<String, Object> settings) {
		maxDistField.setText(""+settings.get(KEY_LINKING_MAX_DISTANCE));
	}


	private void initGUI() {

		setLayout(null);

		JLabel lblSettingsForTracker = new JLabel("Settings for tracker:");
		lblSettingsForTracker.setBounds(10, 11, 280, 20);
		lblSettingsForTracker.setFont(FONT);
		add(lblSettingsForTracker);


		labelTracker = new JLabel(trackerName);
		labelTracker.setFont(BIG_FONT);
		labelTracker.setHorizontalAlignment(SwingConstants.CENTER);
		labelTracker.setBounds(10, 42, 280, 20);
		add(labelTracker);

		labelTrackerDescription = new JLabel("<tracker description>");
		labelTrackerDescription.setFont(FONT.deriveFont(Font.ITALIC));
		labelTrackerDescription.setBounds(10, 67, 280, 225);
		labelTrackerDescription.setText(infoText
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">"));
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

		labelUnits = new JLabel(spaceUnits);
		labelUnits.setFont(FONT);
		labelUnits.setBounds(236, 314, 34, 20);
		add(labelUnits);
	}
}
