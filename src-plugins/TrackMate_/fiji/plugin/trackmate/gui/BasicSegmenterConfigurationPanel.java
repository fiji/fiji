package fiji.plugin.trackmate.gui;

import javax.swing.JComponent;

import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class BasicSegmenterConfigurationPanel extends LogSegmenterConfigurationPanel {

	private static final long serialVersionUID = 4810266006596918360L;

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
	public void setSegmenterSettings(SegmenterSettings settings, String spaceUnits, String timeUnits) {
		jTextFieldBlobDiameter.setText(""+(2*settings.expectedRadius));
		jLabelBlobDiameterUnit.setText(spaceUnits);
	}
	
	@Override
	public SegmenterSettings getSegmenterSettings() {
		SegmenterSettings ss = new SegmenterSettings();
		ss.expectedRadius = Float.parseFloat(jTextFieldBlobDiameter.getText())/2;
		return ss;
	}
	
}
