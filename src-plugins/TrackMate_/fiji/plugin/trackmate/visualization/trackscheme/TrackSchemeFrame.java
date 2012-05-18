package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxStyleUtils;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxPerimeter;
import com.mxgraph.view.mxStylesheet;

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
	private static final boolean DEBUG = false;
	private static final boolean DEBUG_SELECTION = false;
	static final int Y_COLUMN_SIZE = 96;
	static final int X_COLUMN_SIZE = 160;

	static final int DEFAULT_CELL_WIDTH = 128;
	static final int DEFAULT_CELL_HEIGHT = 80;

	public static final ImageIcon TRACK_SCHEME_ICON = new ImageIcon(TrackSchemeFrame.class.getResource("resources/track_scheme.png"));

	private static final long 		serialVersionUID = 1L;
	private static final Dimension 	DEFAULT_SIZE = new Dimension(800, 600);
	static final String 			DEFAULT_STYLE_NAME = "Full"; 
	static final int 				TABLE_CELL_WIDTH 		= 40;
	static final Color 				GRID_COLOR = Color.GRAY;

	/*
	 * FIELDS
	 */

	/** Is linking allowed by default? Can be changed in the toolbar. */
	boolean defaultLinkingEnabled = false;
	/** Are linking costs displayed by default? Can be changed in the toolbar. */
	static final boolean DEFAULT_DO_DISPLAY_COSTS_ON_EDGES = false;
	/** Do we display the background decorations by default? */
	static final boolean DEFAULT_DO_PAINT_DECORATIONS = true;
	
	private Settings settings;
	private JGraphXAdapter graph;

	/** The side pane in which spot selection info will be displayed.	 */
	private InfoPane infoPane;
	/** The graph component in charge of painting the graph. */
	private mxTrackGraphComponent graphComponent;
	/** The layout manager that can be called to re-arrange cells in the graph. */
	private mxTrackGraphLayout graphLayout;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireSelectionChangeEvent = true;
	/** A flag used to prevent double event firing when setting the selection programmatically. */
	private boolean doFireModelChangeEvent = true;
	/** The model this instance is a view of (Yoda I speak like). */
	private TrackMateModel model;
	private Map<String, Object> displaySettings = new HashMap<String, Object>();
	private SpotImageUpdater spotImageUpdater;



	private static final Map<String, Map<String, Object>> VERTEX_STYLES;
	private static final HashMap<String, Object> BASIC_VERTEX_STYLE = new HashMap<String, Object>();
	private static final HashMap<String, Object> SIMPLE_VERTEX_STYLE = new HashMap<String, Object>();
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

		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FILLCOLOR, "white");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_FONTCOLOR, "black");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_RIGHT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_IMAGE_ALIGN, mxConstants.ALIGN_LEFT);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_ROUNDED, true);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_PERIMETER, mxPerimeter.EllipsePerimeter);
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_STROKECOLOR, "#FF00FF");
		SIMPLE_VERTEX_STYLE.put(mxConstants.STYLE_NOLABEL, true);
		
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STARTARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_ENDARROW, mxConstants.NONE);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKEWIDTH, 2.0f);
		BASIC_EDGE_STYLE.put(mxConstants.STYLE_STROKECOLOR, "#FF00FF");
		
		VERTEX_STYLES = new HashMap<String, Map<String, Object> >(2);
		VERTEX_STYLES.put(DEFAULT_STYLE_NAME, BASIC_VERTEX_STYLE);
		VERTEX_STYLES.put("Simple", SIMPLE_VERTEX_STYLE);

	}

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(TrackMateModel model)  {
		setModel(model);
		initDisplaySettings();
		init();
		setSize(DEFAULT_SIZE);
		this.spotImageUpdater = new SpotImageUpdater(model);
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
		this.graph = createGraph();
		this.settings = model.getSettings();
		this.graphLayout = new mxTrackGraphLayout(model, graph, settings.dx);
		String title = "Track scheme";
		if (null != settings.imp)
			title += ": "+settings.imp.getShortTitle();
		setTitle(title);
	}

	/*
	 * Selection management
	 */

	@Override
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		if (DEBUG_SELECTION) 
			System.out.println("[TrackSchemeFrame] selectionChanged: received event "+event.hashCode()+" from "+event.getSource()+". Fire flag is "+doFireSelectionChangeEvent);
		if (!doFireSelectionChangeEvent)
			return;
		doFireSelectionChangeEvent = false;

		/* Performance issue: we do our highlighting here, in batch, bypassing highlight* methods		 */
		{
			ArrayList<Object> newSelection = new ArrayList<Object>(model.getSpotSelection().size() + model.getEdgeSelection().size());
			Iterator<DefaultWeightedEdge> edgeIt = model.getEdgeSelection().iterator();
			while(edgeIt.hasNext()) {
				mxICell cell = graph.getCellFor(edgeIt.next());
				if (null != cell) {
					newSelection.add(cell);
				}
			}
			Iterator<Spot> spotIt = model.getSpotSelection().iterator();
			while(spotIt.hasNext()) {
				mxICell cell = graph.getCellFor(spotIt.next());
				if (null != cell) {
					newSelection.add(cell);
				}
			}
			mxGraphSelectionModel mGSmodel = graph.getSelectionModel();
			mGSmodel.setCells(newSelection.toArray());
		}

		// Center on selection if we added one spot exactly
		Map<Spot, Boolean> spotsAdded = event.getSpots();
		if (spotsAdded != null && spotsAdded.size() == 1) {
			boolean added = spotsAdded.values().iterator().next();
			if (added) {
				Spot spot = spotsAdded.keySet().iterator().next();
				centerViewOn(spot);
			}
		}
		doFireSelectionChangeEvent = true;
	}

	@Override
	public void highlightSpots(Collection<Spot> spots) {}

	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {}

	@Override
	public void centerViewOn(Spot spot) {
		centerViewOn(graph.getCellFor(spot));
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

		// Only catch model changes
		if (event.getEventID() != TrackMateModelChangeEvent.MODEL_MODIFIED)
			return;

		new Thread() {
			public void run() {


				graph.getModel().beginUpdate();
				try {
					ArrayList<mxICell> cellsToRemove = new ArrayList<mxICell>();

					int targetColumn = 0;
					for (int i = 0; i < graphComponent.getColumnWidths().length; i++)
						targetColumn += graphComponent.getColumnWidths()[i];

					if (event.getSpots() != null) {
						for (Spot spot : event.getSpots() ) {

							if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_ADDED) {

								// Update spot image
								spotImageUpdater.update(spot);
								// Put in the graph
								insertSpotInGraph(spot, targetColumn);
								targetColumn++;

							} else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_MODIFIED) {

								mxICell cell = graph.getCellFor(spot);
								if (DEBUG)
									System.out.println("[TrackSchemeFrame] modelChanged: updating cell for spot "+spot);
								if (null == cell) {
									// mxCell not present in graph. Most likely because the corresponding spot belonged
									// to an invisible track, and a cell was not created for it when TrackScheme was
									// launched. So we create one on the fly now.
									cell = insertSpotInGraph(spot, targetColumn);
								}

								// Update spot image
								spotImageUpdater.update(spot);

								// Update cell look
								String style = cell.getStyle();
								style = mxStyleUtils.setStyle(style, mxConstants.STYLE_IMAGE, "data:image/base64,"+spot.getImageString());
								graph.getModel().setStyle(cell, style);
								int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Spot.RADIUS) / settings.dx));
								height = Math.max(height, DEFAULT_CELL_HEIGHT/3);
								graph.getModel().getGeometry(cell).setHeight(height);

							}  else if (event.getSpotFlag(spot) == TrackMateModelChangeEvent.FLAG_SPOT_REMOVED) {

								mxICell cell = graph.getCellFor(spot);
								cellsToRemove.add(cell);
							}

						}
						graph.removeCells(cellsToRemove.toArray(), true);
					}
				} finally {
					graph.getModel().endUpdate();
				}
			};
		}.start();

	}



	public void centerViewOn(mxICell cell) {
		graphComponent.scrollCellToVisible(cell, true);
	}

	public void doTrackLayout() {
		graphLayout.execute(graph.getDefaultParent());

		// Forward painting info to graph component
		graphComponent.setColumnWidths(graphLayout.getTrackColumnWidths());
		graphComponent.setRowForInstant(graphLayout.getRowForInstant());
		graphComponent.setColumnColor(graphLayout.getTrackColors());
	}

	public void plotSelectionData(String xFeature, Set<String> yFeatures) {

		if (yFeatures.isEmpty())
			return;

		Object[] selectedCells = graph.getSelectionCells();
		if (selectedCells == null || selectedCells.length == 0)
			return;

		HashSet<Spot> spots = new HashSet<Spot>();
		for(Object obj : selectedCells) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex()) {
				Spot spot = graph.getSpotFor(cell);

				if (spot == null) {
					// We might have a parent cell, that holds many vertices in it
					// Retrieve them and add them if they are not already.
					int n = cell.getChildCount();
					for (int i = 0; i < n; i++) {
						mxICell child = cell.getChildAt(i);
						Spot childSpot = graph.getSpotFor(child);
						if (null != childSpot)
							spots.add(childSpot);
					}

				} else 
					spots.add(spot);
			}
		}
		if (spots.isEmpty())
			return;

		SpotFeatureGrapher grapher = new SpotFeatureGrapher(xFeature, yFeatures, new ArrayList<Spot>(spots), model);
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
		displaySettings.put(KEY_TRACKS_VISIBLE, true);
		displaySettings.put(KEY_TRACK_DISPLAY_MODE, DEFAULT_TRACK_DISPLAY_MODE);
		displaySettings.put(KEY_TRACK_DISPLAY_DEPTH, DEFAULT_TRACK_DISPLAY_DEPTH);
		displaySettings.put(KEY_COLORMAP, DEFAULT_COLOR_MAP);
	}

	/**
	 * Used to instantiate and configure the {@link JGraphXAdapter} that will be used for display.
	 * Hook for subclassers.
	 */
	protected JGraphXAdapter createGraph() {
		final JGraphXAdapter graph = new JGraphXAdapter(model);
		graph.setAllowLoops(false);
		graph.setAllowDanglingEdges(false);
		graph.setCellsCloneable(false);
		graph.setCellsSelectable(true);
		graph.setCellsDisconnectable(false);
		graph.setCellsMovable(true);
		graph.setGridEnabled(false);
		graph.setLabelsVisible(true);
		graph.setDropEnabled(false);
		
		mxStylesheet styleSheet = graph.getStylesheet();
		styleSheet.setDefaultEdgeStyle(BASIC_EDGE_STYLE);
		styleSheet.setDefaultVertexStyle(BASIC_VERTEX_STYLE);
		styleSheet.putCellStyle(DEFAULT_STYLE_NAME, BASIC_VERTEX_STYLE);
		styleSheet.putCellStyle("Simple", SIMPLE_VERTEX_STYLE);

		// Set spot image to cell style
		try {
			graph.getModel().beginUpdate();
			for(mxICell cell : graph.getVertexCells()) {
				Spot spot = graph.getSpotFor(cell);
				graph.getModel().setStyle(cell, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
			}
		} finally {
			graph.getModel().endUpdate();
		}

		// Cells removed from JGraphX
		graph.addListener(mxEvent.CELLS_REMOVED, new CellRemovalListener());

		// Cell selection change
		graph.getSelectionModel().addListener(mxEvent.CHANGE, new SelectionChangeListener());

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
					displayPopupMenu(gc.getCellAt(e.getX(), e.getY(), false), e.getPoint());
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) 
					displayPopupMenu(gc.getCellAt(e.getX(), e.getY(), false), e.getPoint());
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
	protected void displayPopupMenu(final Object cell, final Point point) {
		TrackSchemePopupMenu menu = new TrackSchemePopupMenu(this, cell,  model, graph, point);
		menu.show(graphComponent.getViewport().getView(), (int) point.getX(), (int) point.getY());
	}

	/**
	 * Insert a spot in the {@link TrackSchemeFrame}, by creating a {@link mxCell} in the 
	 * graph model of this frame and position it according to its feature.
	 */
	protected mxICell insertSpotInGraph(Spot spot, int targetColumn) {
		mxICell cellAdded = graph.getCellFor(spot);
		if (cellAdded != null) {
			// cell for spot already exist, do nothing and return orginal spot
			return cellAdded;
		}
		// Instantiate JGraphX cell
		cellAdded = graph.addJGraphTVertex(spot);
		// Position it
		float instant = spot.getFeature(Spot.POSITION_T);
		double x = (targetColumn-1) * X_COLUMN_SIZE - DEFAULT_CELL_WIDTH/2;
		Integer row = graphComponent.getRowForInstant().get(instant);
		if (null == row) {
			// The spot added is set to a time that is not present yet in the tracks scheme
			// So we had it to the last row, plus one.
			row = 0;
			for(Integer eRow : graphComponent.getRowForInstant().values()) {
				if (eRow > row) 
					row = eRow;
			}
			row = row + 1;
		}
		double y = (0.5 + row) * Y_COLUMN_SIZE - DEFAULT_CELL_HEIGHT/2; 
		int height = Math.min(DEFAULT_CELL_WIDTH, Math.round(2 * spot.getFeature(Spot.RADIUS) / settings.dx));
		height = Math.max(height, 12);
		mxGeometry geometry = new mxGeometry(x, y, DEFAULT_CELL_WIDTH, height);
		cellAdded.setGeometry(geometry);
		// Set its style
		graph.getModel().setStyle(cellAdded, mxConstants.STYLE_IMAGE+"="+"data:image/base64,"+spot.getImageString());
		return cellAdded;
	}


	protected void importTrack(int trackIndex) {
		model.beginUpdate();
		graph.getModel().beginUpdate();
		try {
			// Flag original track as visible
			model.addTrackToVisibleList(trackIndex);
			// Find adequate column
			int targetColumn = getNextFreeColumn();
			// Create cells for track
			Set<Spot> trackSpots = model.getTrackSpots(trackIndex);
			for (Spot trackSpot : trackSpots) {
				insertSpotInGraph(trackSpot, targetColumn);
			}
			Set<DefaultWeightedEdge> trackEdges = model.getTrackEdges(trackIndex);
			for (DefaultWeightedEdge trackEdge : trackEdges) {
				graph.addJGraphTEdge(trackEdge);
			}
		} finally {
			model.endUpdate();
			graph.getModel().endUpdate();
		}
	}

	/**
	 * This method is called when the user has created manually an edge in the graph, by dragging
	 * a link between two spot cells. It checks whether the matching edge in the model exists, 
	 * and tune what should be done accordingly.
	 * @param cell  the mxCell of the edge that has been manually created.
	 */
	protected void addEdgeManually(mxICell cell) {
		if (cell.isEdge()) {
			cell.setValue("New");
			graph.getModel().beginUpdate();
			model.beginUpdate();
			try {
				Spot source = graph.getSpotFor(((mxCell)cell).getSource());
				Spot target = graph.getSpotFor(((mxCell) cell).getTarget());
				// We add a new jGraphT edge to the underlying model, if it does not exist yet.
				DefaultWeightedEdge edge = model.getEdge(source, target); 
				if (null == edge) {
					edge = model.addEdge(source, target, -1);
				} else {
					// Ah. There was an existing edge in the model we were trying to re-add there, from the graph.
					// We remove the graph edge we have added,
					if (DEBUG) {
						System.out.println("[TrackSchemeFrame] addEdgeManually: edge pre-existed. Retrieve it.");
					}
					graph.removeCells(new Object[] { cell } );
					// And re-create a graph edge from the model edge.
					cell = graph.addJGraphTEdge(edge);
					cell.setValue(String.format("%.1f", model.getEdgeWeight(edge)));
					// We also need now to check if the edge belonged to a visible track. If not,
					// we make it visible.
					int index = model.getTrackIndexOf(edge); 
					// This will work, because track indices will be reprocessed only after the model.endUpdate() 
					// reaches 0. So now, it's like we are dealing wioth the track indices priori to modification.
					if (model.isTrackVisible(index)) {
						if (DEBUG) {
							System.out.println("[TrackSchemeFrame] addEdgeManually: track was visible. Do nothing.");
						}
					} else {
						if (DEBUG) {
							System.out.println("[TrackSchemeFrame] addEdgeManually: track was invisible. Make it visible.");
						}
						importTrack(index);
					}
				}
				graph.mapEdgeToCell(edge, cell);

			} finally {
				graph.getModel().endUpdate();
				model.endUpdate();
				model.clearEdgeSelection();
			}
		}
	}

	/**
	 * Return the first free (no cell in lane) column index.
	 */
	protected int getNextFreeColumn() {
		int columnIndex = 2;
		int[] columnWidths = graphComponent.getColumnWidths();
		for (int i = 0; i < columnWidths.length; i++) {
			columnIndex += columnWidths[i] - 1;
		}
		columnIndex += 1;
		return columnIndex;
	}

	// LISTENERS



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
		if (!doFireSelectionChangeEvent)
			return;
		Collection<Spot> spotsToAdd = new ArrayList<Spot>();
		Collection<Spot> spotsToRemove = new ArrayList<Spot>();
		Collection<DefaultWeightedEdge> edgesToAdd = new ArrayList<DefaultWeightedEdge>();
		Collection<DefaultWeightedEdge> edgesToRemove = new ArrayList<DefaultWeightedEdge>();

		if (null != added) {
			for(Object obj : added) {
				mxCell cell = (mxCell) obj;

				if (cell.getChildCount() > 0) {

					for (int i = 0; i < cell.getChildCount(); i++) {
						mxICell child = cell.getChildAt(i);
						if (child.isVertex()) {
							Spot spot = graph.getSpotFor(child);
							spotsToRemove.add(spot);
						} else {
							DefaultWeightedEdge edge = graph.getEdgeFor(child);
							edgesToRemove.add(edge);
						}
					}

				} else {

					if (cell.isVertex()) {
						Spot spot = graph.getSpotFor(cell);
						spotsToRemove.add(spot);
					} else {
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						edgesToRemove.add(edge);
					}
				}
			}
		}

		if (null != removed) {
			for(Object obj : removed) {
				mxCell cell = (mxCell) obj;

				if (cell.getChildCount() > 0) {

					for (int i = 0; i < cell.getChildCount(); i++) {
						mxICell child = cell.getChildAt(i);
						if (child.isVertex()) {
							Spot spot = graph.getSpotFor(child);
							spotsToAdd.add(spot);
						} else {
							DefaultWeightedEdge edge = graph.getEdgeFor(child);
							edgesToAdd.add(edge);
						}
					}

				} else {

					if (cell.isVertex()) {
						Spot spot = graph.getSpotFor(cell);
						spotsToAdd.add(spot);
					} else {
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						edgesToAdd.add(edge);
					}
				}
			}
		}
		if (DEBUG_SELECTION)
			System.out.println("[TrackSchemeFrame] userChangeSelection: sending selection change to model.");
		doFireSelectionChangeEvent = false;
		if (!edgesToAdd.isEmpty())
			model.addEdgeToSelection(edgesToAdd);
		if (!spotsToAdd.isEmpty())
			model.addSpotToSelection(spotsToAdd);
		if (!edgesToRemove.isEmpty())
			model.removeEdgeFromSelection(edgesToRemove);
		if (!spotsToRemove.isEmpty())
			model.removeSpotFromSelection(spotsToRemove);
		doFireSelectionChangeEvent = true;
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
		
		// Add listener for plot events
		infoPane.featureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String xFeature = infoPane.getFeatureSelectionPanel().getXKey();
				Set<String> yFeatures = infoPane.getFeatureSelectionPanel().getYKeys();
				plotSelectionData(xFeature, yFeatures);
			}
		});

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
	public JGraphXAdapter getGraph() {
		return graph;
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

	@Override
	public void render() {
		// TODO Auto-generated method stub

	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getInfoText() {
		return "<html>InfoText for TrachScheme is not redacted.</html>";
	}


	// INNER CLASSES

	private class CellRemovalListener implements mxIEventListener {

		public void invoke(Object sender, mxEventObject evt) {

			if (DEBUG)
				System.out.println("[TrackSchemeFrame] CellRemovalListener: cells removed - Source of event is "+sender.getClass()+". Fire flag is "+doFireModelChangeEvent);

			if (!doFireModelChangeEvent)
				return;

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
						Spot spot = graph.getSpotFor(cell);
						Integer frame = model.getSpots().getFrame(spot);
						if (frame == null) {
							// Already removed; second call to event, have to skip it
							continue;
						}
						spotsToRemove.add(spot);
						fromFrames.add(frame);
						// Clean maps 
						graph.removeMapping(spot);
					} else if (cell.isEdge()) {
						// Build list of removed edges 
						DefaultWeightedEdge edge = graph.getEdgeFor(cell);
						if (null ==edge)
							continue;
						edgesToRemove.add(edge);
						// Clean maps
						graph.removeMapping(edge);
					}
				}
			}

			evt.consume();

			// Clean model
			doFireModelChangeEvent = false;
			model.beginUpdate();
			try {
				model.clearSelection();
				// We remove edges first so that we ensure we do not end having orphan edges.
				// Normally JGraphT handles that well, but we enforce things here. To be sure.
				for (DefaultWeightedEdge edge : edgesToRemove) {
					model.removeEdge(edge);
				}
				for (Spot spot : spotsToRemove)  {
					model.removeSpotFrom(spot, null); 
				}

			} finally {
				model.endUpdate();
			}
			doFireModelChangeEvent = true;
		}


	}

	private class SelectionChangeListener implements mxIEventListener {

		@SuppressWarnings("unchecked")
		public void invoke(Object sender, mxEventObject evt) {
			if (DEBUG_SELECTION)
				System.out.println("[TrackSchemeFrame] SelectionChangeListener: selection changed by "+sender+". Fire event flag is "+doFireSelectionChangeEvent);
			if (!doFireSelectionChangeEvent || sender != graph.getSelectionModel())
				return;
			mxGraphSelectionModel model = (mxGraphSelectionModel) sender;
			Collection<Object> added = (Collection<Object>) evt.getProperty("added");
			Collection<Object> removed = (Collection<Object>) evt.getProperty("removed");
			userChangedSelection(model, added, removed);
		}
	}

}
