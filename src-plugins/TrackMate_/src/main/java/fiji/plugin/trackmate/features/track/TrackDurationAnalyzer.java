package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackDurationAnalyzer implements TrackAnalyzer, MultiThreaded {

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

	private int numThreads;
	private long processingTime;
	private final TrackMateModel model;

	public TrackDurationAnalyzer(TrackMateModel model) {
		this.model = model;
		setNumThreads();
	}
	
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

						// I love brute force.
						Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);
						double minT = Double.POSITIVE_INFINITY;
						double maxT = Double.NEGATIVE_INFINITY;
						Double t;
						Spot startSpot = null;
						Spot endSpot = null;
						for (Spot spot : track) {
							t = spot.getFeature(Spot.POSITION_T);
							if (t < minT) {
								minT = t;
								startSpot = spot;
							}
							if (t > maxT) {
								maxT = t;
								endSpot = spot;
							}
						}

						fm.putTrackFeature(trackID, TRACK_DURATION, (maxT-minT));
						fm.putTrackFeature(trackID, TRACK_START, minT);
						fm.putTrackFeature(trackID, TRACK_STOP, maxT);
						fm.putTrackFeature(trackID, TRACK_DISPLACEMENT, (double) Math.sqrt(startSpot.squareDistanceTo(endSpot)));

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
	}
	
	@Override
	public String toString() {
		return KEY;
	}
}
