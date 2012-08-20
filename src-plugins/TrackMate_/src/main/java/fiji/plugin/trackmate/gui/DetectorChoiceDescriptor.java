package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.DetectorSettings;

public class DetectorChoiceDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "DetectorChoice";
	private ListChooserPanel component;
	private TrackMate_<T> plugin;
	private TrackMateWizard<T> wizard;

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
		String detector = plugin.getModel().getSettings().detector; 
		if (detector != null) {
			int index = plugin.getDetectorFactory().getAvailableDetectors().indexOf(detector);
			if (index < 0) {
				wizard.getLogger().error("[DetectorChoiceDescriptor] Cannot find detector named "+detector+" in current plugin.");
				return;
			}
			component.jComboBoxChoice.setSelectedIndex(index);
		}
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		// Set the settings field of the model
		String detector = component.getChoice();
		plugin.getModel().getSettings().detector = detector;
		
		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		DetectorSettings<T> defaultSettings = plugin.getDetectorFactory().getDefaultSettings(detector);
		DetectorSettings<T> currentSettings = plugin.getModel().getSettings().detectorSettings;
		if (null == currentSettings || currentSettings.getClass() != defaultSettings.getClass()) {
			plugin.getModel().getSettings().detectorSettings = defaultSettings;
		}

		// Instantiate next descriptor for the wizard
		DetectorConfigurationPanelDescriptor<T> descriptor = new DetectorConfigurationPanelDescriptor<T>();
		descriptor.setWizard(wizard);
		descriptor.setPlugin(plugin);
		wizard.registerWizardDescriptor(DetectorConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		List<String> detectorNames = plugin.getDetectorFactory().getAvailableDetectors();
		List<String> infoTexts = new ArrayList<String>(detectorNames.size());
		for(String key : detectorNames) {
			infoTexts.add(plugin.getDetectorFactory().getInfoText(key));
		}
		this.component = new ListChooserPanel(detectorNames, infoTexts, "detector");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
	}

}
