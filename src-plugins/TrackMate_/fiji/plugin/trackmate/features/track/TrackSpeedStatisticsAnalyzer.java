package fiji.plugin.trackmate.features.track;

import java.util.HashSet;
import java.util.Set;

import mpicbg.imglib.util.Util;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackFeature;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackSpeedStatisticsAnalyzer implements TrackFeatureAnalyzer {

	@Override
	public void process(TrackMateModel model) {
		
		for(int index=0; index<model.getNTracks(); index++) {
			
			Set<DefaultWeightedEdge> track = model.getTrackEdges(index);
			
			if (track.size() == 0)
				continue;
			
			double sum = 0;
			double mean = 0;
			double M2 = 0;
			double M3 = 0;
			double M4 = 0;
			double delta, delta_n, delta_n2;
			double term1;
		    int n1;
			
		    // Others
			Double val;
			final double[] velocities = new double[track.size()];
			int n = 0;
			
			for(DefaultWeightedEdge edge : track) {
				Spot source = model.getEdgeSource(edge);
				Spot target = model.getEdgeTarget(edge);
				
				// Edge velocity
				Float d2 = source.squareDistanceTo(target);
				Float dt = source.diffTo(target, SpotFeature.POSITION_T);
				if (d2 == null || dt == null)
					continue;
				val = Math.sqrt(d2) / Math.abs(dt);
				
				// For median, min and max
				velocities[n] = val;
				// For variance and mean
				sum += val;
				
				// For kurtosis
				n1 = n;
				n++;
				delta = val - mean;
				delta_n = delta / n;
				delta_n2 = delta_n * delta_n;
				term1 = delta * delta_n * n1;
				mean = mean + delta_n;
				M4 = M4 + term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
		        M3 = M3 + term1 * delta_n * (n - 2) - 3 * delta_n * M2;
		        M2 = M2 + term1;
			}
			
			Util.quicksort(velocities, 0, track.size()-1);
			double median = velocities[track.size()/2];
			double min = velocities[0];
			double max = velocities[track.size()-1];
			mean = sum / track.size();
			double variance = M2 / (track.size()-1);
			double kurtosis = (n*M4) / (M2*M2) - 3;
			double skewness =  Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0) ;
			
			model.putTrackFeature(index, TrackFeature.TRACK_MEDIAN_SPEED, (float) median);
			model.putTrackFeature(index, TrackFeature.TRACK_MIN_SPEED, (float) min);
			model.putTrackFeature(index, TrackFeature.TRACK_MAX_SPEED, (float) max);
			model.putTrackFeature(index, TrackFeature.TRACK_MEAN_SPEED, (float) mean);
			model.putTrackFeature(index, TrackFeature.TRACK_SPEED_STANDARD_DEVIATION, (float) Math.sqrt(variance));
			model.putTrackFeature(index, TrackFeature.TRACK_SPEED_KURTOSIS, (float) kurtosis);
			model.putTrackFeature(index, TrackFeature.TRACK_SPEED_SKEWNESS, (float) skewness);
			
		}
	}

	@Override
	public Set<TrackFeature> getFeatures() {
		Set<TrackFeature> features = new HashSet<TrackFeature>(7);
		features.add(TrackFeature.TRACK_MEDIAN_SPEED);
		features.add(TrackFeature.TRACK_MIN_SPEED);
		features.add(TrackFeature.TRACK_MAX_SPEED);
		features.add(TrackFeature.TRACK_MEAN_SPEED);
		features.add(TrackFeature.TRACK_SPEED_STANDARD_DEVIATION);
		features.add(TrackFeature.TRACK_SPEED_KURTOSIS);
		features.add(TrackFeature.TRACK_SPEED_SKEWNESS);
		return features;
	}

}
