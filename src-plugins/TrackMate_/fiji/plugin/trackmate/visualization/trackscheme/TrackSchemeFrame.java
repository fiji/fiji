package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.FONT;
import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphCellEditor;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.PortView;
import org.jgrapht.Graph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.ext.JGraphModelAdapter.CellFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.jgraph.layout.JGraphFacade;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;

public class TrackSchemeFrame extends JFrame {

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * CONSTANTS
	 */

	static final int Y_COLUMN_SIZE = 96;
	static final int X_COLUMN_SIZE = 160;

	static final int DEFAULT_CELL_WIDTH = 128;
	static final int DEFAULT_CELL_HEIGHT = 80;
	
	public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/track_scheme.png"));

	private static final long serialVersionUID = 1L;
	private static final Dimension DEFAULT_SIZE = new Dimension(800, 600);
	private static final Color BACKGROUND_COLOR_1 = Color.GRAY;
	private static final Color BACKGROUND_COLOR_2 = Color.LIGHT_GRAY;
	private static final Color LINE_COLOR = Color.BLACK;
	private static final int TABLE_CELL_WIDTH 		= 40;
	private static final int TABLE_ROW_HEADER_WIDTH = 50;
	private static final Color GRID_COLOR = Color.GRAY;
	
	private static final ImageIcon LINKING_ON_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/connect.png")); 
	private static final ImageIcon LINKING_OFF_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/forbid_connect.png")); 
	private static final ImageIcon RESET_ZOOM_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom.png")); 
	private static final ImageIcon ZOOM_IN_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_in.png")); 
	private static final ImageIcon ZOOM_OUT_ICON 	= new ImageIcon(TrackSchemeFrame.class.getResource("resources/zoom_out.png")); 
	private static final ImageIcon REFRESH_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/refresh.png"));
	private static final ImageIcon PLOT_ICON		= new ImageIcon(TrackSchemeFrame.class.getResource("resources/plots.png"));

	/*
	 * FIELDS
	 */

