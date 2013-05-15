package fiji.plugin.trackmate.gui.descriptors;

import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TrackerConfigurationDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ConfigureTracker";
	private final TrackMate trackmate;
	private ConfigurationPanel configPanel;
	private final TrackerProvider trackerProvider;
	
	public TrackerConfigurationDescriptor(TrackerProvider trackerProvider, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.trackerProvider = trackerProvider;
	}
	
	
	/*
	 * METHODS
	 */

	/**
	 * Regenerate the config panel to reflect current settings stored in the trackmate.
	 */
	private void updateComponent() {
		// Regenerate panel
		configPanel = trackerProvider.getTrackerConfigurationPanel(trackmate.getSettings());
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
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		Map<String, Object> settings = configPanel.getSettings();
		boolean settingsOk = trackerProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			Logger logger = trackmate.getModel().getLogger();
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
