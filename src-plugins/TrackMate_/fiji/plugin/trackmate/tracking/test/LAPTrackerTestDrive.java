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

import mpicbg.imglib.algorithm.math.MathLib;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotImp;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.LAPTracker.Settings;
import fiji.plugin.trackmate.tracking.costmatrix.LinkingCostMatrixCreator;
import fiji.plugin.trackmate.tracking.costmatrix.TrackSegmentCostMatrixCreator;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer2D;

public class LAPTrackerTestDrive {
	
	private final static int ZOOM_FACTOR = 4;

	/*
	 * MAIN METHOD
	 */
	
	
	public static void main(String args[]) {
		
		final boolean useCustomCostMatrices = false;
		final int tmax = 5; // nframes
		
		
		// 1 - Set up test spots
		TreeMap<Integer, List<Spot>> wrap = new TreeMap<Integer, List<Spot>>();
		for (int i = 0; i < tmax; i++) 
			wrap.put(i, new ArrayList<Spot>());

		// first track
		for (int i = 0; i < tmax; i++)
			wrap.get(i).add(new SpotImp(new float[] { ZOOM_FACTOR*(1+i), ZOOM_FACTOR*(1+i), 0 } ));
		// second track
		for (int i = 2; i < tmax; i++)
			wrap.get(i).add(new SpotImp(new float[] { ZOOM_FACTOR*(100+i), ZOOM_FACTOR*(1+i), 0 } ));
		
		int count = 0;
		Set<Integer> frames = wrap.keySet();
		for (int frame : frames) {
			Collection<Spot> spots = wrap.get(frame);
			for (Spot spot : spots) {
				spot.putFeature(Feature.POSITION_T, frame);
				spot.putFeature(Feature.MEAN_INTENSITY, 200);
			}
			count++;
		}
		
		ij.ImageJ.main(args);
		ij.ImagePlus imp = createImpFrom(wrap);
		imp.show();
		
		// 2 - Track the test spots
		LAPTracker lap;
		if (!useCustomCostMatrices) {
			lap = new LAPTracker(wrap);
			if (!lap.checkInput() || !lap.process())
				System.out.println(lap.getErrorMessage());
			
			// Print out track segments
			List<SortedSet<Spot>> trackSegments = lap.getTrackSegments();
			for (SortedSet<Spot> trackSegment : trackSegments) {
				System.out.println("\n-*-*-*-*-* New Segment *-*-*-*-*-");
				for (Spot spot : trackSegment)
					System.out.println(MathLib.printCoordinates(spot.getPosition(null)) + ", Frame [" + spot.getFeature(Feature.POSITION_T) + "]");	
			}
			
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
			System.out.print("Spots in frames: \n");
			for(Spot spot : track)
				System.out.println(MathLib.printCoordinates(spot.getPosition(null)) + ", Frame [" + spot.getFeature(Feature.POSITION_T) + "]");
			System.out.println();
			counter++;
		}
		
		// 4 - Detailed info
		System.out.println("Segment costs:");
		LAPUtils.echoMatrix(lap.getSegmentCosts());
		
		System.out.println();
		System.out.println("Fragment costs for 1st frame:");
		LAPUtils.echoMatrix(lap.getLinkingCosts().get(0));
		
		// 5 - Display tracks
		SpotDisplayer2D sd2d = new SpotDisplayer2D(imp, 2, new float[] {1, 1});
		sd2d.setSpots(wrap);
		sd2d.render();
		sd2d.setSpotsToShow(wrap);
		sd2d.setTrackGraph(graph);
		sd2d.setDisplayTrackMode(SpotDisplayer.TrackDisplayMode.ALL_WHOLE_TRACKS, 1);
	}

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
		
		int width = (int) (Math.ceil(xmax+1) );
		int height = (int) (Math.ceil(ymax+1) );
		ImagePlus imp = NewImage.createByteImage("LAPT-test", width, height, maxFrame+1, NewImage.FILL_BLACK);
		imp.setDimensions(1, 1, maxFrame+1);
		int frame, ix, iy, value;
		ImageProcessor ip;
		for (Spot spot : pool) {
			frame = spot.getFeature(Feature.POSITION_T).intValue();
			ix = Math.round( spot.getFeature(Feature.POSITION_X));
			iy = Math.round( spot.getFeature(Feature.POSITION_Y));
			value = Math.round(spot.getFeature(Feature.MEAN_INTENSITY));
			ip = imp.getImageStack().getProcessor(1+frame);
			ip.set(ix, iy, value);
		}
		
		return imp;
	}
}
