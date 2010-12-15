package fiji.util;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.Toolbar;

import ij.plugin.PlugIn;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractTool implements ImageListener, PlugIn {
	protected Toolbar toolbar;
	protected int toolID = -1;

	protected MouseProxy mouseProxy;
	protected MouseWheelProxy mouseWheelProxy;
	protected MouseMotionProxy mouseMotionProxy;
	protected SliceListener sliceListener;
	protected List<SliceObserver> sliceObservers = new ArrayList<SliceObserver>();

	/*
	 * There is currently no way to let the tool know that the toolbar decided to clear the custom tools.
	 * For this reason, we save the tool name and compare (with == instead of equals()!) later to know.
	 */
	protected String savedToolName;

	/*
	 * If there is no space left, or if the tool was registered already, the only way to register the
	 * tool is by blowing away all other custom tools. For debugging purposes, you can set this flag
	 * to "true" to allow this.
	 */
	protected boolean clearToolsIfNecessary;

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public void run(String arg) {
		toolbar = Toolbar.getInstance();
		if (toolbar == null) {
			IJ.error("No toolbar found");
			return;
		}

		boolean clearTools = false;
		if (toolbar.getToolId(getToolName()) >= 0) {
			if (clearToolsIfNecessary)
				clearTools = true;
			else {
				IJ.error("Tool '" + getToolName() + "' already present!");
				return;
			}
		}

		toolID = -1;
		if (!clearTools)
			toolID = toolbar.addTool(getToolName() + " - " + getToolIcon());
		if (toolID < 0 && clearToolsIfNecessary) {
			int previousID = toolbar.getToolId();
			toolbar.addMacroTool(getToolName() + " - " + getToolIcon(), null, 0);
			toolID = toolbar.getToolId(getToolName());
			if (previousID == toolID)
				toolbar.repaint();
		}
		if (toolID < 0) {
			IJ.error("Could not register tool");
			return;
		}
		toolbar.setTool(toolID);
		if (toolbar.getToolId() != toolID) {
			IJ.error("Could not set tool (id = " + toolID + ")");
			return;
		}
		savedToolName = Toolbar.getToolName();

		if (this instanceof MouseListener)
			mouseProxy = new MouseProxy((MouseListener)this);
		if (this instanceof MouseMotionListener)
			mouseMotionProxy = new MouseMotionProxy((MouseMotionListener)this);
		if (this instanceof MouseWheelListener)
			mouseWheelProxy = new MouseWheelProxy((MouseWheelListener)this);
		if (this instanceof SliceListener)
			sliceListener = (SliceListener)this;

		registerTool();
	}

	protected class MouseProxy implements MouseListener {
		protected MouseListener listener;

		public MouseProxy(MouseListener listener) {
			this.listener = listener;
		}

		@Override
		public final void mousePressed(MouseEvent e) {
			if (maybeUnregister())
				return;
			if (isThisTool())
				listener.mousePressed(e);
		}

		@Override
		public final void mouseReleased(MouseEvent e) {
			if (isThisTool())
				listener.mouseReleased(e);
		}

		@Override
		public final void mouseClicked(MouseEvent e) {
			if (isThisTool())
				listener.mouseClicked(e);
		}

		@Override
		public final void mouseEntered(MouseEvent e) {
			if (maybeUnregister())
				return;
			if (isThisTool())
				listener.mouseEntered(e);
		}

		@Override
		public final void mouseExited(MouseEvent e) {
			if (maybeUnregister())
				return;
			if (isThisTool())
				listener.mouseExited(e);
		}
	}

	protected class MouseWheelProxy implements MouseWheelListener {
		protected MouseWheelListener listener;

		public MouseWheelProxy(MouseWheelListener listener) {
			this.listener = listener;
		}

		@Override
		public final void mouseWheelMoved(MouseWheelEvent e) {
			if (isThisTool())
				listener.mouseWheelMoved(e);
		}
	}

	protected class MouseMotionProxy implements MouseMotionListener {
		protected MouseMotionListener listener;

		public MouseMotionProxy(MouseMotionListener listener) {
			this.listener = listener;
		}

		@Override
		public final void mouseDragged(MouseEvent e) {
			if (isThisTool())
				listener.mouseDragged(e);
		}

		@Override
		public final void mouseMoved(MouseEvent e) {
			if (isThisTool())
				listener.mouseMoved(e);
		}
	}

	@Override
	public void imageOpened(ImagePlus image) {
		registerTool(image);
	}

	@Override
	public void imageClosed(ImagePlus image) {
		unregisterTool(image);
	}

	@Override
	public void imageUpdated(ImagePlus image) {
		if (maybeUnregister())
			return;
	}

	public final boolean isThisTool() {
		return Toolbar.getToolId() == toolID;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void registerTool() {
		int[] ids = WindowManager.getIDList();
		if (ids != null)
			for (int id : ids)
				registerTool(WindowManager.getImage(id));
		ImagePlus.addImageListener(this);
	}

	protected void registerTool(ImagePlus image) {
		if (image == null)
			return;
		if (sliceListener != null)
			sliceObservers.add(new SliceObserver(image, new SliceListener() {
				public final void sliceChanged(ImagePlus image) {
					if (isThisTool())
						sliceListener.sliceChanged(image);
				}
			}));
		registerTool(image.getCanvas());
	}

	protected void registerTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		if (mouseProxy != null)
			canvas.addMouseListener(mouseProxy);
		if (mouseMotionProxy != null)
			canvas.addMouseMotionListener(mouseMotionProxy);
		if (mouseWheelProxy != null)
			canvas.addMouseWheelListener(mouseWheelProxy);
	}

	protected boolean maybeUnregister() {
		if (!wasToolbarCleared())
			return false;
		unregisterTool();
		IJ.showStatus("unregistered " + getToolName() + " Tool");
		return true;
	}

	protected boolean wasToolbarCleared() {
		Toolbar current = Toolbar.getInstance();
		if (current != toolbar)
			return true;
		/*
		 * We need to compare with != rather than !equals() so that subsequent calls
		 * of the same plugin will not result in multiple handling.
		 */
		if (Toolbar.getToolId() == toolID && Toolbar.getToolName() != savedToolName)
			return true;
		return false;
	}

	protected void unregisterTool() {
		for (int id : WindowManager.getIDList())
			unregisterTool(WindowManager.getImage(id));
		ImagePlus.removeImageListener(this);
		for (SliceObserver observer : sliceObservers)
			observer.unregister();
		sliceObservers.clear();
	}

	protected void unregisterTool(ImagePlus image) {
		if (image == null)
			return;
		for (Iterator<SliceObserver> iter = sliceObservers.iterator(); iter.hasNext(); ) {
			SliceObserver observer = iter.next();
			if (observer.getImagePlus() != image)
				continue;
			observer.unregister();
			iter.remove();
		}
		unregisterTool(image.getCanvas());
	}

	protected void unregisterTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		if (mouseProxy != null)
			canvas.removeMouseListener(mouseProxy);
		if (mouseMotionProxy != null)
			canvas.removeMouseMotionListener(mouseMotionProxy);
		if (mouseWheelProxy != null)
			canvas.removeMouseWheelListener(mouseWheelProxy);
	}

	/*
	 * METHODS TO OVERRIDE
	 */

	/**
	 * Return the tool name.
	 */
	public String getToolName() {
		return getClass().getName().replace('_', ' ');
	}

	/**
	 * Return the string encoding of the tool icon as it will appear in the
	 * toolbar. See <a href="http://rsb.info.nih.gov/ij/developer/macro/macros.html#icons">syntax</a>
	 * for icon string.
	 */
	public String getToolIcon() {
		// default: "New Tool"
		return "C00aT0509NT5509eT9509wT0e09TT3e09oT8e09oTde09l";
	}
}