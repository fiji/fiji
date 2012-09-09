package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.Map;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.TrackerProvider;

public class TrackingDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "TrackingPanel";
	private LogPanel logPanel;
	private TrackMate_<T> plugin;
	private TrackMateWizard<T> wizard;
	private Logger logger;
	

	@Override
	public void setWizard(TrackMateWizard<T> wizard) { 
		this.wizard = wizard;
		this.logPanel = wizard.getLogPanel();
		this.logger = wizard.getLogger();
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
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
		TrackerProvider<T> provider = plugin.getTrackerProvider();
		// Set the settings field of the model. We instantiate the tracker only now
		// that the model has a proper settings map.
		plugin.getModel().getSettings().tracker = provider.getTracker();

		// Compare current settings with default ones, and substitute default ones
		// only if the old ones are absent or not compatible with it.
		Map<String, Object> currentSettings = plugin.getModel().getSettings().trackerSettings;
		if (!provider.checkSettingsValidity(currentSettings)) {
			Map<String, Object> defaultSettings = provider.getDefaultSettings();
			plugin.getModel().getSettings().trackerSettings = defaultSettings;
		}
		
	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(false);
		final TrackMateModel<T> model = plugin.getModel();
		final TrackerProvider<T> provider = plugin.getTrackerProvider();
		logger.log("Starting tracking using " + model.getSettings().tracker +"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(provider.toString(model.getSettings().trackerSettings));
		new Thread("TrackMate tracking thread") {					
			public void run() {
				try {
					long start = System.currentTimeMillis();
					plugin.execTracking();
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
	public void aboutToHidePanel() { }
}
