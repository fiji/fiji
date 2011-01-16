/*
 * @(#)GraphEd.java 3.3 23-APR-04
 * 
 * Copyright (c) 2001-2004, Gaudenz Alder All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *  
 */
package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.UndoableEditEvent;

import org.jgraph.JGraph;
import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellHandle;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphCellEditor;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.Edge;
import org.jgraph.graph.EdgeRenderer;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphContext;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.GraphUndoManager;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortRenderer;
import org.jgraph.graph.PortView;
import org.jgraph.graph.VertexRenderer;
import org.jgraph.graph.VertexView;

import sun.font.CreatedFontTracker;

public class GraphEd extends JFrame implements GraphSelectionListener, KeyListener {

	// JGraph instance
	protected JGraph graph;

	// Undo Manager
	protected GraphUndoManager undoManager;

	// Actions which Change State
	protected Action undo, redo, remove, group, ungroup, tofront, toback, cut,
			copy, paste;

	// cell count that gets put in cell label
	protected int cellCount = 0;

	// Status Bar
	protected StatusBarGraphListener statusBar;

	//
	// Main
	//

	// Main Method
	public static void main(String[] args) {
		// Switch off D3D because of Sun XOR painting bug
		// See http://www.jgraph.com/forum/viewtopic.php?t=4066
//		System.setProperty("sun.java2d.d3d", "false");
		// Construct Frame
		JFrame frame = new JFrame("GraphEd");
		// Set Close Operation to Exit
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Add an Editor Panel
		frame.getContentPane().add(new GraphEd(null));
		// Fetch URL to Icon Resource
		URL jgraphUrl = GraphEd.class.getResource("resources/jgraph.gif");
		// If Valid URL
		if (jgraphUrl != null) {
			// Load Icon
			ImageIcon jgraphIcon = new ImageIcon(jgraphUrl);
			// Use in Window
			frame.setIconImage(jgraphIcon.getImage());
		}
		// Set Default Size
		frame.setSize(520, 390);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// Show Frame
		frame.setVisible(true);
	}

	//
	// Editor Panel
	//

	// Construct an Editor Panel
	public GraphEd(JGraph graph) {
		// Construct the Graph
//		graph = createGraph();
		this.graph = graph;
		// Use a Custom Marquee Handler
		graph.setMarqueeHandler(createMarqueeHandler());
		// Construct Command History
		//
		// Create a GraphUndoManager which also Updates the ToolBar
		undoManager = new GraphUndoManager() {
			// Override Superclass
			public void undoableEditHappened(UndoableEditEvent e) {
				// First Invoke Superclass
				super.undoableEditHappened(e);
				// Then Update Undo/Redo Buttons
				updateHistoryButtons();
			}
		};
		populateContentPane();
		
		/**********************************************************************
		 * GraphModelChange explaination
		 *********************************************************************/
		// Uncomment out the code below if you want to see the model change outputs
		// the new attributes are obtained from getPreviousAttributes(). The 
		// mode change is the undo edit of the change. That is, if you undo, 
		// that change edit is executed directly. So the previous attributes 
		// are the attributes before the undo and the attributes are the 
		// attributes after the undo. But when listening to a change,
		// getPreviousAttributes() returns the new attributes and
		// getAttributes() the old attributes.
//        graph.getModel().addGraphModelListener(new GraphModelListener() {
//            public void graphChanged(GraphModelEvent e) {
//                GraphModelEvent.GraphModelChange c = e.getChange();
//                if (c.getRemoved() == null && c.getInserted() == null) {
//                	Map previousAttributes = c.getPreviousAttributes();
//                	Set keySet = previousAttributes.keySet();
//                	Iterator iter = keySet.iterator();
//                    while (iter.hasNext()) {
//                    	Object attribute = iter.next();
//                        System.out.println("Prev Key " + String.valueOf(attribute));
//                        Object value = c.getPreviousAttributes().get(attribute);
//                        System.out.println("\t" + String.valueOf(value));
//                    }
//                    Map attributes = c.getAttributes();
//                    keySet = attributes.keySet();
//                    iter = keySet.iterator();
//                    while (iter.hasNext()) {
//                    	Object attribute = iter.next();
//                        System.out.println("Curr Key " + String.valueOf(attribute));
//                        Object value = c.getAttributes().get(attribute);
//                        System.out.println("\t" + String.valueOf(value));
//                    }
//                    System.out.println("\n\n");
//                }     
//            }
//        });

		installListeners(graph);
	}
	
