package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackDurationAnalyzer<T extends RealType<T> & NativeType<T>> implements TrackFeatureAnalyzer<T> {
	
	public static final String KEY = "Track duration";
	public static final String 		TRACK_DURATION = "TRACK_DURATION";
	public static final String 		TRACK_START = "TRACK_START";
	public static final String 		TRACK_STOP = "TRACK_STOP";
	public static final String 		TRACK_DISPLACEMENT = "TRACK_DISPLACEMENT";
	
	public static final List<String> FEATURES = new ArrayList<String>(4);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(4);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(4);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(4);
	
	static {
		FEATURES.add(TRACK_DURATION);
		FEATURES.add(TRACK_START);
		FEATURES.add(TRACK_STOP);
		FEATURES.add(TRACK_DISPLACEMENT);
		
		FEATURE_NAMES.put(TRACK_DURATION, "Duration of track");
		FEATURE_NAMES.put(TRACK_START, "Track start");
		FEATURE_NAMES.put(TRACK_STOP, "Track stop");
		FEATURE_NAMES.put(TRACK_DISPLACEMENT, "Track displacement");

		FEATURE_SHORT_NAMES.put(TRACK_DURATION, "Duration");
		FEATURE_SHORT_NAMES.put(TRACK_START, "T start");
		FEATURE_SHORT_NAMES.put(TRACK_STOP, "T stop");
		FEATURE_SHORT_NAMES.put(TRACK_DISPLACEMENT, "Displacement");
		
		FEATURE_DIMENSIONS.put(TRACK_DURATION, Dimension.TIME);
		FEATURE_DIMENSIONS.put(TRACK_START, Dimension.TIME);
		FEATURE_DIMENSIONS.put(TRACK_STOP, Dimension.TIME);
		FEATURE_DIMENSIONS.put(TRACK_DISPLACEMENT, Dimension.LENGTH);
	}
	
	/*
	 * METHODS
	 */

	@Override
	public void process(final TrackMateModel<T> model) {
		// I love brute force.
		final List<Set<Spot>> allTracks = model.getTrackSpots();
		for(int index=0; index<model.getNTracks(); index++) {
			Set<Spot> track = allTracks.get(index);
			double minT = Double.POSITIVE_INFINITY;
			double maxT = Double.NEGATIVE_INFINITY;
			Double t;
			boolean allNull = true;
			Spot startSpot = null;
			Spot endSpot = null;
			for (Spot spot : track) {
				t = spot.getFeature(Spot.POSITION_T);
				if (null == t)
					continue;
				allNull = false;
				if (t < minT) {
					minT = t;
					startSpot = spot;
				}
				if (t > maxT) {
					maxT = t;
					endSpot = spot;
				}
			}
			if (!allNull) {
				model.getFeatureModel().putTrackFeature(index, TRACK_DURATION, (maxT-minT));
				model.getFeatureModel().putTrackFeature(index, TRACK_START, minT);
				model.getFeatureModel().putTrackFeature(index, TRACK_STOP, maxT);
				model.getFeatureModel().putTrackFeature(index, TRACK_DISPLACEMENT, (double) Math.sqrt(startSpot.squareDistanceTo(endSpot)));
			} else {
				model.getFeatureModel().putTrackFeature(index, TRACK_DURATION, Double.NaN);
				model.getFeatureModel().putTrackFeature(index, TRACK_START, Double.NaN);
				model.getFeatureModel().putTrackFeature(index, TRACK_STOP, Double.NaN);
				model.getFeatureModel().putTrackFeature(index, TRACK_DISPLACEMENT, Double.NaN);
			}
		}
	}
}
