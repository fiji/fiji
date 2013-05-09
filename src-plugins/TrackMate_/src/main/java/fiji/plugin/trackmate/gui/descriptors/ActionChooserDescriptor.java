package fiji.plugin.trackmate.gui.descriptors;

import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.panels.ActionChooserPanel;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.ActionProvider;

public class ActionChooserDescriptor implements WizardPanelDescriptor {


	
	
	private ActionChooserPanel panel;

	public ActionChooserDescriptor(ActionProvider actionProvider, TrackMate trackmate) {
		this.panel = new ActionChooserPanel(actionProvider, trackmate);
	}
	
	@Override
	public ListChooserPanel getComponent() {
		return panel.getPanel();
	}

	@Override
	public void aboutToDisplayPanel() { }

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() { }
	
}
