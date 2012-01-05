package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

public class SegmenterChoiceDescriptor implements WizardPanelDescriptor {

	public static final Object DESCRIPTOR = "SegmenterChoice";
	@SuppressWarnings("rawtypes")
	private ListChooserPanel<SpotSegmenter> component;
	private TrackMate_ plugin;
	
	/*
	 * CONSTRUCTOR
	 */
	
	@SuppressWarnings("rawtypes")
	public SegmenterChoiceDescriptor(TrackMate_ plugin) {
		this.component = new ListChooserPanel<SpotSegmenter>(plugin.getAvailableSpotSegmenters(), "segmenter");
	}
	
	/*
	 * METHODS
	 */

	@Override
	public Component getPanelComponent() {
		return component;
	}

	@Override
	public Object getPanelDescriptorIdentifier() {
		return DESCRIPTOR;
	}

	@Override
	public Object getNextPanelDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getBackPanelDescriptor() {
		return StartDialogPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() { }

	@SuppressWarnings("unchecked")
	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().segmenter = component.getChoice();
	}

	@Override
	public void setWizardModel(WizardModel model) {
		// We do not use the wizard panel here
	}

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
	}

}
