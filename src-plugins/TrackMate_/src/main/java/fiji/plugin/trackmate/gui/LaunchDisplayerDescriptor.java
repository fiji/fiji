package fiji.plugin.trackmate.gui;

import java.awt.Component;

import mpicbg.imglib.multithreading.SimpleMultiThreading;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class LaunchDisplayerDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "LaunchDisplayer";
	private TrackMateWizard wizard;
	private LogPanel logPanel;
	private Logger logger;
	private TrackMate_ plugin;

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
	public String getComponentID() {
		return LogPanel.DESCRIPTOR;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DisplayerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() {
		// Launch renderer
		logger.log("Rendering results...\n",Logger.BLUE_COLOR);
		wizard.setNextButtonEnabled(false);
		final TrackMateModelView displayer = wizard.getDisplayer();

		// Thread for rendering
		Thread renderingThread = new Thread("TrackMate rendering thread") {

			public void run() {
				// Instantiate displayer
				displayer.render();
				logger.log("Rendering done.\n", Logger.BLUE_COLOR);
			}
		};
		
		if (plugin.getModel().getSpots().getNSpots() > 0) {

			/*
			 * We have some spots so we need to compute spot features will we render them.
			 */
			logger.log("Calculating spot features...\n",Logger.BLUE_COLOR);
			// Calculate features
			Thread featureCalculationThread = new Thread("TrackMate spot feature calculating mother thread") {
				public void run() {
					long start = System.currentTimeMillis();
					plugin.computeSpotFeatures(true);		
					long end  = System.currentTimeMillis();
					logger.log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
			};
			// Launch threads
			Thread[] threads = new Thread[] { featureCalculationThread, renderingThread };
			SimpleMultiThreading.startAndJoin(threads);
			
		} else {
			
			/*
			 * We don't have any spot. Let's just render the view.
			 */
			renderingThread.start();
			try {
				renderingThread.join();
			} catch (InterruptedException e) {
				logger.error("Error rendering the view:\n" + e.getLocalizedMessage());
				e.printStackTrace();
			}
			
		}

		// Re-enable the GUI
		wizard.setVisible(true);
		wizard.setNextButtonEnabled(true);

	}

	@Override
	public void aboutToHidePanel() { }

}
