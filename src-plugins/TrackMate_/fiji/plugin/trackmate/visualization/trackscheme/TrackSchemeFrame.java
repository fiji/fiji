package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

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
import fiji.plugin.trackmate.TrackMateModelChangeEvent;
import fiji.plugin.trackmate.TrackMateModelChangeListener;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;
import fiji.plugin.trackmate.visualization.AbstractTrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateModelView;
import fiji.plugin.trackmate.visualization.TrackMateSelectionView;

public class TrackSchemeFrame extends JFrame implements TrackMateModelChangeListener, TrackMateSelectionChangeListener, TrackMateModelView, TrackMateSelectionView {

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

	private ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> lGraph;
	private Settings settings;
	private JGraphXAdapter<Spot, DefaultWeightedEdge> graph;

	/** The side pane in which spot selection info will be displayed.	 */
	private InfoPane infoPane;
	/** The graph component in charge of painting the graph. */
	private mxTrackGraphComponent graphComponent;
	/** The layout manager that can be called to re-arrange cells in the graph. */
	private mxTrackGraphLayout graphLayout;
	/** Is linking allowed by default? Can be changed in the toolbar. */
	boolean defaultLinkingEnabled = false;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireSelectionChangeEvent = true;
	/** The model this instance is a view of (Yoda I speak like). */
	private TrackMateModel model;
	private Map<String, Object> displaySettings = new HashMap<String, Object>();

	
	
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

	public TrackSchemeFrame(TrackMateModel model)  {
		setModel(model);
		initDisplaySettings();
		init();
		setSize(DEFAULT_SIZE);
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public TrackMateModel getModel() {
		return model;
	}

	@Override
	public void setModel(TrackMateModel model) {
		// Model listeners
		if (null != this.model) {
			this.model.removeTrackMateModelChangeListener(this);
			this.model.removeTrackMateSelectionChangeListener(this);
		}
		this.model = model;
		this.model.addTrackMateModelChangeListener(this);
		this.model.addTrackMateSelectionChangeListener(this);
		// Graph to mirror model
		this.lGraph = new ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge>(model.getTrackGraph());
		this.graph = createGraph();
		this.settings = model.getSettings();
		this.graphLayout = new mxTrackGraphLayout(lGraph, graph, settings.dx);
		String title = "Track scheme";
		if (null != settings.imp)
			title += settings.imp.getShortTitle();
		setTitle(title);
	}
	
	/*
	 * Selection management
	 */

	@Override
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		highlightEdges(model.getEdgeSelection());
		highlightSpots(model.getSpotSelection());
		// Center on selection if we added one spot exactly
		Map<Spot, Boolean> spotsAdded = event.getSpots();
		if (spotsAdded != null && spotsAdded.size() == 1) {
			boolean added = spotsAdded.values().iterator().next();
			if (added) {
				Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn(spot);
			}
		}
	}

	@Override
	public void highlightSpots(Collection<Spot> spots) {
		doFireSelectionChangeEvent  = false;
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
		doFireSelectionChangeEvent  = true;
	}

	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {
		doFireSelectionChangeEvent  = false;
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
		doFireSelectionChangeEvent  = true;
	}

	@Override
	public void centerViewOn(Spot spot) {
		centerViewOn(graph.getVertexToCellMap().get(spot));
	}

