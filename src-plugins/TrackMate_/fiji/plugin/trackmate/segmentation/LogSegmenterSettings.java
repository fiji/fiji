package fiji.plugin.trackmate.segmentation;

import fiji.plugin.trackmate.gui.LogSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;

/**
 * A segmenter settings object valid for most spot segmenters based on Log filtering,
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010-2011
 */
public class LogSegmenterSettings extends SegmenterSettings {

	/** The pixel value under which any peak will be discarded from further analysis. */
	public float 	threshold = 		0;
	/** If true, a median filter will be applied before segmenting. */
	public boolean useMedianFilter;
	
	
	@Override
	public String toString() {
		String str = super.toString();
		str += String.format("  Threshold: %f\n", threshold);
		str += "  Median filter: "+useMedianFilter+'\n';
	return str;
	}
	
	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new LogSegmenterConfigurationPanel();
	}
}
