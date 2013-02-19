package ij3d.behaviors;

import ij.IJ;

import java.util.Enumeration;
import java.util.List;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupCondition;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOr;

import orthoslice.OrthoGroup;
import voltex.VolumeRenderer;

/**
 * This class interprets mouse and keyboard events and invokes the
 * desired actions. It uses the ContentTransformer, Picker and
 * ViewPlatformTransformer objects of the universe as helpers.
 *
 * @author Benjamin Schmid
 */
public class InteractiveBehavior extends Behavior {

	protected final DefaultUniverse univ;
	private ImageCanvas3D canvas;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private ContentTransformer contentTransformer;
	private Picker picker;
	private InteractiveViewPlatformTransformer viewTransformer;

	private static final int B1 = MouseEvent.BUTTON1_DOWN_MASK;
	private static final int B2 = MouseEvent.BUTTON2_DOWN_MASK;
	private static final int B3 = MouseEvent.BUTTON3_DOWN_MASK;

	private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	private static final int CTRL  = InputEvent.CTRL_DOWN_MASK;

	private static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	private static final int DELETE_POINT_MASK = InputEvent.ALT_DOWN_MASK |
						MouseEvent.BUTTON1_DOWN_MASK;

	public static final double TWO_RAD = 2 * Math.PI / 180;

	private List<InteractiveBehavior> external;

	public void setExternalBehaviours(List<InteractiveBehavior> bs) {
		external = bs;
	}
	public List<InteractiveBehavior> getExternalBehaviors() { return external; }

	private int lastToolID;

