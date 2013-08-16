package fiji.plugin.trackmate.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;

import javax.swing.JFrame;

public class GuiUtils {

	

	/**
	 * Positions a JFrame more or less cleverly next a {@link Component}.
	 */
	public static void positionWindow(JFrame gui, Component component) {


		if (null != component) {

			// Get total size of all screens
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice[] gs = ge.getScreenDevices();
			int screenWidth = 0;
			for (int i=0; i<gs.length; i++) {
				DisplayMode dm = gs[i].getDisplayMode();
				screenWidth += dm.getWidth();
			}

			Point windowLoc = component.getLocation();
			Dimension windowSize = component.getSize();
			Dimension guiSize = gui.getSize();
			if (guiSize.width > windowLoc.x) {
				if (guiSize.width > screenWidth - (windowLoc.x + windowSize.width)) {
					gui.setLocationRelativeTo(null); // give up
				} else {
					gui.setLocation(windowLoc.x+windowSize.width, windowLoc.y); // put it to the right
				}
			} else {
				gui.setLocation(windowLoc.x-guiSize.width, windowLoc.y); // put it to the left
			}

		} else {
			gui.setLocationRelativeTo(null);
		}
	}
	
	
}
