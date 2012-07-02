package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.tracking.SimpleLAPTracker;

public class ClassNames_TestDrive {

	public static void main(String[] args) {
		
		SimpleLAPTracker obj = new SimpleLAPTracker();
		
		System.out.println();
		System.out.println("Object:");
		System.out.println(obj);
		
		System.out.println();
		System.out.println("Name:");		
		System.out.println(obj.getClass().getName());
		
		System.out.println();
		System.out.println("Canonical name:");
		System.out.println(obj.getClass().getCanonicalName());

		System.out.println();
		System.out.println("Simple name:");
		System.out.println(obj.getClass().getSimpleName());
		
		System.out.println();
		System.out.println();
		
		TrackMateModel model = new TrackMateModel();
		model.getSettings().tracker = new SimpleLAPTracker();
		
		model.getSettings().trackerSettings = model.getSettings().tracker.createDefaultSettings();
		
		TmXmlWriter writer = new TmXmlWriter(model, Logger.DEFAULT_LOGGER);
		writer.appendTrackerSettings();
		
		System.out.println(writer.toString());
		
		

		
	}
	
}
