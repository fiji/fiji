package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.ArrayList;
import java.util.HashSet;

import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.GraphSelectionModel;
import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotDisplayer;

/**
 * A utility class that ensures coherent behavior when playing with spot selection accross
 * a {@link SpotDisplayer} showing the spot and track data on the image and a {@link TrackSchemeFrame}
 * showing a map of tracks.
 * <p>
 * At construction, this class adds 1 listener to each of these 2 obejcts and ensure that a selection
 * in one is reflected in the second.
 * <p> 
 * @author Jean-Yves Tinevez <jeanyves.tinevez@gmail.com> - Feb 2, 2011
 *
 */
public class SpotSelectionManager {

	
	private SpotDisplayer displayer;
	private TrackSchemeFrame trackScheme;
	private HashSet<Spot> currentSpotSelection = new HashSet<Spot>();
	private HashSet<DefaultWeightedEdge> currentEdgeSelection = new HashSet<DefaultWeightedEdge>();
	private boolean doNotify = true;

	
	/*
	 * CONSTRUCTOR
	 */
	public SpotSelectionManager(final SpotDisplayer displayer, final TrackSchemeFrame trackScheme) {
		this.displayer = displayer;
		this.trackScheme= trackScheme;
		initListeners();
	}


	/*
	 * PRIVATE METHODS
	 */
	
	private void initListeners() {
		final GraphSelectionModel selectionModel = trackScheme.getJGraph().getSelectionModel();
			
		// Listen to spot selection modification in DISPLAYER and forward it to the trackscheme.
		displayer.addSpotSelectionListener(new SpotSelectionListener() {

		

			@Override
			public void valueChanged(SpotSelectionEvent event) {
				ArrayList<SpotCell> toAdd = new ArrayList<SpotCell>();
				ArrayList<SpotCell> toRemove = new ArrayList<SpotCell>();
				Spot[] spots = event.getSpots();
				for(Spot spot : spots) {
					SpotCell cell = (SpotCell) trackScheme.getAdapter().getVertexCell(spot);
					if (event.isAddedSpot(spot)) {
						toAdd.add(cell);
						currentSpotSelection.add(spot);
					} else {
						toRemove.add(cell);
						currentSpotSelection.remove(spot);
					}
				}
				doNotify  = false;
				selectionModel.addSelectionCells(toAdd.toArray());
				selectionModel.removeSelectionCells(toRemove.toArray());
				doNotify = true;
				if (spots.length > 0)
					trackScheme.centerViewOn(trackScheme.getAdapter().getVertexCell(spots[0]));
			}
		});

		// Listen to selection change in the TRACKSCHEME, extract SpotCells, and forward it to displayer
		trackScheme.getJGraph().addGraphSelectionListener(new GraphSelectionListener() {

			@Override
			public void valueChanged(GraphSelectionEvent event) {
				if (!doNotify)
					return;
				Object[] cells = event.getCells();
				for(Object cell : cells) {
					if (cell instanceof SpotCell) {
						SpotCell spotCell = (SpotCell) cell;
						if (event.isAddedCell(cell)) 
							currentSpotSelection.add(spotCell.getSpot());
						else
							currentSpotSelection.remove(spotCell.getSpot());
					} else if (cell instanceof TrackEdgeCell) {
						TrackEdgeCell edgeCell = (TrackEdgeCell) cell;
						if (event.isAddedCell(cell)) 
							currentEdgeSelection.add(edgeCell.getEdge());
						else
							currentEdgeSelection.remove(edgeCell.getEdge());
					}
				}
				displayer.highlightEdges(currentEdgeSelection);
				displayer.highlightSpots(currentSpotSelection);
				if (cells.length > 0 && cells[0] instanceof SpotCell) {
					SpotCell spotCell = (SpotCell) cells[0];
					displayer.centerViewOn(spotCell.getSpot());
				}
			}
		});

	}
	
}
