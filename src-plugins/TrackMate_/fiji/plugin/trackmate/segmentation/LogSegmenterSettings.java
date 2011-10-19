package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.gui.LogSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;

/**
 * A segmenter settings object valid for most spot segmenters based on Log filtering,
 * 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010-2011
 */
public class LogSegmenterSettings extends BasicSegmenterSettings {

	private static final String SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME = "threshold";
	private static final String SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME= "usemedianfilter";

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
	
	@Override
	public void  marshall(Element element) {
		element.setAttributes(getAttributes());
	}
	
	@Override
	public void unmarshall(Element element) {
		super.unmarshall(element); // Deal with expected radius
		try {
			float val = Float.parseFloat(element.getAttributeValue(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME));
			threshold = val;
		} catch (NumberFormatException nfe) { }
		useMedianFilter = Boolean.parseBoolean(element.getAttributeValue(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME));
	}
	
	protected List<Attribute> getAttributes() {
		Attribute attThreshold 	= new Attribute(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME, ""+threshold);
		Attribute attMedian 	= new Attribute(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME, ""+useMedianFilter);
		List<Attribute> atts = new ArrayList<Attribute>(3);
		atts.add(super.getAttribute());
		atts.add(attThreshold);
		atts.add(attMedian);
		return atts;
	}
}
