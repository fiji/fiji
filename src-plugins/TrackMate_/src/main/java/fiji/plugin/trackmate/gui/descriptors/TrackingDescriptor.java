package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TrackingDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "Tracking";
	private final LogPanel logPanel;
	private final TrackMate trackmate;
	private final TrackMateGUIController controller;
	
	public TrackingDescriptor(TrackMateGUIController controller) {
		this.controller = controller;
		this.trackmate = controller.getPlugin();
		this.logPanel = controller.getGUI().getLogPanel();
	}
	
	
	@Override
	public LogPanel getComponent() {
		return logPanel;
	}


	@Override
	public void aboutToDisplayPanel() {	}
		
	@Override
	public void displayingPanel() {
		final Logger logger = trackmate.getModel().getLogger();
		TrackerProvider trackerProvider = controller.getTrackerProvider();
		logger.log("Starting tracking using " + trackmate.getSettings().tracker +"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(trackerProvider.toString(trackmate.getSettings().trackerSettings));
		
		new Thread("TrackMate tracking thread") {					
			public void run() {
				controller.disableButtonsAndStoreState();
				long start = System.currentTimeMillis();
				trackmate.execTracking();
				long end = System.currentTimeMillis();
				logger.log("Found "	 + trackmate.getModel().getTrackModel().nTracks(false) + " tracks.\n");
				logger.log(String.format("Tracking done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				controller.restoreButtonsState();
				controller.getGUI().getNextButton().setEnabled(true);
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
	
	@Override
	public String getKey() {
		return KEY;
	}
}
