package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;

public class TrackerConfigurationPanelDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackerConfigurationPanel";
	private TrackMate_ plugin;
	private TrackerConfigurationPanel configPanel;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) { }

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
	}

	@Override
	public Component getPanelComponent() {
		return configPanel;
	}

	@Override
	public String getThisPanelID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextPanelID() {
		return TrackingDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousPanelID() {
		return TrackerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		TrackerSettings settings = plugin.getModel().getSettings().trackerSettings;
		// Bulletproof null
		if (null == settings) {
			SpotTracker tracker = plugin.getModel().getSettings().tracker;
			if (null == tracker) {
				// try to make it right with a default
				tracker = new NearestNeighborTracker();
				plugin.getModel().getSettings().tracker = tracker;
			}
			settings = tracker.createDefaultSettings();
		}
		configPanel = settings.createConfigurationPanel();
		configPanel.setTrackerSettings(plugin.getModel());
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().trackerSettings = configPanel.getTrackerSettings();
	}
}
