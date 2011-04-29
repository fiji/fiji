package ij3d.behaviors;

import ij3d.DefaultUniverse;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * This class relays unconsumed events to AWT listeners.
 *
 * @author Benjamin Schmid
 */
public class Behavior2ListenerProxy extends InteractiveBehavior {
	protected final KeyListener keyListener;
	protected final MouseListener mouseListener;
	protected final MouseMotionListener mouseMotionListener;
	protected final MouseWheelListener mouseWheelListener;

	public Behavior2ListenerProxy(DefaultUniverse universe,
			final KeyListener keyListener, final MouseListener mouseListener, final MouseMotionListener mouseMotionListener, final MouseWheelListener mouseWheelListener) {
		super(universe);
		this.keyListener = keyListener;
		this.mouseListener = mouseListener;
		this.mouseMotionListener = mouseMotionListener;
		this.mouseWheelListener = mouseWheelListener;
	}

	/**
	 * Process key events.
	 * @param e
	 */
	protected void doProcess(KeyEvent e) {
		if (keyListener == null || e.isConsumed())
			return;
		switch (e.getID()) {
		case KeyEvent.KEY_RELEASED:
			keyListener.keyReleased(e);
			break;
		case KeyEvent.KEY_TYPED:
			keyListener.keyTyped(e);
			break;
		case KeyEvent.KEY_PRESSED:
			keyListener.keyPressed(e);
			break;
		}
	}

	/**
	 * Process mouse events.
	 * @param e
	 */
	protected void doProcess(MouseEvent e) {
		if (e.isConsumed())
			return;
		if (mouseListener != null)
			switch (e.getID()) {
			case MouseEvent.MOUSE_PRESSED:
				mouseListener.mousePressed(e);
				break;
			case MouseEvent.MOUSE_RELEASED:
				mouseListener.mouseReleased(e);
				break;
			case MouseEvent.MOUSE_CLICKED:
				mouseListener.mouseClicked(e);
				break;
			case MouseEvent.MOUSE_ENTERED:
				mouseListener.mouseEntered(e);
				break;
			case MouseEvent.MOUSE_EXITED:
				mouseListener.mouseExited(e);
				break;
			}
		if (mouseMotionListener != null)
			switch (e.getID()) {
			case MouseEvent.MOUSE_MOVED:
				mouseMotionListener.mouseMoved(e);
				break;
			case MouseEvent.MOUSE_DRAGGED:
				mouseMotionListener.mouseDragged(e);
				break;
			}
		if (mouseWheelListener != null)
			switch (e.getID()) {
			case MouseEvent.MOUSE_WHEEL:
				mouseWheelListener.mouseWheelMoved((MouseWheelEvent)e);
				break;
			}
	}
}

