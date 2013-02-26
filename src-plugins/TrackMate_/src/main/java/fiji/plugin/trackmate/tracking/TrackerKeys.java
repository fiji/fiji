package fiji.plugin.trackmate.tracking;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.Spot;

public class TrackerKeys {

	/*
	 * MARSHALLING CONSTANTS
	 */
	
	
	public static final String TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME				= "allowed";
	// Alternative costs & blocking
	public static final String TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME 	= "alternatecostfactor";
	public static final String TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME		= "cutoffpercentile";
	public static final String TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME			= "blockingvalue";
	// Cutoff elements
	public static final String TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT				= "TimeCutoff";
	public static final String TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME				= "value";
	public static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT			= "DistanceCutoff";
	public static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME			= "value";
	public static final String TRACKER_SETTINGS_FEATURE_ELEMENT					= "FeatureCondition";
	public static final String TRACKER_SETTINGS_LINKING_ELEMENT					= "LinkingCondition";
	public static final String TRACKER_SETTINGS_GAP_CLOSING_ELEMENT				= "GapClosingCondition";
	public static final String TRACKER_SETTINGS_MERGING_ELEMENT					= "MergingCondition";
	public static final String TRACKER_SETTINGS_SPLITTING_ELEMENT				= "SplittingCondition";


	/*
	 *
	 */
	
	/**
	 * The attribute name for the {@link SpotTracker} key when
	 * marshalling to or unmarhsalling from XML. 
	 */
	public static final String XML_ATTRIBUTE_TRACKER_NAME = "TRACKER_NAME";
	
	/** Key for the parameter specifying the maximal linking distance. The expected value must be a 
	 * Double and should be expressed in physical units.	 */
	public static final String KEY_LINKING_MAX_DISTANCE = "LINKING_MAX_DISTANCE";

	/** A default value for the {@value #KEY_MAX_LINKING_DISTANCE} parameter. */
	public static final double 	DEFAULT_LINKING_MAX_DISTANCE = 15.0;
	
	/** Key for the parameter specifying the feature penalties when linking particles. 
	 * Expected values should be a {@link Map<String, Double>} where the map keys are 
	 * spot feature names. 
	 * @see Spot#getFeature(String)	 */
	public static final String KEY_LINKING_FEATURE_PENALTIES = "LINKING_FEATURE_PENALTIES";
	
	/** A default value for the {@value #KEY_LINKING_FEATURE_PENALTIES} parameter.*/
	public static final Map<String, Double> DEFAULT_LINKING_FEATURE_PENALTIES = new HashMap<String, Double>();

	/** Key for the parameter specifying whether we allow the detection of gap-closing events.
	 * Expected values are {@link Boolean}s. */
	public static final String KEY_ALLOW_GAP_CLOSING = "ALLOW_GAP_CLOSING";
	
	/** A default value for the {@value #KEY_ALLOW_GAP_CLOSING} parameter. */
	public static final boolean DEFAULT_ALLOW_GAP_CLOSING = true;
	
	/** Key for the parameter that specify the maximal number of frames to bridge when 
	 * detecting gap closing. Expected values are {@link Integer}s greater than 0.
	 * A value of 1 means that a detection might be missed in 1 frame, and the track will
	 * not be broken. And so on.  */
	public static final String KEY_GAP_CLOSING_MAX_FRAME_GAP = "MAX_FRAME_GAP";
	
	/** A default value for the {@value #KEY_GAP_CLOSING_MAX_FRAME_GAP} parameter. */
	public static final int DEFAULT_GAP_CLOSING_MAX_FRAME_GAP = 2;
	
	/** Key for the parameter specifying the max gap-closing distance. Expected values 
	 * are {@link Double}s and should be expressed in physical units. If two spots, candidate
	 * for a gap-closing event, are found separated by a distance larger than this parameter
	 * value, gap-closing will not occur. */
	public static final String KEY_GAP_CLOSING_MAX_DISTANCE = "GAP_CLOSING_MAX_DISTANCE";
	
	/** A default value for the {@value #KEY_GAP_CLOSING_MAX_DISTANCE} parameter. */
	public static final double 	DEFAULT_GAP_CLOSING_MAX_DISTANCE = 15.0;
	