	// Hook for subclassers
	protected void populateContentPane() {
		// Use Border Layout
		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		// Add the Graph as Center Component
		getContentPane().add(new JScrollPane(graph), BorderLayout.CENTER);
		statusBar = createStatusBar();
		getContentPane().add(statusBar, BorderLayout.SOUTH);
	}

	// Hook for subclassers
	protected JGraph createGraph() {
		JGraph graph = new MyGraph(new MyModel());
		graph.getGraphLayoutCache().setFactory(new DefaultCellViewFactory() {

			// Override Superclass Method to Return Custom EdgeView
			protected EdgeView createEdgeView(Object cell) {

				// Return Custom EdgeView
				return new EdgeView(cell) {

					/**
					 * Returns a cell handle for the view.
					 */
					public CellHandle getHandle(GraphContext context) {
						return new MyEdgeHandle(this, context);
					}

				};
			}
		});
		return graph;
	}

	// Hook for subclassers
	protected void installListeners(JGraph graph) {
		// Add Listeners to Graph
		//
		// Register UndoManager with the Model
		graph.getModel().addUndoableEditListener(undoManager);
		// Update ToolBar based on Selection Changes
		graph.getSelectionModel().addGraphSelectionListener(this);
		// Listen for Delete Keystroke when the Graph has Focus
		graph.addKeyListener(this);
		graph.getModel().addGraphModelListener(statusBar);
	}

	// Hook for subclassers
	protected void uninstallListeners(JGraph graph) {
		graph.getModel().removeUndoableEditListener(undoManager);
		graph.getSelectionModel().removeGraphSelectionListener(this);
		graph.removeKeyListener(this);
		graph.getModel().removeGraphModelListener(statusBar);
	}

	// Hook for subclassers
	protected BasicMarqueeHandler createMarqueeHandler() {
		return new MyMarqueeHandler();
	}

	// Insert a new Vertex at point
	public void insert(Point2D point) {
		// Construct Vertex with no Label
		DefaultGraphCell vertex = createDefaultGraphCell();
		// Create a Map that holds the attributes for the Vertex
		vertex.getAttributes().applyMap(createCellAttributes(point));
		// Insert the Vertex (including child port and attributes)
		graph.getGraphLayoutCache().insert(vertex);
	}

	// Hook for subclassers
	public Map createCellAttributes(Point2D point) {
		Map map = new Hashtable();
		// Snap the Point to the Grid
		if (graph != null) {
			point = graph.snap((Point2D) point.clone());
		} else {
			point = (Point2D) point.clone();
		}
		// Add a Bounds Attribute to the Map
		GraphConstants.setBounds(map, new Rectangle2D.Double(point.getX(),
				point.getY(), 0, 0));
		// Make sure the cell is resized on insert
		GraphConstants.setResize(map, true);
		// Add a nice looking gradient background
		GraphConstants.setGradientColor(map, Color.blue);
		// Add a Border Color Attribute to the Map
		GraphConstants.setBorderColor(map, Color.black);
		// Add a White Background
		GraphConstants.setBackground(map, Color.white);
		// Make Vertex Opaque
		GraphConstants.setOpaque(map, true);
		return map;
	}

	// Hook for subclassers
	protected DefaultGraphCell createDefaultGraphCell() {
		DefaultGraphCell cell = new DefaultGraphCell("Cell "
				+ new Integer(cellCount++));
		// Add one Floating Port
		cell.addPort();
		return cell;
	}

	// Insert a new Edge between source and target
	public void connect(Port source, Port target) {
		// Construct Edge with no label
		DefaultEdge edge = createDefaultEdge();
		if (graph.getModel().acceptsSource(edge, source)
				&& graph.getModel().acceptsTarget(edge, target)) {
			// Create a Map thath holds the attributes for the edge
			edge.getAttributes().applyMap(createEdgeAttributes());
			// Insert the Edge and its Attributes
			graph.getGraphLayoutCache().insertEdge(edge, source, target);
		}
	}

	// Hook for subclassers
	protected DefaultEdge createDefaultEdge() {
		return new DefaultEdge();
	}

