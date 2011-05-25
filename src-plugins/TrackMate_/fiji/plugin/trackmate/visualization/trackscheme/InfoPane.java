package fiji.plugin.trackmate.visualization.trackscheme;

import static fiji.plugin.trackmate.gui.TrackMateFrame.SMALL_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import org.jgrapht.graph.DefaultWeightedEdge;

import fiji.plugin.trackmate.Feature;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.visualization.TMSelectionDisplayer;

class InfoPane extends JPanel implements TMSelectionDisplayer {

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
	FeaturePlotSelectionPanel<Feature> featureSelectionPanel;

	/*
	 * CONSTRUCTOR
	 */

	public InfoPane() {
		init();
	}

	/*
	 * PUBLIC METHODS
	 */

	/**
	 * Ignored.
	 */
	@Override
	public void highlightEdges(Set<DefaultWeightedEdge> edges) {}

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
			if (null == spot)
				continue;
			Object[] columnData = new Object[Feature.values().length];
			for (int i = 0; i < columnData.length; i++) 
				columnData[i] = String.format("%.1f", spot.getFeature(Feature.values()[i]));
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

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setOpaque(false);
		renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		renderer.setFont(SMALL_FONT);			
		for(int i=0; i<table.getColumnCount(); i++) {
			table.setDefaultRenderer(table.getColumnClass(i), renderer);
			table.getColumnModel().getColumn(i).setPreferredWidth(TrackSchemeFrame.TABLE_CELL_WIDTH);
		}
		for (Component c : scrollTable.getColumnHeader().getComponents())
			c.setBackground(getBackground());
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

		featureSelectionPanel = new FeaturePlotSelectionPanel<Feature>(Feature.POSITION_T);

		setLayout(new BorderLayout());
		add(scrollTable, BorderLayout.CENTER);
		add(featureSelectionPanel, BorderLayout.SOUTH);
	}

}