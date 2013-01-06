package fiji.plugin.trackmate;

import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.DEFAULT_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DOWNSAMPLE_FACTOR;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_MEDIAN_FILTERING;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_DO_SUBPIXEL_LOCALIZATION;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_RADIUS;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.detection.DetectorKeys.XML_ATTRIBUTE_DETECTOR_NAME;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jdom2.Element;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.detection.DogDetectorFactory;
import fiji.plugin.trackmate.detection.DownsampleLogDetectorFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.detection.ManualDetectorFactory;
import fiji.plugin.trackmate.detection.SpotDetector;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.BasicDetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.gui.DownSampleLogDetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.LogDetectorConfigurationPanel;
import fiji.plugin.trackmate.gui.WizardController;

public class DetectorProvider extends AbstractProvider {

	/*
	 * BLANK CONSTRUCTOR
	 */

	/**
	 * This provider provides the GUI with the spot detectors currently available in the 
	 * TrackMate plugin. Each detector is identified by a key String, which can be used 
	 * to retrieve new instance of the detector, settings for the target detector and a 
	 * GUI panel able to configure these settings.
	 * <p>
	 * If you want to add custom detectors to TrackMate, a simple way is to extend this
	 * factory so that it is registered with the custom detectors and pass this 
	 * extended provider to the {@link TrackMate_} plugin.
	 */
	public DetectorProvider() {
		registerDetectors();
		currentKey = LogDetectorFactory.DETECTOR_KEY;
	}


	/*
	 * METHODS
	 */

	/**
	 * Register the standard detectors shipped with TrackMate.
	 */
	protected void registerDetectors() {
		// keys
		keys = new ArrayList<String>(4);
		keys.add(LogDetectorFactory.DETECTOR_KEY);
		keys.add(DogDetectorFactory.DETECTOR_KEY);
		keys.add(DownsampleLogDetectorFactory.DETECTOR_KEY);
		keys.add(ManualDetectorFactory.DETECTOR_KEY);
		// names
		names = new ArrayList<String>(4);
		names.add(LogDetectorFactory.NAME);
		names.add(DogDetectorFactory.NAME);
		names.add(DownsampleLogDetectorFactory.NAME);
		names.add(ManualDetectorFactory.NAME);
		// infoTexts
		infoTexts = new ArrayList<String>(4);
		infoTexts.add(LogDetectorFactory.INFO_TEXT);
		infoTexts.add(DogDetectorFactory.INFO_TEXT);
		infoTexts.add(DownsampleLogDetectorFactory.INFO_TEXT);
		infoTexts.add(ManualDetectorFactory.INFO_TEXT);
	}