	// Hook for subclassers
	public Map createEdgeAttributes() {
		Map map = new Hashtable();
		// Add a Line End Attribute
		GraphConstants.setLineEnd(map, GraphConstants.ARROW_SIMPLE);
		// Add a label along edge attribute
		GraphConstants.setLabelAlongEdge(map, true);
		return map;
	}

	// Create a Group that Contains the Cells
	public void group(Object[] cells) {
		// Order Cells by Model Layering
		cells = graph.order(cells);
		// If Any Cells in View
		if (cells != null && cells.length > 0) {
			DefaultGraphCell group = createGroupCell();
			// Insert into model
			graph.getGraphLayoutCache().insertGroup(group, cells);
		}
	}

	// Hook for subclassers
	protected DefaultGraphCell createGroupCell() {
		return new DefaultGraphCell();
	}

	// Returns the total number of cells in a graph
	protected int getCellCount(JGraph graph) {
		Object[] cells = graph.getDescendants(graph.getRoots());
		return cells.length;
	}

	// Ungroup the Groups in Cells and Select the Children
	public void ungroup(Object[] cells) {
		graph.getGraphLayoutCache().ungroup(cells);
	}

	// Determines if a Cell is a Group
	public boolean isGroup(Object cell) {
		// Map the Cell to its View
		CellView view = graph.getGraphLayoutCache().getMapping(cell, false);
		if (view != null)
			return !view.isLeaf();
		return false;
	}

	// Brings the Specified Cells to Front
	public void toFront(Object[] c) {
		graph.getGraphLayoutCache().toFront(c);
	}

	// Sends the Specified Cells to Back
	public void toBack(Object[] c) {
		graph.getGraphLayoutCache().toBack(c);
	}

	// Undo the last Change to the Model or the View
	public void undo() {
		try {
			undoManager.undo(graph.getGraphLayoutCache());
		} catch (Exception ex) {
			System.err.println(ex);
		} finally {
			updateHistoryButtons();
		}
	}

	// Redo the last Change to the Model or the View
	public void redo() {
		try {
			undoManager.redo(graph.getGraphLayoutCache());
		} catch (Exception ex) {
			System.err.println(ex);
		} finally {
			updateHistoryButtons();
		}
	}

	// Update Undo/Redo Button State based on Undo Manager
	protected void updateHistoryButtons() {
		// The View Argument Defines the Context
		undo.setEnabled(undoManager.canUndo(graph.getGraphLayoutCache()));
		redo.setEnabled(undoManager.canRedo(graph.getGraphLayoutCache()));
	}

	//
	// Listeners
	//

	// From GraphSelectionListener Interface
	public void valueChanged(GraphSelectionEvent e) {
		// Group Button only Enabled if more than One Cell Selected
		group.setEnabled(graph.getSelectionCount() > 1);
		// Update Button States based on Current Selection
		boolean enabled = !graph.isSelectionEmpty();
		remove.setEnabled(enabled);
		ungroup.setEnabled(enabled);
		tofront.setEnabled(enabled);
		toback.setEnabled(enabled);
		copy.setEnabled(enabled);
		cut.setEnabled(enabled);
	}

