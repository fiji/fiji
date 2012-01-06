package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

public class SegmenterConfigurationPanelDescriptor implements WizardPanelDescriptor {

	public static final Object DESCRIPTOR = "SegmenterConfigurationPanel";
	private TrackMate_ plugin;
	private SegmenterConfigurationPanel configPanel;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) { }

	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
	}

	@Override
	public Component getPanelComponent() {
		return configPanel;
	}

	@Override
	public Object getPanelDescriptorIdentifier() {
		return DESCRIPTOR;
	}

	@Override
	public Object getNextPanelDescriptor() {
		if (plugin.getModel().getSettings().segmenter.getClass() == ManualSegmenter.class) {
			return LaunchDisplayerDescriptor.DESCRIPTOR;
		} else {
			return SegmentationDescriptor.DESCRIPTOR;
		}
	}

	@Override
	public Object getBackPanelDescriptor() {
		return SegmenterChoiceDescriptor.DESCRIPTOR;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void aboutToDisplayPanel() {
		SegmenterSettings settings = plugin.getModel().getSettings().segmenterSettings;
		// Bulletproof null
		if (null == settings) {
			SpotSegmenter segmenter = plugin.getModel().getSettings().segmenter;
			if (null == segmenter) {
				// try to make it right with a default
				segmenter = new ManualSegmenter();
				plugin.getModel().getSettings().segmenter = segmenter;
			}
			settings = segmenter.createDefaultSettings();
		}
		configPanel = settings.createConfigurationPanel();
		configPanel.setSegmenterSettings(plugin.getModel());
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().segmenterSettings = configPanel.getSegmenterSettings();
	}

}
