package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;

public class TrackingDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackingPanel";
	private LogPanel logPanel;
	private TrackMate_ plugin;
	private TrackMateWizard wizard;
	private Logger logger;
	

	@Override
	public void setWizard(TrackMateWizard wizard) { 
		this.wizard = wizard;
		this.logPanel = wizard.getLogPanel();
		this.logger = wizard.getLogger();
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
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
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(false);
		final TrackMateModel model = plugin.getModel();
		logger.log("Starting tracking using "+model.getSettings().tracker.toString()+"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(model.getSettings().trackerSettings.toString());
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
