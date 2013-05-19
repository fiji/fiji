package org.imagearchive.lsm.toolbox.gui;

import org.imagearchive.lsm.reader.info.LSMFileInfo;

public class ListBoxImage {

	public String title = "";

	public String fileName = "";

	public String masses = "";

	public int imageIndex;

	public LSMFileInfo lsmFi;

	public ListBoxImage(String title, LSMFileInfo lsmFi ,int imageIndex) {
		this.title = title;
		this.lsmFi = lsmFi;
		this.imageIndex = imageIndex;
	}

	public String toString() {
		return title;
	}
}
