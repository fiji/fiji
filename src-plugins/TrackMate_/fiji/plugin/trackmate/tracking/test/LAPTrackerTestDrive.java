package fiji.plugin.trackmate.tracking.test;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPTracker.Settings;
import fiji.plugin.trackmate.tracking.costmatrix.LinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.costmatrix.TrackSegmentCostMatrixCreator;

public class LAPTrackerTestDrive {
	
	private final static int ZOOM_FACTOR = 4;

	/*
	 * MAIN METHOD
	 */
	
	
	public static void main(String args[]) {
		
		final boolean useCustomCostMatrices = false;
		
		// 1 - Set up test spots
		ArrayList<Spot> t0 = new ArrayList<Spot>();
		ArrayList<Spot> t1 = new ArrayList<Spot>();
		ArrayList<Spot> t2 = new ArrayList<Spot>();
		ArrayList<Spot> t3 = new ArrayList<Spot>();
		ArrayList<Spot> t4 = new ArrayList<Spot>();
		ArrayList<Spot> t5 = new ArrayList<Spot>();

		t0.add(new SpotImp(new float[] {0,0,0}));
		t0.add(new SpotImp(new float[] {1,1,1}));
		t0.add(new SpotImp(new float[] {2,2,2}));
		t0.add(new SpotImp(new float[] {3,3,3}));
		t0.add(new SpotImp(new float[] {4,4,4}));
		t0.add(new SpotImp(new float[] {5,5,5}));
		
		t0.get(0).putFeature(Feature.MEAN_INTENSITY, 100);
		t0.get(1).putFeature(Feature.MEAN_INTENSITY, 200);
		t0.get(2).putFeature(Feature.MEAN_INTENSITY, 300);
		t0.get(3).putFeature(Feature.MEAN_INTENSITY, 400);
		t0.get(4).putFeature(Feature.MEAN_INTENSITY, 500);
		t0.get(5).putFeature(Feature.MEAN_INTENSITY, 600);
		
		t1.add(new SpotImp(new float[] {1.5f,1.5f,1.5f}));
		t1.add(new SpotImp(new float[] {2.5f,2.5f,2.5f}));
		t1.add(new SpotImp(new float[] {3.5f,3.5f,3.5f}));
		t1.add(new SpotImp(new float[] {4.5f,4.5f,4.5f}));
		
		t1.get(0).putFeature(Feature.MEAN_INTENSITY, 200);
		t1.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		t1.get(2).putFeature(Feature.MEAN_INTENSITY, 400);
		t1.get(3).putFeature(Feature.MEAN_INTENSITY, 500);
		
		t2.add(new SpotImp(new float[] {1.5f,1.5f,1.5f}));
		t2.add(new SpotImp(new float[] {2.5f,2.5f,2.5f}));
		t2.add(new SpotImp(new float[] {3.5f,3.5f,3.5f}));
		t2.add(new SpotImp(new float[] {4.5f,4.5f,4.5f}));
		t2.add(new SpotImp(new float[] {10f,10f,10f}));
		
		t2.get(0).putFeature(Feature.MEAN_INTENSITY, 200);
		t2.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		t2.get(2).putFeature(Feature.MEAN_INTENSITY, 400);
		t2.get(3).putFeature(Feature.MEAN_INTENSITY, 500);
		t2.get(4).putFeature(Feature.MEAN_INTENSITY, 100);
		
		t3.add(new SpotImp(new float[] {2.6f,2.6f,2.6f}));
		t3.add(new SpotImp(new float[] {2.4f,2.4f,2.4f}));
		
		t3.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t3.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		
		t4.add(new SpotImp(new float[] {2.8f,2.8f,2.8f}));
		t4.add(new SpotImp(new float[] {2.2f,2.2f,2.2f}));
		
		t4.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t4.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
		
		t5.add(new SpotImp(new float[] {2.8f,2.8f,2.8f}));
		t5.add(new SpotImp(new float[] {2.2f,2.2f,2.2f}));
		
		t5.get(0).putFeature(Feature.MEAN_INTENSITY, 300);
		t5.get(1).putFeature(Feature.MEAN_INTENSITY, 300);
	
		TreeMap<Integer, List<Spot>> wrap = new TreeMap<Integer, List<Spot>>();
		wrap.put(0, t0);
		wrap.put(1, t1);
		wrap.put(2, t2);
		wrap.put(3, t3);
		wrap.put(4, t4);
		wrap.put(5, t5);
		
		int count = 0;
		Set<Integer> frames = wrap.keySet();
		for (int frame : frames) {
			Collection<Spot> spots = wrap.get(frame);
			for (Spot spot : spots)
				spot.putFeature(Feature.POSITION_T, frame);
			count++;
		}
		
//		ij.ImageJ.main(args);
//		ij.ImagePlus imp = createImpFrom(wrap);
//		imp.show();
		
		// 2 - Track the test spots
		LAPTracker lap;
		if (!useCustomCostMatrices) {
			lap = new LAPTracker(wrap);
			if (!lap.checkInput() || !lap.process()) {
				System.out.println(lap.getErrorMessage());
			}
			
			// Print out track segments
//			ArrayList<ArrayList<Spot>> trackSegments = lap.getTrackSegments();
//			for (ArrayList<Spot> trackSegment : trackSegments) {
//				System.out.println("-*-*-*-*-* New Segment *-*-*-*-*-");
//				for (Spot spot : trackSegment) {
//					//System.out.println(spot.toString());
//					System.out.println(MathLib.printCoordinates(spot.getCoordinates()) + ", Frame [" + spot.getFrame() + "]");
//				}
//			}
		} else {
			
			lap = new LAPTracker(wrap);
			
			// Get linking costs
			TreeMap<Integer, double[][]> linkingCosts = new TreeMap<Integer, double[][]>();
			Settings settings = new Settings();
			for (int frame : wrap.keySet()) {
				List<Spot> x = wrap.get(frame);
				List<Spot> y = wrap.get(frame+1); // unsafe
				LinkingCostMatrixCreator l = new LinkingCostMatrixCreator(x, y, settings);
				l.checkInput();
				l.process();
				linkingCosts.put(frame, l.getCostMatrix());
			}
			
			// Link objects to track segments
			lap.setLinkingCosts(linkingCosts);
			lap.checkInput();
			lap.linkObjectsToTrackSegments();
			List<SortedSet<Spot>> tSegs = lap.getTrackSegments();
			
			// Print out track segments
//			for (ArrayList<Spot> trackSegment : tSegs) {
//				System.out.println("-*-*-*-*-* New Segment *-*-*-*-*-");
//				for (Spot spot : trackSegment) {
//					//System.out.println(spot.toString());
//					System.out.println(MathLib.printCoordinates(spot.getCoordinates()));
//				}
//			}
			
			// Get segment costs
			TrackSegmentCostMatrixCreator segCosts = new TrackSegmentCostMatrixCreator(tSegs, settings);
			segCosts.checkInput();
			segCosts.process();
			double[][] segmentCosts = segCosts.getCostMatrix();
			
			// Link track segments to final tracks
			lap.setSegmentCosts(segmentCosts);
			lap.linkTrackSegmentsToFinalTracks();
			
		}

		
		// 3 - Print out results for testing
		
		System.out.println();
		System.out.println();
		System.out.println();
		SimpleGraph<Spot,DefaultEdge> graph = lap.getTrackGraph();
		ConnectivityInspector<Spot, DefaultEdge> inspector = new ConnectivityInspector<Spot, DefaultEdge>(graph);
		List<Set<Spot>> tracks = inspector.connectedSets();
		System.out.println("Found " + tracks.size() + " tracks.");
		System.out.println();
		int counter = 0;
		for(Set<Spot> track : tracks) {
			System.out.println("Track "+counter);
			System.out.print("Spots in frames: ");
			for(Spot spot : track)
				System.out.print(spot.getFeature(Feature.POSITION_T)+", ");
			System.out.println();
			counter++;
		}
	
	}

