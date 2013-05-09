package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class ViewChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DisplayerChoice";
	private final ListChooserPanel component;
	private final ViewProvider viewProvider;

	public ViewChoiceDescriptor(ViewProvider viewProvider) {
		this.viewProvider = viewProvider;
		List<String> viewerNames = viewProvider.getAvailableViews();
		List<String> infoTexts = new ArrayList<String>(viewerNames.size());
		for(String key : viewerNames) {
			infoTexts.add(viewProvider.getInfoText(key));
		}
		this.component = new ListChooserPanel(viewerNames, infoTexts, "view");
	}
	
	
	/*
	 * METHODS
	 */
	
	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		int index = component.getChoice();
		String viewName = viewProvider.getAvailableViews().get(index);
		final TrackMateModelView view = viewProvider.getView(viewName);
		new Thread("TrackMate view rendering thread") {
			public void run() {
				view.render();
			};
		}.start();
	}

}
