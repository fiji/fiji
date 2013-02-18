package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import mpicbg.imglib.util.Util;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackSpeedStatisticsAnalyzer implements TrackAnalyzer, MultiThreaded, Benchmark {


	/*
	 * CONSTANTS
	 */
	public static final String KEY = "Velocity";
	public static final String 		TRACK_MEAN_SPEED = "TRACK_MEAN_SPEED";
	public static final String 		TRACK_MAX_SPEED = "TRACK_MAX_SPEED";
	public static final String 		TRACK_MIN_SPEED = "TRACK_MIN_SPEED";
	public static final String 		TRACK_MEDIAN_SPEED = "TRACK_MEDIAN_SPEED";
	public static final String 		TRACK_STD_SPEED = "TRACK_STD_SPEED";
	//	public static final String 		TRACK_SPEED_KURTOSIS = "TRACK_SPEED_KURTOSIS";
	//	public static final String 		TRACK_SPEED_SKEWNESS = "TRACK_SPEED_SKEWNESS";

	public static final List<String> FEATURES = new ArrayList<String>(5);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(5);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(5);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(5);

	static {
		FEATURES.add(TRACK_MEAN_SPEED);
		FEATURES.add(TRACK_MAX_SPEED);
		FEATURES.add(TRACK_MIN_SPEED);
		FEATURES.add(TRACK_MEDIAN_SPEED);
		FEATURES.add(TRACK_STD_SPEED);
		//		FEATURES.add(TRACK_SPEED_KURTOSIS);
		//		FEATURES.add(TRACK_SPEED_SKEWNESS);

		FEATURE_NAMES.put(TRACK_MEAN_SPEED, "Mean velocity");
		FEATURE_NAMES.put(TRACK_MAX_SPEED, "Maximal velocity");
		FEATURE_NAMES.put(TRACK_MIN_SPEED, "Minimal velocity");
		FEATURE_NAMES.put(TRACK_MEDIAN_SPEED, "Median velocity");
		FEATURE_NAMES.put(TRACK_STD_SPEED, "Velocity standard deviation");
		//		FEATURE_NAMES.put(TRACK_SPEED_KURTOSIS, "Velocity kurtosis");
		//		FEATURE_NAMES.put(TRACK_SPEED_SKEWNESS, "Velocity skewness");

		FEATURE_SHORT_NAMES.put(TRACK_MEAN_SPEED, "Mean V");
		FEATURE_SHORT_NAMES.put(TRACK_MAX_SPEED, "Max V");
		FEATURE_SHORT_NAMES.put(TRACK_MIN_SPEED, "Min V");
		FEATURE_SHORT_NAMES.put(TRACK_MEDIAN_SPEED, "Median V");
		FEATURE_SHORT_NAMES.put(TRACK_STD_SPEED, "V std");
		//		FEATURE_SHORT_NAMES.put(TRACK_SPEED_KURTOSIS, "V kurtosis");
		//		FEATURE_SHORT_NAMES.put(TRACK_SPEED_SKEWNESS, "V skewness");

		FEATURE_DIMENSIONS.put(TRACK_MEAN_SPEED, Dimension.VELOCITY);
		FEATURE_DIMENSIONS.put(TRACK_MAX_SPEED, Dimension.VELOCITY);
		FEATURE_DIMENSIONS.put(TRACK_MIN_SPEED, Dimension.VELOCITY);
		FEATURE_DIMENSIONS.put(TRACK_MEDIAN_SPEED, Dimension.VELOCITY);
		FEATURE_DIMENSIONS.put(TRACK_STD_SPEED, Dimension.VELOCITY);
		//		FEATURE_DIMENSIONS.put(TRACK_SPEED_KURTOSIS, Dimension.NONE);
		//		FEATURE_DIMENSIONS.put(TRACK_SPEED_SKEWNESS, Dimension.NONE);
	}

	private int numThreads;
	private long processingTime;
	private final TrackMateModel model;

	public TrackSpeedStatisticsAnalyzer(final TrackMateModel model) {
		this.model = model;
		setNumThreads();
	}

	/*
	 * METHODS
	 */
	
	@Override
	public boolean isLocal() {
		return true;
	}
	
	@Override
	public void process(final Collection<Integer> trackIDs) {
		
		if (trackIDs.isEmpty()) {
			return;
		}
		
		final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(trackIDs.size(), false, trackIDs);
		final FeatureModel fm = model.getFeatureModel();

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("TrackLocationAnalyzer thread " + i) {
				@Override
				public void run() {
					Integer trackID;
					while ((trackID = queue.poll()) != null) {

						Set<DefaultWeightedEdge> track = model.getTrackModel().getTrackEdges(trackID);

						double sum = 0;
						double mean = 0;
						double M2 = 0;
						//			double M3 = 0;
						//			double M4 = 0;
						double delta, delta_n;
						//			double delta_n2;
						double term1;
						int n1;

						// Others
						Double val;
						final double[] velocities = new double[track.size()];
						int n = 0;

						for(DefaultWeightedEdge edge : track) {
							Spot source = model.getTrackModel().getEdgeSource(edge);
							Spot target = model.getTrackModel().getEdgeTarget(edge);

							// Edge velocity
							Double d2 = source.squareDistanceTo(target);
							Double dt = source.diffTo(target, Spot.POSITION_T);
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
							//				delta_n2 = delta_n * delta_n;
							term1 = delta * delta_n * n1;
							mean = mean + delta_n;
							//				M4 = M4 + term1 * delta_n2 * (n*n - 3*n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
							//		        M3 = M3 + term1 * delta_n * (n - 2) - 3 * delta_n * M2;
							M2 = M2 + term1;
						}

						Util.quicksort(velocities, 0, track.size()-1);
						double median = velocities[track.size()/2];
						double min = velocities[0];
						double max = velocities[track.size()-1];
						mean = sum / track.size();
						double variance = M2 / (track.size()-1);
						//			double kurtosis = (n*M4) / (M2*M2) - 3;
						//			double skewness =  Math.sqrt(n) * M3 / Math.pow(M2, 3/2.0) ;

						fm.putTrackFeature(trackID, TRACK_MEDIAN_SPEED, median);
						fm.putTrackFeature(trackID, TRACK_MIN_SPEED, min);
						fm.putTrackFeature(trackID, TRACK_MAX_SPEED, max);
						fm.putTrackFeature(trackID, TRACK_MEAN_SPEED, mean);
						fm.putTrackFeature(trackID, TRACK_STD_SPEED,  Math.sqrt(variance));
						//			fm.putTrackFeature(index, TRACK_SPEED_KURTOSIS, kurtosis);
						//			fm.putTrackFeature(index, TRACK_SPEED_SKEWNESS, skewness);


					}

				}
			};
		}

		long start = System.currentTimeMillis();
		SimpleMultiThreading.startAndJoin(threads);
		long end = System.currentTimeMillis();
		processingTime = end - start;
	}

	@Override
	public int getNumThreads() {
		return numThreads;
	}

	@Override
	public void setNumThreads() {
		this.numThreads = Runtime.getRuntime().availableProcessors();  
	}

	@Override
	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;

	}

	@Override
	public long getProcessingTime() {
		return processingTime;
	};
	
	@Override
	public String toString() {
		return KEY;
	}
}
