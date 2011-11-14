package fiji.plugin.trackmate.segmentation;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.JNumericTextField;
import fiji.plugin.trackmate.gui.LogSegmenterConfigurationPanel;

public class DownSampleLogSegmenterConfigurationPanel extends LogSegmenterConfigurationPanel {

	private static final long serialVersionUID = 3840748523863902343L;
	private JLabel jLabelDownSample;
	private JNumericTextField jTextFieldDownSample;

	@Override
	protected void initGUI() {
		super.initGUI();
		
		// Make room for a new line
		GridBagLayout thisLayout = (GridBagLayout) this.getLayout();
		thisLayout.rowWeights = new double[] {0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.5, 0.0};
		thisLayout.rowHeights = new int[] {15, 15, 7, 15, 15, 15, 15, 7, 15};
		
		// Remove sub-pixel localization checkbox
		this.remove(jCheckSubPixel);
		
		// Add down sampling text and textfield
		{
			jLabelDownSample = new JLabel();
			this.add(jLabelDownSample, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 5, 0), 0, 0));
			jLabelDownSample.setText("Down-sampling factor:");
			jLabelDownSample.setFont(FONT);
		}
		{
			jTextFieldDownSample = new JNumericTextField();
			this.add(jTextFieldDownSample, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			jTextFieldDownSample.setFont(FONT);
		}
	}
	
	@Override
	public SegmenterSettings getSegmenterSettings() {
		LogSegmenterSettings lss = (LogSegmenterSettings) super.getSegmenterSettings();
		DownSampleLogSegmenterSettings dss = copyToDSLSS(lss);
		dss.downSamplingFactor = Float.parseFloat(jTextFieldDownSample.getText());
		return dss;
	}
	
	@Override
	public void setSegmenterSettings(TrackMateModel model) {
		DownSampleLogSegmenterSettings settings = (DownSampleLogSegmenterSettings) model.getSettings().segmenterSettings;
		
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().segmenter.toString());
		jLabelHelpText.setText(model.getSettings().segmenter.getInfoText().replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
		
		jTextFieldBlobDiameter.setText(""+(2*settings.expectedRadius));
		jCheckBoxMedianFilter.setSelected(settings.useMedianFilter);
		jTextFieldThreshold.setText(""+settings.threshold);
		jTextFieldDownSample.setText(""+settings.downSamplingFactor);
	}
	

	public static DownSampleLogSegmenterSettings copyToDSLSS(LogSegmenterSettings lss) {
		DownSampleLogSegmenterSettings dss = new DownSampleLogSegmenterSettings();
		dss.expectedRadius 	= lss.expectedRadius;
		dss.threshold		= lss.threshold;
		dss.useMedianFilter	= lss.useMedianFilter;
		return dss;
	}
	
	public static LogSegmenterSettings copyToLSS(DownSampleLogSegmenterSettings dss) {
		LogSegmenterSettings lss = new LogSegmenterSettings();
		lss.expectedRadius 	= dss.expectedRadius;
		lss.threshold		= dss.threshold;
		lss.useMedianFilter	= dss.useMedianFilter;
		return lss;
	}
	
	
}
