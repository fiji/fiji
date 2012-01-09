package vib.app;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class FileGroup extends ArrayList<File> {
	public String name;

	public FileGroup(String name) {
		this.name = name;
	}

	public void copy(FileGroup fg) {
		this.name = fg.name;
		clear();
		addAll(fg);
	}

	public String toString() {
		String result = name + ":";
		for (int i = 0; i < size(); i++)
			result += " " + get(i).getName();
		return result;
	}

	public int getIndex(String name) {
		for (int i = 0; i < size(); i++)
			if (get(i).getName().equals(name))
				return i;
		return -1;
	}

	public boolean add(String name) {
		return add(new File(name));
	}

	public void debug() {
		System.out.println("" + this);
	}

	public String toCSV() {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			if (i > 0)
				buf.append(',');
			buf.append(get(i).getAbsolutePath());
		}
		return buf.toString();
	}

	public boolean fromCSV(String s) {
		boolean success = true;
		clear();
		StringTokenizer st = new StringTokenizer(s, ",");
		while(st.hasMoreTokens()){
			String path = st.nextToken();
			File file = new File(path);
			if (!add(file))
				success = false;
		}
		return success;
	}

	public FileGroup clone() {
		FileGroup clone = new FileGroup(this.name);
		clone.addAll(this);
		return clone;
	}
}
