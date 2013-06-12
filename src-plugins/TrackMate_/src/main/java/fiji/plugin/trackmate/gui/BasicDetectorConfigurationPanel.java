package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import fiji.util.NumberParser;
import ij.ImagePlus;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;

public class BasicDetectorConfigurationPanel extends LogDetectorConfigurationPanel {

	private static final long serialVersionUID = -1L;

	public BasicDetectorConfigurationPanel(ImagePlus imp, String infoText, String detectorName, String spaceUnits)  {
		super(imp, infoText, detectorName, spaceUnits);
		final JComponent[] uselessComponents = new JComponent[] {
				super.sliderChannel,
				super.labelChannel,
				super.lblSegmentInChannel,
				super.jCheckBoxMedianFilter,
				super.jCheckSubPixel, 
				super.jLabelThreshold,
				super.jTextFieldThreshold,
				super.jButtonRefresh };
		for(JComponent c : uselessComponents)
			c.setVisible(false);
	}

	
	@Override
	public void setSettings(final Map<String, Object> settings) {
		jTextFieldBlobDiameter.setText("" + ( (Double) settings.get(KEY_RADIUS)* 2) );
	}

	@Override
	public Map<String, Object> getSettings() {
		Map<String, Object> settings = new HashMap<String, Object>(1);
		settings.put(KEY_RADIUS, NumberParser.parseDouble(jTextFieldBlobDiameter.getText()));
		return settings;
	}

}
