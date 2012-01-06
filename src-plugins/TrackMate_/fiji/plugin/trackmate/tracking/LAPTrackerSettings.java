package fiji.plugin.trackmate.tracking;

import static fiji.plugin.trackmate.util.TMUtils.readBooleanAttribute;
import static fiji.plugin.trackmate.util.TMUtils.readDoubleAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.DataConversionException;
import org.jdom.Element;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.SimpleLAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerConfigurationPanel;

public class LAPTrackerSettings implements TrackerSettings {

	/*
	 * MARSHALLING CONSTANTS
	 */
	
	
	private static final String TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME				= "allowed";
	// Alternative costs & blocking
	private static final String TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME 		= "alternatecostfactor";
	private static final String TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME			= "cutoffpercentile";
	private static final String TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME				= "blockingvalue";
	// Cutoff elements
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT				= "TimeCutoff";
	private static final String TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME				= "value";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT			= "DistanceCutoff";
	private static final String TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME			= "value";
	private static final String TRACKER_SETTINGS_FEATURE_ELEMENT					= "FeatureCondition";
	private static final String TRACKER_SETTINGS_LINKING_ELEMENT					= "LinkingCondition";
	private static final String TRACKER_SETTINGS_GAP_CLOSING_ELEMENT				= "GapClosingCondition";
	private static final String TRACKER_SETTINGS_MERGING_ELEMENT					= "MergingCondition";
	private static final String TRACKER_SETTINGS_SPLITTING_ELEMENT					= "SplittingCondition";


	/*
	 * DEFAULT VALUE FOR PUBLIC FIELDS
	 */

	private static final double 	DEFAULT_LINKING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<String, Double> DEFAULT_LINKING_FEATURE_PENALITIES = new HashMap<String, Double>();

	private static final boolean 	DEFAULT_ALLOW_GAP_CLOSING 				= true;
	private static final double 	DEFAULT_GAP_CLOSING_TIME_CUTOFF 		= 4;
	private static final double 	DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF 	= 15.0;
	private static final HashMap<String, Double> DEFAULT_GAP_CLOSING_FEATURE_PENALTIES = new HashMap<String, Double>();

	private static final boolean 	DEFAULT_ALLOW_MERGING 					= false;
	private static final double 	DEFAULT_MERGING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_MERGING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<String, Double> DEFAULT_MERGING_FEATURE_PENALTIES = new HashMap<String, Double>();


	private static final boolean 	DEFAULT_ALLOW_SPLITTING 				= false;
	private static final double 	DEFAULT_SPLITTING_TIME_CUTOFF 			= 1;
	private static final double 	DEFAULT_SPLITTING_DISTANCE_CUTOFF 		= 15.0;
	private static final HashMap<String, Double> DEFAULT_SPLITTING_FEATURE_PENALTIES = new HashMap<String, Double>();

	private static final double 	DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR = 1.05d;
	private static final double 	DEFAULT_CUTOFF_PERCENTILE 				= 0.9d;

	/*
	 * PUBLIC FIELDS
	 */
	
	/** Max time difference over which particle linking is allowed.	 */
	public double linkingDistanceCutOff 		= DEFAULT_LINKING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for linking. */
	public Map<String, Double> linkingFeaturePenalties = DEFAULT_LINKING_FEATURE_PENALITIES; 

	/** Allow track segment gap closing? */
	public boolean allowGapClosing 				= DEFAULT_ALLOW_GAP_CLOSING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double gapClosingTimeCutoff 			= DEFAULT_GAP_CLOSING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double gapClosingDistanceCutoff 		= DEFAULT_GAP_CLOSING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for gap closing. */
	public Map<String, Double> gapClosingFeaturePenalties = DEFAULT_GAP_CLOSING_FEATURE_PENALTIES; 

	/** Allow track segment merging? */
	public boolean allowMerging 				= DEFAULT_ALLOW_MERGING;
	/** Max time difference over which segment gap closing is allowed.	 */
	public double mergingTimeCutoff 			= DEFAULT_MERGING_TIME_CUTOFF;
	/** Max distance over which segment gap closing is allowed. */
	public double mergingDistanceCutoff 		= DEFAULT_MERGING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for merging. */
	public Map<String, Double> mergingFeaturePenalties = DEFAULT_MERGING_FEATURE_PENALTIES; 

	/** Allow track segment splitting? */
	public boolean allowSplitting				= DEFAULT_ALLOW_SPLITTING;
	/** Max time difference over which segment splitting is allowed.	 */
	public double splittingTimeCutoff 			= DEFAULT_SPLITTING_TIME_CUTOFF;
	/** Max distance over which segment splitting is allowed. */
	public double splittingDistanceCutoff 		= DEFAULT_SPLITTING_DISTANCE_CUTOFF;
	/** Feature difference cutoffs for splitting. */
	public Map<String, Double> splittingFeaturePenalties = DEFAULT_SPLITTING_FEATURE_PENALTIES; 

