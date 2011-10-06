package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.gui.DogSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;

public class DogSegmenterSettings extends LogSegmenterSettings {
	
	private static final boolean 	DEFAULT_DO_SUBPIXEL_LOCALIZATION = true;
	
	/** If true, spot locations will be interpolated so as to reach sub-pixel localization	 */
	public boolean doSubPixelLocalization = DEFAULT_DO_SUBPIXEL_LOCALIZATION;
	
	@Override
	public String toString() {
		String str = super.toString();
		str += "  Do sub-pixel localization: "+doSubPixelLocalization+'\n';
		return str;
	}
	
	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new DogSegmenterConfigurationPanel();
	}
	
}
