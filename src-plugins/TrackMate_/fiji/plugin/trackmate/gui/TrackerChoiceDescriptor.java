package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackerChoice";
	private ListChooserPanel<SpotTracker> component;
	private TrackMate_ plugin;
	private TrackMateWizard wizard;
	
	/*
	 * METHODS
	 */
	
	@Override
	public Component getPanelComponent() {
		return component;
	}

	@Override
	public String getThisPanelID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextPanelID() {
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousPanelID() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		// Set the settings field of the model
		SpotTracker tracker = component.getChoice();
		plugin.getModel().getSettings().tracker = tracker;
		plugin.getModel().getSettings().trackerSettings = tracker.createDefaultSettings();

		// Instantiate next descriptor for the wizard
		TrackerConfigurationPanelDescriptor descriptor = new TrackerConfigurationPanelDescriptor();
		descriptor.setWizard(wizard);
		descriptor.setPlugin(plugin);
		wizard.registerWizardDescriptor(TrackerConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		this.component = new ListChooserPanel<SpotTracker>(plugin.getAvailableSpotTrackers(), "tracker");
	}

	@Override
	public void setWizard(TrackMateWizard wizard) {	
		this.wizard = wizard;
	}

}
