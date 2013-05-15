package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class ViewChoiceDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ChooseView";
	private final ListChooserPanel component;
	private final ViewProvider viewProvider;
	private final TrackMateGUIModel guimodel;

	public ViewChoiceDescriptor(ViewProvider viewProvider, TrackMateGUIModel guimodel) {
		this.viewProvider = viewProvider;
		this.guimodel = guimodel;
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
		final int index = component.getChoice();
		new Thread("TrackMate view rendering thread") {
			public void run() {
				String viewName = viewProvider.getAvailableViews().get(index);
				TrackMateModelView view = viewProvider.getView(viewName);
				guimodel.addView(view);
				view.render();
			};
		}.start();
	}

	@Override
	public String getKey() {
		return KEY;
	}
}
