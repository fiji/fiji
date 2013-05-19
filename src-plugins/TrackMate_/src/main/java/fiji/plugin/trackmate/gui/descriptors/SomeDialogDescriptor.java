package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.io.File;

import fiji.plugin.trackmate.gui.LogPanel;

/**
 * An abstract class made for describing panels that generate a dialog, 
 * like save and load panels.
 * @author Jean-Yves Tinevez
 *
 */
public abstract class SomeDialogDescriptor implements WizardPanelDescriptor {

	/**
	 * File that governs saving and loading. We make it a static field so that 
	 * loading and sharing events always point to a single file location by default.
	 */
	protected static File file;
	protected final LogPanel logPanel;
	

	public SomeDialogDescriptor(LogPanel logPanel) {
		this.logPanel = logPanel;
	}
	

	@Override
	public Component getComponent() {
		return logPanel;
	}
	
	@Override
	public void aboutToDisplayPanel() {}

	@Override
	public abstract void displayingPanel();

	@Override
	public void aboutToHidePanel() {}
}