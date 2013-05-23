package fiji.plugin.trackmate.tests;

import fiji.plugin.trackmate.SelectionModel;
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
import org.scijava.util.AppUtils;

@SuppressWarnings("unused")
public class TrackVisualizerTestDrive {
	
	public static void main(String[] args) throws JDOMException, IOException {
	
		File file = new File(AppUtils.getBaseDirectory(TrackMate.class), "samples/FakeTracks.xml");
		ij.ImageJ.main(args);
		
		TmXmlReader reader = new TmXmlReader(file);
		TrackMateModel model = reader.getModel();
		Settings settings = new Settings();
		reader.readSettings(settings, null, null, null, null, null);
		TrackMate trackmate = new TrackMate(model, settings);
		
		System.out.println("From the XML file:");
		System.out.println("Found "+model.getTrackModel().getNTracks()+" tracks in total.");
		System.out.println("There were "+settings.getTrackFilters().size() + " track filter(s) applied on this list,");
		System.out.println("resulting in having only "+model.getTrackModel().getNFilteredTracks()+" visible tracks after filtering.");
		for(int i : model.getTrackModel().getFilteredTrackIDs()) {
			System.out.println(" - "+model.getTrackModel().trackToString(i));
		}
		System.out.println("Filtered tracks at this stage:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());
		System.out.println();
		
		FeatureFilter filter = new FeatureFilter(TrackBranchingAnalyzer.NUMBER_SPOTS, 5d, true);
		System.out.println("We add an extra track filter: "+filter);
		settings.addTrackFilter(filter);
		trackmate.execTrackFiltering(true);
		System.out.println("After filtering, retaining "+model.getTrackModel().getNFilteredTracks()+" tracks, which are:");
		System.out.println(model.getTrackModel().getFilteredTrackIDs());
		System.out.println();
			
		ImagePlus imp = settings.imp;
		
		// Launch ImageJ and display
		if (null != imp) {
			ij.ImageJ.main(args);
			imp.show();
		}
		
		trackmate.computeEdgeFeatures(true);
		
		// Instantiate displayer
		SelectionModel sm = new SelectionModel(model);
		final HyperStackDisplayer displayer = new HyperStackDisplayer(model, sm, settings.imp);
//		final SpotDisplayer3D displayer = new SpotDisplayer3D(model);
//		displayer.setRenderImageData(false);
		displayer.render();
		displayer.refresh();
		
		
		// Display Track scheme
		final TrackScheme trackScheme = new TrackScheme(model, sm);
		trackScheme.render();
		
		// Show control panel
		ConfigureViewsPanel panel = new ConfigureViewsPanel(trackmate);
		JFrame frame = new JFrame();
		frame.getContentPane().add(panel);
		frame.setSize(300, 500);
		frame.setVisible(true);
		
		// Show plot panel
		GrapherPanel plotPanel = new GrapherPanel(trackmate);
		JFrame graphFrame = new JFrame();
		graphFrame.getContentPane().add(plotPanel);
		graphFrame.setSize(300, 500);
		graphFrame.setVisible(true);
	}
}
