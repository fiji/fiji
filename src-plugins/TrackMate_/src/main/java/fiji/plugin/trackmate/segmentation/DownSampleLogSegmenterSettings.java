package fiji.plugin.trackmate.segmentation;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

/**
 * A segmenter settings object valid for the down-sampling Log segmenter.
 * 
 * @author Jean-Yves Tinevez <tinevez@pasteur.fr> 2011
 */
public class DownSampleLogSegmenterSettings extends BasicSegmenterSettings {

	private static final String SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME 			= "threshold";
	private static final String SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME			= "usemedianfilter";
	private static final String SEGMENTER_SETTINGS_DOWN_SAMPLE_FACTOR_ATTRIBUTE_NAME	= "downsamplingfactor";

	private static final int 	DEFAULT_DOWNSAMPLING_FACTOR = 4;
	
	/** The pixel value under which any peak will be discarded from further analysis. */
	public float 	threshold = 		0;
	/** If true, a median filter will be applied before segmenting. */
	public boolean useMedianFilter;
	/** By how much the source image will be down-sampled before filtering.	 */
	public int downSamplingFactor = DEFAULT_DOWNSAMPLING_FACTOR;
	
	
	@Override
	public String toString() {
		String str = super.toString();
		str += String.format("  Threshold: %f\n", threshold);
		str += "  Median filter: "+useMedianFilter+'\n';
		str += "  Down-sampling factor: "+downSamplingFactor+'\n';
	return str;
	}
	
	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new DownSampleLogSegmenterConfigurationPanel();
	}
	
	@Override
	public void  marshall(Element element) {
		for(Attribute att : getAttributes()) {
			element.setAttribute(att);
		}
	}
	
	@Override
	public void unmarshall(Element element) {
		super.unmarshall(element); // Deal with expected radius
		try {
			float val = Float.parseFloat(element.getAttributeValue(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME));
			threshold = val;
		} catch (NumberFormatException nfe) { }
		useMedianFilter = Boolean.parseBoolean(element.getAttributeValue(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME));
		
		try {
			downSamplingFactor = TMUtils.readIntAttribute(element, SEGMENTER_SETTINGS_DOWN_SAMPLE_FACTOR_ATTRIBUTE_NAME, Logger.VOID_LOGGER, DEFAULT_DOWNSAMPLING_FACTOR);
		} catch (NumberFormatException nfe) { }
	}
	
	protected List<Attribute> getAttributes() {
		Attribute attThreshold 	= new Attribute(SEGMENTER_SETTINGS_THRESHOLD_ATTRIBUTE_NAME, ""+threshold);
		Attribute attMedian 	= new Attribute(SEGMENTER_SETTINGS_USE_MEDIAN_ATTRIBUTE_NAME, ""+useMedianFilter);
		Attribute attDwnSpl 	= new Attribute(SEGMENTER_SETTINGS_DOWN_SAMPLE_FACTOR_ATTRIBUTE_NAME, ""+downSamplingFactor);
		List<Attribute> atts = new ArrayList<Attribute>(4);
		atts.add(super.getAttribute());
		atts.add(attThreshold);
		atts.add(attMedian);
		atts.add(attDwnSpl);
		return atts;
	}
}
