package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

public class SegmenterChoiceDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "SegmenterChoice";
	@SuppressWarnings("rawtypes")
	private ListChooserPanel<SpotSegmenter> component;
	private TrackMate_ plugin;
	private TrackMateWizard wizard;
	
	/*
	 * METHODS
	 */

	@Override
	public Component getPanelComponent() {
		return component;
	}

	@Override
	public String getThisPanelID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextPanelID() {
		return SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getPreviousPanelID() {
		return StartDialogPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {	}

	@Override
	public void displayingPanel() { }

	@SuppressWarnings("unchecked")
	@Override
	public void aboutToHidePanel() {
		// Set the settings field of the model
		@SuppressWarnings("rawtypes")
		SpotSegmenter segmenter = component.getChoice();
		plugin.getModel().getSettings().segmenter = segmenter;
		plugin.getModel().getSettings().segmenterSettings = segmenter.createDefaultSettings();
		
		// Instantiate next descriptor for the wizard
		SegmenterConfigurationPanelDescriptor descriptor = new SegmenterConfigurationPanelDescriptor();
		descriptor.setWizard(wizard);
		descriptor.setPlugin(plugin);
		wizard.registerWizardDescriptor(SegmenterConfigurationPanelDescriptor.DESCRIPTOR, descriptor);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
		this.component = new ListChooserPanel<SpotSegmenter>(plugin.getAvailableSpotSegmenters(), "segmenter");
	}

	@Override
	public void setWizard(TrackMateWizard wizard) {
		this.wizard = wizard;
	}

}
