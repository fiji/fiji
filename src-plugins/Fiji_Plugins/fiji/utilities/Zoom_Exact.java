
/* Copyright Albert Cardona @ 2006
 * General Public License applies.
 * Use at your own risk.
 */

package fiji.utilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.Field;

public class Zoom_Exact implements PlugIn {
	
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (null == imp) {
			IJ.log("Zoom Exact: no images open!");
			return;
		}
		ImageWindow win = imp.getWindow();
		if (null == win) {
			IJ.log("Zoom Exact: the image is not shown in a ImageWindow.");
			return;
		}
		ImageCanvas c = win.getCanvas();
		if (null == c) {
			IJ.log("Zoom Exact: the window has no canvas!");
			return;
		}
		GenericDialog gd = new GenericDialog(" Exact Zoom");
		gd.addNumericField("Zoom (%): ", c.getMagnification() * 100, 0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		double mag = gd.getNextNumber() / 100.0;
		if (mag <= 0.0) mag = 1.0;
		win.getCanvas().setMagnification(mag);

		// see if it fits
		double w = imp.getWidth() * mag;
		double h = imp.getHeight() * mag;
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Rectangle r = win.getBounds();
		if (w > screen.width - 10) w = screen.width - 10;
		if (h > screen.height - 30) h = screen.height - 30;
		try {
			Field f_srcRect = c.getClass().getDeclaredField("srcRect");
			f_srcRect.setAccessible(true);
			f_srcRect.set(c, new Rectangle(0, 0, (int)(w/mag), (int)(h/mag)));
			c.setDrawingSize((int)w, (int)h);
			win.pack();
			c.repaint();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
