package fiji.updater.ui;

import fiji.updater.logic.PluginCollection;
import fiji.updater.logic.PluginCollection.UpdateSite;
import fiji.updater.logic.PluginObject;

import fiji.updater.logic.PluginObject.Action;
import fiji.updater.logic.PluginObject.Status;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import javax.swing.event.TableModelEvent;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

/*
 * This class' role is to be in charge of how the Table should be displayed
 */
public class PluginTable extends JTable {
	protected UpdaterFrame updaterFrame;
	protected PluginCollection plugins;
	private PluginTableModel pluginTableModel;
	protected Font plain, bold;

	public PluginTable(UpdaterFrame updaterFrame) {
		this.updaterFrame = updaterFrame;
		plugins = updaterFrame.plugins;

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

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				maybeShowPopupMenu(e);
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

	public PluginCollection getAllPlugins() {
		return plugins;
	}

	public void setPlugins(Iterable<PluginObject> plugins) {
		pluginTableModel.setPlugins(plugins);
	}

	public TableCellEditor getCellEditor(int row, int col) {
		PluginObject plugin = getPlugin(row);

		//As we follow PluginTableModel, 1st column is filename
		if (col == 0)
			return super.getCellEditor(row,col);
		Action[] actions = plugins.getActions(plugin);
		return new DefaultCellEditor(new JComboBox(actions));
	}

	public void maybeShowPopupMenu(MouseEvent e) {
		if (!e.isPopupTrigger())
			return;
		final Iterable<PluginObject> selected = getSelectedPlugins(e.getY() / getRowHeight());
		if (!selected.iterator().hasNext())
			return;
		JPopupMenu menu = new JPopupMenu();
		int count = 0;
		for (final PluginObject.Action action : plugins.getActions(selected)) {
			JMenuItem item = new JMenuItem(action.toString());
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (PluginObject plugin : selected)
						setPluginAction(plugin, action);
				}
			});
			menu.add(item);
			count++;
		}
		if (count == 0) {
			JMenuItem noActions = new JMenuItem("<No common actions>");
			noActions.setEnabled(false);
			menu.add(noActions);
		}
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public PluginObject getPlugin(int row) {
		PluginObject.LabeledPlugin plugin =
			(PluginObject.LabeledPlugin)getValueAt(row, 0);
		return plugin == null ? null : plugin.getPlugin();
	}

	public Iterable<PluginObject> getSelectedPlugins() {
		return getSelectedPlugins(-1);
	}

	public Iterable<PluginObject> getSelectedPlugins(int fallbackRow) {
		int[] rows = getSelectedRows();
		if (rows.length == 0 && fallbackRow >= 0 && getPlugin(fallbackRow) != null)
			rows = new int[] { fallbackRow };
		PluginObject[] result = new PluginObject[rows.length];
		for (int i = 0; i < rows.length; i++)
			result[i] = getPlugin(rows[i]);
		return Arrays.asList(result);
	}

	public String[] getUpdateSitesWithUploads(PluginCollection plugins) {
		Set<String> sites = new HashSet<String>();
		for (PluginObject plugin : plugins.toUpload())
			sites.add(plugin.updateSite);
		return sites.toArray(new String[sites.size()]);
	}

	public boolean areAllSelectedPluginsUploadable() {
		if (getSelectedRows().length == 0)
			return false;
		for (PluginObject plugin : getSelectedPlugins())
			if (!plugin.isUploadable(updaterFrame.plugins))
				return false;
		return true;
	}

	public boolean chooseUpdateSite(PluginCollection plugins, PluginObject plugin) {
		List<String> list = new ArrayList<String>();
		for (String name : plugins.getUpdateSiteNames()) {
			UpdateSite site = plugins.getUpdateSite(name);
			if (site.isUploadable())
				list.add(name);
		}
		if (list.size() == 0) {
			error("No upload site available");
			return false;
		}
		if (list.size() == 1 && list.get(0).equals(PluginCollection.DEFAULT_UPDATE_SITE)) {
			plugin.updateSite = PluginCollection.DEFAULT_UPDATE_SITE;
			return true;
		}
		String updateSite = SwingTools.getChoice(updaterFrame.hidden, updaterFrame,
			list, "To which upload site do you want to upload " + plugin.filename + "?",
			"Upload site");
		if (updateSite == null)
			return false;
		plugin.updateSite = updateSite;
		return true;
	}

	protected void setPluginAction(PluginObject plugin, Action action) {
		if (!plugin.getStatus().isValid(action))
			return;
		if (action == Action.UPLOAD) {
			String[] sitesWithUploads = getUpdateSitesWithUploads(updaterFrame.plugins);
			if (sitesWithUploads.length > 1) {
				error("Internal error: multiple upload sites selected");
				return;
			}
			boolean isNew = plugin.getStatus() == Status.NOT_FIJI;
			if (sitesWithUploads.length == 0) {
				if (isNew && !chooseUpdateSite(updaterFrame.plugins, plugin))
					return;
			}
			else {
				String siteName = sitesWithUploads[0];
				if (isNew)
					plugin.updateSite = siteName;
				else if (!plugin.updateSite.equals(siteName)) {
					error("Already have uploads for site '" + siteName
						+ "', cannot upload to '" + plugin.updateSite +"', too!");
					return;
				}
			}
		}
		plugin.setAction(updaterFrame.plugins, action);
		firePluginChanged(plugin);
	}

	protected class PluginTableModel extends AbstractTableModel {
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
				PluginObject plugin = getPlugin(row);
				setPluginAction(plugin, action);
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

	protected void error(String message) {
		SwingTools.showMessageBox(updaterFrame.hidden, updaterFrame, message, JOptionPane.ERROR_MESSAGE);
	}
}