package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class TrackerChoiceDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "TrackerChoice";
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
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void displayingPanel() { 
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		// Set the settings field of the model
		String trackerName = component.getChoice();
		plugin.getModel().getSettings().tracker = trackerName;

		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		TrackerSettings<T> defaultSettings = plugin.getTrackerFactory().getDefaultSettings(trackerName);
		TrackerSettings<T> currentSettings = plugin.getModel().getSettings().trackerSettings;
		
		if (null == currentSettings || currentSettings.getClass() != defaultSettings.getClass()) {
			plugin.getModel().getSettings().trackerSettings = defaultSettings;
		} 
		 
		// Instantiate next descriptor for the wizard
		TrackerConfigurationPanelDescriptor<T> descriptor = new TrackerConfigurationPanelDescriptor<T>();
		descriptor.setPlugin(plugin);
		descriptor.setWizard(wizard);
		wizard.registerWizardDescriptor(TrackerConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		List<String> trackerNames = plugin.getTrackerFactory().getTrackerKeys();
		List<String> infoTexts = new ArrayList<String>(trackerNames.size());
		for(String key : trackerNames) {
			infoTexts.add(plugin.getTrackerFactory().getInfoText(key));
		}
		this.component = new ListChooserPanel(trackerNames, infoTexts, "tracker");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {	
		this.wizard = wizard;
	}

	void setCurrentChoiceFromPlugin() {
		String tracker = plugin.getModel().getSettings().tracker; 
		int index = plugin.getTrackerFactory().getTrackerKeys().indexOf(tracker);
		if (index >= 0) {
			component.jComboBoxChoice.setSelectedIndex(index);
		}
	}
	
}
