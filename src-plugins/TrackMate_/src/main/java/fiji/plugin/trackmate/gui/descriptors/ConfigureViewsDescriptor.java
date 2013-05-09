package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class ConfigureViewsDescriptor implements WizardPanelDescriptor {

	private ConfigureViewsPanel panel;

	public ConfigureViewsDescriptor(TrackMate trackmate) {
		this.panel = new ConfigureViewsPanel(trackmate);
	}


	@Override
	public ConfigureViewsPanel getComponent() {
		return panel;
	}

	@Override
	public void aboutToDisplayPanel() {}

	@Override
	public void displayingPanel() { 
		panel.refresh();
	}


	@Override
	public void aboutToHidePanel() { }
}
