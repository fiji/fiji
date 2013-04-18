package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.SMALL_FONT;
import ij.measure.ResultsTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;

import fiji.plugin.trackmate.SelectionChangeEvent;
import fiji.plugin.trackmate.SelectionChangeListener;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.features.SpotFeatureGrapher;
import fiji.plugin.trackmate.util.OnRequestUpdater;
import fiji.plugin.trackmate.util.OnRequestUpdater.Refreshable;
import fiji.plugin.trackmate.util.TMUtils;

public class InfoPane extends JPanel implements SelectionChangeListener {

	private static final long serialVersionUID = -1L;

	private class RowHeaderRenderer extends JLabel implements ListCellRenderer, Serializable {

		private static final long serialVersionUID = -1L;

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

	private FeaturePlotSelectionPanel featureSelectionPanel;
	private JTable table;
	private JScrollPane scrollTable;
	private boolean doHighlightSelection = true;
	private List<String> features;
	private Map<String, String> featureNames;
	private final TrackMateModel model;
	private final JGraphXAdapter graph;
	/** A copy of the last spot collection highlighted in this infopane, sorted by frame order. */
	private Collection<Spot> spotSelection;
	private String[] headers;
	private final OnRequestUpdater updater;

	/*
	 * CONSTRUCTOR
	 */

	public InfoPane(final TrackMateModel model, final JGraphXAdapter graph) {
		this.model = model;
		this.graph = graph;
		this.features = model.getFeatureModel().getSpotFeatures();
		this.featureNames = model.getFeatureModel().getSpotFeatureShortNames();
		this.headers = TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[] {});
		this.updater = new OnRequestUpdater(new Refreshable() {
			@Override
			public void refresh() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() { update(); }
				});
			}
		});
		// Add a listener to ensure we remove this panel from the listener list of the model
		addAncestorListener(new AncestorListener() {			
			@Override
			public void ancestorRemoved(AncestorEvent event) {
				model.removeTrackMateSelectionChangeListener(InfoPane.this);
			}
			@Override
			public void ancestorMoved(AncestorEvent event) {}
			@Override
			public void ancestorAdded(AncestorEvent event) {}
		});
		model.addTrackMateSelectionChangeListener(this);
		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void selectionChanged(SelectionChangeEvent event) {
		// Echo changed in a different thread for performance 
		new Thread("TrackScheme info pane thread") {
			public void run() {
				highlightSpots(model.getSelectionModel().getSpotSelection());
			}
		}.start();
	}

	/**
	 * Show the given spot selection as a table displaying their individual features. 
	 */
	private void highlightSpots(final Collection<Spot> spots) {
		if (!doHighlightSelection)
			return;
		if (spots.size() == 0) {
			scrollTable.setVisible(false);
			return;
		}

		// Copy and sort selection by frame 
		spotSelection = spots;
		updater.doUpdate();
	}

	private void update() {
		/* Sort using a list; TreeSet does not allow several identical frames,
		 * which is likely to happen.  */
		List<Spot> sortedSpots = new ArrayList<Spot>(spotSelection);
		Collections.sort(sortedSpots, Spot.frameComparator);
		
		@SuppressWarnings("serial")
		DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
			@Override
			public boolean isCellEditable(int row, int column) { return false; }
		};

		for (Spot spot : sortedSpots) {
			if (null == spot)
				continue;
			Object[] columnData = new Object[features.size()];
			for (int i = 0; i < columnData.length; i++) 
				columnData[i] = String.format("%.1f", spot.getFeature(features.get(i)));
			dm.addColumn(spot.toString(), columnData);
		}
		table.setModel(dm);

		// Tune look
		@SuppressWarnings("serial")
		DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
			public boolean isOpaque() { return false; };
			@Override
			public Color getBackground() {
				return Color.BLUE;
			}
		};
		headerRenderer.setBackground(Color.RED);
		headerRenderer.setFont(FONT);

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setOpaque(false);
		renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		renderer.setFont(SMALL_FONT);

		FontMetrics fm = table.getGraphics().getFontMetrics(FONT);
		for(int i=0; i<table.getColumnCount(); i++) {
			table.setDefaultRenderer(table.getColumnClass(i), renderer);
			// Set width auto
			table.getColumnModel().getColumn(i).setWidth(fm.stringWidth( dm.getColumnName(i) ) );
		}
		for (Component c : scrollTable.getColumnHeader().getComponents()) {
			c.setBackground(getBackground());
		}
		scrollTable.getColumnHeader().setOpaque(false);
		scrollTable.setVisible(true);
		validate();
	}

	/*
	 * PRIVATE METHODS
	 */

	private void displayPopupMenu(Point point) {
		// Prepare menu
		JPopupMenu menu = new JPopupMenu("Selection table");
		JMenuItem exportItem = menu.add("Export to ImageJ table");
		exportItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {	exportTableToImageJ(); 	}
		});
		// Display it
		menu.show(table, (int) point.getX(), (int) point.getY());
	}

	private void exportTableToImageJ() {
		ResultsTable table = new ResultsTable();

		int ncols = spotSelection.size();
		int nrows = headers.length;
		Spot[] spotArray = spotSelection.toArray(new Spot[] {} );

		for (int j = 0; j < nrows; j++) {
			table.incrementCounter();
			String feature = features.get(j);
			table.setLabel(feature, j);
			for (int i = 0; i < ncols; i++) {
				Spot spot =  spotArray[i]; 
				Double val = spot.getFeature(feature);
				if (val == null) {
					val = Double.NaN;
				}
				table.addValue(spot.getName(),  val);
			}
		}

		table.show("TrackMate Selection");
	}

	private void init() {

		@SuppressWarnings("serial")
		AbstractListModel lm = new AbstractListModel() {
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
		table.setSelectionForeground(Color.YELLOW.darker().darker());
		table.setGridColor(TrackScheme.GRID_COLOR);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) 
					displayPopupMenu(e.getPoint());
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) 
					displayPopupMenu(e.getPoint());
			}
		});


		JList rowHeader = new JList(lm);
		rowHeader.setFixedCellHeight(table.getRowHeight());
		rowHeader.setCellRenderer(new RowHeaderRenderer(table));
		rowHeader.setBackground(getBackground());

		scrollTable = new JScrollPane(table);
		scrollTable.setRowHeaderView(rowHeader);
		scrollTable.getRowHeader().setOpaque(false);
		scrollTable.setOpaque(false);
		scrollTable.getViewport().setOpaque(false);
		scrollTable.setVisible(false); // for now

		featureSelectionPanel = new FeaturePlotSelectionPanel(Spot.POSITION_T, features, featureNames);

		setLayout(new BorderLayout());
		add(scrollTable, BorderLayout.CENTER);
		add(featureSelectionPanel, BorderLayout.SOUTH);

		// Add listener for plot events
		featureSelectionPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String xFeature = featureSelectionPanel.getXKey();
				Set<String> yFeatures = featureSelectionPanel.getYKeys();
				plotSelectionData(xFeature, yFeatures);
			}
		});


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
		grapher.render();

	}

}
