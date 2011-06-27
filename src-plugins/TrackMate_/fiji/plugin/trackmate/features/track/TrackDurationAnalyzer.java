package fiji.plugin.trackmate.features.track;

import java.util.HashSet;
import java.util.Set;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackCollection;
import fiji.plugin.trackmate.TrackFeature;

public class TrackDurationAnalyzer implements TrackFeatureAnalyzer{
	
	@Override
	public void process(TrackCollection tracks) {
		// I love brute force.
		for(int index=0; index<tracks.size(); index++) {
			Set<Spot> track = tracks.getTrackSpot(index);
			float minT = Float.POSITIVE_INFINITY;
			float maxT = Float.NEGATIVE_INFINITY;
			float t;
			for (Spot spot : track) {
				t = spot.getFeature(SpotFeature.POSITION_T);
				if (t < minT)
					minT = t;
				if (t > maxT)
					maxT = t;
			}
			tracks.putFeature(index, TrackFeature.TRACK_DURATION, (maxT-minT));
		}
	}

	@Override
	public Set<TrackFeature> getFeatures() {
		Set<TrackFeature> features = new HashSet<TrackFeature>(1);
		features.add(TrackFeature.TRACK_DURATION);
		return features;
	}

}
