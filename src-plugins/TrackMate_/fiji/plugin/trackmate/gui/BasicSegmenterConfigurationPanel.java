package fiji.plugin.trackmate.gui;

import javax.swing.JComponent;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class BasicSegmenterConfigurationPanel extends LogSegmenterConfigurationPanel {

	private static final long serialVersionUID = 4298482387638112651L;

	public BasicSegmenterConfigurationPanel() {
		super();
		JComponent[] uselessComponents = new JComponent[] {
				super.jCheckBoxMedianFilter,
				super.jLabelThreshold,
				super.jTextFieldThreshold,
				super.jButtonRefresh };
		for(JComponent c : uselessComponents)
			c.setVisible(false);
	}
	
	@Override
	public void setSegmenterSettings(TrackMateModel model) {
		jTextFieldBlobDiameter.setText(""+(2*model.getSettings().segmenterSettings.expectedRadius));
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().segmenter.toString());
		jLabelHelpText.setText(model.getSettings().segmenter.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
	}
	
	@Override
	public SegmenterSettings getSegmenterSettings() {
		SegmenterSettings ss = new SegmenterSettings();
		ss.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		return ss;
	}
	
}
