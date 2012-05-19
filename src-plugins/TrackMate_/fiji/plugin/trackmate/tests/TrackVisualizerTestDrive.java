package fiji.plugin.trackmate.tests;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.GrabSpotImageAction;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

public class TrackVisualizerTestDrive {

	
	public static void main(String[] args) throws JDOMException, IOException {
	
		File file;
		if (!IJ.isWindows()) {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file, Logger.DEFAULT_LOGGER);
		reader.parse();
		
		// Load objects 
		final TrackMateModel model = reader.getModel();
		TrackMate_ plugin = new TrackMate_(model);
		
		System.out.println("From the XML file:");
		System.out.println("Found "+model.getNTracks()+" tracks in total.");
		System.out.println("There were "+model.getTrackFilters().size() + " track filter(s) applied on this list,");
		System.out.println("resulting in having only "+model.getNFilteredTracks()+" visible tracks after filtering.");
		for(int i : model.getVisibleTrackIndices()) {
			System.out.println(" - "+model.trackToString(i));
		}
		
		FeatureFilter filter = new FeatureFilter(TrackBranchingAnalyzer.NUMBER_SPOTS, 50f, true);
		model.addTrackFilter(filter);
		plugin.execTrackFiltering();
		System.out.println();
		System.out.println("We had an extra track filter: "+filter);
		System.out.println("After filtering, retaining "+model.getNFilteredTracks()+" tracks.");
			
		ImagePlus imp = reader.getImage();
		Settings settings = reader.getSettings();
		reader.getSegmenterSettings(settings);
		settings.imp = imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		GrabSpotImageAction grabber = new GrabSpotImageAction();
		grabber.execute(plugin);
		
		
		// Instantiate displayer
		final TrackMateModelView displayer = new HyperStackDisplayer();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
		
		// Display Track scheme
		final TrackSchemeFrame frame = new TrackSchemeFrame(model);
		frame.setVisible(true);
		
	}
}