	//
	// KeyListener for Delete KeyStroke
	//
	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		// Listen for Delete Key Press
		if (e.getKeyCode() == KeyEvent.VK_DELETE)
			// Execute Remove Action on Delete Key Press
			remove.actionPerformed(null);
	}

	//
	// Custom Graph
	//

	// Defines a Graph that uses the Shift-Button (Instead of the Right
	// Mouse Button, which is Default) to add/remove point to/from an edge.
	public static class MyGraph extends JGraph {

		// Construct the Graph using the Model as its Data Source
		public MyGraph(GraphModel model) {
			this(model, null);
		}

		// Construct the Graph using the Model as its Data Source
		public MyGraph(GraphModel model, GraphLayoutCache cache) {
			super(model, cache);
			// Make Ports Visible by Default
			setPortsVisible(true);
			// Use the Grid (but don't make it Visible)
			setGridEnabled(true);
			// Set the Grid Size to 10 Pixel
			setGridSize(6);
			// Set the Tolerance to 2 Pixel
			setTolerance(2);
			// Accept edits if click on background
			setInvokesStopCellEditing(true);
			// Allows control-drag
			setCloneable(true);
			// Jump to default port on connect
			setJumpToDefaultPort(true);
		}

	}

	//
	// Custom Edge Handle
	//

	// Defines a EdgeHandle that uses the Shift-Button (Instead of the Right
	// Mouse Button, which is Default) to add/remove point to/from an edge.
	public static class MyEdgeHandle extends EdgeView.EdgeHandle {

		/**
		 * @param edge
		 * @param ctx
		 */
		public MyEdgeHandle(EdgeView edge, GraphContext ctx) {
			super(edge, ctx);
		}

		// Override Superclass Method
		public boolean isAddPointEvent(MouseEvent event) {
			// Points are Added using Shift-Click
			return event.isShiftDown();
		}

		// Override Superclass Method
		public boolean isRemovePointEvent(MouseEvent event) {
			// Points are Removed using Shift-Click
			return event.isShiftDown();
		}

	}

	//
	// Custom Model
	//

	// A Custom Model that does not allow Self-References
	public static class MyModel extends DefaultGraphModel {
		// Override Superclass Method
		public boolean acceptsSource(Object edge, Object port) {
			// Source only Valid if not Equal Target
			return (((Edge) edge).getTarget() != port);
		}

		// Override Superclass Method
		public boolean acceptsTarget(Object edge, Object port) {
			// Target only Valid if not Equal Source
			return (((Edge) edge).getSource() != port);
		}
	}

	//
	// Custom MarqueeHandler

	// MarqueeHandler that Connects Vertices and Displays PopupMenus
	public class MyMarqueeHandler extends BasicMarqueeHandler {

		// Holds the Start and the Current Point
		protected Point2D start, current;

		// Holds the First and the Current Port
		protected PortView port, firstPort;

		/**
		 * Component that is used for highlighting cells if
		 * the graph does not allow XOR painting.
		 */
		protected JComponent highlight = new JPanel();

		public MyMarqueeHandler() {
			// Configures the panel for highlighting ports
			highlight = createHighlight();
		}
	
		/**
		 * Creates the component that is used for highlighting cells if
		 * the graph does not allow XOR painting.
		 */
		protected JComponent createHighlight() {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			panel.setVisible(false);
			panel.setOpaque(false);
	
			return panel;
		}
		
		// Override to Gain Control (for PopupMenu and ConnectMode)
		public boolean isForceMarqueeEvent(MouseEvent e) {
			if (e.isShiftDown())
				return false;
			// If Right Mouse Button we want to Display the PopupMenu
			if (SwingUtilities.isRightMouseButton(e))
				// Return Immediately
				return true;
			// Find and Remember Port
			port = getSourcePortAt(e.getPoint());
			// If Port Found and in ConnectMode (=Ports Visible)
			if (port != null && graph.isPortsVisible())
				return true;
			// Else Call Superclass
			return super.isForceMarqueeEvent(e);
		}

		// Display PopupMenu or Remember Start Location and First Port
		public void mousePressed(final MouseEvent e) {
			// If Right Mouse Button
			if (SwingUtilities.isRightMouseButton(e)) {
				// Find Cell in Model Coordinates
				Object cell = graph.getFirstCellForLocation(e.getX(), e.getY());
				// Create PopupMenu for the Cell
				JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
				// Display PopupMenu
				menu.show(graph, e.getX(), e.getY());
				// Else if in ConnectMode and Remembered Port is Valid
			} else if (port != null && graph.isPortsVisible()) {
				// Remember Start Location
				start = graph.toScreen(port.getLocation());
				// Remember First Port
				firstPort = port;
			} else {
				// Call Superclass
				super.mousePressed(e);
			}
		}

		// Find Port under Mouse and Repaint Connector
		public void mouseDragged(MouseEvent e) {
			// If remembered Start Point is Valid
			if (start != null) {
				// Fetch Graphics from Graph
				Graphics g = graph.getGraphics();
				// Reset Remembered Port
				PortView newPort = getTargetPortAt(e.getPoint());
				// Do not flicker (repaint only on real changes)
				if (newPort == null || newPort != port) {
					// Xor-Paint the old Connector (Hide old Connector)
					paintConnector(Color.black, graph.getBackground(), g);
					// If Port was found then Point to Port Location
					port = newPort;
					if (port != null)
						current = graph.toScreen(port.getLocation());
					// Else If no Port was found then Point to Mouse Location
					else
						current = graph.snap(e.getPoint());
					// Xor-Paint the new Connector
					paintConnector(graph.getBackground(), Color.black, g);
				}
			}
			// Call Superclass
			super.mouseDragged(e);
		}

		public PortView getSourcePortAt(Point2D point) {
			// Disable jumping
			graph.setJumpToDefaultPort(false);
			PortView result;
			try {
				// Find a Port View in Model Coordinates and Remember
				result = graph.getPortViewAt(point.getX(), point.getY());
			} finally {
				graph.setJumpToDefaultPort(true);
			}
			return result;
		}

		// Find a Cell at point and Return its first Port as a PortView
		protected PortView getTargetPortAt(Point2D point) {
			// Find a Port View in Model Coordinates and Remember
			return graph.getPortViewAt(point.getX(), point.getY());
		}

		// Connect the First Port and the Current Port in the Graph or Repaint
		public void mouseReleased(MouseEvent e) {
			highlight(graph, null);
			
			// If Valid Event, Current and First Port
			if (e != null && port != null && firstPort != null
					&& firstPort != port) {
				// Then Establish Connection
				connect((Port) firstPort.getCell(), (Port) port.getCell());
				e.consume();
				// Else Repaint the Graph
			} else
				graph.repaint();
			// Reset Global Vars
			firstPort = port = null;
			start = current = null;
			// Call Superclass
			super.mouseReleased(e);
		}

		// Show Special Cursor if Over Port
		public void mouseMoved(MouseEvent e) {
			// Check Mode and Find Port
			if (e != null && getSourcePortAt(e.getPoint()) != null
					&& graph.isPortsVisible()) {
				// Set Cusor on Graph (Automatically Reset)
				graph.setCursor(new Cursor(Cursor.HAND_CURSOR));
				// Consume Event
				// Note: This is to signal the BasicGraphUI's
				// MouseHandle to stop further event processing.
				e.consume();
			} else
				// Call Superclass
				super.mouseMoved(e);
		}

		// Use Xor-Mode on Graphics to Paint Connector
		protected void paintConnector(Color fg, Color bg, Graphics g) {
			if (graph.isXorEnabled()) {
				// Set Foreground
				g.setColor(fg);
				// Set Xor-Mode Color
				g.setXORMode(bg);
				// Highlight the Current Port
				paintPort(graph.getGraphics());
				
				drawConnectorLine(g);
			} else {
				Rectangle dirty = new Rectangle((int) start.getX(), (int) start.getY(), 1, 1);
				
				if (current != null) {
					dirty.add(current);
				}
				
				dirty.grow(1, 1);
				
				graph.repaint(dirty);
				highlight(graph, port);
			}
		}
		
		// Overrides parent method to paint connector if
		// XOR painting is disabled in the graph
		public void paint(JGraph graph, Graphics g)
		{
			super.paint(graph, g);
			
			if (!graph.isXorEnabled())
			{
				g.setColor(Color.black);
				drawConnectorLine(g);
			}
		}
		
		protected void drawConnectorLine(Graphics g) {
			if (firstPort != null && start != null && current != null) {
				// Then Draw A Line From Start to Current Point
				g.drawLine((int) start.getX(), (int) start.getY(),
						(int) current.getX(), (int) current.getY());
			}
		}

		// Use the Preview Flag to Draw a Highlighted Port
		protected void paintPort(Graphics g) {
			// If Current Port is Valid
			if (port != null) {
				// If Not Floating Port...
				boolean o = (GraphConstants.getOffset(port.getAllAttributes()) != null);
				// ...Then use Parent's Bounds
				Rectangle2D r = (o) ? port.getBounds() : port.getParentView()
						.getBounds();
				// Scale from Model to Screen
				r = graph.toScreen((Rectangle2D) r.clone());
				// Add Space For the Highlight Border
				r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
						.getHeight() + 6);
				// Paint Port in Preview (=Highlight) Mode
				graph.getUI().paintCell(g, port, r, true);
			}
		}

		/**
		 * Highlights the given cell view or removes the highlight if
		 * no cell view is specified.
		 * 
		 * @param graph
		 * @param cellView
		 */
		protected void highlight(JGraph graph, CellView cellView)
		{
			if (cellView != null)
			{
				highlight.setBounds(getHighlightBounds(graph, cellView));

				if (highlight.getParent() == null)
				{
					graph.add(highlight);
					highlight.setVisible(true);
				}
			}
			else
			{
				if (highlight.getParent() != null)
				{
					highlight.setVisible(false);
					highlight.getParent().remove(highlight);
				}
			}
		}

		/**
		 * Returns the bounds to be used to highlight the given cell view.
		 * 
		 * @param graph
		 * @param cellView
		 * @return
		 */
		protected Rectangle getHighlightBounds(JGraph graph, CellView cellView)
		{
			boolean offset = (GraphConstants.getOffset(cellView.getAllAttributes()) != null);
			Rectangle2D r = (offset) ? cellView.getBounds() : cellView
					.getParentView().getBounds();
			r = graph.toScreen((Rectangle2D) r.clone());
			int s = 3;

			return new Rectangle((int) (r.getX() - s), (int) (r.getY() - s),
					(int) (r.getWidth() + 2 * s), (int) (r.getHeight() + 2 * s));
		}


	} // End of Editor.MyMarqueeHandler

	//
	//
	//

	//
	// PopupMenu and ToolBar
	//

	//
	//
	//

	//
	// PopupMenu
	//
	public JPopupMenu createPopupMenu(final Point pt, final Object cell) {
		JPopupMenu menu = new JPopupMenu();
		if (cell != null) {
			// Edit
			menu.add(new AbstractAction("Edit") {
				public void actionPerformed(ActionEvent e) {
					graph.startEditingAtCell(cell);
				}
			});
		}
		// Remove
		if (!graph.isSelectionEmpty()) {
			menu.addSeparator();
			menu.add(new AbstractAction("Remove") {
				public void actionPerformed(ActionEvent e) {
					remove.actionPerformed(e);
				}
			});
		}
		menu.addSeparator();
		// Insert
		menu.add(new AbstractAction("Insert") {
			public void actionPerformed(ActionEvent ev) {
				insert(pt);
			}
		});
		return menu;
	}

	//
	// ToolBar
	//
	public JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		// Insert
		URL insertUrl = getClass().getResource("resources/insert.gif");
		ImageIcon insertIcon = new ImageIcon(insertUrl);
		toolbar.add(new AbstractAction("", insertIcon) {
			public void actionPerformed(ActionEvent e) {
				insert(new Point(10, 10));
			}
		});

		// Toggle Connect Mode
		URL connectUrl = getClass().getResource("resources/connecton.gif");
		ImageIcon connectIcon = new ImageIcon(connectUrl);
		toolbar.add(new AbstractAction("", connectIcon) {
			public void actionPerformed(ActionEvent e) {
				graph.setPortsVisible(!graph.isPortsVisible());
				URL connectUrl;
				if (graph.isPortsVisible())
					connectUrl = getClass().getResource("resources/connecton.gif");
				else
					connectUrl = getClass().getResource("resources/connectoff.gif");
				ImageIcon connectIcon = new ImageIcon(connectUrl);
				putValue(SMALL_ICON, connectIcon);
			}
		});

		// Undo
		toolbar.addSeparator();
		URL undoUrl = getClass().getResource("resources/undo.gif");
		ImageIcon undoIcon = new ImageIcon(undoUrl);
		undo = new AbstractAction("", undoIcon) {
			public void actionPerformed(ActionEvent e) {
				undo();
			}
		};
		undo.setEnabled(false);
		toolbar.add(undo);

		// Redo
		URL redoUrl = getClass().getResource("resources/redo.gif");
		ImageIcon redoIcon = new ImageIcon(redoUrl);
		redo = new AbstractAction("", redoIcon) {
			public void actionPerformed(ActionEvent e) {
				redo();
			}
		};
		redo.setEnabled(false);
		toolbar.add(redo);

		//
		// Edit Block
		//
		toolbar.addSeparator();
		Action action;
		URL url;

		// Copy
		action = javax.swing.TransferHandler.getCopyAction();
		url = getClass().getResource("resources/copy.gif");
		toolbar.add(copy = new EventRedirector(action, new ImageIcon(url)));

		// Paste
		action = javax.swing.TransferHandler.getPasteAction();
		url = getClass().getResource("resources/paste.gif");
		toolbar.add(paste = new EventRedirector(action, new ImageIcon(url)));

		// Cut
		action = javax.swing.TransferHandler.getCutAction();
		url = getClass().getResource("resources/cut.gif");
		toolbar.add(cut = new EventRedirector(action, new ImageIcon(url)));

		// Remove
		URL removeUrl = getClass().getResource("resources/delete.gif");
		ImageIcon removeIcon = new ImageIcon(removeUrl);
		remove = new AbstractAction("", removeIcon) {
			public void actionPerformed(ActionEvent e) {
				if (!graph.isSelectionEmpty()) {
					Object[] cells = graph.getSelectionCells();
					cells = graph.getDescendants(cells);
					graph.getModel().remove(cells);
				}
			}
		};
		remove.setEnabled(false);
		toolbar.add(remove);

		// To Front
		toolbar.addSeparator();
		URL toFrontUrl = getClass().getResource("resources/tofront.gif");
		ImageIcon toFrontIcon = new ImageIcon(toFrontUrl);
		tofront = new AbstractAction("", toFrontIcon) {
			public void actionPerformed(ActionEvent e) {
				if (!graph.isSelectionEmpty())
					toFront(graph.getSelectionCells());
			}
		};
		tofront.setEnabled(false);
		toolbar.add(tofront);

		// To Back
		URL toBackUrl = getClass().getResource("resources/toback.gif");
		ImageIcon toBackIcon = new ImageIcon(toBackUrl);
		toback = new AbstractAction("", toBackIcon) {
			public void actionPerformed(ActionEvent e) {
				if (!graph.isSelectionEmpty())
					toBack(graph.getSelectionCells());
			}
		};
		toback.setEnabled(false);
		toolbar.add(toback);

		// Zoom Std
		toolbar.addSeparator();
		URL zoomUrl = getClass().getResource("resources/zoom.gif");
		ImageIcon zoomIcon = new ImageIcon(zoomUrl);
		toolbar.add(new AbstractAction("", zoomIcon) {
			public void actionPerformed(ActionEvent e) {
				graph.setScale(1.0);
			}
		});
		// Zoom In
		URL zoomInUrl = getClass().getResource("resources/zoomin.gif");
		ImageIcon zoomInIcon = new ImageIcon(zoomInUrl);
		toolbar.add(new AbstractAction("", zoomInIcon) {
			public void actionPerformed(ActionEvent e) {
				graph.setScale(2 * graph.getScale());
			}
		});
		// Zoom Out
		URL zoomOutUrl = getClass().getResource("resources/zoomout.gif");
		ImageIcon zoomOutIcon = new ImageIcon(zoomOutUrl);
		toolbar.add(new AbstractAction("", zoomOutIcon) {
			public void actionPerformed(ActionEvent e) {
				graph.setScale(graph.getScale() / 2);
			}
		});

		// Group
		toolbar.addSeparator();
		URL groupUrl = getClass().getResource("resources/group.gif");
		ImageIcon groupIcon = new ImageIcon(groupUrl);
		group = new AbstractAction("", groupIcon) {
			public void actionPerformed(ActionEvent e) {
				group(graph.getSelectionCells());
			}
		};
		group.setEnabled(false);
		toolbar.add(group);

		// Ungroup
		URL ungroupUrl = getClass().getResource("resources/ungroup.gif");
		ImageIcon ungroupIcon = new ImageIcon(ungroupUrl);
		ungroup = new AbstractAction("", ungroupIcon) {
			public void actionPerformed(ActionEvent e) {
				ungroup(graph.getSelectionCells());
			}
		};
		ungroup.setEnabled(false);
		toolbar.add(ungroup);

		return toolbar;
	}

	/**
	 * @return Returns the graph.
	 */
	public JGraph getGraph() {
		return graph;
	}

	/**
	 * @param graph
	 *            The graph to set.
	 */
	public void setGraph(JGraph graph) {
		this.graph = graph;
	}

	// This will change the source of the actionevent to graph.
	public class EventRedirector extends AbstractAction {

		protected Action action;

		// Construct the "Wrapper" Action
		public EventRedirector(Action a, ImageIcon icon) {
			super("", icon);
			this.action = a;
		}

		// Redirect the Actionevent
		public void actionPerformed(ActionEvent e) {
			e = new ActionEvent(graph, e.getID(), e.getActionCommand(), e
					.getModifiers());
			action.actionPerformed(e);
		}
	}

	/**
	 * Create a status bar
	 */
	protected StatusBarGraphListener createStatusBar() {
		return new EdStatusBar();
	}

	/**
	 * 
	 * @return a String representing the version of this application
	 */
	protected String getVersion() {
		return JGraph.VERSION;
	}

	/**
	 * @return Returns the redo.
	 */
	public Action getRedo() {
		return redo;
	}

	/**
	 * @param redo
	 *            The redo to set.
	 */
	public void setRedo(Action redo) {
		this.redo = redo;
	}

	/**
	 * @return Returns the undo.
	 */
	public Action getUndo() {
		return undo;
	}

	/**
	 * @param undo
	 *            The undo to set.
	 */
	public void setUndo(Action undo) {
		this.undo = undo;
	}

	public class StatusBarGraphListener extends JPanel implements GraphModelListener {

		/**
		 * Graph Model change event
		 */
		public void graphChanged(GraphModelEvent e) {
			updateStatusBar();
		}

		protected void updateStatusBar(){
			
		}
	}

	public class EdStatusBar extends StatusBarGraphListener {
		/**
		 * 
		 */
		protected JLabel leftSideStatus;

		/**
		 * contains the scale for the current graph
		 */
		protected JLabel rightSideStatus;

		/**
		 * Constructor for GPStatusBar.
		 * 
		 */
		public EdStatusBar() {
			super();
			// Add this as graph model change listener
			setLayout(new BorderLayout());
			leftSideStatus = new JLabel(getVersion());
			rightSideStatus = new JLabel("0/0Mb");
			leftSideStatus.setBorder(BorderFactory.createLoweredBevelBorder());
			rightSideStatus.setBorder(BorderFactory.createLoweredBevelBorder());
			add(leftSideStatus, BorderLayout.CENTER);
			add(rightSideStatus, BorderLayout.EAST);
		}

		protected void updateStatusBar() {
			Runtime runtime = Runtime.getRuntime();
			int freeMemory = (int) (runtime.freeMemory() / 1024);
			int totalMemory = (int) (runtime.totalMemory() / 1024);
			int usedMemory = (totalMemory - freeMemory);
			String str = (usedMemory / 1024) + "/" + (totalMemory / 1024)
					+ "Mb";
			rightSideStatus.setText(str);
		}

		/**
		 * @return Returns the leftSideStatus.
		 */
		public JLabel getLeftSideStatus() {
			return leftSideStatus;
		}

		/**
		 * @param leftSideStatus
		 *            The leftSideStatus to set.
		 */
		public void setLeftSideStatus(JLabel leftSideStatus) {
			this.leftSideStatus = leftSideStatus;
		}

		/**
		 * @return Returns the rightSideStatus.
		 */
		public JLabel getRightSideStatus() {
			return rightSideStatus;
		}

		/**
		 * @param rightSideStatus
		 *            The rightSideStatus to set.
		 */
		public void setRightSideStatus(JLabel rightSideStatus) {
			this.rightSideStatus = rightSideStatus;
		}
	}

	/**
	 * @return Returns the copy.
	 */
	public Action getCopy() {
		return copy;
	}

	/**
	 * @param copy
	 *            The copy to set.
	 */
	public void setCopy(Action copy) {
		this.copy = copy;
	}

	/**
	 * @return Returns the cut.
	 */
	public Action getCut() {
		return cut;
	}

	/**
	 * @param cut
	 *            The cut to set.
	 */
	public void setCut(Action cut) {
		this.cut = cut;
	}

	/**
	 * @return Returns the paste.
	 */
	public Action getPaste() {
		return paste;
	}

	/**
	 * @param paste
	 *            The paste to set.
	 */
	public void setPaste(Action paste) {
		this.paste = paste;
	}

	/**
	 * @return Returns the toback.
	 */
	public Action getToback() {
		return toback;
	}

	/**
	 * @param toback
	 *            The toback to set.
	 */
	public void setToback(Action toback) {
		this.toback = toback;
	}

	/**
	 * @return Returns the tofront.
	 */
	public Action getTofront() {
		return tofront;
	}

	/**
	 * @param tofront
	 *            The tofront to set.
	 */
	public void setTofront(Action tofront) {
		this.tofront = tofront;
	}

	/**
	 * @return Returns the remove.
	 */
	public Action getRemove() {
		return remove;
	}

	/**
	 * @param remove
	 *            The remove to set.
	 */
	public void setRemove(Action remove) {
		this.remove = remove;
	}
}
