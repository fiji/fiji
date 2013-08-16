package fiji.plugin.trackmate.gui.descriptors;

import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TrackerConfigurationDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ConfigureTracker";
	private final TrackMate trackmate;
	private ConfigurationPanel configPanel;
	private final TrackerProvider trackerProvider;
	private final TrackMateGUIController controller;

	public TrackerConfigurationDescriptor(final TrackerProvider trackerProvider, final TrackMate trackmate, final TrackMateGUIController controller) {
		this.trackmate = trackmate;
		this.trackerProvider = trackerProvider;
		this.controller = controller;
	}


	/*
	 * METHODS
	 */

	/**
	 * Regenerate the config panel to reflect current settings stored in the trackmate.
	 */
	private void updateComponent() {
		// Regenerate panel
		configPanel = trackerProvider.getTrackerConfigurationPanel();
		Map<String, Object> settings = trackmate.getSettings().trackerSettings;
		// Bulletproof null
		if (null == settings || !trackerProvider.checkSettingsValidity(settings)) {
			settings = trackerProvider.getDefaultSettings();
		}
		configPanel.setSettings(settings);
	}

	@Override
	public ConfigurationPanel getComponent() {
		return configPanel;
	}

	@Override
	public void aboutToDisplayPanel() {
		updateComponent();
	}

	@Override
	public void displayingPanel() {
		if (null == configPanel) {
			// happens after loading.
			aboutToDisplayPanel();
		}
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		Map<String, Object> settings = configPanel.getSettings();
		final boolean settingsOk = trackerProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			final Logger logger = trackmate.getModel().getLogger();
			logger.error("Config panel returned bad settings map:\n"+trackerProvider.getErrorMessage()+"Using defaults settings.\n");
			settings = trackerProvider.getDefaultSettings();
		}
		trackmate.getSettings().trackerSettings = settings;
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
