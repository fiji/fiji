package fiji.plugin.trackmate.tracking;

import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.gui.LAPTrackerSettingsPanel;
import fiji.plugin.trackmate.gui.TrackerSettingsPanel;

public class TrackerSettings {
	
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
	
	public TrackerSettingsPanel createConfigurationPanel() {
		return new LAPTrackerSettingsPanel();
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


}
