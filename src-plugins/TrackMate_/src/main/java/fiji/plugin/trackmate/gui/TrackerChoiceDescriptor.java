package fiji.plugin.trackmate.gui;

import java.awt.Component;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.LAPTrackerSettings;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class TrackerChoiceDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "TrackerChoice";
	private ListChooserPanel<SpotTracker<T>> component;
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
		SpotTracker<T> tracker = component.getChoice();
		plugin.getModel().getSettings().tracker = tracker;

		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		TrackerSettings<T> defaultSettings = tracker.createDefaultSettings();
		TrackerSettings<T> currentSettings = plugin.getModel().getSettings().trackerSettings;
		
		if (null == currentSettings || currentSettings.getClass() != defaultSettings.getClass()) {
		
			plugin.getModel().getSettings().trackerSettings = defaultSettings;
		
		} else if (currentSettings instanceof LAPTrackerSettings) {

			// Deal with special case: the LAPTrackerSettings that exists in 2 flavor
			LAPTrackerSettings<T> clapts = (LAPTrackerSettings<T>) currentSettings;
			LAPTrackerSettings<T> dlapts = (LAPTrackerSettings<T>) defaultSettings;
			// We copy the #useSimpleConfigPanel field to the current settings 
			clapts.setUseSimpleConfigPanel(dlapts.isUseSimpleConfigPanel());
		
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
		this.component = new ListChooserPanel<SpotTracker<T>>(plugin.getAvailableSpotTrackers(), "tracker");
		setCurrentChoiceFromPlugin();
	}

	@Override
	public void setWizard(TrackMateWizard<T> wizard) {	
		this.wizard = wizard;
	}

	void setCurrentChoiceFromPlugin() {
		SpotTracker<T> tracker = plugin.getModel().getSettings().tracker; 
		if (tracker != null) {
			int index = 0;
			for (int i = 0; i < plugin.getAvailableSpotTrackers().size(); i++) {
				if (tracker.toString().equals(plugin.getAvailableSpotTrackers().get(i).toString())) {
					index = i;
					break;
				}
			}
			component.jComboBoxChoice.setSelectedIndex(index);
		}
	}

	
}
