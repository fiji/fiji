package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.GrapherPanel;

public class GrapherDescriptor implements WizardPanelDescriptor  {

	private final GrapherPanel panel;

	public GrapherDescriptor(TrackMate trackmate) {
		this.panel = new GrapherPanel(trackmate);
	}

	@Override
	public Component getComponent() {
		return panel;
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() { }

}
