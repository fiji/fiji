package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.providers.DetectorProvider;

public class DetectorConfigurationDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DetectorConfigurationPanel";
	private final TrackMate trackmate;
	private final DetectorProvider detectorProvider;
	private ConfigurationPanel configPanel;

	public DetectorConfigurationDescriptor(DetectorProvider detectorProvider, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.detectorProvider = detectorProvider;
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
		configPanel = detectorProvider.getDetectorConfigurationPanel();
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
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		Map<String, Object> settings = configPanel.getSettings();
		boolean settingsOk = detectorProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			Logger logger = trackmate.getModel().getLogger();
			logger.error("Config panel returned bad settings map:\n"+detectorProvider.getErrorMessage()+"Using defaults settings.\n");
			settings = detectorProvider.getDefaultSettings();
		}
		trackmate.getSettings().detectorSettings = settings;
	}

}