	/** The factor used to create d and b in the paper, the alternative costs to linking objects. */
	public double alternativeObjectLinkingCostFactor = DEFAULT_ALTERNATIVE_OBJECT_LINKING_COST_FACTOR;
	/** The percentile used to calculate d and b cutoffs in the paper. */
	public double cutoffPercentile 				= DEFAULT_CUTOFF_PERCENTILE;

	/** Value used to block assignments when physically meaningless. */
	public double blockingValue 				= Double.MAX_VALUE;


	/*
	 * PRIVATE FIELDS
	 */
	
	private boolean useSimpleConfigPanel = false;

	/*
	 * METHODS
	 */

	@Override
	public String toString() {
		String str = "";
		str += "  Linking conditions:\n";
		str += String.format("    - distance cutoff: %.1f\n", linkingDistanceCutOff);
		str += echoFeatureCuttofs(linkingFeaturePenalties);

		if (allowGapClosing) {
			str += "  Gap-closing conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", gapClosingDistanceCutoff);
			str += String.format("    - max time interval: %.1f\n", gapClosingTimeCutoff);
			str += echoFeatureCuttofs(gapClosingFeaturePenalties);
		} else {
			str += "  Gap-closing not allowed.\n";
		}

		if (allowSplitting) {
			str += "  Track splitting conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", splittingDistanceCutoff);
			str += String.format("    - max time interval: %.1f\n", splittingTimeCutoff);
			str += echoFeatureCuttofs(splittingFeaturePenalties);
		} else {
			str += "  Track splitting not allowed.\n";
		}

		if (allowMerging) {
			str += "  Track merging conditions:\n";
			str += String.format("    - distance cutoff: %.1f\n", mergingDistanceCutoff);
			str += String.format("    - max time interval: %.1f\n", mergingTimeCutoff);
			str += echoFeatureCuttofs(mergingFeaturePenalties);
		} else {
			str += "  Track merging not allowed.\n";
		}

		return str;
	}

	@Override
	public TrackerConfigurationPanel createConfigurationPanel() {
		if (useSimpleConfigPanel) {
			return new SimpleLAPTrackerSettingsPanel();
		} else {
			return new LAPTrackerSettingsPanel();
		}
	}
	
	/**
	 * If the flag is set to true, then this settings object will return the simplified 
	 * config panel when called by {@link #createConfigurationPanel()}. Otherwise,
	 * the full, standard, config panel is used. 
	 */
	public void setUseSimpleConfigPanel(boolean useSimpleConfigPanel) {
		this.useSimpleConfigPanel = useSimpleConfigPanel;
	}

	@Override
	public void marshall(Element element) {

		element.setAttribute(TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME, 	""+alternativeObjectLinkingCostFactor);
		element.setAttribute(TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME, 		""+cutoffPercentile);
		element.setAttribute(TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME,			""+blockingValue);

		// Linking
		Element linkingElement = new Element(TRACKER_SETTINGS_LINKING_ELEMENT);
		linkingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+linkingDistanceCutOff));
		for(String feature : linkingFeaturePenalties.keySet())
			linkingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature, ""+linkingFeaturePenalties.get(feature)) );
		element.addContent(linkingElement);

		// Gap-closing
		Element gapClosingElement = new Element(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
		gapClosingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowGapClosing);
		gapClosingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+gapClosingDistanceCutoff));
		gapClosingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+gapClosingTimeCutoff));
		for(String feature : gapClosingFeaturePenalties.keySet())
			gapClosingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature, ""+gapClosingFeaturePenalties.get(feature)) );
		element.addContent(gapClosingElement);

		// Splitting
		Element splittingElement = new Element(TRACKER_SETTINGS_SPLITTING_ELEMENT);
		splittingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowSplitting);
		splittingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+splittingDistanceCutoff));
		splittingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+splittingTimeCutoff));
		for(String feature : splittingFeaturePenalties.keySet())
			splittingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature, ""+splittingFeaturePenalties.get(feature)) );
		element.addContent(splittingElement);

		// Merging
		Element mergingElement = new Element(TRACKER_SETTINGS_MERGING_ELEMENT);
		mergingElement.setAttribute(TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, ""+allowMerging);
		mergingElement.addContent(
				new Element(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME, ""+mergingDistanceCutoff));
		mergingElement.addContent(
				new Element(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
				.setAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME, ""+mergingTimeCutoff));
		for(String feature : mergingFeaturePenalties.keySet())
			mergingElement.addContent(
					new Element(TRACKER_SETTINGS_FEATURE_ELEMENT)
					.setAttribute(feature, ""+mergingFeaturePenalties.get(feature)) );
		element.addContent(mergingElement);

	}

	@Override
	public void unmarshall(Element element) {

		alternativeObjectLinkingCostFactor = readDoubleAttribute(element, TRACKER_SETTINGS_ALTERNATE_COST_FACTOR_ATTNAME, Logger.VOID_LOGGER);
		cutoffPercentile 			= readDoubleAttribute(element, TRACKER_SETTINGS_CUTOFF_PERCENTILE_ATTNAME, Logger.VOID_LOGGER);
		blockingValue				= readDoubleAttribute(element, TRACKER_SETTINGS_BLOCKING_VALUE_ATTNAME, Logger.VOID_LOGGER);
		// Linking
		Element linkingElement 		= element.getChild(TRACKER_SETTINGS_LINKING_ELEMENT);
		linkingDistanceCutOff 		= readDistanceCutoffAttribute(linkingElement);
		linkingFeaturePenalties 	= readTrackerFeatureMap(linkingElement);
		// Gap-closing
		Element gapClosingElement	= element.getChild(TRACKER_SETTINGS_GAP_CLOSING_ELEMENT);
		allowGapClosing	= readBooleanAttribute(gapClosingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
		gapClosingDistanceCutoff 	= readDistanceCutoffAttribute(gapClosingElement);
		gapClosingTimeCutoff 		= readTimeCutoffAttribute(gapClosingElement); 
		gapClosingFeaturePenalties 	= readTrackerFeatureMap(gapClosingElement);
		// Splitting
		Element splittingElement	= element.getChild(TRACKER_SETTINGS_SPLITTING_ELEMENT);
		allowSplitting				= readBooleanAttribute(splittingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
		splittingDistanceCutoff		= readDistanceCutoffAttribute(splittingElement);
		splittingTimeCutoff			= readTimeCutoffAttribute(splittingElement);
		splittingFeaturePenalties	= readTrackerFeatureMap(splittingElement);
		// Merging
		Element mergingElement 		= element.getChild(TRACKER_SETTINGS_MERGING_ELEMENT);
		allowMerging				= readBooleanAttribute(mergingElement, TRACKER_SETTINGS_ALLOW_EVENT_ATTNAME, Logger.VOID_LOGGER);
		mergingDistanceCutoff		= readDistanceCutoffAttribute(mergingElement);
		mergingTimeCutoff			= readTimeCutoffAttribute(mergingElement);
		mergingFeaturePenalties		= readTrackerFeatureMap(mergingElement);

	}


	/*
	 * PRIVATE METHODS
	 */

	private static String echoFeatureCuttofs(final Map<String, Double> featurePenalties) {
		String str = "";
		if (featurePenalties.isEmpty()) 
			str += "    - no feature penalties\n";
		else {
			str += "    - with feature penalties:\n";
			for (String feature : featurePenalties.keySet()) {
				str += "      - "+feature.toString() + ": weight = " + String.format("%.1f", featurePenalties.get(feature)) + '\n';
			}
		}
		return str;

	}


	private static final double readDistanceCutoffAttribute(Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_DISTANCE_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_DISTANCE_CUTOFF_ATTNAME).getDoubleValue();
		} catch (DataConversionException e) { }
		return val;
	}

	private static final double readTimeCutoffAttribute(Element element) {
		double val = 0;
		try {
			val = element.getChild(TRACKER_SETTINGS_TIME_CUTOFF_ELEMENT)
					.getAttribute(TRACKER_SETTINGS_TIME_CUTOFF_ATTNAME).getDoubleValue();
		} catch (DataConversionException e) { }
		return val;
	}

	/**
	 * Look for all the sub-elements of <code>element</code> with the name TRACKER_SETTINGS_FEATURE_ELEMENT, 
	 * fetch the feature attributes from them, and returns them in a map.
	 */
	@SuppressWarnings("unchecked")
	private static final Map<String, Double> readTrackerFeatureMap(final Element element) {
		Map<String, Double> map = new HashMap<String, Double>();
		List<Element> featurelinkingElements = element.getChildren(TRACKER_SETTINGS_FEATURE_ELEMENT);
		for (Element el : featurelinkingElements) {
			List<Attribute> atts = el.getAttributes();
			for (Attribute att : atts) {
				String feature = att.getName();
				Double cutoff;
				try {
					cutoff = att.getDoubleValue();
				} catch (DataConversionException e) {
					cutoff = 0d;
				}
				map.put(feature, cutoff);
			}
		}
		return map;
	}
}
