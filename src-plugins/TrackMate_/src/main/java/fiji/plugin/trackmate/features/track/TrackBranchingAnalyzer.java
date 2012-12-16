package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TrackSplitter;

public class TrackBranchingAnalyzer<T extends RealType<T> & NativeType<T>> implements TrackFeatureAnalyzer<T> {

	/*
	 * CONSTANTS
	 */
	public static final String KEY = "BRANCHING";
	public static final String 		NUMBER_GAPS = "NUMBER_GAPS";
	public static final String 		NUMBER_SPLITS = "NUMBER_SPLITS";
	public static final String 		NUMBER_MERGES = "NUMBER_MERGES";
	public static final String 		NUMBER_COMPLEX = "NUMBER_COMPLEX";
	public static final String 		NUMBER_SPOTS = "NUMBER_SPOTS";
	
	public static final List<String> FEATURES = new ArrayList<String>(5);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(5);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(5);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(5);
	
	static {
		FEATURES.add(NUMBER_SPOTS);
		FEATURES.add(NUMBER_GAPS);
		FEATURES.add(NUMBER_SPLITS);
		FEATURES.add(NUMBER_MERGES);
		FEATURES.add(NUMBER_COMPLEX);
		
		FEATURE_NAMES.put(NUMBER_SPOTS, "Number of spots in track");
		FEATURE_NAMES.put(NUMBER_GAPS, "Number of gaps");
		FEATURE_NAMES.put(NUMBER_SPLITS, "Number of split events");
		FEATURE_NAMES.put(NUMBER_MERGES, "Number of merge events");
		FEATURE_NAMES.put(NUMBER_COMPLEX, "Complex points");

		FEATURE_SHORT_NAMES.put(NUMBER_SPOTS, "N spots");
		FEATURE_SHORT_NAMES.put(NUMBER_GAPS, "Gaps");
		FEATURE_SHORT_NAMES.put(NUMBER_SPLITS, "Splits");
		FEATURE_SHORT_NAMES.put(NUMBER_MERGES, "Merges");
		FEATURE_SHORT_NAMES.put(NUMBER_COMPLEX, "Complex");
		
		FEATURE_DIMENSIONS.put(NUMBER_SPOTS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_GAPS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_SPLITS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_MERGES, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_COMPLEX, Dimension.NONE);
	}
	
	/*
	 * METHODS
	 */
	
	
	@Override
	public void process(final TrackMateModel<T> model) {
		final List<Set<Spot>> allTracks = model.getTrackSpots();
		for (int trackIndex = 0; trackIndex < model.getNTracks(); trackIndex++) {
			final Set<Spot> track = allTracks.get(trackIndex);
			int nmerges = 0;
			int nsplits = 0;
			int ncomplex = 0;
			for (Spot spot : track) {
				int type = TrackSplitter.getVertexType(model, spot);
				switch(type) {
				case TrackSplitter.MERGING_POINT:
				case TrackSplitter.MERGING_END:
					nmerges++;
					break;
				case TrackSplitter.SPLITTING_START:
				case TrackSplitter.SPLITTING_POINT:
					nsplits++;
					break;
				case TrackSplitter.COMPLEX_POINT:
					ncomplex++;
					break;
				}
			}
			
			int ngaps = 0;
			for(DefaultWeightedEdge edge : model.getTrackEdges(trackIndex)) {
				Spot source = model.getEdgeSource(edge);
				Spot target = model.getEdgeTarget(edge);
				if (Math.abs( target.diffTo(source, Spot.FRAME)) > 1) {
					ngaps++;
				}
			}
			
			// Put feature data
			model.getFeatureModel().putTrackFeature(trackIndex, NUMBER_GAPS, Double.valueOf(ngaps));
			model.getFeatureModel().putTrackFeature(trackIndex, NUMBER_SPLITS, Double.valueOf(nsplits));
			model.getFeatureModel().putTrackFeature(trackIndex, NUMBER_MERGES, Double.valueOf(nmerges));
			model.getFeatureModel().putTrackFeature(trackIndex, NUMBER_COMPLEX, Double.valueOf(ncomplex));
			model.getFeatureModel().putTrackFeature(trackIndex, NUMBER_SPOTS, Double.valueOf(track.size()));
		}

	}
}
