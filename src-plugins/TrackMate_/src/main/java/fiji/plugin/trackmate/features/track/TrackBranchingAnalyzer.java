package fiji.plugin.trackmate.features.track;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackBranchingAnalyzer implements TrackAnalyzer, MultiThreaded {

	/*
	 * CONSTANTS
	 */
	public static final String 		KEY = "Branching analyzer";
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

	private int numThreads;
	private long processingTime;
	private final TrackMateModel model;

	public TrackBranchingAnalyzer(final TrackMateModel model) {
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

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("TrackBranchingAnalyzer thread " + i) {
				@Override
				public void run() {
					Integer trackID;
					while ((trackID = queue.poll()) != null) {

						Set<Spot> track = model.getTrackModel().getTrackSpots(trackID);

						int nmerges = 0;
						int nsplits = 0;
						int ncomplex = 0;
						for (Spot spot : track) {
							Set<DefaultWeightedEdge> edges = model.getTrackModel().edgesOf(spot);
							
							// get neighbors
							Set<Spot> neighbors = new HashSet<Spot>();
							for(DefaultWeightedEdge edge : edges) {
								neighbors.add(model.getTrackModel().getEdgeSource(edge));
								neighbors.add(model.getTrackModel().getEdgeTarget(edge));
							}
							neighbors.remove(spot);
							
							// inspect neighbors relative time position
							int earlier = 0;
							int later = 0;
							for (Spot neighbor : neighbors) {
								if (spot.diffTo(neighbor, Spot.FRAME) > 0) {
									earlier++; // neighbor is before in time
								} else {
									later++;
								}
							}
							
							// Test for classical spot
							if (earlier == 1 && later == 1) {
								continue;
							}
							
							// classify spot
							if (earlier <= 1 && later > 1) {
								nsplits++;
							} else if (later <=1 && earlier > 1) {
								nmerges++;
							} else if (later > 1 && earlier > 1) {
								ncomplex++;
							}
						}

						int ngaps = 0;
						for(DefaultWeightedEdge edge : model.getTrackModel().getTrackEdges(trackID)) {
							Spot source = model.getTrackModel().getEdgeSource(edge);
							Spot target = model.getTrackModel().getEdgeTarget(edge);
							if (Math.abs( target.diffTo(source, Spot.FRAME)) > 1) {
								ngaps++;
							}
						}

						// Put feature data
						model.getFeatureModel().putTrackFeature(trackID, NUMBER_GAPS, Double.valueOf(ngaps));
						model.getFeatureModel().putTrackFeature(trackID, NUMBER_SPLITS, Double.valueOf(nsplits));
						model.getFeatureModel().putTrackFeature(trackID, NUMBER_MERGES, Double.valueOf(nmerges));
						model.getFeatureModel().putTrackFeature(trackID, NUMBER_COMPLEX, Double.valueOf(ncomplex));
						model.getFeatureModel().putTrackFeature(trackID, NUMBER_SPOTS, Double.valueOf(track.size()));

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
	public String toString() {
		return KEY;
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

}