	/**
	 * Marshall a settings map to a JDom element, ready for saving to XML. 
	 * The element is <b>updated</b> with new attributes.
	 * <p>
	 * Only parameters specific to the target detector factory are marshalled.
	 * The element also always receive an attribute named {@value DetectorKeys#XML_ATTRIBUTE_DETECTOR_NAME}
	 * that saves the target {@link SpotDetectorFactory} key.
	 * 
	 * @return true if marshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean marshall(final Map<String, Object> settings, Element element) {

		element.setAttribute(XML_ATTRIBUTE_DETECTOR_NAME, currentKey);

		if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {

			return writeRadius(settings, element);

		} else if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY) 
				|| currentKey.equals(DogDetectorFactory.DETECTOR_KEY)) {

			return writeTargetChannel(settings, element)
					&& writeRadius(settings, element) 
					&& writeThreshold(settings, element)
					&& writeDoMedian(settings, element)
					&& writeDoSubPixel(settings, element);

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {

			return writeTargetChannel(settings, element)
					&& writeRadius(settings, element) 
					&& writeThreshold(settings, element) 
					&& writeDownsamplingFactor(settings, element);

		} else {

			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;

		}
	}

	/**
	 * Un-marshall a JDom element to update a settings map, and sets the target 
	 * detector factory of this provider from the element. 
	 * <p>
	 * Concretely: the detector key is read from the element, and is used to set 
	 * the target {@link #currentKey} of this provider. The the specific settings 
	 * map for the targeted detector factory is updated from the element.
	 * 
	 * @param element the JDom element to read from.
	 * @param settings the map to update. Is cleared prior to updating, so that it contains
	 * only the parameters specific to the target detector factory.
	 * @return true if unmarshalling was successful. If not, check {@link #getErrorMessage()}
	 */
	public boolean unmarshall(final Element element, Map<String, Object> settings) {
		
		settings.clear();

		String detectorKey = element.getAttributeValue(XML_ATTRIBUTE_DETECTOR_NAME);
		// Try to set the state of this provider from the key read in xml.
		boolean ok = select(detectorKey);
		if (!ok) {
			errorMessage = "Detector key found in XML ("+detectorKey+") is unknown to this provider.\n";
			return false;
		}

		StringBuilder errorHolder = new StringBuilder();
		
		if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {

			return readDoubleAttribute(element, settings, KEY_RADIUS, errorHolder);

		} else if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY) 
				|| currentKey.equals(DogDetectorFactory.DETECTOR_KEY)) {

			ok = true;
			ok = ok & readDoubleAttribute(element, settings, KEY_RADIUS, errorHolder);
			ok = ok & readDoubleAttribute(element, settings, KEY_THRESHOLD, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_DO_SUBPIXEL_LOCALIZATION, errorHolder);
			ok = ok & readBooleanAttribute(element, settings, KEY_DO_MEDIAN_FILTERING, errorHolder);
			ok = ok & readIntegerAttribute(element, settings, KEY_TARGET_CHANNEL, errorHolder);

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {

			ok = ok & readDoubleAttribute(element, settings, KEY_RADIUS, errorHolder);
			ok = ok & readDoubleAttribute(element, settings, KEY_THRESHOLD, errorHolder);
			ok = ok & readIntegerAttribute(element, settings, KEY_DOWNSAMPLE_FACTOR, errorHolder);
			ok = ok & readIntegerAttribute(element, settings, KEY_TARGET_CHANNEL, errorHolder);

		} else {

			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;
		}
		
		if (!checkSettingsValidity(settings)) {
			ok = false;
			errorHolder.append(errorMessage); // get the error from validoty check
		}
		
		if (!ok) {
			errorMessage = errorHolder.toString();
		}
		
		return ok;
	}




	/**
	 * @return a new instance of the target detector identified by the key parameter. If 
	 * the key is unknown to this provider, return <code>null</code>.
	 */
	@SuppressWarnings("rawtypes")
	public SpotDetectorFactory getDetectorFactory() {

		if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY)) {
			return new LogDetectorFactory();

		} else if (currentKey.equals(DogDetectorFactory.DETECTOR_KEY)){
			return new DogDetectorFactory();

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {
			return new DownsampleLogDetectorFactory();

		} else if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {
			return new ManualDetectorFactory();

		} else {
			return null;
		}
	}

	/**
	 * @return a new default settings map suitable for the target detector identified by 
	 * the {@link #currentKey}. Settings are instantiated with default values.  
	 * If the key is unknown to this provider, <code>null</code> is returned. 
	 */
	public Map<String, Object> getDefaultSettings() {
		Map<String, Object> settings = new HashMap<String, Object>();

		if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY) 
				|| currentKey.equals(DogDetectorFactory.DETECTOR_KEY)) {
			settings.put(KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL);
			settings.put(KEY_RADIUS, DEFAULT_RADIUS);
			settings.put(KEY_THRESHOLD, DEFAULT_THRESHOLD);
			settings.put(KEY_DO_MEDIAN_FILTERING, DEFAULT_DO_MEDIAN_FILTERING);
			settings.put(KEY_DO_SUBPIXEL_LOCALIZATION, DEFAULT_DO_SUBPIXEL_LOCALIZATION);

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {
			settings.put(KEY_TARGET_CHANNEL, DEFAULT_TARGET_CHANNEL);
			settings.put(KEY_RADIUS, DEFAULT_RADIUS);
			settings.put(KEY_THRESHOLD, DEFAULT_THRESHOLD);
			settings.put(KEY_DOWNSAMPLE_FACTOR, DEFAULT_DOWNSAMPLE_FACTOR);

		} else if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {
			settings.put(KEY_RADIUS, DEFAULT_RADIUS);

		} else {
			return null;
		}

		return settings;

	}

	/**
	 * @return the html String containing a descriptive information about the target detector,
	 * or <code>null</code> if it is unknown to this provider.
	 */
	public String getInfoText() {

		if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY)) {
			return LogDetectorFactory.INFO_TEXT;

		} else if (currentKey.equals(DogDetectorFactory.DETECTOR_KEY)){
			return DogDetectorFactory.INFO_TEXT;

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {
			return DownsampleLogDetectorFactory.INFO_TEXT;

		} else if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {
			return ManualDetectorFactory.INFO_TEXT;

		} else {
			return null;
		}
	}

	/**
	 * @return a new GUI panel able to configure the settings suitable for the target detector 
	 * factory. If the key is unknown to this provider, <code>null</code> is returned.
	 * The wizard controller parameter is used to retrieve GUI context when instantiating configuration
	 * panels. 
	 */

	public ConfigurationPanel getDetectorConfigurationPanel(final WizardController controller) 	{
		
		ImagePlus imp = controller.getPlugin().getModel().getSettings().imp;
		String spaceUnits = controller.getPlugin().getModel().getSettings().spaceUnits;
		
		if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY)) {
			return new LogDetectorConfigurationPanel(imp, LogDetectorFactory.INFO_TEXT, LogDetectorFactory.NAME, spaceUnits);

		} else if (currentKey.equals(DogDetectorFactory.DETECTOR_KEY)){
			return new LogDetectorConfigurationPanel(imp, DogDetectorFactory.INFO_TEXT, DogDetectorFactory.NAME, spaceUnits);

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {
			return new DownSampleLogDetectorConfigurationPanel(imp, spaceUnits);

		} else if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {
			return new BasicDetectorConfigurationPanel(imp, ManualDetectorFactory.INFO_TEXT, ManualDetectorFactory.NAME, spaceUnits);

		} else {
			return null;
		}
	}

	/**
	 * Check the validity of the given settings map for the target {@link SpotDetector}
	 * set in this provider. The validity check is strict: we check that all needed parameters
	 * are here and are of the right class, and that there is no extra unwanted parameters.
	 * @return  true if the settings map can be used with the target factory. If not, check {@link #getErrorMessage()}
	 */
	public boolean checkSettingsValidity(final Map<String, Object> settings) {
		if (null == settings) {
			errorMessage = "Settings map is null.\n";
			return false;
		}

		StringBuilder errorHolder = new StringBuilder();
		if (currentKey.equals(ManualDetectorFactory.DETECTOR_KEY)) {

			boolean ok = ManualDetectorFactory.checkInput(settings, errorHolder);
			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else if (currentKey.equals(LogDetectorFactory.DETECTOR_KEY) 
				|| currentKey.equals(DogDetectorFactory.DETECTOR_KEY)) {

			boolean ok = LogDetectorFactory.checkInput(settings, errorHolder);
			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else if (currentKey.equals(DownsampleLogDetectorFactory.DETECTOR_KEY)) {

			boolean ok = DownsampleLogDetectorFactory.checkInput(settings, errorHolder);
			if (!ok) {
				errorMessage = errorHolder.toString();
			}
			return ok;

		} else {

			errorMessage = "Unknow detector factory key: "+currentKey+".\n";
			return false;

		}
	}

	/*
	 * PROTECTED METHODS
	 */

	protected boolean writeTargetChannel(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_TARGET_CHANNEL, Integer.class);
	}

	protected boolean writeRadius(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_RADIUS, Double.class);
	}

	protected boolean writeThreshold(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_THRESHOLD, Double.class);
	}

	protected boolean writeDoMedian(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_DO_MEDIAN_FILTERING, Boolean.class);
	}

	protected boolean writeDoSubPixel(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_DO_SUBPIXEL_LOCALIZATION, Boolean.class);
	}

	protected boolean writeDownsamplingFactor(final Map<String, Object> settings, Element element) {
		return writeAttribute(settings, element, KEY_DOWNSAMPLE_FACTOR, Integer.class);
	}
}
