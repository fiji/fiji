package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.SpotTracker;

public class TrackerChoiceDescriptor implements WizardPanelDescriptor {

	public static final Object DESCRIPTOR = "TrackerChoice";
	private ListChooserPanel<SpotTracker> component;
	private TrackMate_ plugin;
	
	/*
	 * METHODS
	 */
	
	@Override
	public Component getPanelComponent() {
		return component;
	}

	@Override
	public Object getPanelDescriptorIdentifier() {
		return DESCRIPTOR;
	}

	@Override
	public Object getNextPanelDescriptor() {
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public Object getBackPanelDescriptor() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().tracker = component.getChoice();
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		this.component = new ListChooserPanel<SpotTracker>(plugin.getAvailableSpotTrackers(), "tracker");
	}

	@Override
	public void setWizard(TrackMateWizard wizard) {	}

}
