package ij3d.behaviors;

import java.util.Enumeration;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.AWTEvent;

import ij.gui.Toolbar;

import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;

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

	private DefaultUniverse univ;
	private ImageCanvas3D canvas;

	private WakeupOnAWTEvent[] mouseEvents;
	private WakeupCondition wakeupCriterion;

	private int toolID;

	private ContentTransformer contentTransformer;
	private Picker picker;
	private InteractiveViewPlatformTransformer viewTransformer;

	private static final int B1 = MouseEvent.BUTTON1_DOWN_MASK;
	private static final int B2 = MouseEvent.BUTTON2_DOWN_MASK;
	private static final int B3 = MouseEvent.BUTTON3_DOWN_MASK;

	private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	private static final int CTRL  = InputEvent.CTRL_DOWN_MASK;

	private static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
	private static final int DELETE_POINT_MASK = InputEvent.SHIFT_DOWN_MASK |
						MouseEvent.BUTTON1_DOWN_MASK;

	public static final double TWO_RAD = 2 * Math.PI / 180;


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
		toolID = Toolbar.getToolId();
		if(toolID != Toolbar.HAND && toolID != Toolbar.MAGNIFIER &&
				toolID != Toolbar.POINT) {
			wakeupOn (wakeupCriterion);
			return;
		}
		WakeupOnAWTEvent wakeup;
		AWTEvent[] events;
		while(criteria.hasMoreElements()) {
			wakeup = (WakeupOnAWTEvent)criteria.nextElement();
			events = (AWTEvent[])wakeup.getAWTEvent();
			for(AWTEvent evt : events) {
				if(evt instanceof MouseEvent)
					doProcess((MouseEvent)evt);
				if(evt instanceof KeyEvent)
					doProcess((KeyEvent)evt);
			}
		}
		wakeupOn(wakeupCriterion);
	}

	private boolean shouldRotate(int mask, int toolID) {
		int onmask = B2, onmask2 = B1;
		int offmask = SHIFT | CTRL;
		boolean b0 = (mask & (onmask | offmask)) == onmask;
		boolean b1 = (toolID == Toolbar.HAND
				&& (mask & (onmask2|offmask)) == onmask2);
		return b0 || b1;
	}

	private boolean shouldTranslate(int mask, int toolID) {
		int onmask = B2 | SHIFT, onmask2 = B1 | SHIFT;
		int offmask = CTRL;
		return (mask & (onmask | offmask)) == onmask ||
			(toolID == Toolbar.HAND
				&& (mask & (onmask2|offmask)) == onmask2);
	}

	private boolean shouldZoom(int mask, int toolID) {
		if(toolID != Toolbar.MAGNIFIER)
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	private boolean shouldMovePoint(int mask, int toolID) {
		if(toolID != Toolbar.POINT)
			return false;
		int onmask = B1;
		int offmask = SHIFT | CTRL;
		return (mask & (onmask | offmask)) == onmask;
	}

	/**
	 * Process key events.
	 * @param e
	 */
	protected void doProcess(KeyEvent e) {
		int id = e.getID();

		if(id == KeyEvent.KEY_RELEASED || id == KeyEvent.KEY_TYPED)
			return;

		Content c = univ.getSelected();
		int code = e.getKeyCode();
		int axis = -1;
		if(canvas.isKeyDown(KeyEvent.VK_X))
			axis = VolumeRenderer.X_AXIS;
		else if(canvas.isKeyDown(KeyEvent.VK_Y))
			axis = VolumeRenderer.Y_AXIS;
		else if(canvas.isKeyDown(KeyEvent.VK_Z))
			axis = VolumeRenderer.Z_AXIS;
		// Consume events if used, to avoid other listeners from reusing the event
		boolean consumed = true;
		try {
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
			OrthoGroup og = (OrthoGroup)c.getContent();
			switch(code) {
				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_UP:
					og.increase(axis);
					univ.fireContentChanged(c);
					return;
				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_DOWN:
					og.decrease(axis);
					univ.fireContentChanged(c);
					return;
				case KeyEvent.VK_SPACE:
					og.setVisible(axis, !og.isVisible(axis));
					univ.fireContentChanged(c);
					return;
			}
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
		// must be last line in try/catch block
		consumed = false;
		} finally {
			// executed when returning anywhere above,
			// since then consumed is not set to false
			if (consumed) e.consume();
		}
	}

	/**
	 * Process mouse events.
	 * @param e
	 */
	protected void doProcess(MouseEvent e) {
		int id = e.getID();
		int mask = e.getModifiersEx();
		Content c = univ.getSelected();
		if(id == MouseEvent.MOUSE_PRESSED) {
			if(c != null && !c.isLocked()) contentTransformer.init(c, e.getX(), e.getY());
			else viewTransformer.init(e);
			if(toolID == Toolbar.POINT) {
				if(c != null)
					c.showPointList(true);
				if(mask == PICK_POINT_MASK) {
					picker.addPoint(c, e);
				} else if(mask == DELETE_POINT_MASK) {
					picker.deletePoint(c, e);
				}
			}
		} else if(id == MouseEvent.MOUSE_DRAGGED) {
			if(shouldTranslate(mask, toolID)) {
				if(c != null && !c.isLocked()) contentTransformer.translate(e);
				else viewTransformer.translate(e);
			} else if(shouldRotate(mask, toolID)) {
				if(c != null && !c.isLocked()) contentTransformer.rotate(e);
				else viewTransformer.rotate(e);
			} else if(shouldZoom(mask, toolID))
				viewTransformer.zoom(e);
			else if(shouldMovePoint(mask, toolID))
				picker.movePoint(c, e);
		} else if(id == MouseEvent.MOUSE_RELEASED) {
			if(toolID == Toolbar.POINT) {
				picker.stopMoving();
			}
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
				OrthoGroup og = (OrthoGroup)c.getContent();
				MouseWheelEvent we = (MouseWheelEvent)e;
				int units = 0;
				if(we.getScrollType() ==
					MouseWheelEvent.WHEEL_UNIT_SCROLL)
					units = we.getUnitsToScroll();
				if(units > 0) og.increase(axis);
				else if(units < 0) og.decrease(axis);
				univ.fireContentChanged(c);

			} else {
				viewTransformer.wheel_zoom(e);
			}
		}
	}
}

