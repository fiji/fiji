package org.imagearchive.lsm.toolbox.gui;


import org.imagearchive.lsm.toolbox.info.LsmFileInfo;

public class ListBoxImage {

	public String title = "";

	public String fileName = "";
	
	public String masses = "";
	
	public int imageIndex;

	public LsmFileInfo lsmFi;
	
	public ListBoxImage(String title, LsmFileInfo lsmFi ,int imageIndex) {
		this.title = title;
		this.lsmFi = lsmFi;
		this.imageIndex = imageIndex;
	}

	public String toString() {
		return title;
	}
}
