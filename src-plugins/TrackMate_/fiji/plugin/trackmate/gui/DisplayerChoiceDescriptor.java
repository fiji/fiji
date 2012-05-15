package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class DisplayerChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "DisplayerChoice";
	private TrackMate_ plugin;
	private ListChooserPanel<TrackMateModelView> component;
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
		if (plugin.getModel().getSettings().segmenter.getClass() == ManualSegmenter.class) {
			return SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
		} else {
			return InitFilterPanel.DESCRIPTOR;
		}
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() { }

	@Override
	public void aboutToHidePanel() {
		TrackMateModelView displayer = component.getChoice();
		wizard.setDisplayer(displayer);
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		this.component = new ListChooserPanel<TrackMateModelView>(plugin.getAvailableTrackMateModelViews(), "displayer");
	}



}
