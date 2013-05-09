
package fiji.plugin.trackmate.gui.descriptors;

import ij.ImagePlus;
import ij.WindowManager;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.TrackMateWizard;
import fiji.plugin.trackmate.gui.panels.StartDialogPanel;

public class StartDialogDescriptor implements WizardPanelDescriptor {

	private final StartDialogPanel panel;
	private final TrackMate trackmate;

	public StartDialogDescriptor(TrackMate trackmate) {
		this.trackmate = trackmate;
		this.panel = new StartDialogPanel();
	}

	/*
	 * WIZARDPANELDESCRIPTOR METHODS
	 */


	@Override
	public StartDialogPanel getComponent() {
		return panel;
	}


	@Override
	public void aboutToDisplayPanel() {
		ImagePlus imp;
		if (null == trackmate.getSettings().imp) {
			imp = WindowManager.getCurrentImage();
		} else {
			panel.echoSettings(trackmate.getSettings());
			imp = trackmate.getSettings().imp;
		}
		panel.refresh(imp);
	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		// Get settings and pass them to the trackmate managed by the wizard
		getSettings(trackmate.getSettings());
		trackmate.getModel().getLogger().log(trackmate.getSettings().toStringImageInfo());
	}

}
