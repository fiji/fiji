package fiji.plugin.trackmate.tests;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.io.TmXmlWriter;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.tracking.SimpleLAPTracker;

public class ClassNames_TestDrive {

	public static <T extends RealType<T> & NativeType<T>> void main(String[] args) {
		
		SimpleLAPTracker<T> obj = new SimpleLAPTracker<T>();
		
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
		
		TrackMateModel<T> model = new TrackMateModel<T>();
		model.getSettings().tracker = SimpleLAPTracker.NAME;
		
		model.getSettings().trackerSettings = new TrackerKeys<T>();
		
		TmXmlWriter<T> writer = new TmXmlWriter<T>(model, Logger.DEFAULT_LOGGER);
		writer.appendTrackerSettings();
		
		System.out.println(writer.toString());
		
		

		
	}
	
}
