package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackCollection;

public class TrackSchemePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = -5168784267411318961L;
	private Object cell;
	private TrackSchemeFrame frame;
//	private Point point;

	public TrackSchemePopupMenu(final TrackSchemeFrame frame, final Point point, final Object cell) {
		this.frame = frame;
//		this.point = point;
		this.cell = cell;
		init();
	}
	
	
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
		
		if (cell != null) {
			// Edit
			add(new AbstractAction("Edit spot name") {
				public void actionPerformed(ActionEvent e) {
					frame.getGraphComponent().startEditingAtCell(cell);
				}
			});
			
			// Fold
			add(new AbstractAction("Fold/Unfold branch") {
				public void actionPerformed(ActionEvent e) {
					Object parent;
					if (frame.getGraph().isCellFoldable(cell, true))
						parent = cell;
					else
						parent = frame.getGraph().getModel().getParent(cell);
					frame.getGraph().foldCells(!frame.getGraph().isCellCollapsed(parent), false, new Object[] { parent });
				}
			});

		} else { 
			
			if (vertices.size() > 1) {

				// Multi edit
				add(new AbstractAction("Edit " + vertices.size() +" spot names") {
					
					public void actionPerformed(ActionEvent e) {
						final mxCell firstCell = vertices.remove(0);
						frame.getGraphComponent().startEditingAtCell(firstCell, e);
						frame.getGraphComponent().addListener(mxEvent.LABEL_CHANGED, new mxIEventListener() {
							
							@Override
							public void invoke(Object sender, mxEventObject evt) {
								for (mxCell cell : vertices) {
									cell.setValue(firstCell.getValue());
									frame.getGraph().getCellToVertexMap().get(cell).setName(firstCell.getValue().toString());
								}
								frame.getGraphComponent().removeListener(this);
							}
						});
					}
				});
			}

			// Link

			Action linkAction = new AbstractAction("Link spots") {

				@Override
				public void actionPerformed(ActionEvent e) {
					// Sort spots by time
					TreeMap<Float, Spot> spotsInTime = new TreeMap<Float, Spot>();
					for (mxCell cell : vertices) {
						Spot spot = frame.getGraph().getCellToVertexMap().get(cell);
						spotsInTime.put(spot.getFeature(SpotFeature.POSITION_T), spot);
					}
					// Then link them in this order
					final TrackCollection tracks = frame.getModel().getTracks();
					tracks.beginUpdate();
					try {
						frame.getGraph().getModel().beginUpdate();
						Iterator<Float> it = spotsInTime.keySet().iterator();
						Float previousTime = it.next();
						Spot previousSpot = spotsInTime.get(previousTime);
						Float currentTime;
						Spot currentSpot;
						while(it.hasNext()) {
							currentTime = it.next();
							currentSpot = spotsInTime.get(currentTime);
							// Link if not linked already
							if (tracks.containsEdge(previousSpot, currentSpot))
								continue;
							// This will update the mxGraph view
							tracks.addEdge(previousSpot, currentSpot, -1);
							// Update the MODEL graph as well
//							frame.getGraph().addEdge(edge, parent, source, target, index)
							previousSpot = currentSpot;
						}
					} finally {
						frame.getGraph().getModel().endUpdate();
						tracks.endUpdate();
					}
				}
			};
			add(linkAction);
		}

		// Remove
		if (selection.length > 0) {
			Action removeAction = new AbstractAction("Remove spots and links") {
				public void actionPerformed(ActionEvent e) {
					try {
					frame.getGraph().getModel().beginUpdate();
					frame.getGraph().removeCells(selection);
					} finally {
						frame.getGraph().getModel().endUpdate();
					}
				}
			};
			add(removeAction);
		}
		
		// Fold
		

	}
	
}
