package org.imagearchive.lsm.toolbox.gui;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

public class LsmImageTableModel extends AbstractTableModel{

    public ArrayList<File> files;

    public String[] columnTitles = {"Filename","Size","Last modifed"};

    public LsmImageTableModel(ArrayList<File> files){
        this.files = files;
    }

    public LsmImageTableModel(){
        files = new ArrayList<File>();
    }

    public int getRowCount() {
        return files.size();
    }

    public int getColumnCount() {
        return columnTitles.length;
    }
    public String getColumnName(int columnIndex){
	return columnTitles[columnIndex];
    }

    public Object getValueAt(int row, int col) {
        File file = (File)files.get(row);
        if (col == 0)
		return file.getName();
        if (col == 1)
		return new DecimalFormat("###.##").format(file.length() / 1024f) + " kbytes";
        if (col == 2)
		return new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(new Date(file.lastModified()));
	return "N/A";
    }

    public Class<String> getColumnClass(int col){
        return String.class;
    }

    public void addFile(File file){
        files.add(file);
        fireTableDataChanged();
    }

    public void removeFile(int index){
        files.remove(index);
        fireTableDataChanged();
    }

    public void removeFile(int row, int col){
        files.remove(row*columnTitles.length+col);
        fireTableDataChanged();
    }
    public void removeAllFiles(){
        files.removeAll(files);
        fireTableDataChanged();
    }

    public void setValueAt(Object object,int row, int col){
        files.set(row*columnTitles.length+col,(File)object);
        fireTableDataChanged();
        fireTableCellUpdated(row, col);
    }

    public void insertFile(Object object,int row, int col){
        files.add(row*columnTitles.length+col,(File)object);
        fireTableDataChanged();
    }

    public void setFileAt(File file,int row, int col){
        setValueAt(file,row,col);
    }
    public File getFileAt(int row, int col){
        return (File)files.get(row*columnTitles.length+col);
    }

    public ArrayList<File> getFiles() {
        return files;
    }
}
