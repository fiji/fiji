package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphContext;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.PortView;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import com.jgraph.layout.JGraphFacade;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class TrackSchemeFrame extends JFrame {

	/*
	 * CONSTANTS
	 */

	public static final int Y_COLUMN_SIZE = 100;
	public static final int X_COLUMN_SIZE = 150;

	public static final int DEFAULT_CELL_WIDTH = 130;
	public static final int DEFAULT_CELL_HEIGHT = 80;


	private static final long serialVersionUID = 1L;
	private static final Dimension DEFAULT_SIZE = new Dimension(530, 320);
	private static final Color BACKGROUND_COLOR_1 = Color.GRAY;
	private static final Color BACKGROUND_COLOR_2 = Color.LIGHT_GRAY;
	private static final Color LINE_COLOR = Color.BLACK;

	/*
	 * INNER CLASSES
	 */

	/**
	 * The customized JPanel used to display a useful background under the graph.
	 * It displays in Y the time, and in X the track identity.
	 */
	private class GraphPane extends JPanel {

		private static final long serialVersionUID = 1L;
		private TreeSet<Float> instants;
		private int[] columnWidths = null;

		public GraphPane(Graph<Spot, DefaultEdge> graph) {
			super();
			setBackground(BACKGROUND_COLOR_1);

			instants = new TreeSet<Float>();
			for (Spot s : graph.vertexSet())
				instants.add(s.getFeature(Feature.POSITION_T));
		}


		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			int width = getSize().width;
			int height = getSize().height;
			float scale = (float) jGraph.getScale();

			// Scaled sizes
			int xcs 			= Math.round(X_COLUMN_SIZE*scale);
			int ycs 			= Math.round(Y_COLUMN_SIZE*scale);
			
			// Alternating row color
			g.setColor(BACKGROUND_COLOR_2);
			int y = 0;
			while (y < height) {
				g.fillRect(0, y, width, ycs);
				y += 2*ycs;
			}

			// Header separator
			g.setColor(LINE_COLOR);
			g.drawLine(0, ycs, width, ycs);
			g.drawLine(xcs, 0, xcs, height);

			// Row headers
			int x = xcs / 4;
			y = 3 * ycs / 2;
			g.setFont(SMALL_FONT.deriveFont(10*scale));
			for(Float instant : instants) {
				g.drawString("t="+instant, x, y);
				y += ycs;
			}

			// Column headers
			if (null != columnWidths) {
				x = xcs;
				for (int i = 0; i < columnWidths.length; i++) {
					x += (columnWidths[i]-1) * xcs;
					g.drawLine(x, 0, x, height);
				}
			}
		}


		public void setColumnWidths(int[] columnWidths) {
			this.columnWidths  = columnWidths;
		}

	}


	/*
	 * FIELDS
	 */


	private SimpleGraph<Spot, DefaultEdge> trackGraph;
	private JGraphModelAdapter<Spot, DefaultEdge> jGMAdapter;
	private ListenableUndirectedGraph<Spot, DefaultEdge> lGraph;
	private JGraph jGraph;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(SimpleGraph<Spot, DefaultEdge> trackGraph) {
		this.trackGraph = trackGraph;
		this.lGraph = new ListenableUndirectedGraph<Spot, DefaultEdge>(trackGraph);
		this.jGraph = createGraph();
		init();
		setSize(DEFAULT_SIZE);
	}

	

	/*
	 * PUBLIC METHODS
	 */



	/*
	 * PRIVATE METHODS
	 */
	
	private void connect(Object source, Object target) {
		if (source instanceof SpotCell && target instanceof SpotCell) {
			SpotCell s = (SpotCell) source;
			SpotCell t = (SpotCell) target;
			DefaultEdge e = lGraph.addEdge(s.getSpot(), t.getSpot());
//			lGraph.setEdgeWeight(e, 1); // Default Weight			
		} else {
			System.out.println("Try to connect a "+source.getClass().getCanonicalName()+" with a "+target.getClass().getCanonicalName());// DEBUG
		}
	}
	
	private void insert(final Point pt) {
		System.out.println("Insert!!");// TODO
	}
	
	private JGraph createGraph() {
		jGMAdapter = new JGraphModelAdapter<Spot, DefaultEdge>(
				lGraph,
				JGraphModelAdapter.createDefaultVertexAttributes(), 
				JGraphModelAdapter.createDefaultEdgeAttributes(lGraph),
				new CellFactory<Spot, DefaultEdge>() {

					@Override
					public org.jgraph.graph.DefaultEdge createEdgeCell(DefaultEdge e) {
						return new org.jgraph.graph.DefaultEdge("");				}

					@Override
					public DefaultGraphCell createVertexCell(Spot s) {
						return new SpotCell(s);
					}
					
				});
		
		SpotCellViewFactory factory = new SpotCellViewFactory();
		GraphLayoutCache graphLayoutCache = new GraphLayoutCache(jGMAdapter, factory);		
		MyGraph myGraph = new MyGraph(jGMAdapter, graphLayoutCache);
		myGraph.setMarqueeHandler(new MyMarqueeHandler());
		return myGraph;

	}

	
	private void init() {
		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		
		// Create back pane
		GraphPane backPane = new GraphPane(lGraph);
		BorderLayout layout = new BorderLayout();
		backPane.setLayout(layout);
		backPane.add(jGraph, BorderLayout.CENTER);
		jGraph.setOpaque(false);

		// Arrange graph layout
		JGraphFacade facade = new JGraphFacade(jGraph);
		JGraphTimeLayout graphLayout = new JGraphTimeLayout(trackGraph, jGMAdapter);
		graphLayout.run(facade);

		@SuppressWarnings("rawtypes")
		Map nested = facade.createNestedMap(false, false); // Obtain a map of the resulting attribute changes from the facade 
		jGraph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph 

		int[] columnWidths = graphLayout.getTrackColumnWidths();
		backPane.setColumnWidths(columnWidths);

		// Add the back pane as Center Component
		JScrollPane scrollPane = new JScrollPane(backPane);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

		getContentPane().add(scrollPane, BorderLayout.CENTER);
	}
	
	/**
	 * Instantiate the toolbar of the track scheme. For now, the toolbar only has the following actions:
	 * <ul>
	 * 	<li> Connect on/off
	 * 	<li> Reset zoom
	 * 	<li> Zoom in
	 * 	<li> Zoom out
	 * </ul>
	 * @return
	 */
	@SuppressWarnings("serial")
	public JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		
		// Toggle Connect Mode
		URL connectUrl = getClass().getResource("resources/connecton.gif");
		ImageIcon connectIcon = new ImageIcon(connectUrl);
		toolbar.add(new AbstractAction("", connectIcon) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setPortsVisible(!jGraph.isPortsVisible());
				URL connectUrl;
				if (jGraph.isPortsVisible())
					connectUrl = getClass().getResource("resources/connecton.gif");
				else
					connectUrl = getClass().getResource("resources/connectoff.gif");
				ImageIcon connectIcon = new ImageIcon(connectUrl);
				putValue(SMALL_ICON, connectIcon);
			}
		});

		// Separator
		toolbar.addSeparator();
		
		// Zoom Std
		toolbar.addSeparator();
		URL zoomUrl = getClass().getResource("resources/zoom.gif");
		ImageIcon zoomIcon = new ImageIcon(zoomUrl);
		toolbar.add(new AbstractAction("", zoomIcon) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setScale(1.0);
			}
		});
		// Zoom In
		URL zoomInUrl = getClass().getResource("resources/zoomin.gif");
		ImageIcon zoomInIcon = new ImageIcon(zoomInUrl);
		toolbar.add(new AbstractAction("", zoomInIcon) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setScale(2 * jGraph.getScale());
			}
		});
		// Zoom Out
		URL zoomOutUrl = getClass().getResource("resources/zoomout.gif");
		ImageIcon zoomOutIcon = new ImageIcon(zoomOutUrl);
		toolbar.add(new AbstractAction("", zoomOutIcon) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setScale(jGraph.getScale() / 2);
			}
		});

		return toolbar;
	}

	
	/**
	 *  PopupMenu
	 */
	@SuppressWarnings("serial")
	public JPopupMenu createPopupMenu(final Point pt, final Object cell) {
		JPopupMenu menu = new JPopupMenu();
		if (cell != null) {
			// Edit
			menu.add(new AbstractAction("Edit") {
				public void actionPerformed(ActionEvent e) {
					jGraph.startEditingAtCell(cell);
				}
			});
		}
		// Remove
		if (!jGraph.isSelectionEmpty()) {
			menu.addSeparator();
			menu.add(new AbstractAction("Remove") {
				public void actionPerformed(ActionEvent e) {
					System.out.println("Remove!!");// TODO
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

	
	
	/*
	 * INNER CLASSES
	 */
	
	/**
	 * Defines a Graph that uses the Shift-Button (Instead of the Right
	 * Mouse Button, which is Default) to add/remove point to/from an edge.
	 */
	public static class MyGraph extends JGraph {

		private static final long serialVersionUID = 5454138486162686890L;

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

	/**
	 * Custom MarqueeHandler
	 * MarqueeHandler that Connects Vertices and Displays PopupMenus
	 */
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

		private Object targetObject;

		private Object sourceObject;

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
			if (port != null && jGraph.isPortsVisible())
				return true;
			// Else Call Superclass
			return super.isForceMarqueeEvent(e);
		}

		// Display PopupMenu or Remember Start Location and First Port
		public void mousePressed(final MouseEvent e) {
			// If Right Mouse Button
			if (SwingUtilities.isRightMouseButton(e)) {
				// Find Cell in Model Coordinates
				Object cell = jGraph.getFirstCellForLocation(e.getX(), e.getY());
				// Create PopupMenu for the Cell
				JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
				// Display PopupMenu
				menu.show(jGraph, e.getX(), e.getY());
				// Else if in ConnectMode and Remembered Port is Valid
			} else if (port != null && jGraph.isPortsVisible()) {
				// Remember Start Location
				start = jGraph.toScreen(port.getLocation());
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
				Graphics g = jGraph.getGraphics();
				// Reset Remembered Port
				PortView newPort = getTargetPortAt(e.getPoint());
				// Do not flicker (repaint only on real changes)
				if (newPort == null || newPort != port) {
					// Xor-Paint the old Connector (Hide old Connector)
					paintConnector(Color.black, jGraph.getBackground(), g);
					// If Port was found then Point to Port Location
					port = newPort;
					if (port != null)
						current = jGraph.toScreen(port.getLocation());
					// Else If no Port was found then Point to Mouse Location
					else
						current = jGraph.snap(e.getPoint());
					// Xor-Paint the new Connector
					paintConnector(jGraph.getBackground(), Color.black, g);
				}
			}
			// Call Superclass
			super.mouseDragged(e);
		}

		public PortView getSourcePortAt(Point2D point) {
			// Disable jumping
			jGraph.setJumpToDefaultPort(false);
			PortView result;
			try {
				// Find a Port View in Model Coordinates and Remember
				result = jGraph.getPortViewAt(point.getX(), point.getY());
			} finally {
				jGraph.setJumpToDefaultPort(true);
			}
			Object obj = jGraph.getFirstCellForLocation(point.getX(), point.getY());
			sourceObject = obj;
			return result;
		}

		// Find a Cell at point and Return its first Port as a PortView
		protected PortView getTargetPortAt(Point2D point) {
			Object obj = jGraph.getFirstCellForLocation(point.getX(), point.getY());
			targetObject = obj;
			// Find a Port View in Model Coordinates and Remember
			return jGraph.getPortViewAt(point.getX(), point.getY());
		}

		// Connect the First Port and the Current Port in the Graph or Repaint
		public void mouseReleased(MouseEvent e) {
			highlight(jGraph, null);
			
			// If Valid Event, Current and First Port
			if (e != null && port != null && firstPort != null	&& firstPort != port) {
				// Then Establish Connection
				connect(sourceObject, targetObject);
				e.consume();
				// Else Repaint the Graph
			} else
				jGraph.repaint();
			// Reset Global Vars
			firstPort = port = null;
			start = current = null;
			// Call Superclass
			super.mouseReleased(e);
		}

		// Show Special Cursor if Over Port
		public void mouseMoved(MouseEvent e) {
			// Check Mode and Find Port
			if (e != null && getSourcePortAt(e.getPoint()) != null && jGraph.isPortsVisible()) {
				// Set Cusor on Graph (Automatically Reset)
				jGraph.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
			if (jGraph.isXorEnabled()) {
				// Set Foreground
				g.setColor(fg);
				// Set Xor-Mode Color
				g.setXORMode(bg);
				// Highlight the Current Port
				paintPort(jGraph.getGraphics());
				
				drawConnectorLine(g);
			} else {
				Rectangle dirty = new Rectangle((int) start.getX(), (int) start.getY(), 1, 1);
				
				if (current != null) {
					dirty.add(current);
				}
				
				dirty.grow(1, 1);
				
				jGraph.repaint(dirty);
				highlight(jGraph, port);
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
				Graphics2D g2d = (Graphics2D) g;
				g2d.setStroke(new BasicStroke(2));
				g.drawLine((int) start.getX(), (int) start.getY(), (int) current.getX(), (int) current.getY());
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
				r = jGraph.toScreen((Rectangle2D) r.clone());
				// Add Space For the Highlight Border
				r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
						.getHeight() + 6);
				// Paint Port in Preview (=Highlight) Mode
				jGraph.getUI().paintCell(g, port, r, true);
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
	

	/**
	 * Defines a EdgeHandle that uses the Shift-Button (Instead of the Right
	 * Mouse Button, which is Default) to add/remove point to/from an edge.
	 */
	public static class MyEdgeHandle extends EdgeView.EdgeHandle {
		private static final long serialVersionUID = -9009802856074145078L;

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
}
