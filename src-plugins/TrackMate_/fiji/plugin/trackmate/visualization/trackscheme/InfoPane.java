package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
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

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMateModel;
import fiji.plugin.trackmate.TrackMateSelectionChangeEvent;
import fiji.plugin.trackmate.TrackMateSelectionChangeListener;
import fiji.plugin.trackmate.util.TMUtils;
import fiji.plugin.trackmate.visualization.TrackMateSelectionView;

class InfoPane extends JPanel implements TrackMateSelectionView, TrackMateSelectionChangeListener {

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
	private FeaturePlotSelectionPanel featureSelectionPanel;
	private boolean doHighlightSelection = true;
	private TrackMateModel model;
	private List<String> features;
	private Map<String, String> featureNames;

	/*
	 * CONSTRUCTOR
	 */

	public InfoPane(final TrackMateModel model) {
		this.model = model;
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
	 * Ignored.
	 */
	@Override
	public void highlightEdges(Collection<DefaultWeightedEdge> edges) {}

	/**
	 * Ignored.
	 */
	@Override
	public void centerViewOn(Spot spot) {}

	/**
	 * Show the given spot selection as a table displaying their individual features. 
	 */
	@Override
	@SuppressWarnings("serial")
	public void highlightSpots(Collection<Spot> spots) {
		if (!doHighlightSelection)
			return;
		if (spots.size() == 0) {
			scrollTable.setVisible(false);
			return;
		}

		// Fill feature table
		try { // Dummy protection for ultra fast selection / de-selection events. Ugly.

			DefaultTableModel dm = new DefaultTableModel() { // Un-editable model
				@Override
				public boolean isCellEditable(int row, int column) { return false; }
			};

			for (Spot spot : spots) {
				if (null == spot)
					continue;
				Object[] columnData = new Object[features.size()];
				for (int i = 0; i < columnData.length; i++) 
					columnData[i] = String.format("%.1f", spot.getFeature(features.get(i)));
				dm.addColumn(spot.toString(), columnData);
			}
			table.setModel(dm);
		} catch (ConcurrentModificationException cme) {
			// do nothing
		} catch (ArrayIndexOutOfBoundsException aobe) {
			// do nothing
		}
		
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
			table.getColumnModel().getColumn(i).setPreferredWidth(TrackSchemeFrame.TABLE_CELL_WIDTH);
		}
		for (Component c : scrollTable.getColumnHeader().getComponents()) {
			c.setBackground(getBackground());
		}
		scrollTable.getColumnHeader().setOpaque(false);
		scrollTable.setVisible(true);
		revalidate();
	}

	/*
	 * PRIVATE METHODS
	 */

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
		table.setSelectionForeground(Color.YELLOW.darker());
		table.setGridColor(TrackSchemeFrame.GRID_COLOR);

		JList rowHeader = new JList(lm);
		rowHeader.setFixedCellWidth(TrackSchemeFrame.TABLE_ROW_HEADER_WIDTH);
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
	}

	public FeaturePlotSelectionPanel getFeatureSelectionPanel() {
		return featureSelectionPanel;
	}

}
