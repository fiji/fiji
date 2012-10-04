package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;

import fiji.plugin.trackmate.TrackMateModel;

public class TrackSchemeFrame  <T extends RealType<T> & NativeType<T>> extends JFrame  {

	/*
	 * CONSTANTS
	 */

	private static final long 		serialVersionUID = 1L;
	
	/*
	 * FIELDS
	 */

	/** The side pane in which spot selection info will be displayed.	 */
	protected InfoPane<T> infoPane;
	/** The graph component in charge of painting the graph. */
	protected mxTrackGraphComponent<T> graphComponent;
	private final TrackMateModel<T> model;
	private final JGraphXAdapter<T> graph;
	private final TrackScheme<T> trackScheme;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(final JGraphXAdapter<T> graph, final TrackMateModel<T> model, final TrackScheme<T> trackScheme)  {
		this.model = model;
		this.graph = graph;
		this.trackScheme = trackScheme;
		init();
	}

	/*
	 * PUBLIC METHODS
	 */
	

	/*
	 * Selection management
	 */

	public void centerViewOn(mxICell cell) {
		graphComponent.scrollCellToVisible(cell, true);
	}

	/**
	 * Instantiate the graph component in charge of painting the graph.
	 * Hook for sub-classers.
	 */
	protected mxTrackGraphComponent<T> createGraphComponent() {
		final mxTrackGraphComponent<T> gc = new mxTrackGraphComponent<T>(graph, model, trackScheme);
		gc.getVerticalScrollBar().setUnitIncrement(16);
		gc.getHorizontalScrollBar().setUnitIncrement(16);
		gc.setExportEnabled(true); // Seems to be required to have a preview when we move cells. Also give the ability to export a cell as an image clipping 
		gc.getConnectionHandler().setEnabled(TrackScheme.DEFAULT_LINKING_ENABLED); // By default, can be changed in the track scheme toolbar

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
		return new TrackSchemeToolbar<T>(trackScheme);
	}

	/**
	 *  PopupMenu
	 */
	protected void displayPopupMenu(final Object cell, final Point point) {
		TrackSchemePopupMenu<T> menu = new TrackSchemePopupMenu<T>(trackScheme, cell, point);
		menu.show(graphComponent.getViewport().getView(), (int) point.getX(), (int) point.getY());
	}

	private void init() {
		// Frame look
		setIconImage(TrackScheme.TRACK_SCHEME_ICON.getImage());

		// Layout
		getContentPane().setLayout(new BorderLayout());
		
		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// GraphComponent
		graphComponent = createGraphComponent();

		// Add the info pane
		infoPane = new InfoPane<T>(model, graph);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, graphComponent);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);

	}

}
