package fiji.plugin.trackmate.gui;

import javax.swing.JComponent;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.BasicDetectorSettings;
import fiji.plugin.trackmate.detection.DetectorSettings;

public class BasicDetectorConfigurationPanel <T extends RealType<T> & NativeType<T>> extends LogDetectorConfigurationPanel<T> {

	private static final long serialVersionUID = -1L;
	private final String infoText;

	public BasicDetectorConfigurationPanel(String infoText) {
		super();
		this.infoText = infoText;
		final JComponent[] uselessComponents = new JComponent[] {
				super.jCheckBoxMedianFilter,
				super.jCheckSubPixel, 
				super.jLabelThreshold,
				super.jTextFieldThreshold,
				super.jButtonRefresh };
		for(JComponent c : uselessComponents)
			c.setVisible(false);
	}

	@Override
	public void setDetectorSettings(TrackMateModel<T> model) {
		jTextFieldBlobDiameter.setText(""+(((BasicDetectorSettings<T>)model.getSettings().detectorSettings).expectedRadius * 2));
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().detector.toString());
		jLabelHelpText.setText(infoText
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">"));
	}

	@Override
	public DetectorSettings<T> getDetectorSettings() {
		BasicDetectorSettings<T> ss = new BasicDetectorSettings<T>();
		ss.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		return ss;
	}

}
