package fiji.pluginManager.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import fiji.pluginManager.logic.PluginCollection;
import fiji.pluginManager.logic.PluginObject;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	private PluginTableModel pluginTableModel;
	private MainUserInterface mainUserInterface;

	public PluginTable(PluginCollection pluginList, MainUserInterface mainUserInterface) {
		this.mainUserInterface = mainUserInterface;
		setupTable(pluginList);
	}

	public void setupTable(PluginCollection pluginList) {
		//default display: All plugins shown
		setupTableModel(pluginList);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			//Called when a row is selected
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = getSelectedRow();
				if (viewRow >= 0) {
					int modelRow = convertRowIndexToModel(viewRow);
					PluginObject plugin = pluginTableModel.getEntry(modelRow);
					mainUserInterface.displayPluginDetails(plugin);
				}
			}

		});

		//Set appearance of table
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0,0));
		setAutoResizeMode(PluginTable.AUTO_RESIZE_ALL_COLUMNS);
		setRequestFocusEnabled(false);
		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

			// method to over-ride - returns cell renderer component
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {

				// let the default renderer prepare the component for us
				Component comp = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				PluginObject plugin = pluginTableModel.getEntry(modelRow);

				comp.setFont(comp.getFont().deriveFont(plugin.actionSpecified() ? Font.BOLD : Font.PLAIN));

				return comp;
			}
		});

		//set up the table properties and other settings
		setCellSelectionEnabled(true);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		requestFocusInWindow();
	}

	private void setColumnWidths(int col1Width, int col2Width) {
		TableColumn col1 = getColumnModel().getColumn(0);
		TableColumn col2 = getColumnModel().getColumn(1);

		col1.setPreferredWidth(col1Width);
		col1.setMinWidth(col1Width);
		col1.setResizable(false);
		col2.setPreferredWidth(col2Width);
		col2.setMinWidth(col2Width);
		col2.setResizable(true);
	}

	//Set up table model, to be called each time display list is to be changed
	public void setupTableModel(PluginCollection plugins) {
		setModel(pluginTableModel = new PluginTableModel(plugins));
		getModel().addTableModelListener(this);
		getModel().addTableModelListener(mainUserInterface); //listen for changes (tableChanged(TableModelEvent e))
		setColumnWidths(250, 100);
		pluginTableModel.fireTableChanged(new TableModelEvent(pluginTableModel));
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginObject plugin = getPluginFromRow(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0)
			return super.getCellEditor(row,col);
		String[] labels = plugin.getStatus().getActionLabels(mainUserInterface.isDeveloper());
		return new DefaultCellEditor(new JComboBox(labels));
	}

	public PluginObject getPluginFromRow(int viewRow) {
		int modelRow = convertRowIndexToModel(viewRow);
		return pluginTableModel.getEntry(modelRow);
	}

	class PluginTableModel extends AbstractTableModel {
		private PluginCollection plugins;

		public PluginTableModel(PluginCollection plugins) {
			this.plugins = plugins;
		}

		public int getColumnCount() {
			return 2; //Name of plugin, status
		}

		public Class getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0: return String.class; //filename
				case 1: return String.class; //status/action
				default: return Object.class;
			}
		}

		public String getColumnName(int column) {
			switch (column) {
				case 0: return "Name";
				case 1: return "Status/Action";
				default: throw new Error("Column out of range");
			}
		}

		public PluginObject getEntry(int rowIndex) {
			return plugins.get(rowIndex);
		}

		public int getRowCount() {
			return plugins.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			PluginObject plugin = plugins.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return plugin.getFilename();
				case 1:
					return plugin.getAction().getLabel();
			}
			throw new Error("Column out of range");
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (columnIndex == 1) {
				PluginObject plugin = plugins.get(rowIndex);
				plugin.setAction(plugin.getAction((String)value));
				fireTableChanged(new TableModelEvent(this));
			}
		}
	}
}
