package fiji.plugin.trackmate.tests;

import java.util.Random;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotCollectionHashSet;
import fiji.plugin.trackmate.features.FeatureFilter;

public class SpotCollectionPerformance_TestDrive {

	private static final int N_SPOTS = 500; // 1e3 / frame
	private static final int N_FRAMES = 100; // 1e5 spots total
	private static final Random ran = new Random();

	public static void main(String[] args) {

		RunnableTimer timer = new RunnableTimer();

		System.out.println("--------------------------------.");
		System.out.println("Assessing current SpotCollection.");
		System.out.println("--------------------------------.");
		System.out.println();

		System.out.println("Adding " + (N_FRAMES*N_SPOTS) + " spots to collection.");
		SpotCollection s0 = new SpotCollection();
		s0.setNumThreads(1);
		AddSpotsTask_S0 task0a = new AddSpotsTask_S0(s0, N_FRAMES, N_SPOTS);	
		System.out.println("Task done in "+(timer.time(task0a))+" ms.");
		System.out.println();

		System.out.println("Adding " + (10*N_FRAMES*N_SPOTS) + " spots to collection.");
		s0 = new SpotCollection();
		task0a = new AddSpotsTask_S0(s0, N_FRAMES, 10*N_SPOTS);	
		System.out.println("Task done in "+(timer.time(task0a))+" ms.");
		System.out.println();

		System.out.println("Filtering out 99% of the spots in the collection.");
		FilterSpotsTask_S0 task0b = new FilterSpotsTask_S0(s0, 0.01);	
		System.out.println("Task done in "+(timer.time(task0b))+" ms.");
		System.out.println();

		System.out.println("Filtering out 1% of the spots in the collection.");
		task0b = new FilterSpotsTask_S0(s0, 0.99);	
		System.out.println("Task done in "+(timer.time(task0b))+" ms.");
		System.out.println();
		
		s0 = null;
		
		
		System.out.println("--------------------------------------.");
		System.out.println("Assessing HashSet based SpotCollection.");
		System.out.println("--------------------------------------.");
		System.out.println();

		System.out.println("Adding " + (N_FRAMES*N_SPOTS) + " spots to collection.");
		SpotCollectionHashSet s1 = new SpotCollectionHashSet();
		s1.setNumThreads(1);
		AddSpotsTask_S1 task1a = new AddSpotsTask_S1(s1, N_FRAMES, N_SPOTS);	
		System.out.println("Task done in "+(timer.time(task1a))+" ms.");
		System.out.println();

		System.out.println("Adding " + (10*N_FRAMES*N_SPOTS) + " spots to collection.");
		s1 = new SpotCollectionHashSet();
		task1a = new AddSpotsTask_S1(s1, N_FRAMES, 10*N_SPOTS);	
		System.out.println("Task done in "+(timer.time(task1a))+" ms.");
		System.out.println();

		System.out.println("Filtering out 99% of the spots in the collection.");
		FilterSpotsTask_S1 task1b = new FilterSpotsTask_S1(s1, 0.01);	
		System.out.println("Task done in "+(timer.time(task1b))+" ms.");
		System.out.println();

		System.out.println("Filtering out 1% of the spots in the collection.");
		task1b = new FilterSpotsTask_S1(s1, 0.99);	
		System.out.println("Task done in "+(timer.time(task1b))+" ms.");
		System.out.println();

		
	}

	private static final Spot makeSpot(int frame) {
		double[] coords = new double[] { ran.nextDouble(), ran.nextDouble(), ran.nextDouble() };
		Spot spot = new Spot(coords);
		spot.putFeature(Spot.QUALITY, ran.nextDouble());
		spot.putFeature(Spot.FRAME, Double.valueOf(frame));
		return spot;
	}

	private static class AddSpotsTask_S1 implements Runnable {

		private final SpotCollectionHashSet spotCollection;
		private final int nFrames;
		private final int nSpotsPerFrame;

		public AddSpotsTask_S1(SpotCollectionHashSet spotCollection, int nFrames, int nSpotsPerFrame) {
			this.spotCollection = spotCollection;
			this.nFrames = nFrames;
			this.nSpotsPerFrame = nSpotsPerFrame;
		}

		@Override
		public void run() {
			for (int i = 0; i < nFrames; i++) {
				for (int j = 0; j < nSpotsPerFrame; j++) {
					Spot spot = makeSpot(i);
					spotCollection.add(spot, i);
				}
			}
		}

	}

	private static class AddSpotsTask_S0 implements Runnable {

		private final SpotCollection spotCollection;
		private final int nFrames;
		private final int nSpotsPerFrame;

		public AddSpotsTask_S0(SpotCollection spotCollection, int nFrames, int nSpotsPerFrame) {
			this.spotCollection = spotCollection;
			this.nFrames = nFrames;
			this.nSpotsPerFrame = nSpotsPerFrame;
		}

		@Override
		public void run() {
			for (int i = 0; i < nFrames; i++) {
				for (int j = 0; j < nSpotsPerFrame; j++) {
					Spot spot = makeSpot(i);
					spotCollection.add(spot, i);
				}
			}
		}

	}

	private static class FilterSpotsTask_S1 implements Runnable {

		private final SpotCollectionHashSet spotCollection;
		private final double value;

		public FilterSpotsTask_S1(SpotCollectionHashSet spotCollection, double value) {
			this.spotCollection = spotCollection;
			this.value = value;
		}

		@Override
		public void run() {
			FeatureFilter filter = new FeatureFilter(Spot.QUALITY, value, true);
			spotCollection.filter(filter);
		}

	}
	
	private static class FilterSpotsTask_S0 implements Runnable {

		private final SpotCollection spotCollection;
		private final double value;

		public FilterSpotsTask_S0(SpotCollection spotCollection, double value) {
			this.spotCollection = spotCollection;
			this.value = value;
		}

		@Override
		public void run() {
			FeatureFilter filter = new FeatureFilter(Spot.QUALITY, value, true);
			spotCollection.filter(filter);
		}

	}

	private static class RunnableTimer {

		public long time(Runnable runnable) {
			long start = System.currentTimeMillis();
			runnable.run();
			long end = System.currentTimeMillis();
			return (end-start);
		}
	}

}
