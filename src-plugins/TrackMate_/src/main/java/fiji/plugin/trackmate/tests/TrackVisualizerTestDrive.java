package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.FeatureFilter;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.action.GrabSpotImageAction;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.gui.GrapherPanel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.jdom.JDOMException;

public class TrackVisualizerTestDrive {

	
	public static <T extends RealType<T> & NativeType<T>>  void main(String[] args) throws JDOMException, IOException {
	
		File file;
		if (!IJ.isWindows()) {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TrackMate_<T> plugin = new TrackMate_<T>();
		plugin.initModules();
		TmXmlReader<T> reader = new TmXmlReader<T>(file, plugin);
		if (!reader.checkInput() && !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel<T> model = plugin.getModel();
		
		System.out.println("From the XML file:");
		System.out.println("Found "+model.getNTracks()+" tracks in total.");
		System.out.println("There were "+model.getSettings().getTrackFilters().size() + " track filter(s) applied on this list,");
		System.out.println("resulting in having only "+model.getNFilteredTracks()+" visible tracks after filtering.");
		plugin.computeTrackFeatures();
		for(int i : model.getFilteredTrackIDs()) {
			System.out.println(" - "+model.trackToString(i));
		}
		System.out.println("Filtered tracks at this stage:");
		System.out.println(model.getFilteredTrackIDs());
		System.out.println();
		
		FeatureFilter filter = new FeatureFilter(TrackBranchingAnalyzer.NUMBER_SPOTS, 50d, true);
		System.out.println("We add an extra track filter: "+filter);
		model.getSettings().addTrackFilter(filter);
		plugin.execTrackFiltering(true);
		System.out.println("After filtering, retaining "+model.getNFilteredTracks()+" tracks, which are:");
		System.out.println(model.getFilteredTrackIDs());
		System.out.println();
			
		Settings<T> settings = model.getSettings();
		ImagePlus imp = settings.imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		GrabSpotImageAction<T> grabber = new GrabSpotImageAction<T>();
		grabber.execute(plugin);
		
		
		// Instantiate displayer
		final TrackMateModelView<T> displayer = new HyperStackDisplayer<T>();
		displayer.setModel(model);
		displayer.render();
		displayer.refresh();
		
		// Display Track scheme
		final TrackScheme<T> trackScheme = new TrackScheme<T>(model);
		trackScheme.render();
		
		// Show control panel
		DisplayerPanel<T> panel = new DisplayerPanel<T>();
		panel.setPlugin(plugin);
		panel.register(trackScheme);
		panel.register(displayer);
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);
		
		// Show plot panel
		GrapherPanel<T> plotPanel = new GrapherPanel<T>();
		plotPanel.setPlugin(plugin);
		JFrame graphFrame = new JFrame();
		graphFrame.getContentPane().add(plotPanel);
		graphFrame.setSize(300, 500);
		graphFrame.setVisible(true);
	}
}
