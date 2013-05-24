package fiji.plugin.trackmate.gui.panels.detector;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import ij.ImagePlus;

public class DogDetectorConfigurationPanel extends LogDetectorConfigurationPanel {

	private static final long serialVersionUID = 1L;

	public DogDetectorConfigurationPanel(ImagePlus imp, String infoText, String detectorName, TrackMateModel model) {
		super(imp, infoText, detectorName, model);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	protected SpotDetectorFactory<?> getDetectorFactory() {
		return new DogDetectorFactory();
	}

}
