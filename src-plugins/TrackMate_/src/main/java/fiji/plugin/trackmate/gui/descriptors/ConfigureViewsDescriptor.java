package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;

public class ConfigureViewsDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ConfigureViews";
	private ConfigureViewsPanel panel;

	public ConfigureViewsDescriptor(TrackMate trackmate) {
		this.panel = new ConfigureViewsPanel(trackmate.getModel());
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


	@Override
	public String getKey() {
		return KEY;
	}
}
