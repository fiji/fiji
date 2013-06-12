package fiji.plugin.trackmate;

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.util.NumberParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

/**
 * A base class for providers, that contains utility methods and fields.
 * @author Jean-Yves Tinevez - 2012
 */
public abstract class AbstractProvider {


	protected static final String XML_MAP_KEY_ATTRIBUTE_NAME = "KEY";
	protected static final String XML_MAP_VALUE_ATTRIBUTE_NAME = "VALUE";
	/** The target keys. These names will be used as keys to access relevant classes. 
	 * The list order determines how target classes are presented in the GUI. */
	protected List<String> keys;
	/** The currently selected key. It must belong to the {@link #keys} list. */
	protected String currentKey;
	/** Storage for error messages. */
	protected String errorMessage;
	/** The target classes pretty names. With the same order that for {@link #keys}. */
	protected ArrayList<String> names;
	/** The target classes info texts. With the same order that for {@link #keys}. */
	protected ArrayList<String> infoTexts;

	

	/**
	 * @return the currently selected key.
	 */
	public String getCurrentKey() {
		return currentKey;
	}
	
	/**
	 * Configure this provider for the target {@link SpotDetectorFactory} identified by 
	 * the given key. If the key is not found in this provider's list, the 
	 * provider state is not changed.
	 * @return true if the given key was found and the target detector was changed.
	 */
	public boolean select(final String key) {
		if (keys.contains(key)) {
			currentKey = key;
			errorMessage = null;
			return true;
		} else {
			errorMessage = "Unknown key: "+key+".\n";
			return false;
		}
	}

	/**
	 * @return an error message for the last unsuccessful methods call.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	
	/**
	 * Add a parameter attribute to the given element, taken from the given settings map. 
	 * Basic checks are made to ensure that the parameter value can be found and is of 
	 * the right class.
	 * @param settings  the map to take the parameter value from
	 * @param element  the JDom element to update
	 * @param parameterKey  the key to the parameter value in the map
	 * @param expectedClass  the expected class for the value
	 * @return  true if the parameter was found, of the right class, and was successfully added to the element.
	 */
	protected boolean writeAttribute(final Map<String, Object> settings, Element element, String parameterKey, Class<?> expectedClass) {
		Object obj = settings.get(parameterKey);

		if (null == obj) {
			errorMessage = "Could not find parameter "+parameterKey+" in settings map.\n";
			return false;
		}

		if (!expectedClass.isInstance(obj)) {
			errorMessage = "Exoected "+parameterKey+" parameter to be a "+expectedClass.getName()+" but was a "+obj.getClass().getName()+".\n";
			return false;
		}

		element.setAttribute(parameterKey, ""+obj);
		return true;
	}

	/** 
	 * Stores the given mapping in a given JDon element, using attributes in a KEY="VALUE" fashion.
	 */
	protected void marshallMap(final Map<String, Double> map, final Element element) {
		for (String key : map.keySet()) {
			element.setAttribute(key, map.get(key).toString());
		}
	}
	
	/** 
	 * Unmarshall the attributes of a JDom element in a map of doubles. 
	 * Mappings are added to the given map. If a value is found not to be a double, an 
	 * error is returned.
	 * @return  true if all values were found and mapped as doubles, false otherwise and 
	 * {@link #errorMessage} is updated. 
	 */
	protected boolean unmarshallMap(final Element element, final Map<String, Double> map, StringBuilder errorHolder) {
		boolean ok = true;
		List<Attribute> attributes = element.getAttributes();
		for(Attribute att : attributes) {
			String key = att.getName();
			try {
				double val = att.getDoubleValue();
				map.put(key, val);
			} catch (DataConversionException e) {
				errorHolder.append("Could not convert the "+key+" attribute to double. Got "+att.getValue()+".\n");
				ok = false;
			}
		}
		return ok;
	}
	

	protected boolean readDoubleAttribute(final Element element, Map<String, Object> settings, String parameterKey, StringBuilder errorHolder) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorHolder.append("Attribute "+parameterKey+" could not be found in XML element.\n");
			return false;
		}
		try {
			double val = NumberParser.parseDouble(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorHolder.append("Could not read "+parameterKey+" attribute as a double value. Got "+str+".\n");
			return false;
		}
		return true;
	}

	protected boolean readIntegerAttribute(final Element element, Map<String, Object> settings, String parameterKey, StringBuilder errorHolder) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorHolder.append("Attribute "+parameterKey+" could not be found in XML element.\n");
			return false;
		}
		try {
			int val = NumberParser.parseInteger(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorHolder.append("Could not read "+parameterKey+" attribute as an integer value. Got "+str+".\n");
			return false;
		}
		return true;
	}

	protected boolean readBooleanAttribute(final Element element, Map<String, Object> settings, String parameterKey, StringBuilder errorHolder) {
		String str = element.getAttributeValue(parameterKey);
		if (null == str) {
			errorHolder.append("Attribute "+parameterKey+" could not be found in XML element.\n");
			return false;
		}
		try {
			boolean val = Boolean.parseBoolean(str);
			settings.put(parameterKey, val);
		} catch (NumberFormatException nfe) {
			errorHolder.append("Could not read "+parameterKey+" attribute as an boolean value. Got "+str+".");
			return false;
		}
		return true;
	}


	/**
	 * @return a list of the detector keys available through this provider.
	 */
	public List<String> getKeys() {
		return keys;
	}

	/**
	 * @return a list of the detector names available through this provider.
	 */
	public List<String> getNames() {
		return names;
	}

	/**
	 * @return a list of the detector informative texts available through this provider.
	 */
	public List<String> getInfoTexts() {
		return infoTexts;
	}
	
}
