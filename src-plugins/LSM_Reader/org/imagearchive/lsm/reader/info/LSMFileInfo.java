package org.imagearchive.lsm.reader.info;

import ij.io.FileInfo;

import java.util.ArrayList;

public class LSMFileInfo extends FileInfo {
	public boolean fullyRead = false;
	public ArrayList imageDirectories = new ArrayList();
}