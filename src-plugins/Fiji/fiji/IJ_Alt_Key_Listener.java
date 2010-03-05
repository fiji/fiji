package fiji;

import ij.IJ;
import ij.ImageJ;

import java.awt.AWTException;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.PopupMenu;
import java.awt.Robot;

import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import java.lang.reflect.Method;

public class IJ_Alt_Key_Listener extends KeyAdapter implements FocusListener {
	boolean altPressed;
	int pressedKeys;
	Runnable openMenu = getOpener();

	public void run() {
		if (removeRegisteredListeners()) {
			if (IJ.debugMode)
				IJ.showStatus("Alt Key listener removed.");
			return;
		}
		if (openMenu != null) {
			ImageJ ij = IJ.getInstance();
			ij.addKeyListener(this);
			ij.addFocusListener(this);
			if (IJ.debugMode)
				IJ.showStatus("Alt Key listener installed.");
		}
	}

	public static boolean removeRegisteredListeners() {
		return removeRegisteredKeyListener() &&
			removeRegisteredFocusListener();
	}

	public static boolean removeRegisteredKeyListener() {
		for (KeyListener listener : IJ.getInstance().getKeyListeners())
			if (listener instanceof IJ_Alt_Key_Listener) {
				IJ.getInstance().removeKeyListener(listener);
				return true;
			}
		return false;
	}

	public static boolean removeRegisteredFocusListener() {
		for (FocusListener listener : IJ.getInstance().getFocusListeners())
			if (listener instanceof IJ_Alt_Key_Listener) {
				IJ.getInstance().removeFocusListener(listener);
				return true;
			}
		return false;
	}

	public void keyPressed(KeyEvent e) {
		if (pressedKeys == 0 && e.getKeyCode() == KeyEvent.VK_ALT)
			altPressed = true;
		pressedKeys++;
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			altPressed = false;
			if (pressedKeys == 1 && openMenu != null)
				openMenu.run();
		}
		pressedKeys = Math.max(0, pressedKeys - 1);
	}

	public void focusGained(FocusEvent e) {
		pressedKeys = 0;
	}

	public void focusLost(FocusEvent e) {
		pressedKeys = 0;
	}

	static Runnable getOpener() {
		try {
			return getX11Opener();
		} catch (Exception e) { /* ignore */ }
		try {
			return getAquaOpener();
		} catch (Exception e) { /* ignore */ }
		return null;
	}

	static Runnable getX11Opener() throws NoSuchMethodException {
		final MenuBar bar = IJ.getInstance().getMenuBar();
		final Method method = bar.getPeer().getClass()
			.getDeclaredMethod("handleF10KeyPress",
					new Class[] { KeyEvent.class });
		method.setAccessible(true);
		return new Runnable() {
			public void run() {
				KeyEvent event = new KeyEvent(IJ.getInstance(),
					KeyEvent.VK_F10,
					System.currentTimeMillis(), 0,
					KeyEvent.VK_F10);
				try {
					method.invoke(bar.getPeer(),
						new Object[] { event });
				} catch (Exception e) { /* ignore */ }
			}
		};
	}

	static Runnable getAquaOpener() throws UnsupportedOperationException {
		if (!IJ.isMacOSX())
			throw new UnsupportedOperationException("No Aqua available");
		/*
		 * After a short delay, send Ctrl+F2, which is the shortcut on
		 * MacOSX to gain keyboard control to the menu bar.
		 */
		return new Runnable() {
			public void run() {
				try {
					Robot robot = new Robot();
					robot.delay(10);
					robot.keyPress(KeyEvent.VK_CONTROL);
					robot.keyPress(KeyEvent.VK_F2);
					robot.keyRelease(KeyEvent.VK_F2);
					robot.keyRelease(KeyEvent.VK_CONTROL);
				} catch (AWTException e) {
					IJ.handleException(e);
				}
			}
		};
	}

	public static void main(String[] args) {
		//System.err.println("Hello");
		new IJ_Alt_Key_Listener().run();
	}
}
