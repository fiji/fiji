package fiji.plugin.trackmate.features.track;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackDurationAnalyzer implements TrackFeatureAnalyzer{

	@Override
	public void process(final TrackMateModel model) {
		// I love brute force.
		final List<Set<Spot>> allTracks = model.getTrackSpots();
		for(int index=0; index<model.getNTracks(); index++) {
			Set<Spot> track = allTracks.get(index);
			float minT = Float.POSITIVE_INFINITY;
			float maxT = Float.NEGATIVE_INFINITY;
			Float t;
			boolean allNull = true;
			Spot startSpot = null;
			Spot endSpot = null;
			for (Spot spot : track) {
				t = spot.getFeature(SpotFeature.POSITION_T);
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
				model.putTrackFeature(index, TrackFeature.TRACK_DURATION, (maxT-minT));
				model.putTrackFeature(index, TrackFeature.TRACK_START, minT);
				model.putTrackFeature(index, TrackFeature.TRACK_STOP, maxT);
				model.putTrackFeature(index, TrackFeature.TRACK_DISPLACEMENT, (float) Math.sqrt(startSpot.squareDistanceTo(endSpot)));
			}
		}
	}

	@Override
	public Set<TrackFeature> getFeatures() {
		Set<TrackFeature> features = new HashSet<TrackFeature>(4);
		features.add(TrackFeature.TRACK_DURATION);
		features.add(TrackFeature.TRACK_START);
		features.add(TrackFeature.TRACK_STOP);
		features.add(TrackFeature.TRACK_DISPLACEMENT);
		return features;
	}

}
