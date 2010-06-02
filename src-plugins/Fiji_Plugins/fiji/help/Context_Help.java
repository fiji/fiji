package fiji.help;

import ij.IJ;
import ij.ImageJ;
import ij.Menus;

import ij.plugin.BrowserLauncher;
import ij.plugin.PlugIn;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.util.Hashtable;

public class Context_Help implements KeyListener, PlugIn {
        public final static String url =
                "http://pacific.mpi-cbg.de/wiki/index.php/";
        protected static ImageJ ij;
        protected static Cursor cursor, helpCursor;
        protected static Hashtable actions;

        public void run(String arg) {
                if (arg == null || arg.equals(""))
                        setActionsToHelp();
                else {
                        resetActions();
			action(arg);
                }
        }

	protected void action(String arg) {
		IJ.showStatus("Opening help for " + arg + "...");
		new BrowserLauncher().run(url + arg.replace(' ', '_')
			+ "?menuentry=yes");
	}

        protected String getHelpAction(Object key) {
                return getClass().getName() + "(\"" + key + "\")";
        }

        public void setActionsToHelp() {
                ij = IJ.getInstance();
                if (ij == null)
                        return;

                actions = new Hashtable();
                Hashtable table = Menus.getCommands();
                for (Object key : table.keySet()) {
                        actions.put(key, table.get(key));
                        table.put(key, getHelpAction(key));
                }
                cursor = ij.getCursor();
                if (helpCursor == null) {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			String path = System.getProperty("fiji.dir")
				+ "/images/help-cursor.gif";
			Image help = toolkit.getImage(path);
			Point hotSpot = new Point(6, 7);
			helpCursor = toolkit.createCustomCursor(help, hotSpot,
				"ContextHelp");
		}
                ij.setCursor(helpCursor);

                ij.addKeyListener(this);
		IJ.showStatus("Click menu entry for context help (Esc to abort)");
        }

        public void resetActions() {
                if (actions == null)
                        return;
                Hashtable table = Menus.getCommands();
                for (Object key : table.keySet()) {
                        if (!table.get(key).equals(getHelpAction(key)))
                                continue;
                        table.put(key, actions.get(key));
                }
                actions = null;
                ij.setCursor(cursor);
                ij.removeKeyListener(this);
        }

        public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == e.VK_ESCAPE) {
                        resetActions();
			IJ.showStatus("Context help aborted");
		}
        }

        public void keyTyped(KeyEvent e) {}
        public void keyReleased(KeyEvent e) {}
}
