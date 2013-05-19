import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;

import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;

public class Scrollable_StackWindow implements PlugIn {
	public void run(String arg) {
		ImagePlus image = IJ.getImage();
		image.setWindow(new Window(image, image.getCanvas()));
	}

	static class Window extends StackWindow implements MouseWheelListener {
		public Window(ImagePlus image, ImageCanvas canvas) {
			super(image, canvas);
			addMouseWheelListener(this);
		}

		/* For some funny reason, we get each event twice */
		boolean skip;

		public void mouseWheelMoved(MouseWheelEvent event) {
			synchronized(this) {
				skip = !skip;
				if (skip)
					return;

				int slice = imp.getCurrentSlice()
					+ event.getWheelRotation();
				if (slice < 1)
					slice = 1;
				else if (slice > imp.getStack().getSize())
					slice = imp.getStack().getSize();
				imp.setSlice(slice);
			}
		}
	}
}

