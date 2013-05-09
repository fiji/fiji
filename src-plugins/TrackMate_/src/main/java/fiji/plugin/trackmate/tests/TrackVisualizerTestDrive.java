package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.features.track.TrackBranchingAnalyzer;
import fiji.plugin.trackmate.gui.GrapherPanel;
import fiji.plugin.trackmate.gui.panels.ConfigureViewsPanel;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.visualization.threedviewer.SpotDisplayer3D;
import fiji.plugin.trackmate.visualization.trackscheme.TrackScheme;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import org.jdom2.JDOMException;

@SuppressWarnings("unused")
public class TrackVisualizerTestDrive {
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		File file;
		if (!IJ.isWindows()) {
			file = new File("/Users/tinevez/Desktop/Data/FakeTracks.xml");
		} else {
			file = new File("E:/Users/JeanYves/Desktop/Data/FakeTracks.xml");
		}
		ij.ImageJ.main(args);
		
		TrackMate trackmate = new TrackMate();
		TmXmlReader reader = new TmXmlReader(file, trackmate);
		if (!reader.checkInput() || !reader.process()) {
			System.err.println("Problem loading the file:");
			System.err.println(reader.getErrorMessage());
		}
		TrackMateModel model = trackmate.getModel();
		
		System.out.println("From the XML file:");
		System.out.println("Found "+model.getTrackModel().getNTracks()+" tracks in total.");
		System.out.println("There were "+trackmate.getSettings().getTrackFilters().size() + " track filter(s) applied on this list,");
		System.out.println("resulting in having only "+model.getTrackModel().getNFilteredTracks()+" visible tracks after filtering.");
		trackmate.computeTrackFeatures(true);
		for(int i : model.getTrackModel().getFilteredTrackIDs()) {
			System.out.println(" - "+model.getTrackModel().trackToString(i));
		}
		System.out.println("Filtered tracks at this stage:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());
		System.out.println();
		
		FeatureFilter filter = new FeatureFilter(TrackBranchingAnalyzer.NUMBER_SPOTS, 5d, true);
		System.out.println("We add an extra track filter: "+filter);
		trackmate.getSettings().addTrackFilter(filter);
		trackmate.execTrackFiltering(true);
		System.out.println("After filtering, retaining "+model.getTrackModel().getNFilteredTracks()+" tracks, which are:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());
		System.out.println();
			
		Settings settings = trackmate.getSettings();
		ImagePlus imp = settings.imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		trackmate.computeEdgeFeatures(true);
		
		// Instantiate displayer
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model, trackmate.getSettings());
//		final SpotDisplayer3D displayer = new SpotDisplayer3D(model);
//		displayer.setRenderImageData(false);
		displayer.render();
		displayer.refresh();
		
		
		// Display Track scheme
		final TrackScheme trackScheme = new TrackScheme(model, trackmate.getSettings());
		trackScheme.render();
		
		// Show control panel
		ConfigureViewsPanel panel = new ConfigureViewsPanel();
		panel.setPlugin(trackmate);
		panel.register(trackScheme);
		panel.register(displayer);
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);
		
		// Show plot panel
		GrapherPanel plotPanel = new GrapherPanel();
		plotPanel.setPlugin(trackmate);
		JFrame graphFrame = new JFrame();
		graphFrame.getContentPane().add(plotPanel);
		graphFrame.setSize(300, 500);
		graphFrame.setVisible(true);
	}
}
