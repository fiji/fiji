package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.providers.TrackerProvider;

public class TrackingDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "TrackingPanel";
	private final LogPanel logPanel;
	private final TrackMate trackmate;
	private final TrackerProvider trackerProvider;
	
	public TrackingDescriptor(LogPanel logPanel, TrackerProvider trackerProvider, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.logPanel = logPanel;
		this.trackerProvider = trackerProvider;
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
		logger.log("Starting tracking using " + trackmate.getSettings().tracker +"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(trackerProvider.toString(trackmate.getSettings().trackerSettings));
		new Thread("TrackMate tracking thread") {					
			public void run() {
				long start = System.currentTimeMillis();
				trackmate.execTracking();
				long end = System.currentTimeMillis();
				logger.log(String.format("Tracking done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
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
