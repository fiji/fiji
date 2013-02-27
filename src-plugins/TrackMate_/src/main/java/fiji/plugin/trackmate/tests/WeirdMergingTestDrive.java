package fiji.plugin.trackmate.tests;

import java.io.File;
import java.util.Map;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.LAPUtils;
import fiji.plugin.trackmate.tracking.TrackerKeys;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;

public class WeirdMergingTestDrive {

	public static void main(String[] args) {
		
		TrackMate_ plugin = new TrackMate_();
		plugin.initModules();
		plugin.setLogger(Logger.DEFAULT_LOGGER);

		File file = new File("/Users/tinevez/Desktop/Test2.xml");
		TmXmlReader reader = new TmXmlReader(file, plugin);
		
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Could not read file: "+reader.getErrorMessage());
			return;
		}
	
		TrackMateModel model = plugin.getModel();
		System.out.println(model);
		
		LAPTracker tracker = new LAPTracker(model.getFilteredSpots(), Logger.DEFAULT_LOGGER);
		
		Map<String, Object> settings = LAPUtils.getDefaultLAPSettingsMap();
		settings.put(TrackerKeys.KEY_ALLOW_TRACK_MERGING, true);
		settings.put(TrackerKeys.KEY_ALLOW_TRACK_SPLITTING, false);
		System.out.println(TMUtils.echoMap(settings, 0));
		tracker.setSettings(settings);
		
		if (!tracker.checkInput() || !tracker.process()) {
			System.err.println("Error during tracking: "+tracker.getErrorMessage());
			return;
		}
		
		model.getTrackModel().setGraph(tracker.getResult());
		
		TrackScheme view = new TrackScheme(model);
		view.render();

	}

}
