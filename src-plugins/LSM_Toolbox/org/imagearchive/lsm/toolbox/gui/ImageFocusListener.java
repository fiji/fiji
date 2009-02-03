package org.imagearchive.lsm.toolbox.gui;

import ij.ImagePlus;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.imagearchive.lsm.toolbox.MasterModel;
import org.imagearchive.lsm.toolbox.info.LsmFileInfo;

public class ImageFocusListener extends WindowAdapter {
	private ImagePlus imp;

	private LsmFileInfo lfi;

	private MasterModel masterModel;

	public ImageFocusListener(MasterModel masterModel, LsmFileInfo lfi,
			ImagePlus imp) {
		this.imp = imp;
		this.lfi = lfi;
		this.masterModel = masterModel;
	}

	public void windowActivated(WindowEvent e) {
		masterModel.setLSMFI(lfi);
	}

	public void windowLostFocus(WindowEvent e) {
		masterModel.setLSMFI(lfi);
	}
}
