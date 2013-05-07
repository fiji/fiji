package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.List;
import java.util.Map;

import fiji.plugin.trackmate.DetectorProvider;
import fiji.plugin.trackmate.TrackMate;

public class DetectorChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DetectorChoice";
	private ListChooserPanel component;
	private TrackMate trackmate;
	private TrackMateWizard wizard;

	/*
	 * METHODS
	 */

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return DetectorConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return StartDialogPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		setCurrentChoiceFromPlugin();
		wizard.setNextButtonEnabled(true);
	}
	
	private void setCurrentChoiceFromPlugin() {
		String key;
		if (null != trackmate.getSettings().detectorFactory) {
			key = trackmate.getSettings().detectorFactory.getKey();
		} else {
			key = trackmate.getDetectorProvider().getCurrentKey(); // back to default 
		}
		int index = trackmate.getDetectorProvider().getKeys().indexOf(key);
		if (index < 0) {
			wizard.getLogger().error("[DetectorChoiceDescriptor] Cannot find detector named "+key+" in current trackmate.");
			return;
		}
		component.jComboBoxChoice.setSelectedIndex(index);
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		
		// Configure the detector provider with choice made in panel
		DetectorProvider provider = trackmate.getDetectorProvider();
		int index = component.jComboBoxChoice.getSelectedIndex();
		String key = provider.getKeys().get(index);
		provider.select(key);
		
		trackmate.getSettings().detectorFactory = provider.getDetectorFactory();
		
		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		Map<String, Object> currentSettings = trackmate.getSettings().detectorSettings;
		if (!provider.checkSettingsValidity(currentSettings)) {
			Map<String, Object> defaultSettings = provider.getDefaultSettings();
			trackmate.getSettings().detectorSettings = defaultSettings;
		}
		// Instantiate next descriptor for the wizard
		DetectorConfigurationPanelDescriptor descriptor = new DetectorConfigurationPanelDescriptor();
		descriptor.setWizard(wizard);
		descriptor.setPlugin(trackmate);
		wizard.registerWizardDescriptor(DetectorConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate trackmate) {
		this.trackmate = trackmate;
		DetectorProvider provider = trackmate.getDetectorProvider();
		List<String> detectorNames =  provider.getNames();
		List<String> infoTexts = provider.getInfoTexts();
		this.component = new ListChooserPanel(detectorNames, infoTexts, "detector");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

}