	@SuppressWarnings("unused")
	private static ImagePlus createImpFrom(TreeMap<Integer, List<Spot>> wrap) {
		List<Spot> pool = new ArrayList<Spot>();
		for(int frame : wrap.keySet()) 
			pool.addAll(wrap.get(frame));
		
		float xmax = Float.NEGATIVE_INFINITY;
		float ymax = Float.NEGATIVE_INFINITY;
		int maxFrame = 0;
		float x, y;
		int t;
		for (Spot spot : pool) {
			x = spot.getFeature(Feature.POSITION_X);
			y = spot.getFeature(Feature.POSITION_Y);
			t = spot.getFeature(Feature.POSITION_T).intValue();
			if (x > xmax) xmax = x;
			if (y > ymax) ymax = y;
			if (t > maxFrame) maxFrame = t;
		}
		
		int width = (int) (Math.ceil(xmax+1) * ZOOM_FACTOR);
		int height = (int) (Math.ceil(ymax+1) * ZOOM_FACTOR);
		ImagePlus imp = NewImage.createShortImage("LAPT-test", width, height, maxFrame+1, NewImage.FILL_BLACK);
		
		int frame, ix, iy, value;
		ImageProcessor ip;
		for (Spot spot : pool) {
			frame = spot.getFeature(Feature.POSITION_T).intValue();
			ix = Math.round(ZOOM_FACTOR * spot.getFeature(Feature.POSITION_X));
			iy = Math.round(ZOOM_FACTOR * spot.getFeature(Feature.POSITION_Y));
			value = Math.round(spot.getFeature(Feature.MEAN_INTENSITY));
			ip = imp.getImageStack().getProcessor(1+frame);
			ip.set(ix, iy, value);
		}
		
		return imp;
	}
}