	private SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;
	private JGraphModelAdapter<Spot, DefaultWeightedEdge> jGMAdapter;
	private ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> lGraph;
	private JGraph jGraph;
	private InfoPane infoPane;
	private ArrayList<GraphListener<Spot, DefaultWeightedEdge>> graphListeners = new ArrayList<GraphListener<Spot,DefaultWeightedEdge>>();
	private GraphPane backPane;
	/** The spots currently selected. */
	private HashSet<Spot> spotSelection = new HashSet<Spot>();
	private JScrollPane scrollPane;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph) {
		this.trackGraph = trackGraph;
		this.lGraph = new ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge>(trackGraph);
		this.jGraph = createGraph();
		init();
		setSize(DEFAULT_SIZE);
	}

	/*
	 * PUBLIC METHODS
	 */

	public void addGraphListener(GraphListener<Spot, DefaultWeightedEdge> listener) {
		graphListeners.add(listener);
	}

	public boolean removeGraphListener(GraphListener<Spot, DefaultWeightedEdge> listener) {
		return graphListeners.remove(listener);
	}
	
	public List<GraphListener<Spot, DefaultWeightedEdge>> getGraphListeners() {
		return graphListeners;
	}
	
	/**
	 * Return an updated reference of the {@link Graph} that acts as a model for tracks. This graph will
	 * have his edges and vertices updated by the manual interaction occuring in this view.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getTrackModel() {
		return trackGraph;
	}
	
	/**
	 * Return a reference to the {@link JGraph} view in charge of rendering the track scheme.
	 */
	public JGraph getJGraph() {
		return jGraph;
	}
	
	/**
	 * Return a reference to the adapter in charge of converting the {@link Spot} and
	 * {@link DefaultWeightedEdge} for display in this JGraph frame.
	 */
	public JGraphModelAdapter<Spot, DefaultWeightedEdge> getAdapter() {
		return jGMAdapter;
	}
	
	public void centerViewOn(DefaultGraphCell cell) {
		Rectangle2D bounds = jGraph.getCellBounds(cell);
		Point2D center = new Point2D.Double(bounds.getCenterX()*jGraph.getScale(), bounds.getCenterY()*jGraph.getScale());
		scrollPane.getHorizontalScrollBar().setValue((int) center.getX() - scrollPane.getWidth()/2);
		scrollPane.getVerticalScrollBar().setValue((int) center.getY() - scrollPane.getHeight()/2);
	}
	

	/*
	 * PRIVATE METHODS
	 */
	
	private void plotSelectionData() {
		Feature xFeature = infoPane.featureSelectionPanel.getXKey();
		Set<Feature> yFeatures = infoPane.featureSelectionPanel.getYKeys();
		if (yFeatures.isEmpty())
			return;
		
		Object[] selectedCells = jGraph.getSelectionCells();
		if (selectedCells == null || selectedCells.length == 0)
			return;
		
		List<Spot> spots = new ArrayList<Spot>();
		for(Object cell : selectedCells)
			if (cell instanceof SpotCell)
				spots.add(((SpotCell)cell).getSpot());
		if (spots.isEmpty())
			return;
		
		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, spots, trackGraph);
		grapher.setVisible(true);
		
	}
	
	private void connect(Object source, Object target) {
		if (source instanceof SpotCell && target instanceof SpotCell) {
			SpotCell s = (SpotCell) source;
			SpotCell t = (SpotCell) target;
			// Update the listenable graph so that the VIEW is updated
			DefaultWeightedEdge edge = lGraph.addEdge(s.getSpot(), t.getSpot());
			if (null == edge) {
				infoPane.textPane.setText("Invalid edge.");
				return;
			}
			lGraph.setEdgeWeight(edge, -1); // Default Weight
			// Update the MODEL graph as well
			trackGraph.addEdge(s.getSpot(), t.getSpot(), edge);
		} else {
			System.out.println("Try to connect a "+source.getClass().getCanonicalName()+" with a "+target.getClass().getCanonicalName());// DEBUG
		}
	}
	
	private void remove(Object[] cells) {
		for (Object cell : cells) {
			if (cell instanceof TrackEdgeCell) {
				TrackEdgeCell trackEdge = (TrackEdgeCell) cell;
				DefaultWeightedEdge edge = trackEdge.getEdge();
				lGraph.removeEdge(edge);
				trackGraph.removeEdge(edge);
			} else if (cell instanceof SpotCell) {
				SpotCell spotCell = (SpotCell) cell;
				lGraph.removeVertex(spotCell.getSpot());
				trackGraph.removeVertex(spotCell.getSpot());
			}
		}
	}
	
	private JGraph createGraph() {
		jGMAdapter = new JGraphModelAdapter<Spot, DefaultWeightedEdge>(
				lGraph,
				JGraphModelAdapter.createDefaultVertexAttributes(), 
				JGraphModelAdapter.createDefaultEdgeAttributes(lGraph),
				new CellFactory<Spot, DefaultWeightedEdge>() {

					@Override
					public org.jgraph.graph.DefaultEdge createEdgeCell(DefaultWeightedEdge edge) {
						return new TrackEdgeCell(edge, lGraph);			}

					@Override
					public DefaultGraphCell createVertexCell(Spot spot) {
						return new SpotCell(spot);
					}
					
				});
		
		SpotCellViewFactory factory = new SpotCellViewFactory();
		GraphLayoutCache graphLayoutCache = new GraphLayoutCache(jGMAdapter, factory);		
		MyGraph myGraph = new MyGraph(jGMAdapter, graphLayoutCache);
		myGraph.setMarqueeHandler(new MyMarqueeHandler());
		AbstractCellView.cellEditor = new MyGraphCellEditor();
		return myGraph;
	}

	
	private void init() {
		// Frame look
		setIconImage(TRACK_SCHEME_ICON.getImage());
		setTitle("Track scheme");
		
		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);
		
		// Create back pane
		backPane = new GraphPane(lGraph);
		BorderLayout layout = new BorderLayout();
		backPane.setLayout(layout);
		backPane.add(jGraph, BorderLayout.CENTER);
		jGraph.setOpaque(false);

		// Arrange graph layout
		doTrackLayout();
		
		// Add the back pane as Center Component
		scrollPane = new JScrollPane(backPane);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
		
		// Add the info pane
		infoPane = new InfoPane();
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, scrollPane);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);

		// Listeners
		jGraph.addGraphSelectionListener(new GraphSelectionListener() {

			@Override
			public void valueChanged(GraphSelectionEvent e) {
				Object[] cells = e.getCells();
				for(Object cell : cells) {
					if (cell instanceof SpotCell) {
						SpotCell spotCell = (SpotCell) cell;
						if (e.isAddedCell(cell))
							spotSelection.add(spotCell.getSpot());
						else 
							spotSelection.remove(spotCell.getSpot());
					}
				}
				infoPane.echo(spotSelection);
				if (spotSelection.isEmpty())
					infoPane.scrollTable.setVisible(false);
				else
					infoPane.scrollTable.setVisible(true);
			}
		});

		// Forward graph change events to the listeners registered with this frame 
		lGraph.addGraphListener(new GraphListener<Spot, DefaultWeightedEdge>() {
			@Override
			public void vertexAdded(GraphVertexChangeEvent<Spot> e) {
				for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
					graphListener.vertexAdded(e);
			}
			@Override
			public void vertexRemoved(GraphVertexChangeEvent<Spot> e) {
				for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
					graphListener.vertexRemoved(e);
			}
			@Override
			public void edgeAdded(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
					graphListener.edgeAdded(e);
			}
			@Override
			public void edgeRemoved(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> e) {
				for (GraphListener<Spot, DefaultWeightedEdge> graphListener : graphListeners)
					graphListener.edgeRemoved(e);
			}
		});	
	}
	
	private void doTrackLayout() {
		JGraphFacade facade = new JGraphFacade(jGraph);
		JGraphTimeLayout graphLayout = new JGraphTimeLayout(trackGraph, jGMAdapter);
		graphLayout.run(facade);

		@SuppressWarnings("rawtypes")
		Map nested = facade.createNestedMap(false, false); // Obtain a map of the resulting attribute changes from the facade 
		jGraph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph 

		// Forward painting info to back pane
		backPane.setColumnWidths(graphLayout.getTrackColumnWidths());
		backPane.setColumnColor(graphLayout.getTrackColors());

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
	private JToolBar createToolBar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		
		// Toggle Connect Mode
		toolbar.add(new AbstractAction("Toggle linking", LINKING_ON_ICON) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setPortsVisible(!jGraph.isPortsVisible());
				ImageIcon connectIcon;
				if (jGraph.isPortsVisible())
					connectIcon = LINKING_ON_ICON;
				else
					connectIcon = LINKING_OFF_ICON;
				putValue(SMALL_ICON, connectIcon);
			}
		});

		// Separator
		toolbar.addSeparator();
		

		final Action zoomInAction;
		final Action zoomOutAction;
		final JButton zoomInButton = new JButton();
		final JButton zoomOutButton = new JButton();
		
		zoomInAction = new AbstractAction(null, ZOOM_IN_ICON) {
			public void actionPerformed(ActionEvent e) {
				double scale = jGraph.getScale();
				if (scale < 2)
					jGraph.setScale(2 * scale);
				if (scale > 2)
					zoomInButton.setEnabled(false);
				zoomOutButton.setEnabled(true);
			}
		};
		zoomOutAction = new AbstractAction(null, ZOOM_OUT_ICON) {
			public void actionPerformed(ActionEvent e) {
				double scale = jGraph.getScale();
				if (scale > (double)1/16)
					jGraph.setScale(scale/2);
				if (scale < (double)1/16)
					zoomOutButton.setEnabled(false);
				zoomInButton.setEnabled(true);
			}
		};
		
		zoomInButton.setAction(zoomInAction);
		zoomInButton.setToolTipText("Zoom in 2x");
		zoomOutButton.setAction(zoomOutAction);
		zoomOutButton.setToolTipText("Zoom out 2x");
		
			
		// Zoom Std
		toolbar.add(new AbstractAction("Reset zoom", RESET_ZOOM_ICON) {
			public void actionPerformed(ActionEvent e) {
				jGraph.setScale(1.0);
			}
		});
		// Zoom In
		
		toolbar.add(zoomInButton);
		// Zoom Out
		toolbar.add(zoomOutButton);

		// Separator
		toolbar.addSeparator();

		// Redo layout
		toolbar.add(new AbstractAction("Refresh", REFRESH_ICON) {
			public void actionPerformed(ActionEvent e) {
				doTrackLayout();
			}
		});
		
		// Separator
		toolbar.addSeparator();
		
		// Plot selection data
		toolbar.add(new AbstractAction("Plot selection data", PLOT_ICON) {
			@Override
			public void actionPerformed(ActionEvent e) {
				plotSelectionData();
			}
		});
		
		return toolbar;
	}

	
	/**
	 *  PopupMenu
	 */
	@SuppressWarnings("serial")
	private JPopupMenu createPopupMenu(final Point pt, final Object cell) {
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
		menu.addSeparator();
		Action removeAction = new AbstractAction("Remove") {
			public void actionPerformed(ActionEvent e) {
				remove(jGraph.getSelectionCells());
			}
		};
		menu.add(removeAction);
		if (jGraph.isSelectionEmpty()) 
			removeAction.setEnabled(false);
		return menu;
	}

	

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
		private Color[] columnColors;

		public GraphPane(Graph<Spot, DefaultWeightedEdge> graph) {
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
			g.setFont(FONT.deriveFont(12*scale).deriveFont(Font.BOLD));
			for(Float instant : instants) {
				g.drawString(String.format("Frame %.0f", instant+1), x, y);
				y += ycs;
			}

			// Column headers
			if (null != columnWidths) {
				x = xcs;
				for (int i = 0; i < columnWidths.length; i++) {
					int cw = columnWidths[i]-1;
					g.setColor(columnColors[i]);
					g.drawString(String.format("Track %d", i+1), x+20, ycs/2);
					g.setColor(LINE_COLOR);
//					((Graphics2D)g).setStroke(new BasicStroke(1));
//					for (int x2 = x + xcs; x2 < x + cw * xcs; x2 = x2 + xcs) 
//						g.drawLine(x2, 0, x2, height);						
//					((Graphics2D)g).setStroke(new BasicStroke(2));					
					x += cw * xcs;
					g.drawLine(x, 0, x, height);
				}
			}
		}


		public void setColumnWidths(int[] columnWidths) {
			this.columnWidths  = columnWidths;
		}
		
		public void setColumnColor(Color[] columnColors) {
			this.columnColors = columnColors;
		}


	}


	@SuppressWarnings("serial")
	private class InfoPane extends JPanel {

		private class RowHeaderRenderer extends JLabel implements ListCellRenderer {

			RowHeaderRenderer(JTable table) {
				JTableHeader header = table.getTableHeader();
				setOpaque(false);
				setBorder(UIManager.getBorder("TableHeader.cellBorder"));
				setForeground(header.getForeground());
				setBackground(header.getBackground());
				setFont(SMALL_FONT.deriveFont(9.0f));
				setHorizontalAlignment(SwingConstants.LEFT);				
			}

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				setText((value == null) ? "" : value.toString());
				return this;
			}
		}
		
		private JTextPane textPane;
		private JTable table;
		private JScrollPane scrollTable;
		private FeaturePlotSelectionPanel<Feature> featureSelectionPanel;

		public InfoPane() {
			init();
		}

		public void echo(Set<Spot> spots) {
			// Fill feature table
			DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
				@Override
				public boolean isCellEditable(int row, int column) { return false; }
			};
			for (Spot spot : spots) {
				Object[] columnData = new Object[Feature.values().length];
				for (int i = 0; i < columnData.length; i++) 
					columnData[i] = String.format("%.1f", spot.getFeature(Feature.values()[i]));
				dm.addColumn(spot.getName(), columnData);
			}
			table.setModel(dm);
			// Tune look
			
			DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
				public boolean isOpaque() { return false; };
				@Override
				public Color getBackground() {
					return Color.BLUE;
				}
			};
			headerRenderer.setBackground(Color.RED);
			
			
			DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
			renderer.setOpaque(false);
			renderer.setHorizontalAlignment(SwingConstants.RIGHT);
			renderer.setFont(SMALL_FONT);			
			for(int i=0; i<table.getColumnCount(); i++) {
				table.setDefaultRenderer(table.getColumnClass(i), renderer);
				table.getColumnModel().getColumn(i).setPreferredWidth(TABLE_CELL_WIDTH);
			}
			for (Component c : scrollTable.getColumnHeader().getComponents())
				c.setBackground(getBackground());
			scrollTable.getColumnHeader().setOpaque(false);
			
			// Set text
			textPane.setText("Selection:");
		}

		private void init() {

			AbstractListModel lm = new AbstractListModel() {
				String headers[] = new String[Feature.values().length];
				{
					for(int i=0; i<headers.length; i++)
						headers[i] = Feature.values()[i].shortName();			    	  
				}

				public int getSize() {
					return headers.length;
				}

				public Object getElementAt(int index) {
					return headers[index];
				}
			};

			table = new JTable();
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setOpaque(false);
			table.setFont(SMALL_FONT);
			table.setPreferredScrollableViewportSize(new Dimension(120, 400));
			table.getTableHeader().setOpaque(false);
			table.setSelectionForeground(Color.YELLOW.darker());
			table.setGridColor(GRID_COLOR);
			
			JList rowHeader = new JList(lm);
			rowHeader.setFixedCellWidth(TABLE_ROW_HEADER_WIDTH);
			rowHeader.setFixedCellHeight(table.getRowHeight());
			rowHeader.setCellRenderer(new RowHeaderRenderer(table));
			rowHeader.setBackground(getBackground());
			
			scrollTable = new JScrollPane(table);
			scrollTable.setRowHeaderView(rowHeader);
			scrollTable.getRowHeader().setOpaque(false);
			scrollTable.setOpaque(false);
			scrollTable.getViewport().setOpaque(false);
			scrollTable.setVisible(false); // for now

			textPane = new JTextPane();
			textPane.setCaretPosition(0);
//			StyledDocument styledDoc = textPane.getStyledDocument();
			textPane.setEditable(false);
			textPane.setOpaque(false);
			textPane.setFont(SMALL_FONT);
			
			featureSelectionPanel = new FeaturePlotSelectionPanel<Feature>(Feature.POSITION_T);
			
			setLayout(new BorderLayout());
			add(textPane, BorderLayout.NORTH);
			add(scrollTable, BorderLayout.CENTER);
			add(featureSelectionPanel, BorderLayout.SOUTH);
			
		}
		
	}
	
	
	@SuppressWarnings("serial")
	private static class MyGraphCellEditor extends DefaultGraphCellEditor {
		private Object target;
		
		public MyGraphCellEditor() {
			addCellEditorListener(new CellEditorListener() {

				@Override
				public void editingStopped(ChangeEvent e) {
					if (target instanceof SpotCell) {
						SpotCell spotCell = (SpotCell) target;
						spotCell.getSpot().setName(""+getCellEditorValue());
					}
				}

				@Override
				public void editingCanceled(ChangeEvent e) {}
			});
		}
		
		@Override
		public Component getGraphCellEditorComponent(JGraph graph, Object cell, boolean isSelected) {
			target = cell;
			return super.getGraphCellEditorComponent(graph, cell, isSelected);
		};
		
		
	};

	
	/**
	 * Defines a Graph that uses the Shift-Button (Instead of the Right
	 * Mouse Button, which is Default) to add/remove point to/from an edge.
	 */
	private static class MyGraph extends JGraph {

		private static final long serialVersionUID = 5454138486162686890L;
		
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
	private class MyMarqueeHandler extends BasicMarqueeHandler {

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

	


	
	
}
