package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import org.jgrapht.Graph;
import org.jgrapht.event.GraphEdgeChangeEvent;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxPerimeter;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.visualization.TMModelEditEvent;
import fiji.plugin.trackmate.visualization.TMModelEditListener;
import fiji.plugin.trackmate.visualization.TMSelectionChangeEvent;
import fiji.plugin.trackmate.visualization.TMSelectionChangeListener;
import fiji.plugin.trackmate.visualization.TMSelectionDisplayer;
import fiji.plugin.trackmate.visualization.TrackMateModelView;

public class TrackSchemeFrame extends JFrame implements TMModelEditListener, TMSelectionDisplayer {

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
	static final int TABLE_CELL_WIDTH 		= 40;
	static final int TABLE_ROW_HEADER_WIDTH = 50;
	static final Color GRID_COLOR = Color.GRAY;

	/*
	 * FIELDS
	 */

	SimpleWeightedGraph<Spot, DefaultWeightedEdge> trackGraph;
	ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> lGraph;
	Settings settings;
	JGraphXAdapter<Spot, DefaultWeightedEdge> graph;

	/** The spots currently selected. */
	private HashSet<Spot> spotSelection = new HashSet<Spot>();
	/** The side pane in which spot selection info will be displayed.	 */
	InfoPane infoPane;
	/** The graph component in charge of painting the graph. */
	mxTrackGraphComponent graphComponent;
	/** The layout manager that can be called to re-arrange cells in the graph. */
	mxTrackGraphLayout graphLayout;
	/** Is linking allowed by default? Can be changed in the toolbar. */
	boolean defaultLinkingEnabled = false;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireTMSelectionChangeEvent = true;

	private ArrayList<TMModelEditListener> modelEditListeners = new ArrayList<TMModelEditListener>();
	private ArrayList<TMSelectionChangeListener> selectionChangeListeners = new ArrayList<TMSelectionChangeListener>();
	private TrackMateModel model;


