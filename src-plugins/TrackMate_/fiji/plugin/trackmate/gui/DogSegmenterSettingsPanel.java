package fiji.plugin.trackmate.gui;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JCheckBox;

import fiji.plugin.trackmate.segmentation.DogSegmenterSettings;
import fiji.plugin.trackmate.segmentation.SegmenterSettings;

public class DogSegmenterSettingsPanel extends SegmenterSettingsPanel {

	private static final long serialVersionUID = 1587146031133276446L;
	private JCheckBox jCheckSubPixel;

	/**
	 * @param segmenterSettings  must be a {@link DogSegmenterSettings}
	 */
	public DogSegmenterSettingsPanel(SegmenterSettings segmenterSettings) {
		super(segmenterSettings);
	}
	
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
			jCheckSubPixel.setSelected(((DogSegmenterSettings)settings).doSubPixelLocalization);
		}
	}
	
	@Override
	public SegmenterSettings getSettings() {
		DogSegmenterSettings s = (DogSegmenterSettings) super.getSettings();
		s.doSubPixelLocalization = jCheckSubPixel.isSelected();
		return s;
	}
	
}
