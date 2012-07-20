package fiji.plugin.trackmate.gui;

import java.awt.Component;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.detection.ManualDetector;
import fiji.plugin.trackmate.detection.DetectorSettings;
import fiji.plugin.trackmate.detection.SpotDetector;

public class DetectorConfigurationPanelDescriptor <T extends RealType<T> & NativeType<T>> implements WizardPanelDescriptor<T> {

	public static final String DESCRIPTOR = "SegmenterConfigurationPanel";
	private TrackMate_<T> plugin;
	private DetectorConfigurationPanel<T> configPanel;
	private TrackMateWizard<T> wizard;
	
	/*
	 * METHODS
	 */

	@Override
	public void setWizard(TrackMateWizard<T> wizard) { 
		this.wizard = wizard;
	}

	@Override
	public void setPlugin(TrackMate_<T> plugin) {
		this.plugin = plugin;
		DetectorSettings<T> settings = plugin.getModel().getSettings().detectorSettings;
		// Bulletproof null
		if (null == settings) {
			SpotDetector<T> segmenter = plugin.getModel().getSettings().detector;
			if (null == segmenter) {
				// try to make it right with a default
				segmenter = new ManualDetector<T>();
				plugin.getModel().getSettings().detector = segmenter;
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
		if (plugin.getModel().getSettings().detector.getClass() == ManualDetector.class) {
			return DisplayerChoiceDescriptor.DESCRIPTOR;
		} else {
			return DetectorDescriptor.DESCRIPTOR;
		}
	}

	@Override
	public String getPreviousDescriptorID() {
		return DetectorChoiceDescriptor.DESCRIPTOR;
	}

	@Override
	public void aboutToDisplayPanel() {
		configPanel.setDetectorSettings(plugin.getModel());
		wizard.setNextButtonEnabled(true);
	}

	@Override
	public void displayingPanel() {	}

	@Override
	public void aboutToHidePanel() {
		plugin.getModel().getSettings().detectorSettings = configPanel.getDetectorSettings();
	}

}
