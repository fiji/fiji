package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.handler.mxRubberband;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.TrackMateModel;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.awt.Dimension;

public class TrackSchemeFrame extends JFrame  {

	/*
	 * CONSTANTS
	 */

	private static final long 		serialVersionUID = 1L;

	/*
	 * FIELDS
	 */

	/** The side pane in which spot selection info will be displayed.	 */
	private InfoPane infoPane;
	private final TrackMateModel model;
	private JGraphXAdapter graph;
	private final TrackScheme trackScheme;

	/** The graph component in charge of painting the graph. */
	TrackSchemeGraphComponent graphComponent;
	/** The {@link Logger} that sends messages to the TrackScheme status bar. */
	final Logger logger;

	/*
	 * CONSTRUCTORS
	 */

	public TrackSchemeFrame(final TrackScheme trackScheme)  {
		this.trackScheme = trackScheme;
		this.model = trackScheme.getModel();

		// Frame look
		setIconImage(TrackScheme.TRACK_SCHEME_ICON.getImage());

		// Layout
		getContentPane().setLayout(new BorderLayout());

		// Add a ToolBar
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// Add the status bar
		JPanel statusPanel = new JPanel();
		getContentPane().add(statusPanel, BorderLayout.SOUTH);

		statusPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		final JLabel statusLabel = new JLabel(" ");
		statusLabel.setFont(SMALL_FONT);
		statusLabel.setHorizontalAlignment(JLabel.RIGHT);
		statusLabel.setPreferredSize(new Dimension(200, 12));
		statusPanel.add(statusLabel);

		final JProgressBar progressBar = new JProgressBar();
		progressBar.setPreferredSize(new Dimension(146, 12));
		statusPanel.add(progressBar);

		this.logger = new Logger() {
			@Override
			public void log(String message, Color color) {
				statusLabel.setText(message);
				statusLabel.setForeground(color);
			}
			@Override public void error(String message) { log(message, Color.RED);}
			@Override public void setProgress(double val) { progressBar.setValue( (int) (val * 100) ); }
			@Override public void setStatus(String status) { log(status, Logger.BLUE_COLOR); }
		};

	}


	/*
	 * PUBLIC METHODS
	 */

	public void init(JGraphXAdapter graph) {
		this.graph = graph;
		// GraphComponent
		graphComponent = createGraphComponent();

		// Add the info pane
		infoPane = new InfoPane(model, graph);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, infoPane, graphComponent);
		splitPane.setDividerLocation(170);
		getContentPane().add(splitPane, BorderLayout.CENTER);
	}


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
	private TrackSchemeGraphComponent createGraphComponent() {
		final TrackSchemeGraphComponent gc = new TrackSchemeGraphComponent(graph, model, trackScheme);
		gc.getVerticalScrollBar().setUnitIncrement(16);
		gc.getHorizontalScrollBar().setUnitIncrement(16);
		//		gc.setExportEnabled(true); // Seems to be required to have a preview when we move cells. Also give the ability to export a cell as an image clipping 
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
	 * Instantiate the toolbar of the track scheme. 
	 */
	private JToolBar createToolBar() {
		return new TrackSchemeToolbar(trackScheme);
	}

	/**
	 *  PopupMenu
	 */
	private void displayPopupMenu(final Object cell, final Point point) {
		TrackSchemePopupMenu menu = new TrackSchemePopupMenu(trackScheme, cell, point);
		menu.show(graphComponent.getViewport().getView(), (int) point.getX(), (int) point.getY());
	}



}

