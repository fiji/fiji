package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;

import fiji.plugin.trackmate.gui.LogPanel;

public class LogPanelDescriptor implements WizardPanelDescriptor {
	
	public static final String DESCRIPTOR = "LogPanel";
	private final LogPanel logPanel;

	public LogPanelDescriptor(LogPanel logPanel) {
		this.logPanel = logPanel;
	}
	
	@Override
	public Component getComponent() {
		return logPanel;
	}

	@Override
	public void aboutToDisplayPanel() {}

	@Override
	public void displayingPanel() {}

	@Override
	public void aboutToHidePanel() {}

}
