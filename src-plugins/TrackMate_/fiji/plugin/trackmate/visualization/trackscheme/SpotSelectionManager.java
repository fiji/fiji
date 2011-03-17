package fiji.plugin.trackmate.visualization.trackscheme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraphSelectionModel;

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
		final mxGraphSelectionModel selectionModel = trackScheme.getGraph().getSelectionModel();
			
		// Listen to spot selection modification in DISPLAYER and forward it to the trackscheme.
		displayer.addSpotSelectionListener(new SpotSelectionListener() {

			@Override
			public void valueChanged(SpotSelectionEvent event) {
				ArrayList<mxCell> toAdd = new ArrayList<mxCell>();
				ArrayList<mxCell> toRemove = new ArrayList<mxCell>();
				Spot[] spots = event.getSpots();
				for(Spot spot : spots) {
					mxCell cell = (mxCell) trackScheme.getGraph().getVertexToCellMap().get(spot);
					if (event.isAddedSpot(spot)) {
						toAdd.add(cell);
						currentSpotSelection.add(spot);
					} else {
						toRemove.add(cell);
						currentSpotSelection.remove(spot);
					}
				}
				doNotify  = false;
				selectionModel.addCells(toAdd.toArray());
				selectionModel.removeCells(toRemove.toArray());
				doNotify = true;
				if (spots.length > 0)
					trackScheme.centerViewOn(trackScheme.getGraph().getVertexToCellMap().get(spots[0]));
			}
		});

		// Listen to selection change in the TRACKSCHEME, extract SpotCells, and forward it to displayer
		selectionModel.addListener(mxEvent.CHANGE, new mxIEventListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void invoke(Object sender, mxEventObject event) {
				if (!doNotify)
					return;

				Collection<Object> removed = (Collection<Object>) event.getProperty("added");
				Collection<Object> added = (Collection<Object>) event.getProperty("removed");

				if (null != added)
					for(Object obj : added) {
						mxCell cell = (mxCell) obj;
						if (cell.isVertex())
							currentSpotSelection.add(trackScheme.getGraph().getCellToVertexMap().get(cell));
						else if (cell.isEdge())
							currentEdgeSelection.add(trackScheme.getGraph().getCellToEdgeMap().get(cell));
					}

				if (null != removed)
					for(Object obj : removed) {
						mxCell cell = (mxCell) obj;
						if (cell.isVertex())
							currentSpotSelection.remove(trackScheme.getGraph().getCellToVertexMap().get(cell));
						else if (cell.isEdge())
							currentEdgeSelection.remove(trackScheme.getGraph().getCellToEdgeMap().get(cell));
					}
				
				displayer.highlightEdges(currentEdgeSelection);
				displayer.highlightSpots(currentSpotSelection);
				if (added != null && added.size() > 0) {
					Spot first = trackScheme.getGraph().getCellToVertexMap().get(added.iterator().next());
					if (null != first)
						displayer.centerViewOn(first);
				}
			}
		});

	}
	
}
