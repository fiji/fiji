package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackSchemePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = -5168784267411318961L;
	private static final boolean DEBUG = true;
	/** 
	 * The cell where the right-click was made, <code>null</code> if the right-click is made out of a cell.
	 */
	private Object cell;
	/**
	 * The TrackScheme instance.
	 */
	private TrackSchemeFrame frame;
	/**
	 * The right-click location.
	 */
	private Point point;
	/**
	 * The TrackMate model.
	 */
	private TrackMateModel model;
	/**
	 * The JGraphX model.
	 */
	private JGraphXAdapter graph;

	public TrackSchemePopupMenu(final TrackSchemeFrame frame, final Object cell, final TrackMateModel model, final JGraphXAdapter graph, final Point point) {
		this.frame = frame;
		this.cell = cell;
		this.model = model;
		this.graph = graph;
		this.point = point;
		init();
	}


	/*
	 * ACTIONS
	 */


	private void selectWholeTrack(final ArrayList<mxCell> vertices, final ArrayList<mxCell> edges) {
		List<Set<Spot>> trackSpots = model.getTrackSpots();
		List<Set<DefaultWeightedEdge>> trackEdges = model.getTrackEdges();
		int ntracks = trackSpots.size();
		for(int i=0; i<ntracks; i++) {

			Set<Spot> spots = trackSpots.get(i);
			Set<DefaultWeightedEdge> dwes = trackEdges.get(i);

			// From spots
			for(mxCell cell : vertices) {
				Spot spot = graph.getSpotFor(cell);
				if (null == spot) {
					if (DEBUG) {
						System.out.println("[TrackSchemePopupMenu] selectWholeTrack: tried to retrieve cell "+cell+", unknown to spot map.");
					}
					continue;
				}
				if (spots.contains(spot)) {
					model.addSpotToSelection(spots);
					model.addEdgeToSelection(dwes);
					vertices.remove(spot); // to speed up a bit
					break;
				}

			}

			// From spots
			for(mxCell cell : edges) {
				DefaultWeightedEdge dwe = graph.getEdgeFor(cell);
				if (null == dwe) {
					if (DEBUG) {
						System.out.println("[TrackSchemePopupMenu] select whole track: tried to retrieve cell "+cell+", unknown to edge map.");
					}
					continue;
				}
				if (edges.contains(dwe)) {
					model.addSpotToSelection(spots);
					model.addEdgeToSelection(dwes);
					vertices.remove(dwe); // to speed up a bit
					break;
				}

			}

		}
	}

	private void editSpotName() {
		frame.getGraphComponent().startEditingAtCell(cell);
	}

	private void toggleBranchFolding() {
		Object parent;
		if (frame.getGraph().isCellFoldable(cell, true))
			parent = cell;
		else
			parent = frame.getGraph().getModel().getParent(cell);
		frame.getGraph().foldCells(!frame.getGraph().isCellCollapsed(parent), false, new Object[] { parent });
	}

	private void multiEditSpotName(final ArrayList<mxCell> vertices, EventObject triggerEvent) {
		/* We want to display the editing window in the cell that is the closer
		 to where the user clicked. That is not perfect, because we can imaging the click
		 is made for from the selected cells, and that the editing window will not even
		 be displayed on the screen. No idea for that yet, because JGraphX is expecting to
		 receive a cell as location for the editing window.
		 */
		final mxCell tc = getClosestCell(vertices);
		vertices.remove(tc);
		frame.getGraphComponent().startEditingAtCell(tc, triggerEvent);
		frame.getGraphComponent().addListener(mxEvent.LABEL_CHANGED, new mxIEventListener() {

			@Override
			public void invoke(Object sender, mxEventObject evt) {
				for (mxCell cell : vertices) {
					cell.setValue(tc.getValue());
					frame.getGraph().getSpotFor(cell).setName(tc.getValue().toString());
				}
				frame.getGraphComponent().refresh();
				frame.getGraphComponent().removeListener(this);
			}
		});
	}

	/**
	 * Return, from the given list of cell, the one which is the closer to the {@link #point} of this
	 * instance.
	 */
	private mxCell getClosestCell(Iterable<mxCell> vertices) {
		double min_dist = Double.POSITIVE_INFINITY;
		mxCell target_cell = null;
		for (mxCell cell : vertices) {
			Point location = cell.getGeometry().getPoint();
			double dist = location.distanceSq(point);
			if (dist < min_dist) {
				min_dist = dist;
				target_cell = cell;
			}
		}
		return target_cell;
	}

	private void linkSpots() {

		// Sort spots by time
		TreeMap<Float, Spot> spotsInTime = new TreeMap<Float, Spot>();
		for (Spot spot : model.getSpotSelection()) {
			spotsInTime.put(spot.getFeature(Spot.POSITION_T), spot);
		}

		// Find adequate column
		int targetColumn = frame.getNextFreeColumn();

		// Then link them in this order
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try {
			Iterator<Float> it = spotsInTime.keySet().iterator();
			Float previousTime = it.next();
			Spot previousSpot = spotsInTime.get(previousTime);
			// If this spot belong to an invisible track, we make it visible
			Integer index = model.getTrackIndexOf(previousSpot);
			if (index != null && !model.isTrackVisible(index)) {
				frame.importTrack(index);
			}

			while(it.hasNext()) {
				Float currentTime = it.next();
				Spot currentSpot = spotsInTime.get(currentTime);
				// If this spot belong to an invisible track, we make it visible
				index = model.getTrackIndexOf(currentSpot);
				if (index != null && !model.isTrackVisible(index)) {
					frame.importTrack(index);
				}
				// Check that the cells matching the 2 spots exist in the graph
				mxICell currentCell = graph.getCellFor(currentSpot);
				if (null == currentCell) {
					currentCell = frame.insertSpotInGraph(currentSpot, targetColumn);
					if (DEBUG) {
						System.out.println("[TrackSchemePopupMenu] linkSpots: creating cell "+currentCell+" for spot "+currentSpot);
					}
				}
				mxICell previousCell = graph.getCellFor(previousSpot);
				if (null == previousCell) {
					previousCell = frame.insertSpotInGraph(previousSpot, targetColumn);
					if (DEBUG) {
						System.out.println("[TrackSchemePopupMenu] linkSpots: creating cell "+previousCell+" for spot "+previousSpot);
					}
				}
				// Check if the model does not have already a edge for these 2 spots (that is 
				// the case if the 2 spot are in an invisible track, which track scheme does not
				// know of).
				DefaultWeightedEdge edge = model.getEdge(previousSpot, currentSpot); 
				if (null == edge) {
					// We create a new edge between 2 spots, and pair it with a new cell edge.
					edge = model.addEdge(previousSpot, currentSpot, -1);
					mxCell cell = (mxCell) graph.addJGraphTEdge(edge);
					cell.setValue("New");
				} else {
					// We retrieve the edge, and pair it with a new cell edge.
					mxCell cell = (mxCell) graph.addJGraphTEdge(edge);
					cell.setValue(String.format("%.1f", model.getEdgeWeight(edge)));
					// Also, if the existing edge belonged to an existing invisible track, we make it visible.
					index = model.getTrackIndexOf(edge);
					if (index != null && !model.isTrackVisible(index)) {
						frame.importTrack(index);
					}
				}
				previousSpot = currentSpot;
			}
		} finally {
			graph.getModel().endUpdate();
			model.endUpdate();
		}
	}

	private void remove() {
		frame.getGraph().getModel().beginUpdate();
		try {
			frame.getGraph().removeCells(frame.getGraph().getSelectionCells());
		} finally {
			frame.getGraph().getModel().endUpdate();
		}
	}

	/*
	 * MENU COMPOSITION
	 */

	@SuppressWarnings("serial")
	private void init() {

		// Build selection categories
		final Object[] selection = frame.getGraph().getSelectionCells();
		final ArrayList<mxCell> vertices = new ArrayList<mxCell>();
		final ArrayList<mxCell> edges = new ArrayList<mxCell>();
		for(Object obj : selection) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex()) 
				vertices.add(cell);
			else if (cell.isEdge()) 
				edges.add(cell);
		}


		// Select whole tracks

		add(new AbstractAction("Select whole track") {
			public void actionPerformed(ActionEvent e) { selectWholeTrack(vertices, edges);	}
		});

		if (cell != null) {
			// Edit
			add(new AbstractAction("Edit spot name") {
				public void actionPerformed(ActionEvent e) { editSpotName(); }
			});

			// Fold
			add(new AbstractAction("Fold/Unfold branch") {
				public void actionPerformed(ActionEvent e) { toggleBranchFolding(); }
			});


		} else { 

			if (vertices.size() > 1) {

				// Multi edit
				add(new AbstractAction("Edit " + vertices.size() +" spot names") {
					public void actionPerformed(ActionEvent e) { multiEditSpotName(vertices, e); }
				});
			}

			// Link
			Action linkAction = new AbstractAction("Link " + model.getSpotSelection().size() +" spots") {
				public void actionPerformed(ActionEvent e) { linkSpots(); }
			};
			if (model.getSpotSelection().size() > 1) {
				add(linkAction);
			}
		}

		// Remove
		if (selection.length > 0) {
			Action removeAction = new AbstractAction("Remove spots and links") {
				public void actionPerformed(ActionEvent e) { remove(); 	}
			};
			add(removeAction);
		}
	}

}