	/** Key for the parameter specifying the feature penalties when detecting gap-closing
	 * events. Expected values should be a {@link Map<String, Double>} where the map keys are 
	 * spot feature names. 
	 * @see Spot#getFeature(String)	 */
	public static final String KEY_GAP_CLOSING_FEATURE_PENALTIES = "GAP_CLOSING_FEATURE_PENALTIES";
	
	/** A default value for the {@value #KEY_GAP_CLOSING_FEATURE_PENALTIES} parameter. */
	public static final Map<String, Double> DEFAULT_GAP_CLOSING_FEATURE_PENALTIES = new HashMap<String, Double>();

	/** Key for the parameter specifying whether we allow the detection of merging events.
	 * Expected values are {@link Boolean}s. */
	public static final String KEY_ALLOW_TRACK_MERGING = "ALLOW_TRACK_MERGING";
	
	/** A default value for the {@value #KEY_ALLOW_TRACK_MERGING} parameter. */
	public static final boolean DEFAULT_ALLOW_TRACK_MERGING = false;

	/** Key for the parameter specifying the max merging distance. Expected values 
	 * are {@link Double}s and should be expressed in physical units. If two spots, candidate
	 * for a merging event, are found separated by a distance larger than this parameter
	 * value, track merging will not occur. */ 
	public static final String KEY_MERGING_MAX_DISTANCE = "MERGING_MAX_DISTANCE";
	
	/** A default value for the {@value #KEY_MERGING_MAX_DISTANCE} parameter. */
	public static final double 	DEFAULT_MERGING_MAX_DISTANCE 		= 15.0;
	
	/** Key for the parameter specifying the feature penalties when dealing with merging events. 
	 * Expected values should be a {@link Map<String, Double>} where the map keys are 
	 * spot feature names. 
	 * @see Spot#getFeature(String)	 */
	public static final String KEY_MERGING_FEATURE_PENALTIES = "MERGING_FEATURE_PENALTIES";
	
	/** A default value for the {@value #KEY_MERGING_FEATURE_PENALTIES} parameter.  */
	public static final Map<String, Double> DEFAULT_MERGING_FEATURE_PENALTIES = new HashMap<String, Double>();

	/** Key for the parameter specifying whether we allow the detection of splitting events.
	 * Expected values are {@link Boolean}s. */
	public static final String KEY_ALLOW_TRACK_SPLITTING = "ALLOW_TRACK_SPLITTING";

	/** A default value for the {@value #KEY_ALLOW_TRACK_SPLITTING} parameter. */
	public static final boolean 	DEFAULT_ALLOW_TRACK_SPLITTING 				= false;

	/** Key for the parameter specifying the max splitting distance. Expected values 
	 * are {@link Double}s and should be expressed in physical units. If two spots, candidate
	 * for a merging event, are found separated by a distance larger than this parameter
	 * value, track splitting will not occur. */ 
	public static final String KEY_SPLITTING_MAX_DISTANCE = "SPLITTING_MAX_DISTANCE";
	
	/** A default valeu for the {@link #KEY_SPLITTING_MAX_DISTANCE} parameter. */
	public static final double 	DEFAULT_SPLITTING_MAX_DISTANCE 		= 15.0;
	
	/** Key for the parameter specifying the feature penalties when dealing with splitting events. 
	 * Expected values should be a {@link Map<String, Double>} where the map keys are 
	 * spot feature names. 
	 * @see Spot#getFeature(String)	 */
	public static final String KEY_SPLITTING_FEATURE_PENALTIES = "SPLITTING_FEATURE_PENALTIES";
	
	/** A default value for the {@value #KEY_SPLITTING_FEATURE_PENALTIES} parameter.  */
	public static final Map<String, Double> DEFAULT_SPLITTING_FEATURE_PENALTIES = new HashMap<String, Double>();

	/** Key for the parameter specifying the factor used to compute alternative linking costs.
	 * Expected values are {@link Double}s.  */
	public static final String KEY_ALTERNATIVE_LINKING_COST_FACTOR = "ALTERNATIVE_LINKING_COST_FACTOR";
	
	/** A default value for the {@value #KEY_ALTERNATIVE_LINKING_COST_FACTOR} parameter. */
	public static final double 	DEFAULT_ALTERNATIVE_LINKING_COST_FACTOR = 1.05d;
	
	/** Key for the cutoff percentile parameter. Exepcted values are {@link Double}s.  */
	public static final String KEY_CUTOFF_PERCENTILE = "CUTOFF_PERCENTILE";
	
