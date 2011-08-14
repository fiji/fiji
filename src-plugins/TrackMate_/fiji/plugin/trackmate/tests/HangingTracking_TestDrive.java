package fiji.plugin.trackmate.tests;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackerSettings;

public class HangingTracking_TestDrive {

	private static final File file = new File("E:/Users/JeanYves/Desktop/Data/Scoobidoo.xml");


	public static void main(String[] args) throws JDOMException, IOException {

		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		SpotCollection spots = reader.getAllSpots();
		SpotCollection filteredSpots = reader.getFilteredSpots(spots);
		TrackMateModel model = reader.getModel();

		int frame = 1;

		System.out.println();
		System.out.println("Without feature condition:");
		TrackerSettings trackerSettings = reader.getTrackerSettings();

		LAPTracker tracker = new LAPTracker(filteredSpots, trackerSettings);
		tracker.createLinkingCostMatrices();
		SortedMap<Integer, double[][]> costs1 = tracker.getLinkingCosts();
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" to link to "+filteredSpots.getNSpots(frame+1));
		double[][] cost1 = costs1.get(frame);
		LAPUtils.echoMatrix(cost1);

		System.out.println();
		System.out.println("With feature condition:");
		trackerSettings.linkingFeatureCutoffs.put(SpotFeature.MEAN_INTENSITY, (double) 1);
		tracker = new LAPTracker(filteredSpots, trackerSettings);
		tracker.createLinkingCostMatrices();
		SortedMap<Integer, double[][]> costs2 = tracker.getLinkingCosts();
		System.out.println("For frame pair "+frame+" -> "+(frame+1)+":");
		System.out.println("There are "+filteredSpots.getNSpots(frame)+" to link to "+filteredSpots.getNSpots(frame+1));
		double[][] cost2 = costs2.get(frame);
		LAPUtils.echoMatrix(cost2);

	}

}
