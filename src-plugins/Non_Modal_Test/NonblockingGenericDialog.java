import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.event.ActionEvent;

public class NonblockingGenericDialog extends GenericDialog {
	public NonblockingGenericDialog(String title) {
		super(title, null);
		setModal(false);
	}

	public synchronized void showDialog() {
		super.showDialog();
		try {
			wait();
		} catch (InterruptedException e) {
			IJ.error("Dialog " + getTitle() + " was interrupted.");
		}
	}

	public synchronized void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if (wasOKed() || wasCanceled())
			notify();
	}
}