	/** A default value for the {@value #KEY_CUTOFF_PERCENTILE} parameter. */
	public static final double DEFAULT_CUTOFF_PERCENTILE = 0.9d;
	
	/** Key for the parameter that stores the blocking value: cost for non-physical, forbidden links.
	 * Expected values are {@link Double}s, and are typically very large.  */
	public static final String KEY_BLOCKING_VALUE = "BLOCKING_VALUE";
	
	/** A default value for the {@value #KEY_BLOCKING_VALUE} parameter.  */ 
	public static final double 	DEFAULT_BLOCKING_VALUE = Double.POSITIVE_INFINITY;
	
	/*
	 * METHODS
	 */

//	@Override
//	public String toString() {
//		String str = "";
//		str += "  Linking conditions:\n";
//		str += String.format("    - distance cutoff: %.1f\n", linkingDistanceCutOff);
//		str += echoFeatureCuttofs(linkingFeaturePenalties);
//
//		if (allowGapClosing) {
//			str += "  Gap-closing conditions:\n";
//			str += String.format("    - distance cutoff: %.1f\n", gapClosingDistanceCutoff);
//			str += String.format("    - max time interval: %.1f\n", gapClosingTimeCutoff);
//			str += echoFeatureCuttofs(gapClosingFeaturePenalties);
//		} else {
//			str += "  Gap-closing not allowed.\n";
//		}
//
//		if (allowSplitting) {
//			str += "  Track splitting conditions:\n";
//			str += String.format("    - distance cutoff: %.1f\n", splittingDistanceCutoff);
//			str += String.format("    - max time interval: %.1f\n", splittingTimeCutoff);
//			str += echoFeatureCuttofs(splittingFeaturePenalties);
//		} else {
//			str += "  Track splitting not allowed.\n";
//		}
//
//		if (allowMerging) {
//			str += "  Track merging conditions:\n";
//			str += String.format("    - distance cutoff: %.1f\n", mergingDistanceCutoff);
//			str += String.format("    - max time interval: %.1f\n", mergingTimeCutoff);
//			str += echoFeatureCuttofs(mergingFeaturePenalties);
//		} else {
//			str += "  Track merging not allowed.\n";
//		}
//
//		return str;
//	}
//
//	@Override
//	public void marshall(Element element) {
//
//		element.setAttribute(TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME, 	""+alternativeObjectLinkingCostFactor);
//		element.setAttribute(TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME, 		""+cutoffPercentile);
//		element.setAttribute(TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME,			""+blockingValue);
//
//		// Linking
//		Element linkingElement = new Element(TRACKER_SETTINGS_LINKING_ELEMENT);
//		linkingElement.addContent(
//				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+linkingDistanceCutOff));
//		for(String feature : linkingFeaturePenalties.keySet())
//			linkingElement.addContent(
//					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
//					.setAttribute(feature, ""+linkingFeaturePenalties.get(feature)) );
//		element.addContent(linkingElement);
//
//		// Gap-closing
//		Element gapClosingElement = new Element(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
//		gapClosingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowGapClosing);
//		gapClosingElement.addContent(
//				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+gapClosingDistanceCutoff));
//		gapClosingElement.addContent(
//				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+gapClosingTimeCutoff));
//		for(String feature : gapClosingFeaturePenalties.keySet())
//			gapClosingElement.addContent(
//					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
//					.setAttribute(feature, ""+gapClosingFeaturePenalties.get(feature)) );
//		element.addContent(gapClosingElement);
//
//		// Splitting
//		Element splittingElement = new Element(TRACKER_SETTINGS_SPLITTING_ELEMENT);
//		splittingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowSplitting);
//		splittingElement.addContent(
//				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+splittingDistanceCutoff));
//		splittingElement.addContent(
//				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+splittingTimeCutoff));
//		for(String feature : splittingFeaturePenalties.keySet())
//			splittingElement.addContent(
//					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
//					.setAttribute(feature, ""+splittingFeaturePenalties.get(feature)) );
//		element.addContent(splittingElement);
//
//		// Merging
//		Element mergingElement = new Element(TRACKER_SETTINGS_MERGING_ELEMENT);
//		mergingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowMerging);
//		mergingElement.addContent(
//				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+mergingDistanceCutoff));
//		mergingElement.addContent(
//				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
//				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+mergingTimeCutoff));
//		for(String feature : mergingFeaturePenalties.keySet())
//			mergingElement.addContent(
//					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
//					.setAttribute(feature, ""+mergingFeaturePenalties.get(feature)) );
//		element.addContent(mergingElement);
//
//	}
//
//	@Override
//	public void unmarshall(Element element) {
//
//		alternativeObjectLinkingCostFactor = readDoubleAttribute(element, TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME, Logger.VOID_LOGGER);
//		cutoffPercentile 			= readDoubleAttribute(element, TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME, Logger.VOID_LOGGER);
//		blockingValue				= readDoubleAttribute(element, TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME, Logger.VOID_LOGGER);
//		// Linking
//		Element linkingElement 		= element.getChild(TRACKER_SETTINGS_LINKING_ELEMENT);
//		linkingDistanceCutOff 		= readDistanceCutoffAttribute(linkingElement);
//		linkingFeaturePenalties 	= readTrackerFeatureMap(linkingElement);
//		// Gap-closing
//		Element gapClosingElement	= element.getChild(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
//		allowGapClosing	= readBooleanAttribute(gapClosingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
//		gapClosingDistanceCutoff 	= readDistanceCutoffAttribute(gapClosingElement);
//		gapClosingTimeCutoff 		= readTimeCutoffAttribute(gapClosingElement); 
//		gapClosingFeaturePenalties 	= readTrackerFeatureMap(gapClosingElement);
//		// Splitting
//		Element splittingElement	= element.getChild(TRACKER_SETTINGS_SPLITTING_ELEMENT);
//		allowSplitting				= readBooleanAttribute(splittingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
//		splittingDistanceCutoff		= readDistanceCutoffAttribute(splittingElement);
//		splittingTimeCutoff			= readTimeCutoffAttribute(splittingElement);
//		splittingFeaturePenalties	= readTrackerFeatureMap(splittingElement);
//		// Merging
//		Element mergingElement 		= element.getChild(TRACKER_SETTINGS_MERGING_ELEMENT);
//		allowMerging				= readBooleanAttribute(mergingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
//		mergingDistanceCutoff		= readDistanceCutoffAttribute(mergingElement);
//		mergingTimeCutoff			= readTimeCutoffAttribute(mergingElement);
//		mergingFeaturePenalties		= readTrackerFeatureMap(mergingElement);
//
//	}
//
//
//	/*
//	 * PRIVATE METHODS
//	 */
//
//	public static String echoFeatureCuttofs(final Map<String, Double> featurePenalties) {
//		String str = "";
//		if (featurePenalties.isEmpty()) 
//			str += "    - no feature penalties\n";
//		else {
//			str += "    - with feature penalties:\n";
//			for (String feature : featurePenalties.keySet()) {
//				str += "      - "+feature.toString() + ": weight = " + String.format("%.1f", featurePenalties.get(feature)) + '\n';
//			}
//		}
//		return str;
//
//	}
//
//
//	public static final double readDistanceCutoffAttribute(Element element) {
//		double val = 0;
//		try {
//			val = element.getChild(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
//					.getAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME).getDoubleValue();
//		} catch (DataConversionException e) { }
//		return val;
//	}
//
//	public static final double readTimeCutoffAttribute(Element element) {
//		double val = 0;
//		try {
//			val = element.getChild(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
//					.getAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME).getDoubleValue();
//		} catch (DataConversionException e) { }
//		return val;
//	}
//
//	/**
//	 * Look for all the sub-elements of <code>element</code> with the name TRACKER_SETTINGS_FEATURE_ELEMENT, 
//	 * fetch the feature attributes from them, and returns them in a map.
//	 */
//	@SuppressWarnings("unchecked")
//	public static final Map<String, Double> readTrackerFeatureMap(final Element element) {
//		Map<String, Double> map = new HashMap<String, Double>();
//		List<Element> featurelinkingElements = element.getChildren(TRACKER_SETTINGS_FEATURE_ELEMENT);
//		for (Element el : featurelinkingElements) {
//			List<Attribute> atts = el.getAttributes();
//			for (Attribute att : atts) {
//				String feature = att.getName();
//				Double cutoff;
//				try {
//					cutoff = att.getDoubleValue();
//				} catch (DataConversionException e) {
//					cutoff = 0d;
//				}
//				map.put(feature, cutoff);
//			}
//		}
//		return map;
//	}
}
