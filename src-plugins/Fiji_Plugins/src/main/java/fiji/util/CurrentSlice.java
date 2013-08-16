package fiji.util;

import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageWindow;

import ij.process.ImageProcessor;

import java.awt.Component;
import java.awt.Scrollbar;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/*
 * This class provides the basis for all plugins which need to be notified
 * whenever the current slice (or image) changes.
 */
public abstract class CurrentSlice
		implements AdjustmentListener, ImageListener, WindowListener {
	protected ImagePlus image;
	protected ImageProcessor ip;
	protected int slice;

	protected void obsolete() { } // called just before
	protected abstract void changed(); // called just after updating ip

	public CurrentSlice() {
		ImagePlus.addImageListener(this);
		handleImages(true);
		check(WindowManager.getCurrentImage());
	}

	public void dispose() {
		ImagePlus.removeImageListener(this);
		handleImages(false);
	}

	protected void handleImages(boolean addAsListener) {
		int[] ids = WindowManager.getIDList();
		if (ids != null)
			for (int id : ids)
				handleImage(WindowManager.getImage(id),
						addAsListener);
	}

	protected void handleImage(ImagePlus image, boolean addAsListener) {
		ImageWindow window = image.getWindow();

		for (Component comp : window.getComponents())
			if (comp instanceof Scrollbar) {
				Scrollbar bar = (Scrollbar)comp;
				if (addAsListener)
					bar.addAdjustmentListener(this);
				else
					bar.removeAdjustmentListener(this);
			}

		if (addAsListener)
			window.addWindowListener(this);
		else
			window.removeWindowListener(this);
	}

	public void check(ImagePlus imp) {
		if (image == imp && (image == null ||
					(ip == image.getProcessor() &&
					 slice == image.getCurrentSlice())))
			return;

		if (image != null && image.getProcessor() == null)
			image = null;
		if (image != null)
			obsolete();

		image = imp;
		ip = image == null ? null : image.getProcessor();
		slice = image == null ? 0 : image.getCurrentSlice();
		changed();
	}

	public void imageOpened(ImagePlus imp) {
		handleImage(imp, true);
		check(imp);
	}

	public void imageClosed(ImagePlus imp) {
		handleImage(imp, false);
		check(imp);
	}

	public void imageUpdated(ImagePlus imp) { check(imp); }

	protected void checkWindow(Component window) {
		if (!(window instanceof ImageWindow))
			return;
		check(((ImageWindow)window).getImagePlus());
	}

	public void adjustmentValueChanged(AdjustmentEvent event) {
		Scrollbar bar = (Scrollbar)event.getSource();
		checkWindow(bar.getParent());
	}

        public void windowActivated(WindowEvent e) {
		checkWindow((Component)e.getSource());
        }

        public void windowClosing(WindowEvent e) { }
        public void windowClosed(WindowEvent e) {}
        public void windowDeactivated(WindowEvent e) {}
        public void windowDeiconified(WindowEvent e) {}
        public void windowIconified(WindowEvent e) {}
        public void windowOpened(WindowEvent e) {}

}
