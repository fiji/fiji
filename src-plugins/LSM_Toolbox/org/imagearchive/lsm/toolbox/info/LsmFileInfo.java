package org.imagearchive.lsm.toolbox.info;

import ij.io.FileInfo;

import java.util.ArrayList;

import org.imagearchive.lsm.toolbox.MasterModel;

public class LsmFileInfo extends FileInfo{
	private MasterModel masterModel;
	public LsmFileInfo(MasterModel masterModel){
		this.masterModel = masterModel;
	}
	public ArrayList imageDirectories = new ArrayList(); //list of ImageDirectories
	
	
	public MasterModel getMasterModel(){
		return masterModel;
	}
}
