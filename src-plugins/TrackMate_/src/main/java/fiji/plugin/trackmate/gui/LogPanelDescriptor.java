package fiji.plugin.trackmate.gui;

import java.awt.Component;

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
	public String getComponentID() {
		return LogPanel.DESCRIPTOR;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return null;
	}

	@Override
	public String getPreviousDescriptorID() {
		return null;
	}

	@Override
	public void aboutToDisplayPanel() {}

	@Override
	public void displayingPanel() {}

	@Override
	public void aboutToHidePanel() {}

}
