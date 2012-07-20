package fiji.plugin.trackmate.gui;

import java.awt.Component;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.DetectorSettings;
import fiji.plugin.trackmate.detection.SpotDetector;

public class DetectorChoiceDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "SegmenterChoice";
	private ListChooserPanel<SpotDetector<T>> component;
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
		SpotDetector<? extends RealType<?>> segmenter = plugin.getModel().getSettings().detector; 
		if (segmenter != null) {
			int index = 0;
			for (int i = 0; i < plugin.getAvailableSpotDetectors().size(); i++) {
				if (segmenter.toString().equals(plugin.getAvailableSpotDetectors().get(i).toString())) {
					index = i;
					break;
				}
			}
			component.jComboBoxChoice.setSelectedIndex(index);
		}
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		// Set the settings field of the model
		SpotDetector<T> segmenter = component.getChoice();
		plugin.getModel().getSettings().detector = segmenter;
		
		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		DetectorSettings<T> defaultSettings = segmenter.createDefaultSettings();
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
		this.component = new ListChooserPanel<SpotDetector<T>>(plugin.getAvailableSpotDetectors(), "segmenter");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
	}

}
