package fiji.plugin.trackmate.segmentation;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.gui.BasicSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;


/**
 * A simple settings, made of only one field (the expected radius),
 * suitable for manual segmenters.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public class BasicSegmenterSettings implements SegmenterSettings {
	
	private static final String SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME 	= "expectedradius";

	private static final float DEFAULT_EXPECTED_DIAMETER	= 10f;

	/** The expected spot diameter in physical units. */
	public float 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	
	/*
	 * METHODS
	 */
	
	@Override
	public String toString() {
		String str = "";
		str += String.format("  Expected radius: %f\n", expectedRadius);
		return str;
	}
	
	
	@Override
	public SegmenterConfigurationPanel createConfigurationPanel() {
		return new BasicSegmenterConfigurationPanel();
	}
	
	@Override
	public void marshall(Element element) {
		element.setAttribute(getAttribute());
	}
	
	@Override
	public void unmarshall(Element element) {
		try {
			float val = Float.parseFloat(element.getAttributeValue(SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME));
			expectedRadius = val;
		} catch (NumberFormatException nfe) {
			// Do nothing. We keep the current instance settings
		}
	}
	
	/** 
	 * Return a JDom attribute that contains the expected radius.
	 */
	protected Attribute getAttribute() {
		Attribute att = new Attribute(SEGMENTER_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME, ""+expectedRadius);
		return att;
	}
}
