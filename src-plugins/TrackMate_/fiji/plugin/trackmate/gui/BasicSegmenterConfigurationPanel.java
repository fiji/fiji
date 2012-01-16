package fiji.plugin.trackmate.gui;

import javax.swing.JComponent;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.BasicSegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class BasicSegmenterConfigurationPanel extends LogSegmenterConfigurationPanel {

	private static final long serialVersionUID = 4298482387638112651L;

	public BasicSegmenterConfigurationPanel() {
		super();
		JComponent[] uselessComponents = new JComponent[] {
				super.jCheckBoxMedianFilter,
				super.jCheckSubPixel, 
				super.jLabelThreshold,
				super.jTextFieldThreshold,
				super.jButtonRefresh };
		for(JComponent c : uselessComponents)
			c.setVisible(false);
	}
	
	@Override
	public void setSegmenterSettings(TrackMateModel model) {
		jTextFieldBlobDiameter.setText(""+(((BasicSegmenterSettings)model.getSettings().segmenterSettings).expectedRadius * 2));
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().segmenter.toString());
		jLabelHelpText.setText(model.getSettings().segmenter.getInfoText()
				.replace("<br>", "")
				.replace("<p>", "<p align=\"justify\">")
				.replace("<html>", "<html><p align=\"justify\">"));
	}
	
	@Override
	public SegmenterSettings getSegmenterSettings() {
		BasicSegmenterSettings ss = new BasicSegmenterSettings();
		ss.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		return ss;
	}
	
}
