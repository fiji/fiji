package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import org.imagearchive.lsm.reader.info.LSMFileInfo;
import org.imagearchive.lsm.toolbox.MasterModel;

public class LSMWindow extends ImageWindow {

	LSMFileInfo lsm;

	public LSMWindow(MasterModel masterModel, ImagePlus imp, ImageCanvas ic) {
		super(imp, ic);
	}

	public LSMWindow(MasterModel masterModel, String title) {
		super(title);
	}

	public LSMWindow(MasterModel masterModel, ImagePlus imp) {
		super(imp);
	}

}
