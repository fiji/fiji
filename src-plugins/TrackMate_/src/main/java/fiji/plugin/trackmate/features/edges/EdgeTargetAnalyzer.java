package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.FeatureModel;

public class EdgeTargetAnalyzer <T extends RealType<T> & NativeType<T>>  implements EdgeFeatureAnalyzer {

	public static final String KEY = "TARGET";
	/*
	 * FEATURE NAMES 
	 */
	private static final String SPOT1_NAME = "SPOT1_NAME";
	private static final String SPOT2_NAME = "SPOT1_NAME";
	private static final String SPOT1_ID = "SPOT1_ID";
	private static final String SPOT2_ID = "SPOT2_ID";
	private static final String EDGE_COST = "COST";
	private static final String TRACK_ID = "TRACK_ID";
	
	private final TrackMateModel<T> model;
	private final FeatureModel<T> featureModel;
	
	public static final List<String> FEATURES = new ArrayList<String>(6);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(6);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(6);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(6);
	
	static {
		FEATURES.add(SPOT1_NAME);
		FEATURES.add(SPOT2_NAME);
		FEATURES.add(SPOT1_ID);
		FEATURES.add(SPOT2_ID);
		FEATURES.add(EDGE_COST);
		FEATURES.add(TRACK_ID);
		
		FEATURE_NAMES.put(SPOT1_NAME, "Source spot name");
		FEATURE_NAMES.put(SPOT2_NAME, "Target spot name");
		FEATURE_NAMES.put(SPOT1_ID, "Source spot ID");
		FEATURE_NAMES.put(SPOT2_ID, "Target spot ID");
		FEATURE_NAMES.put(EDGE_COST, "Link cost");
		FEATURE_NAMES.put(TRACK_ID, "Track ID");
		
		FEATURE_SHORT_NAMES.put(SPOT1_NAME, "Source");
		FEATURE_SHORT_NAMES.put(SPOT2_NAME, "Target");
		FEATURE_SHORT_NAMES.put(SPOT1_ID, "Source ID");
		FEATURE_SHORT_NAMES.put(SPOT2_ID, "Target ID");
		FEATURE_SHORT_NAMES.put(EDGE_COST, "Cost");
		FEATURE_SHORT_NAMES.put(TRACK_ID, "Track");
		
		FEATURE_DIMENSIONS.put(SPOT1_NAME, Dimension.STRING);
		FEATURE_DIMENSIONS.put(SPOT2_NAME, Dimension.STRING);
		FEATURE_DIMENSIONS.put(SPOT1_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(SPOT2_ID, Dimension.NONE);
		FEATURE_DIMENSIONS.put(EDGE_COST, Dimension.NONE);
		FEATURE_DIMENSIONS.put(TRACK_ID, Dimension.NONE);
	}

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTargetAnalyzer(final TrackMateModel<T> model) {
		this.model = model;
		this.featureModel = model.getFeatureModel();
	}

	@Override
	public void process(final DefaultWeightedEdge edge) {
		// Edge weight
		featureModel.putEdgeFeature(edge, EDGE_COST, model.getEdgeWeight(edge));
		// Source & target name & ID
		Spot source = model.getEdgeSource(edge);
		featureModel.putEdgeFeature(edge, SPOT1_NAME, source.getName());
		featureModel.putEdgeFeature(edge, SPOT1_ID, source.ID());
		Spot target = model.getEdgeTarget(edge);
		featureModel.putEdgeFeature(edge, SPOT2_NAME, target.getName());
		featureModel.putEdgeFeature(edge, SPOT2_ID, target.ID());
		// Track it belong to
		int trackId = -1;
		final Map<Integer,Set<DefaultWeightedEdge>> tracks = model.getTrackEdges();
		for (int trackID : tracks.keySet()) {
			if (tracks.get(trackID).contains(edge)) {
				featureModel.putEdgeFeature(edge, TRACK_ID, trackId);
				break;
			}
		}
	}

}
