package fiji.plugin.trackmate.util;

import java.util.List;

import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

/**
 * A collection of helper methods meant to simply GUI actions.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 1, 2011
 *
 */
public class GUIUtils {
	
	/**
	 * Launch and display a track scheme frame, based on the given model, and ensures it is properly
	 * linked to the given displayer. 
	 *  
	 * @param model  The model to picture in the {@link TrackSchemeFrame}
	 * @param displayer  The {@link SpotDisplayer} to link the {@link TrackSchemeFrame} to
	 * @return  the created track scheme frame
	 */
	public static TrackSchemeFrame launchTrackScheme(final TrackMateModelInterface model, final Iterable<SpotDisplayer> displayers) {

		// Display Track scheme
		final TrackSchemeFrame trackScheme = new TrackSchemeFrame(model.getTrackGraph(), model.getSettings());
		trackScheme.setVisible(true);

		// Link it with displayer:		

		// Manual edit listener
		for(SpotDisplayer displayer : displayers)
			displayer.addSpotCollectionEditListener(trackScheme);

		// Selection manager
//		for(SpotDisplayer displayer : displayers)
//			new SpotSelectionManager(displayer, trackScheme);

		// Graph modification listener
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
				for(SpotDisplayer displayer : displayers) {
					displayer.setSpotsToShow(spots);
					displayer.setTrackGraph(trackScheme.getTrackModel());
					displayer.refresh();
				}
			}
			@Override
			public void vertexAdded(GraphVertexChangeEvent<Spot> e) {}
			@Override
			public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				model.setTrackGraph(trackScheme.getTrackModel());
				for(SpotDisplayer displayer : displayers) {
					displayer.setTrackGraph(trackScheme.getTrackModel());
					displayer.refresh();
				}
			}
			@Override
			public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				for(SpotDisplayer displayer : displayers) {
					displayer.setTrackGraph(trackScheme.getTrackModel());
					displayer.refresh();
				}
			}
		});
		
		return trackScheme;
	}


}
