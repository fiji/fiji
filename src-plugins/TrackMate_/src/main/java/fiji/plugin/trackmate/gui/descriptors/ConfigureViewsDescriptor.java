package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.visualization.FeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerEdgeFeatureColorGenerator;
import fiji.plugin.trackmate.visualization.PerTrackFeatureColorGenerator;

public class ConfigureViewsDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ConfigureViews";
	private ConfigureViewsPanel panel;

	public ConfigureViewsDescriptor(TrackMate trackmate, FeatureColorGenerator<Spot> spotColorGenerator, PerEdgeFeatureColorGenerator edgeColorGenerator, PerTrackFeatureColorGenerator trackColorGenerator) {
		this.panel = new ConfigureViewsPanel(trackmate.getModel());
		panel.setSpotColorGenerator(spotColorGenerator);
		panel.setEdgeColorGenerator(edgeColorGenerator);
		panel.setTrackColorGenerator(trackColorGenerator);
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
