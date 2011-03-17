package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class TrackSchemePopupMenu extends JPopupMenu {

	private Object cell;
	private TrackSchemeFrame frame;
	private Point point;

	public TrackSchemePopupMenu(final TrackSchemeFrame frame, final Point point, final Object cell) {
		this.frame = frame;
		this.point = point;
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
					frame.graphComponent.startEditingAtCell(cell);
				}
			});

		} else { 
			
			if (vertices.size() > 1) {

				// Multi edit
				add(new AbstractAction("Edit " + vertices.size() +" spot names") {
					
					public void actionPerformed(ActionEvent e) {

						final JTextField editField = new JTextField();
						editField.setFont(FONT);
						editField.setBounds(point.x, point.y, 100, 20);
						frame.graphComponent.add(editField);
						editField.setVisible(true);
						editField.revalidate();
						frame.graphComponent.repaint();
						editField.requestFocusInWindow();
						editField.addActionListener(new ActionListener() {

							@Override
							public void actionPerformed(ActionEvent e) {
								for (mxCell cell : vertices)
									frame.getGraph().getCellToVertexMap().get(cell).setName(editField.getText());
								frame.graphComponent.remove(editField);
								frame.graphComponent.refresh();
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
						spotsInTime.put(spot.getFeature(Feature.POSITION_T), spot);
					}
					// Then link them in this order
					Iterator<Float> it = spotsInTime.keySet().iterator();
					Float previousTime = it.next();
					Spot previousSpot = spotsInTime.get(previousTime);
					Float currentTime;
					Spot currentSpot;
					while(it.hasNext()) {
						currentTime = it.next();
						currentSpot = spotsInTime.get(currentTime);
						// Link if not linked already
						if (frame.trackGraph.containsEdge(previousSpot, currentSpot))
							continue;
						DefaultWeightedEdge edge = frame.lGraph.addEdge(previousSpot, currentSpot);
						frame.lGraph.setEdgeWeight(edge, -1); // Default Weight
						// Update the MODEL graph as well
						frame.trackGraph.addEdge(previousSpot, currentSpot, edge);
						previousSpot = currentSpot;
					}
				}
			};
			add(linkAction);
		}

		// Remove
		if (selection.length > 0) {
			Action removeAction = new AbstractAction("Remove spots and links") {
				public void actionPerformed(ActionEvent e) {
					frame.getGraph().removeCells(selection);
				}
			};
			add(removeAction);
		}

	}
	
}
