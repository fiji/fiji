package fiji.plugin.trackmate.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMateModelInterface;
import fiji.plugin.trackmate.gui.DisplayerPanel;
import fiji.plugin.trackmate.visualization.SpotDisplayer;
import fiji.plugin.trackmate.visualization.trackscheme.SpotSelectionManager;
import fiji.plugin.trackmate.visualization.trackscheme.TrackSchemeFrame;

/**
 * A collection of helper methods meant to simply GUI actions.
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> Apr 1, 2011
 *
 */
public class GUIUtils {
	
	/**
	 * Link the displayer to the tuning display panel in the view.
	 */
	public static void execLinkDisplayerToTuningGUI(final DisplayerPanel displayerPanel, final SpotDisplayer spotDisplayer, final TrackMateModelInterface model) {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				
				displayerPanel.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent event) {
						if (event == displayerPanel.SPOT_COLOR_MODE_CHANGED) {
							spotDisplayer.setColorByFeature(displayerPanel.getColorSpotByFeature());
						} else if (event == displayerPanel.SPOT_VISIBILITY_CHANGED) {
							spotDisplayer.setSpotVisible(displayerPanel.isDisplaySpotSelected());
						} else if (event == displayerPanel.TRACK_DISPLAY_MODE_CHANGED) {
							spotDisplayer.setDisplayTrackMode(displayerPanel.getTrackDisplayMode(), displayerPanel.getTrackDisplayDepth());
						} else if (event == displayerPanel.TRACK_VISIBILITY_CHANGED) {
							spotDisplayer.setTrackVisible(displayerPanel.isDisplayTrackSelected());
						} else if (event == displayerPanel.SPOT_DISPLAY_RADIUS_CHANGED) {
							spotDisplayer.setRadiusDisplayRatio((float) displayerPanel.getSpotDisplayRadiusRatio());
						} else if (event == displayerPanel.SPOT_DISPLAY_LABEL_CHANGED) {
							spotDisplayer.setSpotNameVisible(displayerPanel.isDisplaySpotNameSelected());
						} else if (event == displayerPanel.TRACK_SCHEME_BUTTON_PRESSED) {
							launchTrackScheme(model, spotDisplayer);
						} 
					}
				});
			}
		});
	}
	

	/**
	 * Launch and display a track scheme frame, based on the given model, and ensures it is properly
	 * linked to the given displayer. 
	 *  
	 * @param model  The model to picture in the {@link TrackSchemeFrame}
	 * @param displayer  The {@link SpotDisplayer} to link the {@link TrackSchemeFrame} to
	 * @return  the created track scheme frame
	 */
	public static TrackSchemeFrame launchTrackScheme(final TrackMateModelInterface model, final SpotDisplayer displayer) {

		// Display Track scheme
		final TrackSchemeFrame trackScheme = new TrackSchemeFrame(model.getTrackGraph(), model.getSettings());
		trackScheme.setVisible(true);

		// Link it with displayer:		

		// Manual edit listener
		displayer.addSpotCollectionEditListener(trackScheme);

		// Selection manager
		new SpotSelectionManager(displayer, trackScheme);

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
		
		return trackScheme;
	}


}
