package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.List;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;

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
		
		// Configure the detector provider with choice made in panel
		TrackerProvider<T> provider = plugin.getTrackerProvider();
		int index = component.jComboBoxChoice.getSelectedIndex();
		String key = provider.getKeys().get(index);
		provider.select(key);

		// Set the settings field of the model
		plugin.getModel().getSettings().tracker = provider.getTracker();

		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		Map<String, Object> currentSettings = plugin.getModel().getSettings().detectorSettings;
		if (!provider.checkSettingsValidity(currentSettings)) {
			Map<String, Object> defaultSettings = provider.getDefaultSettings();
			plugin.getModel().getSettings().detectorSettings = defaultSettings;
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
		TrackerProvider<T> provider = plugin.getTrackerProvider();
		List<String> trackerNames = provider.getNames();
		List<String> infoTexts = provider.getInfoTexts();
		this.component = new ListChooserPanel(trackerNames, infoTexts, "tracker");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {	
		this.wizard = wizard;
	}

	void setCurrentChoiceFromPlugin() {
		String key;
		if (null != plugin.getModel().getSettings().tracker) {
			key = plugin.getModel().getSettings().tracker.getKey();
		} else {
			key = plugin.getTrackerProvider().getCurrentKey(); // back to default 
		}
		int index = plugin.getTrackerProvider().getKeys().indexOf(key);
		if (index < 0) {
			wizard.getLogger().error("[TrackerChoiceDescriptor] Cannot find tracker named "+key+" in current plugin.");
			return;
		}
		component.jComboBoxChoice.setSelectedIndex(index);
	}
	
}
