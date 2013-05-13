package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;

public class TrackerConfigurationPanelDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackerConfigurationPanel";
	private TrackMate_ plugin;
	private ConfigurationPanel configPanel;
	private TrackMateWizard wizard;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) { 
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
	}

	/**
	 * Regenerate the config panel to reflect current settings stored in the plugin.
	 */
	public void updateComponent() {
		// We assume the provider is already configured with the right target detector factory
		TrackerProvider provider = plugin.getTrackerProvider();
		// Regenerate panel
		configPanel = provider.getTrackerConfigurationPanel();
		Map<String, Object> settings = plugin.getModel().getSettings().trackerSettings;
		// Bulletproof null
		if (null == settings || !provider.checkSettingsValidity(settings)) {
			settings = provider.getDefaultSettings();
		}
		configPanel.setSettings(settings);
	}
	
	@Override
	public Component getComponent() {
		return configPanel;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}
	
	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return TrackingDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return TrackerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		updateComponent();
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		Map<String, Object> settings = configPanel.getSettings();
		TrackerProvider trackerProvider = plugin.getTrackerProvider();
		boolean settingsOk = trackerProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			Logger logger = wizard.getLogger();
			logger.error("Config panel returned bad settings map:\n"+trackerProvider.getErrorMessage()+"Using defaults settings.\n");
			settings = trackerProvider.getDefaultSettings();
		}
		plugin.getModel().getSettings().trackerSettings = settings;
	}
}
