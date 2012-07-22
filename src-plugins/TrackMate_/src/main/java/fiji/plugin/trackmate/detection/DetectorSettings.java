package fiji.plugin.trackmate.detection;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.Attribute;
import org.jdom.Element;

import fiji.plugin.trackmate.io.TmXmlKeys;

/** 
 * Mother interface for spot detector settings, to pass settings to the concrete 
 * implementations of {@link SpotDetector}s.
 * <p>
 * The concrete derivation of this class should be matched to the concrete implementation
 * of {@link SpotDetector}, and contain only public fields.
 * <p>
 * There is a bit of a edgy part: the {@link #createConfurationPanel()} method. It 
 * links a GUI object (the panel) to this settings object. This is the only
 * way I could come with - yet - in order to have a generic detector framework,
 * with objects having methods that can generate the whole context needed to
 * configure them. A more clever approach might be investigated. 
 *
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> 2010, 2011
 *
 */
public interface DetectorSettings <T extends RealType<T> & NativeType<T>> {

	/**
	 * Marshall this concrete instance to a JDom element, ready for saving to XML.
	 * <p>
	 * Marshalling should be done by adding {@link Attribute}s to the given element, 
	 * and/or child {@link Element}s. In the XML file, the mother element will have the
	 * name {@link TmXmlKeys#DETECTOR_SETTINGS_ELEMENT_KEY} and at least one attribute
	 * with name {@link TmXmlKeys#DETECTOR_SETTINGS_CLASS_ATTRIBUTE_NAME} and 
	 * value the name of the concrete settings class, to allow for unmarshsalling.
	 * 
	 * @return  the JDom element
	 */
	public void marshall(Element element);
	
	/**
	 * Load the field values stored in the JDom element to this instance.
	 */
	public void unmarshall(Element element);

}
