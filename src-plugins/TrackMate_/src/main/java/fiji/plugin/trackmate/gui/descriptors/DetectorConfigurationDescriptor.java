package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class DetectorConfigurationDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ConfigureDetector";
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;
	private ConfigurationPanel configPanel;
	private final TrackMateGUIController controller;

	public DetectorConfigurationDescriptor(final DetectorProvider detectorProvider, final TrackMate trackmate, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
		this.controller = controller;
	}


	/*
	 * METHODS
	 */

	@Override
	public Component getComponent() {
		return configPanel;
	}

	/**
	 * Regenerate the config panel to reflect current settings stored in the trackmate.
	 */
	private void updateComponent() {
		// Regenerate panel
		configPanel = detectorProvider.getDetectorConfigurationPanel(trackmate.getSettings());
		// We assume the provider is already configured with the right target detector factory
		Map<String, Object> settings = trackmate.getSettings().detectorSettings;
		// Bulletproof null
		if (null == settings || !detectorProvider.checkSettingsValidity(settings)) {
			settings = detectorProvider.getDefaultSettings();
		}
		configPanel.setSettings(settings);
	}

	@Override
	public void aboutToDisplayPanel() {
		updateComponent();
	}

	@Override
	public void displayingPanel() {
		if (null == configPanel) {
			// May happen if we move backward here after loading
			updateComponent();
		}
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		Map<String, Object> settings = configPanel.getSettings();
		final boolean settingsOk = detectorProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			final Logger logger = trackmate.getModel().getLogger();
			logger.error("Config panel returned bad settings map:\n"+detectorProvider.getErrorMessage()+"Using defaults settings.\n");
			settings = detectorProvider.getDefaultSettings();
		}
		trackmate.getSettings().detectorSettings = settings;
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
