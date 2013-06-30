package fiji.plugin.trackmate.tests;

import ij.ImageJ;

import java.util.Random;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.tracking.dumb.DumbTracker;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class DumbTracker_TestDrive {

	public final void test() {
		
		double ranAmpl = 0.1;
		
		Random ran = new Random(5l);
		SpotCollection spots = new SpotCollection();
		
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j <= Math.min(i, 5); j++) {
				Spot spot = new Spot(new double[] { 
						1 + 10 * (j + ranAmpl * ran.nextGaussian()), 
						1 + 5 * (i + ranAmpl * ran.nextDouble()), 
						0 });
				spot.putFeature(Spot.RADIUS, 1d);
				spots.add(spot, i);
			}
		}
		
		for (int i = 10; i < 15; i++) {
			for (int j = 0; j < 15-i; j++) {
				Spot spot = new Spot(new double[] { 
						1 + 10 * (j + ranAmpl * ran.nextGaussian()), 
						1 + 5 * (i + ranAmpl * ran.nextDouble()), 
						0 });
				spot.putFeature(Spot.RADIUS, 1d);
				spots.add(spot, i);
			}
		}
		
		/*
		 * 2nd tier
		 */
		
		for (int i = 20; i < 30; i++) {
			for (int j = 0; j <= Math.min(i, 5); j++) {
				Spot spot = new Spot(new double[] { 
						1 + 10 * (j + ranAmpl * ran.nextGaussian()), 
						1 + 5 * (i + ranAmpl * ran.nextDouble()), 
						0 });
				spot.putFeature(Spot.RADIUS, 1d);
				spots.add(spot, i);
			}
		}
		
		for (int i = 40; i < 50; i++) {
			for (int j = 0; j < 5; j++) {
				Spot spot = new Spot(new double[] { 
						1 + 10 * (j + ranAmpl * ran.nextGaussian()), 
						1 + 5 * (i - 10 + ranAmpl * ran.nextDouble()), 
						0 });
				spot.putFeature(Spot.RADIUS, 1d);
				spots.add(spot, i);
			}
		}
		
		DumbTracker tracker = new DumbTracker(spots);
		if (!tracker.checkInput() || !tracker.process()) {
			System.err.println(tracker.getErrorMessage());
			return;
		}
		
		System.out.println("Done!");// DEBUG
		
		Model model = new Model();
		model.setSpots(spots, false);
		model.getTrackModel().setGraph(tracker.getResult());
		
		SelectionModel selectionModel = new SelectionModel(model);
		
		HyperStackDisplayer view = new HyperStackDisplayer(model, selectionModel);
		view.render();
		
		
		TrackScheme trackscheme = new TrackScheme(model, selectionModel);
		trackscheme.render();
		
	}
	
	
	public static void main(String[] args) {
		new ImageJ();
		new DumbTracker_TestDrive().test();
	}

}
