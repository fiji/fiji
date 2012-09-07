package fiji.plugin.trackmate.gui;

import java.awt.Component;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.tracking.SpotTracker;
import fiji.plugin.trackmate.tracking.TrackerSettings;
import fiji.plugin.trackmate.tracking.kdtree.NearestNeighborTracker;

public class TrackerConfigurationPanelDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "TrackerConfigurationPanel";
	private TrackMate_<T> plugin;
	private TrackerConfigurationPanel<T> configPanel;
	private TrackMateWizard<T> wizard;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard<T> wizard) { 
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		String trackerName = plugin.getModel().getSettings().tracker;
		TrackerSettings<T> settings = plugin.getModel().getSettings().trackerSettings;
		// Bulletproof null
		if (null == settings) {
			SpotTracker<T> tracker = plugin.getTrackerProvider().getTracker(trackerName);
			if (null == tracker) {
				// try to make it right with a default
				trackerName = NearestNeighborTracker.NAME;
				plugin.getModel().getSettings().tracker = trackerName;
			}
			settings = plugin.getTrackerProvider().getDefaultSettings(trackerName);
		}
		configPanel = plugin.getTrackerProvider().getTrackerConfigurationPanel(trackerName);
		configPanel.setTrackerSettings(plugin.getModel());
	}

	@Override
	public Component getComponent() {
		return configPanel;
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
		return TrackingDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return TrackerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		configPanel.setTrackerSettings(plugin.getModel());
	}

	@Override
	public void displayingPanel() {	
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().trackerSettings = configPanel.getTrackerSettings();
	}
}
