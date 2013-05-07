package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackerProvider;

public class TrackingDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackingPanel";
	private LogPanel logPanel;
	private TrackMate trackmate;
	private TrackMateWizard wizard;
	private Logger logger;
	

	@Override
	public void setWizard(TrackMateWizard wizard) { 
		this.wizard = wizard;
		this.logPanel = wizard.getLogPanel();
		this.logger = wizard.getLogger();
	}

	@Override
	public void setPlugin(TrackMate trackmate) {
		this.trackmate = trackmate;
	}

	@Override
	public Component getComponent() {
		return logPanel;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}
	
	@Override
	public String getComponentID() {
		return LogPanel.DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return TrackFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return TrackerConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	
		TrackerProvider provider = trackmate.getTrackerProvider();
		// Set the settings field of the model. We instantiate the tracker only now
		// that the model has a proper settings map.
		trackmate.getSettings().tracker = provider.getTracker();

		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		Map<String, Object> currentSettings = trackmate.getSettings().trackerSettings;
		if (!provider.checkSettingsValidity(currentSettings)) {
			Map<String, Object> defaultSettings = provider.getDefaultSettings();
			trackmate.getSettings().trackerSettings = defaultSettings;
		}
		
	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(false);
		final TrackMateModel model = trackmate.getModel();
		final TrackerProvider provider = trackmate.getTrackerProvider();
		logger.log("Starting tracking using " + trackmate.getSettings().tracker +"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(provider.toString(trackmate.getSettings().trackerSettings));
		new Thread("TrackMate tracking thread") {					
			public void run() {
				try {
					long start = System.currentTimeMillis();
					trackmate.execTracking();
					// Re-enable the GUI
					long end = System.currentTimeMillis();
					logger.log(String.format("Tracking done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				} finally {
					wizard.setNextButtonEnabled(true);
				}
			}
		}.start();
	}

	@Override
	public void aboutToHidePanel() { 
		Thread trackFeatureCalculationThread = new Thread("TrackMate track feature calculation thread") {
			@Override
			public void run() {
				trackmate.computeTrackFeatures(true);
			}
		};
		trackFeatureCalculationThread.start();
		try {
			trackFeatureCalculationThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}
