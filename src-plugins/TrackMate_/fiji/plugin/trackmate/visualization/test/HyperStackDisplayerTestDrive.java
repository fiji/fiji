package fiji.plugin.trackmate.visualization.test;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;

import org.jdom.JDOMException;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.TMSelectionManager;
import fiji.plugin.trackmate.visualization.TrackMateModelManager;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;


public class HyperStackDisplayerTestDrive {

	public static void main(String[] args) throws JDOMException, IOException {
		
//		File file = new File(HyperStackDisplayerTestDrive.class.getResource("FakeTracks.xml").getFile());
//		File file = new File("E:/Users/JeanYves/Desktop/data/MAX_Celegans-5pc_17timepoints.xml");
		File file = new File("/Volumes/Data/Data/Confocal_LSM700/10-01-21/10-01-21-2hours.xml");
		TmXmlReader reader = new TmXmlReader(file);
		reader.parse();
		
		ij.ImageJ.main(args);

		SpotCollection spots = reader.getAllSpots();
		SpotCollection selectedSpots = reader.getSpotSelection(spots);
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph = reader.getTracks(selectedSpots);
		Settings settings = reader.getSettings();
		ImagePlus imp = reader.getImage();
		settings.imp = imp;
		settings.segmenterSettings = reader.getSegmenterSettings();
		
		final TrackMateModel model = new TrackMate_();
		model.setSettings(settings);
		model.setSpots(spots);
		model.setSpotSelection(selectedSpots);
		model.setTrackGraph(trackGraph);

		// Grab spot icons
		if (null != settings.imp)
			model.computeFeatures();
				
		final TrackMateModelView displayer = TrackMateModelView.instantiateView(TrackMateModelView.ViewType.HYPERSTACK_DISPLAYER, model);
		displayer.setSpots(spots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(trackGraph);
		displayer.setDisplayTrackMode(TrackDisplayMode.LOCAL_WHOLE_TRACKS, 5);
		displayer.setSpotNameVisible(true);
		
		
		final TrackSchemeFrame trackScheme = new TrackSchemeFrame(model);
		trackScheme.setVisible(true);
		
		/**
		 * This is used to echo the modifications made by the user in the track scheme.
		 */
//		trackScheme.addGraphListener(new GraphListener<Spot, DefaultWeightedEdge>() {
//
//			@Override
//			public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
//				Spot removedSpot = e.getVertex();
//				SpotCollection spots = model.getSelectedSpots();
//				for(List<Spot> st : spots.values())
//					if (st.remove(removedSpot))
//						break;
//				model.setSpotSelection(spots);
//				model.setTrackGraph(trackScheme.getTrackModel());
//				displayer.setSpotsToShow(spots);
//				displayer.setTrackGraph(trackScheme.getTrackModel());
//				displayer.refresh();
//			}
//
//			@Override
//			public void vertexAdded(GraphVertexChangeEvent<Spot> e) {}
//			
//			@Override
//			public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
//				model.setTrackGraph(trackScheme.getTrackModel());
//				displayer.setTrackGraph(trackScheme.getTrackModel());
//				displayer.refresh();
//			}
//
//			@Override
//			public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
//				displayer.setTrackGraph(trackScheme.getTrackModel());
//				displayer.refresh();
//			}
//
//		});

		
		TrackMateModelManager manager = new TrackMateModelManager(model);
		displayer.addSpotCollectionEditListener(manager); // Needed to compute the new feature first
		displayer.addSpotCollectionEditListener(trackScheme); // In the other order, it does not work (edge)
		
		TMSelectionManager selectionManager = new TMSelectionManager();
		selectionManager.registerDisplayer(displayer);
		selectionManager.registerDisplayer(trackScheme);
		
		displayer.addTMSelectionChangeListener(selectionManager);
		trackScheme.addTMSelectionChangeListener(selectionManager);
		
	}
	
}
