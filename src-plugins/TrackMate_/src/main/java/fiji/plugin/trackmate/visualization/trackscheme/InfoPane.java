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
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxICell;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;
import fiji.plugin.trackmate.util.TMUtils;

public class InfoPane <T extends RealType<T> & NativeType<T>> extends JPanel implements TrackMateSelectionChangeListener {

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
	private final TrackMateModel<T> model;
	private final JGraphXAdapter<T> graph;

	/*
	 * CONSTRUCTOR
	 */

	public InfoPane(final TrackMateModel<T> model, final JGraphXAdapter<T> graph) {
		this.model = model;
		this.graph = graph;
		this.features = model.getFeatureModel().getSpotFeatures();
		this.featureNames = model.getFeatureModel().getSpotFeatureShortNames();
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
	public void selectionChanged(TrackMateSelectionChangeEvent event) {
		// Echo changed in a different thread for performance 
		new Thread("TrackScheme info pane thread") {
			public void run() {
				highlightSpots(model.getSpotSelection());
			}
		}.start();
	}

	/**
	 * Show the given spot selection as a table displaying their individual features. 
	 */
	@SuppressWarnings("serial")
	private void highlightSpots(final Collection<Spot> spots) {
		if (!doHighlightSelection)
			return;
		if (spots.size() == 0) {
			scrollTable.setVisible(false);
			return;
		}
		
		// Copy and sort selection by frame 
		final TreeSet<Spot> sortedSpot = new TreeSet<Spot>(Spot.frameComparator);
		sortedSpot.addAll(spots);

		// Fill feature table
		try { // Dummy protection for ultra fast selection / de-selection events. Ugly.

			DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
				@Override
				public boolean isCellEditable(int row, int column) { return false; }
			};

			for (Spot spot : sortedSpot) {
				if (null == spot)
					continue;
				Object[] columnData = new Object[features.size()];
				for (int i = 0; i < columnData.length; i++) 
					columnData[i] = String.format("%.1f", spot.getFeature(features.get(i)));
				dm.addColumn(spot.toString(), columnData);
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

		} catch (ConcurrentModificationException cme) {
			// do nothing
		} catch (ArrayIndexOutOfBoundsException aobe) {
			// do nothing
		}
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
		TableModel tm = table.getModel();
		ResultsTable table = new ResultsTable();
		
		int ncols = tm.getColumnCount();
		int nrows = tm.getRowCount();
		JList rowList = (JList) scrollTable.getRowHeader().getView();
		
		
		
		for (int j = 0; j < nrows; j++) {
			table.incrementCounter();
			table.setLabel( (String) rowList.getModel().getElementAt(j), j);
			for (int i = 0; i < ncols; i++) {
				String headings = tm.getColumnName(i);
				table.addValue(headings, Double.parseDouble((String) tm.getValueAt(j, i)) );
			}
		}

		table.show("TrackMate Selection");
	}
	
	private void init() {

		@SuppressWarnings("serial")
		AbstractListModel lm = new AbstractListModel() {
			String headers[] = TMUtils.getArrayFromMaping(features, featureNames).toArray(new String[] {});
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

		SpotFeatureGrapher<T> grapher = new SpotFeatureGrapher<T>(xFeature, yFeatures, new ArrayList<Spot>(spots), model);
		grapher.setVisible(true);

	}

}
