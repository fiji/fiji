package fiji.plugin.trackmate.detection;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.gui.JNumericTextField;
import fiji.plugin.trackmate.gui.LogDetectorConfigurationPanel;

public class DownSampleLogDetectorConfigurationPanel <T extends RealType<T>  & NativeType<T>> extends LogDetectorConfigurationPanel<T> {

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
			jLabelDownSample.setText("Down-sampling factor (integer):");
			jLabelDownSample.setFont(FONT);
		}
		{
			jTextFieldDownSample = new JNumericTextField();
			this.add(jTextFieldDownSample, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			jTextFieldDownSample.setFont(FONT);
		}
	}
	
	@Override
	public DetectorSettings<T> getDetectorSettings() {
		LogDetectorSettings<T> lss = (LogDetectorSettings<T>) super.getDetectorSettings();
		DownSampleLogDetectorSettings<T> dss = copyToDSLSS(lss);
		dss.downSamplingFactor = Integer.parseInt(jTextFieldDownSample.getText());
		return dss;
	}
	
	@Override
	public void setDetectorSettings(TrackMateModel<T> model) {
		DownSampleLogDetectorSettings<T> settings = (DownSampleLogDetectorSettings<T>) model.getSettings().detectorSettings;
		
		jLabelBlobDiameterUnit.setText(model.getSettings().spaceUnits);
		jLabelSegmenterName.setText(model.getSettings().detector.toString());
		jLabelHelpText.setText(DownsampleLogDetector.INFO_TEXT.replace("<br>", "").replace("<html>", "<html><p align=\"justify\">"));
		
		jTextFieldBlobDiameter.setText(""+(2*settings.expectedRadius));
		jCheckBoxMedianFilter.setSelected(settings.useMedianFilter);
		jTextFieldThreshold.setText(""+settings.threshold);
		jTextFieldDownSample.setText(""+settings.downSamplingFactor);
	}
	

	public static <T extends RealType<T>  & NativeType<T>> DownSampleLogDetectorSettings<T> copyToDSLSS(LogDetectorSettings<T> lss) {
		DownSampleLogDetectorSettings<T> dss = new DownSampleLogDetectorSettings<T>();
		dss.expectedRadius 	= lss.expectedRadius;
		dss.threshold		= lss.threshold;
		dss.useMedianFilter	= lss.useMedianFilter;
		return dss;
	}
	
	public static <T extends RealType<T>  & NativeType<T>> LogDetectorSettings<T> copyToLSS(DownSampleLogDetectorSettings<T> dss) {
		LogDetectorSettings<T> lss = new LogDetectorSettings<T>();
		lss.expectedRadius 	= dss.expectedRadius;
		lss.threshold		= dss.threshold;
		lss.useMedianFilter	= dss.useMedianFilter;
		return lss;
	}
	
	
}
