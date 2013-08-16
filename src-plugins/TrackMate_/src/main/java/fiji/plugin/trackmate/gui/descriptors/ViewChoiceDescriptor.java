package fiji.plugin.trackmate.gui.descriptors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.gui.TrackMateGUIController;
import fiji.plugin.trackmate.gui.TrackMateGUIModel;
import fiji.plugin.trackmate.gui.panels.ListChooserPanel;
import fiji.plugin.trackmate.providers.ViewProvider;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;

public class ViewChoiceDescriptor implements WizardPanelDescriptor {

	private static final String KEY = "ChooseView";
	private final ListChooserPanel component;
	private final ViewProvider viewProvider;
	private final TrackMateGUIModel guimodel;
	private final TrackMateGUIController controller;

	public ViewChoiceDescriptor(final ViewProvider viewProvider, final TrackMateGUIModel guimodel, final TrackMateGUIController controller) {
		this.viewProvider = viewProvider;
		this.guimodel = guimodel;
		this.controller = controller;
		final List<String> viewerNames = viewProvider.getAvailableViews();
		final List<String> infoTexts = new ArrayList<String>(viewerNames.size());
		for(final String key : viewerNames) {
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
	public void displayingPanel() {
		controller.getGUI().setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		final int index = component.getChoice();
		new Thread("TrackMate view rendering thread") {
			@Override
			public void run() {
				final String viewName = viewProvider.getAvailableViews().get(index);

				if (viewName.equals(HyperStackDisplayer.NAME)) {
					return; // it is already on.
				}

				final TrackMateModelView view = viewProvider.getView(viewName);
				for (final String settingKey : guimodel.getDisplaySettings().keySet()) {
					view.setDisplaySettings(settingKey, guimodel.getDisplaySettings().get(settingKey));
				}
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
