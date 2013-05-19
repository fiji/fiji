package org.imagearchive.lsm.toolbox.gui;

import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import org.imagearchive.lsm.toolbox.ServiceMediator;

public class ImageFocusListener implements WindowFocusListener {

	public ImageFocusListener() {
	}

	public void windowGainedFocus(WindowEvent e) {
		DetailsFrame details = ServiceMediator.getDetailsFrame();
		InfoFrame info = ServiceMediator.getInfoFrame();
		if (info != null)
			info.updateInfoFrame();
		if (details != null)
			details.updateTreeAndLabels();
	}

	public void windowLostFocus(WindowEvent e) {
	}
}