	/**
	 * Initializes a new InteractiveBehavior.
	 * @param univ
	 */
	public InteractiveBehavior(DefaultUniverse univ) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
		this.contentTransformer = univ.getContentTransformer();
		this.picker = univ.getPicker();
		this.viewTransformer = univ.getViewPlatformTransformer();
		mouseEvents = new WakeupOnAWTEvent[6];
		lastToolID = univ.ui.getToolId();
	}

	/**
	 * @see Behavior#initialize() Behavior.initialize
	 */
	public void initialize() {
		mouseEvents[0]= new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
		mouseEvents[1]= new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
		mouseEvents[2]= new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
		mouseEvents[3]= new WakeupOnAWTEvent(MouseEvent.MOUSE_CLICKED);
		mouseEvents[4]= new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL);
		mouseEvents[5]= new WakeupOnAWTEvent(AWTEvent.KEY_EVENT_MASK);
		wakeupCriterion = new WakeupOr(mouseEvents);
		this.wakeupOn(wakeupCriterion);
	}

	/**
	 * @see Behavior#processStimulus(Enumeration) Behavior.processStimulus
	 */
	public void processStimulus(Enumeration criteria) {
		/*
		if(!univ.ui.isHandTool() &&
			!univ.ui.isMagnifierTool() &&
			!univ.ui.isPointTool()) {

			wakeupOn(wakeupCriterion);
			return;
		}
		*/
		while(criteria.hasMoreElements()) {
			WakeupOnAWTEvent wakeup = (WakeupOnAWTEvent)criteria.nextElement();
			AWTEvent[] events = (AWTEvent[])wakeup.getAWTEvent();
			for(AWTEvent evt : events) {
				if(evt instanceof MouseEvent)
					doProcess((MouseEvent)evt);
				if(evt instanceof KeyEvent)
					doProcess((KeyEvent)evt);
			}
		}
		wakeupOn(wakeupCriterion);
	}

	private boolean shouldRotate(int mask) {
		int onmask = B2, onmask2 = B1;
		int offmask = SHIFT | CTRL;
		boolean b0 = (mask & (onmask | offmask)) == onmask;
		boolean b1 = (univ.ui.isHandTool()
				&& (mask & (onmask2|offmask)) == onmask2);
		return b0 || b1;
	}

	private boolean shouldTranslate(int mask) {
		int onmask = B2 | SHIFT, onmask2 = B1 | SHIFT;
		int offmask = CTRL;
		return (mask & (onmask | offmask)) == onmask ||
			(univ.ui.isHandTool()
				&& (mask & (onmask2|offmask)) == onmask2);
	}

	private boolean shouldZoom(int mask) {
		if(!univ.ui.isMagnifierTool())
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	private boolean shouldMovePoint(int mask) {
		if(!univ.ui.isPointTool())
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	private boolean isXYZKey(KeyEvent e) {
		int c = e.getKeyCode();
		boolean b = c == KeyEvent.VK_X ||
			c == KeyEvent.VK_Y ||
			c == KeyEvent.VK_Z;
		return b;
	}

	/**
	 * Process key events.
	 * @param e
	 */
	protected void doProcess(KeyEvent e) {

		if (null != external) {
			// Delegate to external behaviours
			for (InteractiveBehavior b : external) {
				b.doProcess(e);
				if (e.isConsumed()) return;
			}
		}

		int id = e.getID();
		int code = e.getKeyCode();

		boolean consumed = true;
		try {

		/*
		 * Forward keyReleased to the canvas, which keeps
		 * track of pressed keys.
		 */
		if(id == KeyEvent.KEY_RELEASED) {
			canvas.keyReleased(e);
			if(!isXYZKey(e))
				consumed = false;
			return;
		}

		if(id == KeyEvent.KEY_TYPED)
			return;

		/*
		 * Forward keyReleased to the canvas, which keeps
		 * track of pressed keys.
		 */
		canvas.keyPressed(e);
		if(!isXYZKey(e))
			consumed = false;
		else
			return;

		/*
		 * Handle escape key, which switches between the
		 * HAND tool and the last used tool.
		 */
		if (code == KeyEvent.VK_ESCAPE) {
			if(((Image3DUniverse)univ).isFullScreen())
				((Image3DUniverse)univ).setFullScreen(false);
			else if (univ.ui.isHandTool())
				univ.ui.setTool(lastToolID);
			else {
				lastToolID = univ.ui.getToolId();
				univ.ui.setHandTool();
			}
			return; // consumed
		}

		Content c = univ.getSelected();
		int axis = -1;
		if(canvas.isKeyDown(KeyEvent.VK_X))
			axis = VolumeRenderer.X_AXIS;
		else if(canvas.isKeyDown(KeyEvent.VK_Y))
			axis = VolumeRenderer.Y_AXIS;
		else if(canvas.isKeyDown(KeyEvent.VK_Z))
			axis = VolumeRenderer.Z_AXIS;
		// Consume events if used, to avoid other listeners from reusing the event
		if(e.isShiftDown()) {
			if(c != null && !c.isLocked())
				contentTransformer.init(c, 0, 0);
			switch(code) {
				case KeyEvent.VK_RIGHT:
					if(c != null && !c.isLocked())
						contentTransformer.translate(2, 0);
					else
						viewTransformer.translateXY(2, 0);
					return;
				case KeyEvent.VK_LEFT:
					if(c != null && !c.isLocked())
						contentTransformer.translate(-2, 0);
					else
						viewTransformer.translateXY(-2, 0);
					return;
				case KeyEvent.VK_UP:
					if(c != null && !c.isLocked())
						contentTransformer.translate(0, -2);
					else
						viewTransformer.translateXY(0, -2);
					return;
				case KeyEvent.VK_DOWN:
					if(c != null && !c.isLocked())
						contentTransformer.translate(0, 2);
					else
						viewTransformer.translateXY(0, 2);
					return;
			}
		} else if(e.isAltDown()) {
			switch(code) {
				case KeyEvent.VK_UP: viewTransformer.zoom(1); return;
				case KeyEvent.VK_DOWN: viewTransformer.zoom(-1); return;
			}
		} else if(c != null && c.getType() == Content.ORTHO && axis != -1) {
			boolean changed = false;
			for(ContentInstant ci : c.getInstants().values()) {
				OrthoGroup og = (OrthoGroup)ci.getContent();
				switch(code) {
					case KeyEvent.VK_RIGHT:
					case KeyEvent.VK_UP:
						og.increase(axis);
						changed = true;
						break;
					case KeyEvent.VK_LEFT:
					case KeyEvent.VK_DOWN:
						og.decrease(axis);
						changed = true;
						break;
					case KeyEvent.VK_SPACE:
						og.setVisible(axis, !og.isVisible(axis));
						changed = true;
						break;
				}
			}
			if(changed)
				univ.fireContentChanged(c);
		} else {
			if(c != null && !c.isLocked())
				contentTransformer.init(c, 0, 0);
			switch(code) {
				case KeyEvent.VK_RIGHT:
					if(c != null && !c.isLocked())
						contentTransformer.rotate(5, 0);
					else
						viewTransformer.rotateY(-TWO_RAD);
					return;
				case KeyEvent.VK_LEFT:
					if(c != null && !c.isLocked())
						contentTransformer.rotate(-5, 0);
					else
						viewTransformer.rotateY(TWO_RAD);
					return;
				case KeyEvent.VK_UP:
					if(c != null && !c.isLocked())
						contentTransformer.rotate(0, -5);
					else
						viewTransformer.rotateX(TWO_RAD);
					return;
				case KeyEvent.VK_DOWN:
					if(c != null && !c.isLocked())
						contentTransformer.rotate(0, 5);
					else
						viewTransformer.rotateX(-TWO_RAD);
					return;
				case KeyEvent.VK_PAGE_UP:
					viewTransformer.zoom(1); return;
				case KeyEvent.VK_PAGE_DOWN:
					viewTransformer.zoom(-1); return;

			}
		}
		// If we arrive here, the event was not handled.
		// We give it to ImageJ
		consumed = false;
		} finally {
			// executed when returning anywhere above,
			// since then consumed is not set to false
			if (consumed)
				e.consume();
			if(!e.isConsumed() && IJ.getInstance() != null)
				if(code == KeyEvent.VK_L || code == KeyEvent.VK_ENTER)
					IJ.getInstance().keyPressed(e);
		}
	}

	/**
	 * Process mouse events.
	 * @param e
	 */
	protected void doProcess(MouseEvent e) {

		if (null != external) {
			// Delegate to external behaviours
			for (InteractiveBehavior b : external) {
				b.doProcess(e);
				if (e.isConsumed()) return;
			}
		}

		int id = e.getID();
		int mask = e.getModifiersEx();
		Content c = univ.getSelected();
		if(id == MouseEvent.MOUSE_PRESSED) {
			if(c != null && !c.isLocked()) contentTransformer.init(c, e.getX(), e.getY());
			else viewTransformer.init(e);
			if(univ.ui.isPointTool()) {
				Content sel = c;
				if(sel == null && ((Image3DUniverse)univ).getContents().size() == 1)
					sel = (Content)univ.contents().next();
				if(sel != null) {
					sel.showPointList(true);
					e.consume();
				} if(mask == PICK_POINT_MASK) {
					picker.addPoint(sel, e);
					e.consume();
				} else if((mask & DELETE_POINT_MASK) == DELETE_POINT_MASK) {
					picker.deletePoint(sel, e);
					e.consume();
				}
			}
			if(!e.isConsumed())
				canvas.getRoiCanvas().mousePressed(e);
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			if(shouldTranslate(mask)) {
				if(c != null && !c.isLocked()) contentTransformer.translate(e);
				else viewTransformer.translate(e);
				e.consume();
			} else if(shouldRotate(mask)) {
				if(c != null && !c.isLocked() && (MouseEvent.BUTTON1_DOWN_MASK == (mask & MouseEvent.BUTTON1_DOWN_MASK))) contentTransformer.rotate(e);
				else viewTransformer.rotate(e);
				e.consume();
			} else if(shouldZoom(mask)) {
				viewTransformer.zoom(e);
				e.consume();
			} else if(shouldMovePoint(mask)) {
				picker.movePoint(c, e);
				e.consume();
			}
			if(!e.isConsumed()) {
				canvas.getRoiCanvas().mouseDragged(e);
			}
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(univ.ui.isPointTool()) {
				picker.stopMoving();
				e.consume();
			}
			if(!e.isConsumed())
				canvas.getRoiCanvas().mouseReleased(e);
		}
		if(id == MouseEvent.MOUSE_WHEEL) {
			int axis = -1;
			if(canvas.isKeyDown(KeyEvent.VK_X))
				axis = VolumeRenderer.X_AXIS;
			else if(canvas.isKeyDown(KeyEvent.VK_Y))
				axis = VolumeRenderer.Y_AXIS;
			else if(canvas.isKeyDown(KeyEvent.VK_Z))
				axis = VolumeRenderer.Z_AXIS;
			if(c != null && c.getType() == Content.ORTHO
								&& axis != -1) {
				MouseWheelEvent we = (MouseWheelEvent)e;
				int units = 0;
				if(we.getScrollType() ==
					MouseWheelEvent.WHEEL_UNIT_SCROLL)
					units = we.getUnitsToScroll();
				for(ContentInstant ci : c.getInstants().values()) {
					OrthoGroup og = (OrthoGroup)ci.getContent();
					if(units > 0) og.increase(axis);
					else if(units < 0) og.decrease(axis);
				}
				univ.fireContentChanged(c);
			} else {
				viewTransformer.wheel_zoom(e);
			}
			e.consume();
		}
	}
}

