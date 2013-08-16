package fiji;

import fiji.gui.InvokeLater;
import ij.IJ;
import ij.ImageJ;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Method;

public class IJ_Alt_Key_Listener extends KeyAdapter implements FocusListener, Runnable {
	private int pressedKeys;
	private final Runnable openMenu = getOpener();

	@Override
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

	@Override
	public void keyPressed(KeyEvent e) {
		pressedKeys++;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			if (pressedKeys == 1 && openMenu != null) {
				new InvokeLater(25, openMenu).later(50);
			}
		}
		pressedKeys = Math.max(0, pressedKeys - 1);
	}

	@Override
	public void focusGained(FocusEvent e) {
		pressedKeys = 0;
	}

	@Override
	public void focusLost(FocusEvent e) {
		pressedKeys = 0;
	}

	public static Runnable getOpener() {
		try {
			return getX11Opener();
		} catch (Exception e) { /* ignore */ }
		try {
			return getAquaOpener();
		} catch (Exception e) { /* ignore */ }
		return null;
	}

	static Runnable getX11Opener() throws NoSuchMethodException {
		@SuppressWarnings("deprecation")
		final Method method = IJ.getInstance().getMenuBar()
			.getPeer().getClass()
			.getDeclaredMethod("handleF10KeyPress",
					new Class[] { KeyEvent.class });
		method.setAccessible(true);
		return new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				final ImageJ ij = IJ.getInstance();
				if (ij == null ||!ij.isFocused()) return;
				KeyEvent event = new KeyEvent(IJ.getInstance(),
					KeyEvent.VK_F10,
					System.currentTimeMillis(), 0,
					KeyEvent.VK_F10,
					KeyEvent.CHAR_UNDEFINED);
				try {
					method.invoke(ij.getMenuBar().getPeer(),
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
			@Override
			public void run() {
				final ImageJ ij = IJ.getInstance();
				if (ij == null ||!ij.isFocused()) return;
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
