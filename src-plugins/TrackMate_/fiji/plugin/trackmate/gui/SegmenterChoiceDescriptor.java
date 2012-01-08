package fiji.plugin.trackmate.gui;

import java.awt.Component;

import mpicbg.imglib.type.numeric.RealType;

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
	public Component getComponent() {
		return component;
	}

	@Override
	public String getDescriptorID() {
		return DESCRIPTOR;
	}

	@Override
	public String getNextDescriptorID() {
		return SegmenterConfigurationPanelDescriptor.DESCRIPTOR;
	}

	@Override
	public String getComponentID() {
		return DESCRIPTOR;
	}

	@Override
	public String getPreviousDescriptorID() {
		return StartDialogPanel.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		SpotSegmenter<? extends RealType<?>> segmenter = plugin.getModel().getSettings().segmenter; 
		if (segmenter != null) {
			int index = 0;
			for (int i = 0; i < plugin.getAvailableSpotSegmenters().size(); i++) {
				if (segmenter.toString().equals(plugin.getAvailableSpotSegmenters().get(i).toString())) {
					index = i;
					break;
				}
			}
			component.jComboBoxChoice.setSelectedIndex(index);
		}
	}

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
