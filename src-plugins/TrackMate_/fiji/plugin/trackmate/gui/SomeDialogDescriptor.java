package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMate_;

/**
 * An abstract class made for descripting panels that generate a dialog, 
 * like save and load panels.
 * @author JeanYves
 *
 */
public abstract class SomeDialogDescriptor implements WizardPanelDescriptor {

	protected LogPanel logPanel;
	protected TrackMate_ plugin;
	protected TrackMateWizard wizard;
	protected Logger logger;
	protected File file;
	protected String targetID;
	private boolean previousPreviousButtonState;

	public void setTargetNextID(String ID) {
		this.targetID = ID;
	}
	
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
	public abstract String getDescriptorID();

	@Override
	public String getNextDescriptorID() {
		return targetID;
	}

	@Override
	public String getPreviousDescriptorID() {
		return null;
	}

	@Override
	public void aboutToDisplayPanel() {
		// To be able to put it back later.
		previousPreviousButtonState = wizard.jButtonPrevious.isEnabled();
		// Disable everything
		wizard.setPreviousButtonEnabled(false);
		wizard.setNextButtonEnabled(false);
		wizard.setLoadButtonEnabled(false);
		wizard.setSaveButtonEnabled(false);
		// Change name
		wizard.jButtonNext.setText("Resume");
	}

	@Override
	public abstract void displayingPanel();

	@Override
	public void aboutToHidePanel() {
		wizard.jButtonNext.setText("Next");
		wizard.setSaveButtonEnabled(true);
		wizard.setLoadButtonEnabled(true);
		wizard.setPreviousButtonEnabled(previousPreviousButtonState);
	}
}