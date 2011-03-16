package fiji.plugin.trackmate.visualization.test;

import ij.ImagePlus;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jdom.JDOMException;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.TrackMate_;
import fiji.plugin.trackmate.io.TmXmlReader;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.SpotDisplayer.TrackDisplayMode;
import fiji.plugin.trackmate.visualization.trackscheme.SpotSelectionManager;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;
import fiji.plugin.trackmate.visualization.TrackMateModelManager;


public class HyperStackDisplayerTestDrive {

	public static void main(String[] args) throws JDOMException, IOException {
		
		File file = new File(HyperStackDisplayerTestDrive.class.getResource("FakeTracks.xml").getFile());
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
		
		final TrackMateModelInterface model = new TrackMate_();
		model.setSettings(settings);
		model.setSpots(spots);
		model.setSpotSelection(selectedSpots);
		model.setTrackGraph(trackGraph);
				
		final SpotDisplayer displayer = SpotDisplayer.instantiateDisplayer(SpotDisplayer.DisplayerType.HYPERSTACK_DISPLAYER, model);
		displayer.setSpots(spots);
		displayer.setSpotsToShow(selectedSpots);
		displayer.setTrackGraph(trackGraph);
		displayer.setDisplayTrackMode(TrackDisplayMode.LOCAL_WHOLE_TRACKS, 5);
		
		final TrackSchemeFrame trackScheme = new TrackSchemeFrame(trackGraph, settings);
		trackScheme.setVisible(true);
		
		/**
		 * This is used to echo the modifications made by the user in the track scheme.
		 */
		trackScheme.addGraphListener(new GraphListener<Spot, DefaultWeightedEdge>() {

			@Override
			public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
				Spot removedSpot = e.getVertex();
				SpotCollection spots = model.getSelectedSpots();
				for(List<Spot> st : spots.values())
					if (st.remove(removedSpot))
						break;
				model.setSpotSelection(spots);
				model.setTrackGraph(trackScheme.getTrackModel());
				displayer.setSpotsToShow(spots);
				displayer.setTrackGraph(trackScheme.getTrackModel());
				displayer.refresh();
			}

			@Override
			public void vertexAdded(GraphVertexChangeEvent<Spot> e) {}
			
			@Override
			public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				model.setTrackGraph(trackScheme.getTrackModel());
				displayer.setTrackGraph(trackScheme.getTrackModel());
				displayer.refresh();
			}

			@Override
			public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				displayer.setTrackGraph(trackScheme.getTrackModel());
				displayer.refresh();
			}

		});

		displayer.addSpotCollectionEditListener(trackScheme);
		
		TrackMateModelManager manager = new TrackMateModelManager(model);
		displayer.addSpotCollectionEditListener(manager);
		
		new SpotSelectionManager(displayer, trackScheme);
		
	}
	
}
