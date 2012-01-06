package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class LaunchDisplayerDescriptor implements WizardPanelDescriptor {

	public static final Object DESCRIPTOR = "LaunchDisplayer";
	private TrackMateWizard wizard;
	private LogPanel logPanel;
	private Logger logger;
	private TrackMate_ plugin;
	private boolean renderingDone;
	private boolean calculateFeaturesDone;
	private TrackMateModelView displayer;

	@Override
	public void setWizard(TrackMateWizard wizard) { 
		this.wizard = wizard;
		this.logPanel = wizard.getLogPanel();
		this.logger = wizard.getLogger();
		this.displayer = wizard.getDisplayer();
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
	}

	@Override
	public Component getPanelComponent() {
		return logPanel;
	}

	@Override
	public Object getPanelDescriptorIdentifier() {
		return DESCRIPTOR;
	}

	@Override
	public Object getNextPanelDescriptor() {
		return SpotFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public Object getBackPanelDescriptor() {
		return DisplayerChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() {
		// Launch renderer
		logger.log("Rendering results...\n",Logger.BLUE_COLOR);
		renderingDone = false;
		calculateFeaturesDone = false;
		wizard.setNextButtonEnabled(false);
		
		if (!plugin.getModel().getSpots().isEmpty()) {
			logger.log("Calculating features...\n",Logger.BLUE_COLOR);
			// Calculate features
			new Thread("TrackMate spot feature calculating mother thread") {
				public void run() {
					try {
						long start = System.currentTimeMillis();
						plugin.computeSpotFeatures();		
						long end  = System.currentTimeMillis();
						logger.log(String.format("Calculating features done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
					} finally {
						calculateFeaturesDone = true;
						if (renderingDone) {
							wizard.setNextButtonEnabled(true);
						}
					}
				}
			}.start();
			
		} else {
			renderingDone = true;
		}

		// Thread for rendering
		new Thread("TrackMate rendering thread") {
			
			public void run() {
				// Instantiate displayer
				if (null != displayer) {
					displayer.clear();
				}
				try {
					displayer.setModel(plugin.getModel());
					displayer.render();
				} finally {
					// Re-enable the GUI
					renderingDone = true;
					logger.log("Rendering done.\n", Logger.BLUE_COLOR);
					if (calculateFeaturesDone) {
						wizard.setNextButtonEnabled(true);
					}
				}
			}
		}.start();
		
	}

	@Override
	public void aboutToHidePanel() { }

}
