package fiji.plugin.trackmate.gui;

import javax.swing.JComponent;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.detection.BasicSegmenterSettings;
import fiji.plugin.trackmate.detection.SegmenterSettings;

public class BasicSegmenterConfigurationPanel <T extends RealType<T> & NativeType<T>> extends LogSegmenterConfigurationPanel<T> {

	private static final long serialVersionUID = 4298482387638112651L;

	public BasicSegmenterConfigurationPanel() {
		super();
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
	public void setSegmenterSettings(TrackMateModel<T> model) {
		jTextFieldBlobDiameter.setText(""+(((BasicSegmenterSettings<T>)model.getSettings().segmenterSettings).expectedRadius * 2));
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().segmenter.toString());
		jLabelHelpText.setText(model.getSettings().segmenter.getInfoText()
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">"));
	}

	@Override
	public SegmenterSettings<T> getSegmenterSettings() {
		BasicSegmenterSettings<T> ss = new BasicSegmenterSettings<T>();
		ss.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		return ss;
	}

}
