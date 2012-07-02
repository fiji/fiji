package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.util.TrackSplitter;

public class TrackBranchingAnalyzer implements TrackFeatureAnalyzer {

	/*
	 * CONSTANTS
	 */
	
	public static final String 		NUMBER_SPLITS = "NUMBER_SPLITS";
	public static final String 		NUMBER_MERGES = "NUMBER_MERGES";
	public static final String 		NUMBER_COMPLEX = "NUMBER_COMPLEX";
	public static final String 		NUMBER_SPOTS = "NUMBER_SPOTS";
	
	private static final List<String> FEATURES = new ArrayList<String>(4);
	private static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(4);
	private static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(4);
	private static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(4);
	
	static {
		FEATURES.add(NUMBER_SPOTS);
		FEATURES.add(NUMBER_SPLITS);
		FEATURES.add(NUMBER_MERGES);
		FEATURES.add(NUMBER_COMPLEX);
		
		FEATURE_NAMES.put(NUMBER_SPOTS, "Number of spots in track");
		FEATURE_NAMES.put(NUMBER_SPLITS, "Number of split events");
		FEATURE_NAMES.put(NUMBER_MERGES, "Number of merge events");
		FEATURE_NAMES.put(NUMBER_COMPLEX, "Complex points");

		FEATURE_SHORT_NAMES.put(NUMBER_SPOTS, "N spots");
		FEATURE_SHORT_NAMES.put(NUMBER_SPLITS, "Splits");
		FEATURE_SHORT_NAMES.put(NUMBER_MERGES, "Merges");
		FEATURE_SHORT_NAMES.put(NUMBER_COMPLEX, "Complex");
		
		FEATURE_DIMENSIONS.put(NUMBER_SPOTS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_SPLITS, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_MERGES, Dimension.NONE);
		FEATURE_DIMENSIONS.put(NUMBER_COMPLEX, Dimension.NONE);
	}
	
	/*
	 * METHODS
	 */
	
	
	@Override
	public void process(final TrackMateModel model) {
		final List<Set<Spot>> allTracks = model.getTrackSpots();
		for (int i = 0; i < model.getNTracks(); i++) {
			final Set<Spot> track = allTracks.get(i);
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
			// Put feature data
			model.getFeatureModel().putTrackFeature(i, NUMBER_SPLITS, (float) nsplits);
			model.getFeatureModel().putTrackFeature(i, NUMBER_MERGES, (float) nmerges);
			model.getFeatureModel().putTrackFeature(i, NUMBER_COMPLEX, (float) ncomplex);
			model.getFeatureModel().putTrackFeature(i, NUMBER_SPOTS, (float) track.size());
		}

	}

	@Override
	public Map<String, String> getFeatureShortNames() {
		return FEATURE_SHORT_NAMES;
	}


	@Override
	public Map<String, String> getFeatureNames() {
		return FEATURE_NAMES;
	}


	@Override
	public Map<String, Dimension> getFeatureDimensions() {
		return FEATURE_DIMENSIONS;
	}

	@Override
	public Collection<String> getFeatures() {
		return FEATURES;
	}

}
