package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jgrapht.Graph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxPerimeter;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.SpotCollectionEditEvent;
import fiji.plugin.trackmate.visualization.SpotCollectionEditListener;
import fiji.plugin.trackmate.visualization.SpotDisplayer;

public class TrackSchemeFrame extends JFrame implements SpotCollectionEditListener {

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
	private static final int TABLE_CELL_WIDTH 		= 40;
	private static final int TABLE_ROW_HEADER_WIDTH = 50;
	private static final Color GRID_COLOR = Color.GRAY;


	/*
	 * FIELDS
	 */

	SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;
	ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> lGraph;
	private JGraphXAdapter<Spot, DefaultWeightedEdge> graph;
	InfoPane infoPane;
	private ArrayList<GraphListener<Spot, DefaultWeightedEdge>> graphListeners = new ArrayList<GraphListener<Spot,DefaultWeightedEdge>>();
	/** The spots currently selected. */
	private HashSet<Spot> spotSelection = new HashSet<Spot>();
	Settings settings;
	mxTrackGraphComponent graphComponent;
	private mxRubberband rubberband;
	private mxKeyboardHandler keyboardHandler;
	
	private static final HashMap<String, Object> BASIC_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> BASIC_EDGE_STYLE = new HashMap<String, Object>();
	static {

		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, Integer.toHexString(Color.WHITE.getRGB()));
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, Integer.toHexString(Color.BLACK.getRGB()));
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxScaledLabelShape.SHAPE_NAME);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter);
		
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKEWIDTH, 2.0f);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKECOLOR, Integer.toHexString(SpotDisplayer.DEFAULT_COLOR.getRGB()));

	}

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(final SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph, final Settings settings) {
		this.trackGraph = trackGraph;
		this.lGraph = new ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge>(trackGraph);
		this.graph = createGraph();
		graph.getStylesheet().getDefaultVertexStyle();
		this.settings = settings;
		init();
		setSize(DEFAULT_SIZE);
	}


	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Used to catch spot creation events that occurred elsewhere, for instance by manual editing in 
	 * the {@link SpotDisplayer}. 
	 */
	@Override
	public void collectionChanged(SpotCollectionEditEvent event) {

		if (event.getFlag() == SpotCollectionEditEvent.SPOT_CREATED) {

			int targetColumn = 0;
			for (int i = 0; i < graphComponent.getColumnWidths().length; i++)
				targetColumn += graphComponent.getColumnWidths()[i];

			mxCell cell = null;
			for (Spot spot : event.getSpots()) {
				// Instantiate JGraphX cell
				String spotName = (spot.getName() == null || spot.getName() != "") ? "ID"+spot.ID() : spot.getName();
				cell = new mxCell(spotName);
				cell.setId(null);
				cell.setVertex(true);
				// Position it
				float instant = spot.getFeature(Feature.POSITION_T);
				double x = (targetColumn-2) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
				double y = (0.5 + graphComponent.getRowForInstant().get(instant)) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2; 
				int height = Math.min(DEFAULT_CELL_WIDTH, spot.getImage().getHeight());
				height = Math.max(height, 12);
				mxGeometry geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);
				cell.setGeometry(geometry);
				// Finally add it to the mxGraph
				graph.addCell(cell, graph.getDefaultParent());
				// Echo the new cell to the maps
				graph.getVertexToCellMap().put(spot, cell);
				graph.getCellToVertexMap().put(cell, spot);
			}
			centerViewOn(cell);
		}

	}

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
	 * have his edges and vertices updated by the manual interaction occurring in this view.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getTrackModel() {
		return trackGraph;
	}

	/**
	 * Return a reference to the {@link mxGraph} view in charge of rendering the track scheme.
	 */
	public JGraphXAdapter<Spot, DefaultWeightedEdge> getGraph() {
		return graph;
	}

	public void centerViewOn(mxCell cell) {
		mxRectangle bounds = graph.getCellBounds(cell);
		if (null == bounds)
			return;
		double scale = graphComponent.getZoomFactor();
		Point2D center = new Point2D.Double(bounds.getCenterX()*scale, bounds.getCenterY()*scale);
		graphComponent.getHorizontalScrollBar().setValue((int) center.getX() - graphComponent.getWidth()/2);
		graphComponent.getVerticalScrollBar().setValue((int) center.getY() - graphComponent.getHeight()/2);
	}

	public void doTrackLayout() {
		mxTrackGraphLayout graphLayout = new mxTrackGraphLayout(lGraph, graph);
		graphLayout.execute(graph.getDefaultParent());

		// Forward painting info to graph component
		graphComponent.setColumnWidths(graphLayout.getTrackColumnWidths());
		graphComponent.setRowForInstant(graphLayout.getRowForInstant());
		graphComponent.setColumnColor(graphLayout.getTrackColors());
	}

	public void plotSelectionData() {
		Feature xFeature = infoPane.featureSelectionPanel.getXKey();
		Set<Feature> yFeatures = infoPane.featureSelectionPanel.getYKeys();
		if (yFeatures.isEmpty())
			return;

		Object[] selectedCells = graph.getSelectionCells();
		if (selectedCells == null || selectedCells.length == 0)
			return;

		List<Spot> spots = new ArrayList<Spot>();
		for(Object obj : selectedCells) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex()) {
				Spot spot = graph.getCellToVertexMap().get(cell);
				spots.add(spot);
			}
		}
		if (spots.isEmpty())
			return;

		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, spots, trackGraph, settings);
		grapher.setVisible(true);

	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be used for display.
	 */
	protected JGraphXAdapter<Spot, DefaultWeightedEdge> createGraph() {
		final JGraphXAdapter<Spot, DefaultWeightedEdge> graph = new JGraphXAdapter<Spot, DefaultWeightedEdge>(lGraph);
		graph.setAllowLoops(false);
		graph.setAllowDanglingEdges(false);
		graph.setCellsCloneable(false);
		graph.setCellsSelectable(true);
		graph.setCellsDisconnectable(false);
		graph.setGridEnabled(false);
		graph.setLabelsVisible(true);
		graph.setDropEnabled(false);
		graph.getStylesheet().setDefaultEdgeStyle(BASIC_EDGE_STYLE);
		graph.getStylesheet().setDefaultVertexStyle(BASIC_VERTEX_STYLE);

		// Set up listeners

		// Cells removed from JGraphX
		graph.addListener(mxEvent.CELLS_REMOVED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] objects = (Object[]) evt.getProperty("cells");
				for(Object obj : objects) {
					mxCell cell = (mxCell) obj;
					if (cell.isVertex()) {
						Spot spot = graph.getCellToVertexMap().get(cell);
						lGraph.removeVertex(spot);
						trackGraph.removeVertex(spot);
						fireVertexChangeEvent(new GraphVertexChangeEvent<Spot>(graph, GraphVertexChangeEvent.VERTEX_REMOVED, spot));
					} else if (cell.isEdge()) {
						DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
						lGraph.removeEdge(edge);
						trackGraph.removeEdge(edge);
						fireEdgeChangeEvent(new GraphEdgeChangeEvent<Spot, DefaultWeightedEdge>(graph, GraphEdgeChangeEvent.EDGE_REMOVED, edge));
					}
				}
			}
		});

		// Cell selection change
		graph.getSelectionModel().addListener(
				mxEvent.CHANGE, new mxIEventListener(){
					@SuppressWarnings("unchecked")
					public void invoke(Object sender, mxEventObject evt) {
						mxGraphSelectionModel model = (mxGraphSelectionModel) sender;
						Collection<Object> added = (Collection<Object>) evt.getProperty("added");
						Collection<Object> removed = (Collection<Object>) evt.getProperty("removed");
						selectionChanged(model, added, removed);
					}
				});
		
		// Return graph
		return graph;
	}

	protected mxTrackGraphComponent createGraphComponent() {
		final mxTrackGraphComponent gc = new mxTrackGraphComponent(this);
		gc.getVerticalScrollBar().setUnitIncrement(16);
		gc.getHorizontalScrollBar().setUnitIncrement(16);
		gc.setExportEnabled(true); // Seems to be required to have a preview when we move cells. Also give the ability to export a cell as an image clipping 
		gc.setSwimlaneSelectionEnabled(true);

		rubberband = new mxRubberband(gc);
		keyboardHandler = new mxKeyboardHandler(gc);
		gc.getGraphHandler();

		gc.getGraphControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					TrackSchemePopupMenu menu = new TrackSchemePopupMenu(TrackSchemeFrame.this, e.getPoint(), gc.getCellAt(e.getX(), e.getY(), false));
					menu.show(gc.getViewport().getView(), e.getX(), e.getY());
					e.consume();
				}
			}
		});
		
		return gc;

	}


	/**
	 * Instantiate the toolbar of the track scheme. Hook for subclassers.
	 */
	protected JToolBar createToolBar() {
		return new TrackSchemeToolbar(this);		
	}

	/*
	 * PRIVATE METHODS
	 */
	
	private void fireEdgeChangeEvent(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> event) {
		for(GraphListener<Spot, DefaultWeightedEdge> listener : graphListeners) {
			if (event.getType() == GraphEdgeChangeEvent.EDGE_ADDED)
				listener.edgeAdded(event);
			else if (event.getType() == GraphEdgeChangeEvent.EDGE_REMOVED)
				listener.edgeRemoved(event);
		}
	}

	private void fireVertexChangeEvent(GraphVertexChangeEvent<Spot> event) {
		for(GraphListener<Spot, DefaultWeightedEdge> listener : graphListeners) {
			if (event.getType() == GraphVertexChangeEvent.VERTEX_ADDED)
				listener.vertexAdded(event);
			else if (event.getType() == GraphVertexChangeEvent.VERTEX_REMOVED)
				listener.vertexRemoved(event);
		}
	}
	

	private void selectionChanged(mxGraphSelectionModel model, Collection<Object> added, Collection<Object> removed) { // Seems to be inverted
		spotSelection.clear();
		Object[] objects = model.getCells();
		for(Object obj : objects) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex())
				spotSelection.add(graph.getCellToVertexMap().get(cell));
		}
		infoPane.echo(spotSelection);		
	}

	private void remove(Object[] cells) {
		for (Object obj : cells) {
			mxCell cell = (mxCell) obj;
			if (cell.isEdge()) {
				DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
				lGraph.removeEdge(edge);
				trackGraph.removeEdge(edge);
			} else if (cell.isVertex()) {
				Spot spot = graph.getCellToVertexMap().get(cell);
				lGraph.removeVertex(spot);
				trackGraph.removeVertex(spot);
			}
		}
	}



	private void init() {
		// Frame look
		setIconImage(TRACK_SCHEME_ICON.getImage());
		setTitle("Track scheme");

		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// GraphComponent
		graphComponent = createGraphComponent();


		// Arrange graph layout
		doTrackLayout();

		// Add the info pane
		infoPane = new InfoPane();

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, graphComponent);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);
	}






	/**
	 *  PopupMenu
	 */
	protected JPopupMenu createPopupMenu(final Point point, final Object cell) {
		return new TrackSchemePopupMenu(this, point, cell);
		
	}



	/*
	 * INNER CLASSES
	 */

	private class InfoPane extends JPanel {

		private static final long serialVersionUID = 5889316637017869042L;

		private class RowHeaderRenderer extends JLabel implements ListCellRenderer, Serializable {

			private static final long serialVersionUID = -4068369886241557528L;

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

		private JTable table;
		private JScrollPane scrollTable;
		private FeaturePlotSelectionPanel<Feature> featureSelectionPanel;

		public InfoPane() {
			init();
		}

		@SuppressWarnings("serial")
		public void echo(Set<Spot> spots) {
			if (spots.size() == 0) {
				scrollTable.setVisible(false);
				return;
			}
			
			// Fill feature table
			DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
				@Override
				public boolean isCellEditable(int row, int column) { return false; }
			};
			for (Spot spot : spots) {
				Object[] columnData = new Object[Feature.values().length];
				for (int i = 0; i < columnData.length; i++) 
					columnData[i] = String.format("%.1f", spot.getFeature(Feature.values()[i]));
				String spotName = (spot.getName() == null || spot.getName() != "") ? "ID"+spot.ID() : spot.getName();
				dm.addColumn(spotName, columnData);
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
			scrollTable.setVisible(true);
			revalidate();
		}

		private void init() {

			@SuppressWarnings("serial")
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

			featureSelectionPanel = new FeaturePlotSelectionPanel<Feature>(Feature.POSITION_T);

			setLayout(new BorderLayout());
			add(scrollTable, BorderLayout.CENTER);
			add(featureSelectionPanel, BorderLayout.SOUTH);
		}
	}
}
