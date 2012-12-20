package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.util.TMUtils;

public class DetectorDescriptor implements WizardPanelDescriptor {
	
	public static final String DESCRIPTOR = "DetectionPanel";
	protected LogPanel logPanel;
	protected TrackMate_ plugin;
	protected TrackMateWizard wizard;
	protected Logger logger;
	

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
		return InitFilterDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DetectorConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(false);
		final Settings settings = plugin.getModel().getSettings();
		logger.log("Starting detection using "+settings.detectorFactory.toString()+"\n", Logger.BLUE_COLOR);
		logger.log("with settings:\n");
		logger.log(TMUtils.echoMap(settings.detectorSettings, 2));
		new Thread("TrackMate detection mother thread") {					
			public void run() {
				long start = System.currentTimeMillis();
				try {
					plugin.execDetection();
				} catch (Exception e) {
					logger.error("An error occured:\n"+e+'\n');
					e.printStackTrace(logger);
				} finally {
					wizard.setNextButtonEnabled(true);
					long end = System.currentTimeMillis();
					logger.log(String.format("Detection done in %.1f s.\n", (end-start)/1e3f), Logger.BLUE_COLOR);
				}
			}
		}.start();
	}

	@Override
	public void aboutToHidePanel() { }
}
