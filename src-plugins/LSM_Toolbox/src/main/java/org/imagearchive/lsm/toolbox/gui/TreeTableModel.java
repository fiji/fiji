package org.imagearchive.lsm.toolbox.gui;

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.table.AbstractTableModel;

class TreeTableModel extends AbstractTableModel {

	private String[] columnNames = { "Tag", "Property" };

	private LinkedHashMap<String, Object> dataMap = null;

	private Object[][] data = null;

	private boolean filtered = false;

	public TreeTableModel(LinkedHashMap<String, Object> dataMap) {
		this.dataMap = dataMap;
		setData(dataMap);
	}

	public TreeTableModel() {
	}

	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	public int getRowCount() {
		if (data == null)
			return 0;
		return data.length;
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public void setValueAt(Object value, int row, int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}

	public void setData(LinkedHashMap<String, Object> dataMap) {
		if (dataMap != null) {
			if (filtered) dataMap = getFilteredMap(dataMap);
			Iterator<String> iterator = dataMap.keySet().iterator();
			String tag;
			data = new Object[dataMap.size()][2];
			for (int i = 0; iterator.hasNext(); i++) {
				tag = (String) iterator.next();
				data[i][0] = tag;
				data[i][1] = dataMap.get(tag);
			}
		} else
			data = null;
		fireTableDataChanged();
	}

	public LinkedHashMap<String, Object> getFilteredMap(LinkedHashMap<String, Object> dataMap) {
		LinkedHashMap<String, Object> filteredMap = new LinkedHashMap<String, Object>();
		Iterator<String> iterator = dataMap.keySet().iterator();
		String tag;
		data = new Object[dataMap.size()][2];
		for (int i = 0; iterator.hasNext(); i++) {
			tag = (String) iterator.next();
			if (tag.indexOf("<UNKNOWN@") == -1)
				filteredMap.put(tag,dataMap.get(tag));
		}
		return filteredMap;
	}

	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}

	public boolean getFiltered() {
		return filtered;
	}
}