	private static final HashMap<String, Object> BASIC_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> BASIC_EDGE_STYLE = new HashMap<String, Object>();
	static {

		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxScaledLabelShape.SHAPE_NAME);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.RectanglePerimeter);
		BASIC_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, "#FF00FF");

		BASIC_EDGE_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKEWIDTH, 2.0f);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKECOLOR, "#FF00FF");

	}

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(TrackMateModel model) {
		this.model = model;
		this.trackGraph = model.getTrackGraph();
		this.lGraph = new ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge>(trackGraph);
		this.graph = createGraph();
		this.settings = model.getSettings();
		this.graphLayout = new mxTrackGraphLayout(lGraph, graph, settings.dx);

		init();
		setSize(DEFAULT_SIZE);
	}

	/*
	 * PUBLIC METHODS
	 */

	/*
	 * Selection management
	 */

	public void addTMSelectionChangeListener(TMSelectionChangeListener listener) {
		selectionChangeListeners.add(listener);
	}

	public boolean removeTMSelectionChangeListener(TMSelectionChangeListener listener) {
		return selectionChangeListeners.remove(listener);
	}


	@Override
	public void highlightSpots(Collection<Spot> spots) {
		doFireTMSelectionChangeEvent  = false;
		mxGraphSelectionModel model = graph.getSelectionModel();
		// Remove old spots
		Object[] objects = model.getCells();
		for (Object obj : objects) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex())
				model.removeCell(cell);
		}
		// Add new ones
		Object[] newSpots = new Object[spots.size()];
		Iterator<Spot> it = spots.iterator();
		for (int i = 0; i < newSpots.length; i++) 
			newSpots[i] = graph.getVertexToCellMap().get(it.next());
		model.addCells(newSpots);
		doFireTMSelectionChangeEvent  = true;
	}

	@Override
	public void highlightEdges(Set<DefaultWeightedEdge> edges) {
		doFireTMSelectionChangeEvent  = false;
		mxGraphSelectionModel model = graph.getSelectionModel();
		// Remove old edges
		Object[] objects = model.getCells();
		for (Object obj : objects) {
			mxCell cell = (mxCell) obj;
			if (!cell.isVertex())
				model.removeCell(cell);
		}
		// Add new ones
		Object[] newEdges = new Object[edges.size()];
		Iterator<DefaultWeightedEdge> it = edges.iterator();
		for (int i = 0; i < newEdges.length; i++) 
			newEdges[i] = graph.getEdgeToCellMap().get(it.next());
		model.addCells(newEdges);
		doFireTMSelectionChangeEvent  = true;
	}

	@Override
	public void centerViewOn(Spot spot) {
		centerViewOn(graph.getVertexToCellMap().get(spot));
	}

	/*
	 * TM model editing management
	 */

	public void addTMModelEditListener(TMModelEditListener listener) {
		modelEditListeners.add(listener);
	}

	public boolean removeTMModelEditListener(TMModelEditListener listener) {
		return modelEditListeners.remove(listener);
	}

	/**
	 * Used to catch spot creation events that occurred elsewhere, for instance by manual editing in 
	 * the {@link TrackMateModelView}. 
	 * <p>
	 * We have to deal with the graph modification ourselves here, because the {@link TrackMateModel} model
	 * holds a non-listenable JGraphT instance. A modification made to the model would not be reflected
	 * on the graph here.
	 */
	@Override
	public void modelChanged(final TMModelEditEvent event) {

		try {
			graph.getModel().beginUpdate();
			mxCell cellAdded = null;
			ArrayList<mxCell> cellsToRemove = new ArrayList<mxCell>();

			int targetColumn = 0;
			for (int i = 0; i < graphComponent.getColumnWidths().length; i++)
				targetColumn += graphComponent.getColumnWidths()[i];


			for (Spot spot : event.getSpots() ) {

				if (event.getSpotFlag(spot) == TMModelEditEvent.SPOT_ADDED) {

					// Instantiate JGraphX cell
					cellAdded = new mxCell(spot.toString());
					cellAdded.setId(null);
					cellAdded.setVertex(true);
					// Position it
					float instant = spot.getFeature(Feature.POSITION_T);
					double x = (targetColumn-2) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
					double y = (0.5 + graphComponent.getRowForInstant().get(instant)) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2; 
					int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Feature.RADIUS) / settings.dx));
					height = Math.max(height, 12);
					mxGeometry geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);
					cellAdded.setGeometry(geometry);
					// Set its style
					graph.getModel().setStyle(cellAdded, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
					// Finally add it to the mxGraph
					graph.addCell(cellAdded, graph.getDefaultParent());
					// Echo the new cell to the maps
					graph.getVertexToCellMap().put(spot, cellAdded);
					graph.getCellToVertexMap().put(cellAdded, spot);
					targetColumn++;

				} else if (event.getSpotFlag(spot) == TMModelEditEvent.SPOT_MODIFIED) {

					mxCell cell = graph.getVertexToCellMap().get(spot);
					String style = cell.getStyle();
					style = mxUtils.setStyle(style, mxConstants.STYLE_IMAGE, "data:image/base64,"+spot.getImageString());
					graph.getModel().setStyle(cell, style);
					int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Feature.RADIUS) / settings.dx));
					graph.getModel().getGeometry(cell).setHeight(height);

				}  else if (event.getSpotFlag(spot) == TMModelEditEvent.SPOT_DELETED) {

					mxCell cell = graph.getVertexToCellMap().get(spot);
					cellsToRemove .add(cell);
				}

			}

			graph.removeCells(cellsToRemove.toArray(), true);

		} finally {
			graph.getModel().endUpdate();
		}
	}

	/**
	 * Return an updated reference of the {@link Graph} that acts as a model for tracks. This graph will
	 * have his edges and vertices updated by the manual interaction occurring in this view.
	 */
	public SimpleWeightedGraph<Spot, DefaultWeightedEdge> getTrackModel() {
		return trackGraph;
	}

	public void centerViewOn(mxCell cell) {
		graphComponent.scrollCellToVisible(cell, true);
	}

	public void doTrackLayout() {
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

		HashSet<Spot> spots = new HashSet<Spot>();
		for(Object obj : selectedCells) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex()) {
				Spot spot = graph.getCellToVertexMap().get(cell);

				if (spot == null) {
					// We might have a parent cell, that holds many vertices in it
					// Retrieve them and add them if they are not already.
					int n = cell.getChildCount();
					for (int i = 0; i < n; i++) {
						mxICell child = cell.getChildAt(i);
						Spot childSpot = graph.getCellToVertexMap().get(child);
						if (null != childSpot)
							spots.add(childSpot);
					}

				} else 
					spots.add(spot);
			}
		}
		if (spots.isEmpty())
			return;

		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, new ArrayList<Spot>(spots), trackGraph, settings);
		grapher.setVisible(true);

	}

	/*
	 * PROTECTED METHODS
	 */

	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be used for display.
	 * Hook for subclassers.
	 */
	protected JGraphXAdapter<Spot, DefaultWeightedEdge> createGraph() {
		final JGraphXAdapter<Spot, DefaultWeightedEdge> graph = new JGraphXAdapter<Spot, DefaultWeightedEdge>(lGraph) {

			/**
			 * Overridden method so that when a label is changed, we change the target spot's name.
			 */
			@Override
			public void cellLabelChanged(Object cell, Object value, boolean autoSize) {
				model.beginUpdate();
				try {
					Spot spot = getCellToVertexMap().get(cell);
					if (null == spot)
						return;
					String str = (String) value;
					spot.setName(str);
					getModel().setValue(cell, str);

					if (autoSize) {
						cellSizeUpdated(cell, false);
					}
				} finally {
					model.endUpdate();
				}
			}
		};

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


		// Set spot image to cell style
		try {
			graph.getModel().beginUpdate();
			for(mxCell cell : graph.getCellToVertexMap().keySet()) {
				Spot spot = graph.getCellToVertexMap().get(cell);
				graph.getModel().setStyle(cell, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
			}
		} finally {
			graph.getModel().endUpdate();
		}

		// Set up listeners

		// Cells removed from JGraphX
		graph.addListener(mxEvent.CELLS_REMOVED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				Object[] objects = (Object[]) evt.getProperty("cells");
				for(Object obj : objects) {
					mxCell cell = (mxCell) obj;
					if (null != cell) {
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
						if (doFireTMSelectionChangeEvent)
							userChangedSelection(model, added, removed);
					}
				});

		// Return graph
		return graph;
	}

	/**
	 * Instantiate the graph component in charge of painting the graph.
	 * Hook for sub-classers.
	 */
	protected mxTrackGraphComponent createGraphComponent() {
		final mxTrackGraphComponent gc = new mxTrackGraphComponent(this);
		gc.getVerticalScrollBar().setUnitIncrement(16);
		gc.getHorizontalScrollBar().setUnitIncrement(16);
		gc.setExportEnabled(true); // Seems to be required to have a preview when we move cells. Also give the ability to export a cell as an image clipping 
		gc.getConnectionHandler().setEnabled(defaultLinkingEnabled); // By default, can be changed in the track scheme toolbar

		new mxRubberband(gc);
		new mxKeyboardHandler(gc);

		// Popup menu
		gc.getGraphControl().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) 
					displayPopupMenu(e.getPoint(), gc.getCellAt(e.getX(), e.getY(), false));
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) 
					displayPopupMenu(e.getPoint(), gc.getCellAt(e.getX(), e.getY(), false));
			}
		});

		return gc;
	}

	/**
	 * Instantiate the toolbar of the track scheme. Hook for sub-classers.
	 */
	protected JToolBar createToolBar() {
		return new TrackSchemeToolbar(this);		
	}

	/**
	 *  PopupMenu
	 */
	protected void displayPopupMenu(final Point point, final Object cell) {
		TrackSchemePopupMenu menu = new TrackSchemePopupMenu(TrackSchemeFrame.this, point, cell);
		menu.show(graphComponent.getViewport().getView(), (int) point.getX(), (int) point.getY());
	}


	/*
	 * PRIVATE METHODS
	 */

	private void fireEdgeChangeEvent(GraphEdgeChangeEvent<Spot, DefaultWeightedEdge> event) {
		//		for(GraphListener<Spot, DefaultWeightedEdge> listener : graphListeners) {
		//			if (event.getType() == GraphEdgeChangeEvent.EDGE_ADDED)
		//				listener.edgeAdded(event);
		//			else if (event.getType() == GraphEdgeChangeEvent.EDGE_REMOVED)
		//				listener.edgeRemoved(event);
		//		}
	}

	private void fireVertexChangeEvent(GraphVertexChangeEvent<Spot> event) {
		//		for(GraphListener<Spot, DefaultWeightedEdge> listener : graphListeners) {
		//			if (event.getType() == GraphVertexChangeEvent.VERTEX_ADDED)
		//				listener.vertexAdded(event);
		//			else if (event.getType() == GraphVertexChangeEvent.VERTEX_REMOVED)
		//				listener.vertexRemoved(event);
		//		}
	}

	/**
	 * Called when the user makes a selection change in the graph. Used to forward this event 
	 * to the {@link InfoPane} and to other {@link TMSelectionChangeListener}s.
	 * @param model the selection model 
	 * @param added  the cells  <b>removed</b> from selection (careful, inverted)
	 * @param removed  the cells <b>added</b> to selection (careful, inverted)
	 */
	private void userChangedSelection(mxGraphSelectionModel model, Collection<Object> added, Collection<Object> removed) { // Seems to be inverted
		// Forward to info pane
		spotSelection.clear();
		Object[] objects = model.getCells();
		for(Object obj : objects) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex())
				spotSelection.add(graph.getCellToVertexMap().get(cell));
		}
		infoPane.highlightSpots(spotSelection);

		// Forward to other listeners
		HashMap<Spot, Boolean> spots = new HashMap<Spot, Boolean>();
		HashMap<DefaultWeightedEdge, Boolean> edges = new HashMap<DefaultWeightedEdge, Boolean>();


		if (null != removed) {
			spots = new HashMap<Spot, Boolean>();
			for(Object obj : removed) {
				mxCell cell = (mxCell) obj;
				if (cell.isVertex()) {
					Spot spot = graph.getCellToVertexMap().get(cell);
					spots.put(spot, true);
				} else {
					DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
					edges.put(edge, true);
				}
			}
		}

		if (null != added) {
			for(Object obj : added) {
				mxCell cell = (mxCell) obj;
				if (cell.isVertex()) {
					Spot spot = graph.getCellToVertexMap().get(cell);
					spots.put(spot, false);
				} else {
					DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
					edges.put(edge, false);
				}
			}
		}

		TMSelectionChangeEvent event = new TMSelectionChangeEvent(this, spots, edges);
		for(TMSelectionChangeListener listener : selectionChangeListeners) {
			listener.selectionChanged(event);
		}
	}

	private void init() {
		// Frame look
		setIconImage(TRACK_SCHEME_ICON.getImage());
		String title = "Track scheme";
		if (null != settings.imp)
			title += settings.imp.getShortTitle();
		setTitle(title);

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

}
