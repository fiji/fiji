package fiji.plugin.trackmate.features.edges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.multithreading.SimpleMultiThreading;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class EdgeTimeLocationAnalyzer implements EdgeAnalyzer, MultiThreaded {

	public static final String KEY = "Edge mean location";
	/*
	 * FEATURE NAMES 
	 */
	public static final String TIME = "TIME";
	public static final String X_LOCATION = "X_LOCATION";
	public static final String Y_LOCATION = "Y_LOCATION";
	public static final String Z_LOCATION = "Z_LOCATION";

	public static final List<String> FEATURES = new ArrayList<String>(2);
	public static final Map<String, String> FEATURE_NAMES = new HashMap<String, String>(2);
	public static final Map<String, String> FEATURE_SHORT_NAMES = new HashMap<String, String>(2);
	public static final Map<String, Dimension> FEATURE_DIMENSIONS = new HashMap<String, Dimension>(2);

	static {
		FEATURES.add(TIME);
		FEATURES.add(X_LOCATION);
		FEATURES.add(Y_LOCATION);
		FEATURES.add(Z_LOCATION);

		FEATURE_NAMES.put(TIME, "Time (mean)");
		FEATURE_NAMES.put(X_LOCATION, "X Location (mean)");
		FEATURE_NAMES.put(Y_LOCATION, "Y Location (mean)");
		FEATURE_NAMES.put(Z_LOCATION, "Z Location (mean)");

		FEATURE_SHORT_NAMES.put(TIME, "T");
		FEATURE_SHORT_NAMES.put(X_LOCATION, "X");
		FEATURE_SHORT_NAMES.put(Y_LOCATION, "Y");
		FEATURE_SHORT_NAMES.put(Z_LOCATION, "Z");

		FEATURE_DIMENSIONS.put(TIME, Dimension.TIME);
		FEATURE_DIMENSIONS.put(X_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Y_LOCATION, Dimension.POSITION);
		FEATURE_DIMENSIONS.put(Z_LOCATION, Dimension.POSITION);
	}

	private int numThreads;
	private long processingTime;
	private final FeatureModel featureModel;
	private final TrackMateModel model;

	/*
	 * CONSTRUCTOR
	 */

	public EdgeTimeLocationAnalyzer(final TrackMateModel model) {
		this.model = model;
		this.featureModel = model.getFeatureModel();
		setNumThreads();
	}

	@Override
	public boolean isLocal() {
		return true;
	}


	@Override
	public void process(final Collection<DefaultWeightedEdge> edges) {

		if (edges.isEmpty()) {
			return;
		}
		
		final ArrayBlockingQueue<DefaultWeightedEdge> queue = new ArrayBlockingQueue<DefaultWeightedEdge>(edges.size(), false, edges);

		Thread[] threads = SimpleMultiThreading.newThreads(numThreads);
		for (int i = 0; i < threads.length; i++) {
			threads[i] = new Thread("EdgeTimeLocationAnalyzer thread " + i) {
				@Override
				public void run() {
					DefaultWeightedEdge edge;
					while ((edge = queue.poll()) != null) {

						Spot source = model.getTrackModel().getEdgeSource(edge);
						Spot target = model.getTrackModel().getEdgeTarget(edge);

						double x = 0.5 * ( source.getFeature(Spot.POSITION_X) + target.getFeature(Spot.POSITION_X) ); 
						double y = 0.5 * ( source.getFeature(Spot.POSITION_Y) + target.getFeature(Spot.POSITION_Y) ); 
						double z = 0.5 * ( source.getFeature(Spot.POSITION_Z) + target.getFeature(Spot.POSITION_Z) ); 
						double t = 0.5 * ( source.getFeature(Spot.POSITION_T) + target.getFeature(Spot.POSITION_T) ); 

						featureModel.putEdgeFeature(edge, TIME, t);
						featureModel.putEdgeFeature(edge, X_LOCATION, x);
						featureModel.putEdgeFeature(edge, Y_LOCATION, y);
						featureModel.putEdgeFeature(edge, Z_LOCATION, z);
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
	};
}

