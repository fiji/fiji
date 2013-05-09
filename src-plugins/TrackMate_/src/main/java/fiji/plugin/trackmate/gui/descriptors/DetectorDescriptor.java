package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.LogPanel;
import fiji.plugin.trackmate.util.TMUtils;

public class DetectorDescriptor implements WizardPanelDescriptor {
	
	public static final String DESCRIPTOR = "DetectionPanel";
	protected final LogPanel logPanel;
	protected final TrackMate trackmate;
	protected Thread motherThread;


	public DetectorDescriptor(LogPanel logPanel, TrackMate trackmate) {
		this.trackmate = trackmate;
		this.logPanel = logPanel;
	}
	
	@Override
	public Component getComponent() {
		return logPanel;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		final Settings settings = trackmate.getSettings();
		final Logger logger = logPanel.getLogger();
		logger.log("Starting detection using "+settings.detectorFactory.toString()+"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(TMUtils.echoMap(settings.detectorSettings, 2));
		motherThread = new Thread("TrackMate detection mother thread") {
			public void run() {
				long start = System.currentTimeMillis();
				try {
					trackmate.execDetection();
				} catch (Exception e) {
					logger.error("An error occured:\n"+e+'\n');
					e.printStackTrace(logger);
				} finally {
					long end = System.currentTimeMillis();
					logger.log(String.format("Detection done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
				motherThread = null;
			}
		};
		motherThread.start();
	}

	@Override
	public synchronized void aboutToHidePanel() {
		final Thread thread = motherThread;
		if (thread != null) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException exc) {
				// ignore
			}
		}
	}
}
