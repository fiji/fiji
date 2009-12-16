package fiji.updater.ui;

import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginObject;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import fiji.updater.util.Util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	private PluginTableModel pluginTableModel;
	protected Font plain, bold;

	public PluginTable(PluginCollection plugins) {
		//Set appearance of table
		setShowGrid(false);
		setIntercellSpacing(new Dimension(0,0));
		setAutoResizeMode(PluginTable.AUTO_RESIZE_ALL_COLUMNS);
		setRequestFocusEnabled(false);

		//set up the table properties and other settings
		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setCellSelectionEnabled(true);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);

		pluginTableModel = new PluginTableModel(plugins);
		setModel(pluginTableModel);
		getModel().addTableModelListener(this);
		setColumnWidths(250, 100);

		setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(
					JTable table, Object value,
					boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component comp = super
					.getTableCellRendererComponent(table,
						value, isSelected, hasFocus,
						row, column);
				setStyle(comp, row, column);
				return comp;
			}
		});
	}

	/*
	 * This sets the font to bold when the user selected an action for
	 * this plugin, or when it is locally modified.
	 *
	 * It also warns loudly when the plugin is obsolete, but locally
	 * modified.
	 */
	protected void setStyle(Component comp, int row, int column) {
		if (plain == null) {
			plain = comp.getFont();
			bold = plain.deriveFont(Font.BOLD);
		}
		PluginObject plugin = getPlugin(row);
		if (plugin == null)
			return;
		comp.setFont(plugin.actionSpecified() ||
				plugin.isLocallyModified() ? bold : plain);
		comp.setForeground(plugin.getStatus() ==
				Status.OBSOLETE_MODIFIED ?
				Color.red : Color.black);
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

	public void setPlugins(Iterable<PluginObject> plugins) {
		pluginTableModel.setPlugins(plugins);
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginObject plugin = getPlugin(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0)
			return super.getCellEditor(row,col);
		Action[] actions = plugin.getStatus().getActions();
		return new DefaultCellEditor(new JComboBox(actions));
	}

	public PluginObject getPlugin(int row) {
		PluginObject.LabeledPlugin plugin =
			(PluginObject.LabeledPlugin)getValueAt(row, 0);
		return plugin == null ? null : plugin.getPlugin();
	}

	public Iterable<PluginObject> getSelectedPlugins() {
		int[] rows = getSelectedRows();
		PluginObject[] result = new PluginObject[rows.length];
		for (int i = 0; i < rows.length; i++)
			result[i] = getPlugin(rows[i]);
		return Arrays.asList(result);
	}

	class PluginTableModel extends AbstractTableModel {
		private PluginCollection plugins;
		Map<PluginObject, Integer> pluginToRow;

		public PluginTableModel(PluginCollection plugins) {
			this.plugins = plugins;
		}

		public void setPlugins(Iterable<PluginObject> plugins) {
			setPlugins(PluginCollection.clone(plugins));
		}

		public void setPlugins(PluginCollection plugins) {
			this.plugins = plugins;
			pluginToRow = null;
			fireTableChanged(new TableModelEvent(pluginTableModel));
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

		public Object getValueAt(int row, int column) {
			if (row < 0 || row >= plugins.size())
				return null;
			return plugins.get(row).getLabeledPlugin(column);
		}

		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 1;
		}

		public void setValueAt(Object value, int row, int column) {
			if (column == 1) {
				Action action = (Action)value;
				if (getPlugin(row).getStatus().isValid(action))
					getPlugin(row).setAction(action);
				fireRowChanged(row);
			}
		}

		public void fireRowChanged(int row) {
			fireTableRowsUpdated(row, row);
		}

		public void firePluginChanged(PluginObject plugin) {
			if (pluginToRow == null) {
				pluginToRow =
					new HashMap<PluginObject, Integer>();
			// the table may be sorted, and we need the model's row
				int i = 0;
				for (PluginObject p : plugins)
					pluginToRow.put(p, new Integer(i++));
			}
			Integer row = pluginToRow.get(plugin);
			if (row != null)
				fireRowChanged(row.intValue());
		}
	}

	public void firePluginChanged(PluginObject plugin) {
		pluginTableModel.firePluginChanged(plugin);
	}
}
