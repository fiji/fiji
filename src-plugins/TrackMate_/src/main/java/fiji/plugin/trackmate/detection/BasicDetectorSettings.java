package fiji.plugin.trackmate.detection;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.gui.BasicDetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.DetectorConfigurationPanel;


/**
 * A simple settings, made of only one field (the expected radius),
 * suitable for manual segmentation.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2011
 *
 */
public class BasicDetectorSettings <T extends RealType<T> & NativeType<T>> implements DetectorSettings<T> {
	
	private static final String DETECTOR_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME 	= "expectedradius";

	private static final double DEFAULT_EXPECTED_DIAMETER	= 10;

	/** The expected spot diameter in physical units. */
	public double 	expectedRadius = DEFAULT_EXPECTED_DIAMETER/2;
	
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
	public DetectorConfigurationPanel<T> createConfigurationPanel() {
		return new BasicDetectorConfigurationPanel<T>();
	}
	
	@Override
	public void marshall(Element element) {
		element.setAttribute(getAttribute());
	}
	
	@Override
	public void unmarshall(Element element) {
		try {
			float val = Float.parseFloat(element.getAttributeValue(DETECTOR_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME));
			expectedRadius = val;
		} catch (NumberFormatException nfe) {
			// Do nothing. We keep the current instance settings
		}
	}
	
	/** 
	 * Return a JDom attribute that contains the expected radius.
	 */
	protected Attribute getAttribute() {
		Attribute att = new Attribute(DETECTOR_SETTINGS_EXPECTED_RADIUS_ATTRIBUTE_NAME, ""+expectedRadius);
		return att;
	}
}
