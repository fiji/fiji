package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;

import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.segmentation.DogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.LogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class DogSegmenterConfigurationPanel extends LogSegmenterConfigurationPanel {

	private static final long serialVersionUID = -1866975600466866636L;
	private JCheckBox jCheckSubPixel;
	
	@Override
	protected void initGUI() {
		super.initGUI();
		
		// Make room for a new line
		GridBagLayout thisLayout = (GridBagLayout) this.getLayout();
		thisLayout.rowWeights = new double[] {0.0, 0.0, 0.5, 0.0, 0.0, 0.0, 0.0, 0.5, 0.0};
		thisLayout.rowHeights = new int[] {15, 15, 7, 15, 15, 15, 15, 7, 15};
		
		// Add sub-pixel checkbox
		{
			jCheckSubPixel = new JCheckBox();
			this.add(jCheckSubPixel, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
			jCheckSubPixel.setText("Do sub-pixel localization ");
			jCheckSubPixel.setFont(FONT);
		}
	}
	
	@Override
	public SegmenterSettings getSegmenterSettings() {
		LogSegmenterSettings lss = (LogSegmenterSettings) super.getSegmenterSettings();
		DogSegmenterSettings dss = copyToDOGS(lss);
		dss.doSubPixelLocalization = jCheckSubPixel.isSelected();
		return dss;
	}
	
	@Override
	public void setSegmenterSettings(TrackMateModel model) {
		super.setSegmenterSettings(model);
		jCheckSubPixel.setSelected(((DogSegmenterSettings)model.getSettings().segmenterSettings).doSubPixelLocalization);
	}
	
	
	public static DogSegmenterSettings copyToDOGS(LogSegmenterSettings lss) {
		DogSegmenterSettings dss = new DogSegmenterSettings();
		dss.expectedRadius 	= lss.expectedRadius;
		dss.threshold		= lss.threshold;
		dss.useMedianFilter	= lss.useMedianFilter;
		return dss;
	}
}
