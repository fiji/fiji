package fiji.plugin.trackmate.features.edges;

import java.util.List;
import java.util.Set;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jgrapht.graph.DefaultWeightedEdge;

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
	private static final String TRACK_ID = "TRACK";

	private final TrackMateModel<T> model;
	private final FeatureModel<T> featureModel;

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
		final List<Set<DefaultWeightedEdge>> tracks = model.getTrackEdges();
		for (int i = 0; i < tracks.size(); i++) {
			if (tracks.contains(edge)) {
				trackId = i;
				break;
			}
		}
		featureModel.putEdgeFeature(edge, TRACK_ID, trackId);

	}

}
