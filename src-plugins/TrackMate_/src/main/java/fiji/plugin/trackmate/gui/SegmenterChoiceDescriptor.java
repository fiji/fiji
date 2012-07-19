package fiji.plugin.trackmate.gui;

import java.awt.Component;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.SegmenterSettings;
import fiji.plugin.trackmate.detection.SpotSegmenter;

public class SegmenterChoiceDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "SegmenterChoice";
	private ListChooserPanel<SpotSegmenter<T>> component;
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
		return SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
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
		SpotSegmenter<? extends RealType<?>> segmenter = plugin.getModel().getSettings().segmenter; 
		if (segmenter != null) {
			int index = 0;
			for (int i = 0; i < plugin.getAvailableSpotSegmenters().size(); i++) {
				if (segmenter.toString().equals(plugin.getAvailableSpotSegmenters().get(i).toString())) {
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
		SpotSegmenter<T> segmenter = component.getChoice();
		plugin.getModel().getSettings().segmenter = segmenter;
		
		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		SegmenterSettings<T> defaultSettings = segmenter.createDefaultSettings();
		SegmenterSettings<T> currentSettings = plugin.getModel().getSettings().segmenterSettings;
		if (null == currentSettings || currentSettings.getClass() != defaultSettings.getClass()) {
			plugin.getModel().getSettings().segmenterSettings = defaultSettings;
		}

		// Instantiate next descriptor for the wizard
		SegmenterConfigurationPanelDescriptor<T> descriptor = new SegmenterConfigurationPanelDescriptor<T>();
		descriptor.setWizard(wizard);
		descriptor.setPlugin(plugin);
		wizard.registerWizardDescriptor(SegmenterConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		this.component = new ListChooserPanel<SpotSegmenter<T>>(plugin.getAvailableSpotSegmenters(), "segmenter");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {
		this.wizard = wizard;
	}

}
