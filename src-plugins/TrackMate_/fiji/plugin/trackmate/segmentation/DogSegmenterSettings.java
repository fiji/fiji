package fiji.plugin.trackmate.segmentation;

import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.gui.DogSegmenterConfigurationPanel;
import fiji.plugin.trackmate.gui.SegmenterConfigurationPanel;

public class DogSegmenterSettings extends LogSegmenterSettings {
	
	private static final String SEGMENTER_SETTINGS_DO_SUBPIXEL_ATTRIBUTE_NAME	= "doSubPixelLocalization";

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
	
	@Override
	public void marshall(Element element) {
		element.setAttributes(getAttributes());
	}
	
	@Override
	public void unmarshall(Element element) {
		super.unmarshall(element);
		doSubPixelLocalization = Boolean.parseBoolean(element.getAttributeValue(SEGMENTER_SETTINGS_DO_SUBPIXEL_ATTRIBUTE_NAME));
	}
	
	
	@Override
	protected List<Attribute> getAttributes() {
		List<Attribute> atts = super.getAttributes();
		atts.add(new Attribute(SEGMENTER_SETTINGS_DO_SUBPIXEL_ATTRIBUTE_NAME, ""+doSubPixelLocalization));
		return atts;
	}
	
}
