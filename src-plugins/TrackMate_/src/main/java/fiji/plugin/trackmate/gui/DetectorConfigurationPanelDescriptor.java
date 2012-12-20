package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;

public class DetectorConfigurationPanelDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DetectorConfigurationPanel";
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
		if (plugin.getModel().getSettings().detectorFactory.getKey().equals(ManualDetectorFactory.DETECTOR_KEY)) {
			return DisplayerChoiceDescriptor.DESCRIPTOR;
		} else {
			return DetectorDescriptor.DESCRIPTOR;
		}
	}

	@Override
	public String getPreviousDescriptorID() {
		return DetectorChoiceDescriptor.DESCRIPTOR;
	}

	/**
	 * Regenerate the config panel to reflect current settings stored in the plugin.
	 */
	public void updateComponent() {
		// Regenerate panel
		configPanel = plugin.getDetectorProvider().getDetectorConfigurationPanel(wizard.getController());
		// We assume the provider is already configured with the right target detector factory
		DetectorProvider provider = plugin.getDetectorProvider(); 
		Map<String, Object> settings = plugin.getModel().getSettings().detectorSettings;
		// Bulletproof null
		if (null == settings || !provider.checkSettingsValidity(settings)) {
			settings = provider.getDefaultSettings();
		}
		configPanel.setSettings(settings);
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
		DetectorProvider detectorProvider = plugin.getDetectorProvider();
		boolean settingsOk = detectorProvider.checkSettingsValidity(settings);
		if (!settingsOk) {
			Logger logger = wizard.getLogger();
			logger.error("Config panel returned bad settings map:\n"+detectorProvider.getErrorMessage()+"Using defaults settings.\n");
			settings = detectorProvider.getDefaultSettings();
		}
		plugin.getModel().getSettings().detectorSettings = settings;
	}

}