	/**
	 * Used to catch spot creation events that occurred elsewhere, for instance by manual editing in 
	 * the {@link AbstractTrackMateModelView}. 
	 * <p>
	 * We have to deal with the graph modification ourselves here, because the {@link TrackMateModel} model
	 * holds a non-listenable JGraphT instance. A modification made to the model would not be reflected
	 * on the graph here.
	 */
	@Override
	public void modelChanged(final TrackMateModelChangeEvent event) {

		try {
			graph.getModel().beginUpdate();
			mxCell cellAdded = null;
			ArrayList<mxCell> cellsToRemove = new ArrayList<mxCell>();

			int targetColumn = 0;
			for (int i = 0; i < graphComponent.getColumnWidths().length; i++)
				targetColumn += graphComponent.getColumnWidths()[i];


			for (Spot spot : event.getSpots() ) {

				if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.SPOT_ADDED) {

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

				} else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.SPOT_MODIFIED) {

					mxCell cell = graph.getVertexToCellMap().get(spot);
					String style = cell.getStyle();
					style = mxUtils.setStyle(style, mxConstants.STYLE_IMAGE, "data:image/base64,"+spot.getImageString());
					graph.getModel().setStyle(cell, style);
					int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Feature.RADIUS) / settings.dx));
					graph.getModel().getGeometry(cell).setHeight(height);

				}  else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.SPOT_REMOVED) {

					mxCell cell = graph.getVertexToCellMap().get(spot);
					cellsToRemove .add(cell);
				}

			}

			graph.removeCells(cellsToRemove.toArray(), true);

		} finally {
			graph.getModel().endUpdate();
		}
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
		Feature xFeature = infoPane.getFeatureSelectionPanel().getXKey();
		Set<Feature> yFeatures = infoPane.getFeatureSelectionPanel().getYKeys();
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

		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, new ArrayList<Spot>(spots), model.getTrackGraph(), settings);
		grapher.setVisible(true);

	}

	/*
	 * PROTECTED METHODS
	 */

	protected void initDisplaySettings() {
		displaySettings.put(KEY_SPOTS_VISIBLE, true);
		displaySettings.put(KEY_DISPLAY_SPOT_NAMES, false);
		displaySettings.put(KEY_SPOT_COLOR_FEATURE, null);
		displaySettings.put(KEY_SPOT_RADIUS_RATIO, 1.0f);
		displaySettings.put(KEY_DISPLAY_TRACKS, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
	}
	
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
				
				System.out.println("Source of event is "+sender.getClass());// DEBUG
				
				// Separate spots from edges
				Object[] objects = (Object[]) evt.getProperty("cells");
				ArrayList<Spot> spotsToRemove = new ArrayList<Spot>();
				ArrayList<Integer> fromFrames = new ArrayList<Integer>();
				ArrayList<DefaultWeightedEdge> edgesToRemove = new ArrayList<DefaultWeightedEdge>();
				for(Object obj : objects) {
					mxCell cell = (mxCell) obj;
					if (null != cell) {
						if (cell.isVertex()) {
							// Build list of removed spots 
							Spot spot = graph.getCellToVertexMap().get(cell);
							Integer frame = model.getSpots().getFrame(spot);
							if (frame == null) {
								// Already removed; second call to event, have to skip it
								return;
							}
							spotsToRemove.add(spot);
							fromFrames.add(frame);
							// Clean maps 
							graph.getVertexToCellMap().remove(spot);
							graph.getCellToVertexMap().remove(cell);
						} else if (cell.isEdge()) {
							// Build list of removed edges 
							DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
							edgesToRemove.add(edge);
							// Clean maps
							graph.getEdgeToCellMap().remove(edge);
							graph.getCellToEdgeMap().remove(cell);
						}
					}
				}
				
				// Clean listenable graph
				lGraph.removeAllVertices(spotsToRemove);
				lGraph.removeAllEdges(edgesToRemove);
				
				// Clean model
				model.removeSpotFrom(spotsToRemove, fromFrames, true);
			}
		});

		// Cell selection change
		graph.getSelectionModel().addListener(
				mxEvent.CHANGE, new mxIEventListener(){
					@SuppressWarnings("unchecked")
					public void invoke(Object sender, mxEventObject evt) {
						if (!doFireSelectionChangeEvent)
							return;
						mxGraphSelectionModel model = (mxGraphSelectionModel) sender;
						Collection<Object> added = (Collection<Object>) evt.getProperty("added");
						Collection<Object> removed = (Collection<Object>) evt.getProperty("removed");
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

	/**
	 * Called when the user makes a selection change in the graph. Used to forward this event 
	 * to the {@link InfoPane} and to other {@link TrackMateSelectionChangeListener}s.
	 * @param model the selection model 
	 * @param added  the cells  <b>removed</b> from selection (careful, inverted)
	 * @param removed  the cells <b>added</b> to selection (careful, inverted)
	 */
	private void userChangedSelection(mxGraphSelectionModel mxGSmodel, Collection<Object> added, Collection<Object> removed) { // Seems to be inverted
		// Forward to other listeners
		Collection<Spot> spotsToAdd = new ArrayList<Spot>();
		Collection<Spot> spotsToRemove = new ArrayList<Spot>();
		Collection<DefaultWeightedEdge> edgesToAdd = new ArrayList<DefaultWeightedEdge>();
		Collection<DefaultWeightedEdge> edgesToRemove = new ArrayList<DefaultWeightedEdge>();

		if (null != added) {
			for(Object obj : added) {
				mxCell cell = (mxCell) obj;
				if (cell.isVertex()) {
					Spot spot = graph.getCellToVertexMap().get(cell);
					spotsToRemove.add(spot);
				} else {
					DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
					edgesToRemove.add(edge);
				}
			}
		}

		if (null != removed) {
			for(Object obj : removed) {
				mxCell cell = (mxCell) obj;
				if (cell.isVertex()) {
					Spot spot = graph.getCellToVertexMap().get(cell);
					spotsToAdd.add(spot);
				} else {
					DefaultWeightedEdge edge = graph.getCellToEdgeMap().get(cell);
					edgesToAdd.add(edge);
				}
			}
		}
		
		model.addEdgeToSelection(edgesToAdd);
		model.addSpotToSelection(spotsToAdd);
		model.removeEdgeFromSelection(edgesToRemove);
		model.removeSpotFromSelection(spotsToRemove);		
	}

	private void init() {
		// Frame look
		setIconImage(TRACK_SCHEME_ICON.getImage());
		
		getContentPane().setLayout(new BorderLayout());
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// GraphComponent
		graphComponent = createGraphComponent();

		// Arrange graph layout
		doTrackLayout();

		// Add the info pane
		infoPane = new InfoPane(model);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, graphComponent);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		// Add a listener to ensure we remove this frame from the listener list of the model when it closes
		addWindowListener(new  WindowListener() {
			@Override
			public void windowClosed(WindowEvent e) {
				model.removeTrackMateSelectionChangeListener(TrackSchemeFrame.this);				
				model.removeTrackMateModelChangeListener(TrackSchemeFrame.this);				
			}
			
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
			@Override
			public void windowClosing(WindowEvent e) {}
			@Override
			public void windowActivated(WindowEvent e) {}
		});
			
			

	}

	/**
	 * Return the {@link JGraphXAdapter} that serves as a model for the graph displayed in this frame.
	 */
	public JGraphXAdapter<Spot, DefaultWeightedEdge> getGraph() {
		return graph;
	}
	
	/**
	 * Return the JGraphT listenable graph that bridges the track model and the JGraphX display.
	 * @return
	 */
	public ListenableUndirectedWeightedGraph<Spot, DefaultWeightedEdge> getGraphT() {
		return lGraph;
	}
	
	public mxTrackGraphComponent getGraphComponent() {
		return graphComponent;
	}
	
	/**
	 * Return the graph layout in charge of arranging the cells on the graph.
	 */
	public mxTrackGraphLayout getGraphLayout() {
		return graphLayout;	
	}

	@Override
	public Map<String, Object> getDisplaySettings() {
		return displaySettings;
	}

	@Override
	public void setDisplaySettings(String key, Object value) {
		displaySettings.put(key, value);
	}

	@Override
	public Object getDisplaySettings(String key) {
		return displaySettings.get(key);
	}

}
