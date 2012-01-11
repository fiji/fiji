package fiji.plugin.trackmate.gui;

import java.awt.Component;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.segmentation.ManualSegmenter;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;
import fiji.plugin.trackmate.segmentation.SpotSegmenter;

public class SegmenterConfigurationPanelDescriptor implements WizardPanelDescriptor {

	public static final String DESCRIPTOR = "SegmenterConfigurationPanel";
	private TrackMate_ plugin;
	private SegmenterConfigurationPanel configPanel;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard wizard) { }

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void setPlugin(TrackMate_ plugin) {
		this.plugin = plugin;
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
	}

	@Override
	public Component getComponent() {
		return configPanel;
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
		if (plugin.getModel().getSettings().segmenter.getClass() == ManualSegmenter.class) {
			return DisplayerChoiceDescriptor.DESCRIPTOR;
		} else {
			return SegmentationDescriptor.DESCRIPTOR;
		}
	}

	@Override
	public String getPreviousDescriptorID() {
		return SegmenterChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		configPanel.setSegmenterSettings(plugin.getModel());
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().segmenterSettings = configPanel.getSegmenterSettings();
	}

}
