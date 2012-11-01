package fiji.util;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import ij.plugin.PlugIn;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import java.net.URL;

import java.util.Hashtable;

/**
 * A temporary diverter for all the menu items
 *
 * Use this class to intercept the next click on a menu item. The action()
 * method will be called, passing the name of the menu item as parameter.
 *
 * The 'Escape' key cancels the diversion.
 */
public abstract class MenuItemDiverter implements KeyListener, PlugIn, WindowFocusListener {
	protected static ImageJ ij;
	protected static Cursor cursor, diversionCursor;
	protected static Hashtable actions;

	protected abstract String getTitle();

	protected abstract void action(String arg);

	protected URL getCursorPath() {
		return getClass().getResource("/help-cursor.gif");
	}

	@Override
	public void run(String arg) {
		if (arg == null || arg.equals(""))
			setActions();
		else {
			resetActions();
			action(arg);
		}
	}

	protected String getAction(Object key) {
		return getClass().getName() + "(\"" + key + "\")";
	}

	public void setActions() {
		ij = IJ.getInstance();
		if (ij == null)
			return;

		actions = new Hashtable();
		Hashtable table = Menus.getCommands();
		if (table.size() == 0) {
			IJ.error("No menu items found!");
			return;
		}

		if (!"ij.plugin.Commands(\"quit\")".equals(table.get("Quit"))) {
			IJ.error("Cannot install another menu item diverter!");
			return;
		}
		for (Object key : table.keySet()) {
			actions.put(key, table.get(key));
			table.put(key, getAction(key));
		}
		cursor = ij.getCursor();
		if (diversionCursor == null) {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			URL url = getCursorPath();
			Image diversion = toolkit.getImage(url);
			Point hotSpot = new Point(6, 7);
			diversionCursor = toolkit.createCustomCursor(diversion, hotSpot, "cursor-" + getTitle().replace(' ', '-'));
		}
		ij.setCursor(diversionCursor);

		ij.addKeyListener(this);
		for (Component component : ij.getComponents())
			component.addKeyListener(this);
		ij.addWindowFocusListener(this);
		IJ.showStatus("Click menu entry for " + getTitle() + " (Esc to abort)");
	}

	public void resetActions() {
		if (actions == null)
			return;
		Hashtable table = Menus.getCommands();
		for (Object key : table.keySet()) {
			if (!table.get(key).equals(getAction(key)))
				continue;
			table.put(key, actions.get(key));
		}
		actions = null;
		ij.setCursor(cursor);
		ij.removeKeyListener(this);
		for (Component component : ij.getComponents())
			component.removeKeyListener(this);
		ij.removeWindowFocusListener(this);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			resetActions();
			IJ.showStatus(getTitle() + " aborted");
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {}
	@Override
	public void keyReleased(KeyEvent e) {}

       	@Override
	public void windowGainedFocus(WindowEvent e) {
		resetActions();
	}

	@Override
	public void windowLostFocus(WindowEvent e) {}
}