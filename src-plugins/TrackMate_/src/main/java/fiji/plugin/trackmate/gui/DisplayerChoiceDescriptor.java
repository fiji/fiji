package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class DisplayerChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DisplayerChoice";
	private TrackMate_ plugin;
	private ListChooserPanel component;
	private TrackMateWizard wizard;
	
	/*
	 * METHODS
	 */
	
	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

	@Override
	public Component getComponent() {
		return component;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}
	
	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return LaunchDisplayerDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return DetectorConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() {
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void aboutToHidePanel() {
		String displayerName = component.getChoice();
		TrackMateModelView displayer = plugin.getViewProvider().getView(displayerName);
		wizard.setDisplayer(displayer);
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		List<String> viewerNames = plugin.getViewProvider().getAvailableViews();
		List<String> infoTexts = new ArrayList<String>(viewerNames.size());
		for(String key : viewerNames) {
			infoTexts.add(plugin.getViewProvider().getInfoText(key));
		}
		this.component = new ListChooserPanel(viewerNames, infoTexts, "view");
	}


}
