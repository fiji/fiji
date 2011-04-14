package fiji.tool;

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Toolbar;

import ij.plugin.PlugIn;

import java.awt.Component;

import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractTool implements ImageListener, WindowFocusListener, PlugIn {
	protected Toolbar toolbar;
	protected int toolID = -1;

	protected MouseProxy mouseProxy;
	protected MouseWheelProxy mouseWheelProxy;
	protected MouseMotionProxy mouseMotionProxy;
	protected KeyProxy keyProxy;
	protected KeyProxyIfNotConsumed ijKeyProxy;
	protected SliceListener sliceListener;
	protected List<SliceObserver> sliceObservers = new ArrayList<SliceObserver>();
	protected ToolbarMouseAdapter toolbarMouseListener;
	protected ToolToggleListener toolToggleListener;

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
	 * Remember what the current state of the tool is.
	 */
	protected boolean toolActive;

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
		if (this instanceof KeyListener) {
			keyProxy = new KeyProxy((KeyListener)this);
			ijKeyProxy = new KeyProxyIfNotConsumed(IJ.getInstance());
		}
		if (this instanceof SliceListener)
			sliceListener = (SliceListener)this;
		if (this instanceof ToolWithOptions)
			toolbarMouseListener = new ToolbarMouseAdapter((ToolWithOptions)this);
		if (this instanceof ToolToggleListener) {
			toolToggleListener = (ToolToggleListener)this;
			toolToggleListener.toolToggled(true);
			if (toolbarMouseListener == null)
				toolbarMouseListener = new ToolbarMouseAdapter(null);
		}

		toolActive = true;
		registerTool();
	}

	/**
	 * This class is used to monitor tool toggles and double-clicks on the toolbar icon of this concrete tool.
	 */
	protected class ToolbarMouseAdapter extends MouseAdapter {
		protected ToolWithOptions this2;

		public ToolbarMouseAdapter(ToolWithOptions tool) {
			this2 = tool;
		}

		public void mouseReleased(MouseEvent e) {
			if (maybeUnregister())
				return;
			if (isThisTool() && this2 != null && e.getClickCount() > 1)
				this2.showOptionDialog();
		}
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

	protected class MouseWheelProxyIfNotConsumed implements MouseWheelListener {
		protected MouseWheelListener listener;

		public MouseWheelProxyIfNotConsumed(MouseWheelListener listener) {
			this.listener = listener;
		}

		@Override
		public final void mouseWheelMoved(MouseWheelEvent e) {
			if (!e.isConsumed())
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

	protected class MouseMotionProxyIfNotConsumed implements MouseMotionListener {
		protected MouseMotionListener listener;

		public MouseMotionProxyIfNotConsumed(MouseMotionListener listener) {
			this.listener = listener;
		}

		@Override
		public final void mouseDragged(MouseEvent e) {
			if (!e.isConsumed())
				listener.mouseDragged(e);
		}

		@Override
		public final void mouseMoved(MouseEvent e) {
			if (!e.isConsumed())
				listener.mouseMoved(e);
		}
	}

	protected class KeyProxy implements KeyListener {
		protected KeyListener listener;

		public KeyProxy(KeyListener listener) {
			this.listener = listener;
		}

		@Override
		public final void keyPressed(KeyEvent e) {
			if (isThisTool())
				listener.keyPressed(e);
		}

		@Override
		public final void keyReleased(KeyEvent e) {
			if (isThisTool())
				listener.keyReleased(e);
		}

		@Override
		public final void keyTyped(KeyEvent e) {
			if (isThisTool())
				listener.keyTyped(e);
		}
	}

	protected class KeyProxyIfNotConsumed implements KeyListener {
		protected KeyListener listener;

		public KeyProxyIfNotConsumed(KeyListener listener) {
			this.listener = listener;
		}

		@Override
		public final void keyPressed(KeyEvent e) {
			if (!e.isConsumed())
				listener.keyPressed(e);
		}

		@Override
		public final void keyReleased(KeyEvent e) {
			if (!e.isConsumed())
				listener.keyReleased(e);
		}

		@Override
		public final void keyTyped(KeyEvent e) {
			if (!e.isConsumed())
				listener.keyTyped(e);
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

	@Override
	public void windowGainedFocus(WindowEvent e) {
		if (maybeUnregister())
			return;
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		if (maybeUnregister())
			return;
	}

	public final int getToolID() {
		return toolID;
	}

	public final boolean isThisTool() {
		boolean active = Toolbar.getToolId() == toolID;
		if (toolToggleListener != null && active != toolActive)
			toolToggleListener.toolToggled(active);
		toolActive = active;
		return active;
	}

	/*
	 * PROTECTED METHODS
	 */

	protected void registerTool() {
		int[] ids = WindowManager.getIDList();
		if (ids != null)
			for (int id : ids)
				registerTool(WindowManager.getImage(id));
		if (toolbarMouseListener != null)
			toolbar.addMouseListener(toolbarMouseListener);
		ImagePlus.addImageListener(this);
		IJ.getInstance().addWindowFocusListener(this);
	}

	protected void registerTool(ImagePlus image) {
		if (image == null)
			return;
		if (sliceListener != null)
			sliceObservers.add(new SliceObserver(image, new SliceListener() {
				public final void sliceChanged(ImagePlus image) {
					if (maybeUnregister())
						return;
					if (isThisTool())
						sliceListener.sliceChanged(image);
				}
			}));
		if (image.getCanvas() != null)
			registerTool(image.getCanvas());
		ImageWindow window = image.getWindow();
		if (window != null) {
			window.addWindowFocusListener(this);
			if (keyProxy != null)
				addKeyListener(window);
			addMouseWheelListener(window);
		}
	}

	protected void registerTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		if (mouseProxy != null)
			canvas.addMouseListener(mouseProxy);
		addMouseMotionListener(canvas);
		addKeyListener(canvas);
	}

	protected void addKeyListener(Component component) {
		if (keyProxy != null) {
			component.addKeyListener(keyProxy);
			// make sure that IJ gets only unconsumed key events
			KeyListener ij = null;
			for (KeyListener listener : component.getKeyListeners())
				if (listener == ijKeyProxy.listener ||
						listener.getClass().getName().endsWith("KeyProxyIfNotConsumed"))
					ij = listener;
			if (ij == null)
				ij = ijKeyProxy;
			else {
				component.removeKeyListener(ij);
				if (ij == ijKeyProxy.listener)
					ij = ijKeyProxy;
			}
			component.addKeyListener(ij);
		}
	}

	protected void addMouseMotionListener(ImageCanvas canvas) {
		if (mouseMotionProxy != null) {
			canvas.addMouseMotionListener(mouseMotionProxy);
			// make sure that IJ gets only unconsumed key events
			MouseMotionListener listener = null;
			for (MouseMotionListener listener2 : canvas.getMouseMotionListeners())
				if (listener2 == canvas ||
						listener2.getClass().getName().endsWith("MouseMotionProxyIfNotConsumed"))
					listener = listener2;
			if (listener == null)
				listener = new MouseMotionProxyIfNotConsumed(canvas);
			else {
				canvas.removeMouseMotionListener(listener);
				if (listener == canvas)
					listener = new MouseMotionProxyIfNotConsumed(canvas);
			}
			canvas.addMouseMotionListener(listener);
		}
	}

	protected void addMouseWheelListener(ImageWindow window) {
		if (mouseWheelProxy != null) {
			window.addMouseWheelListener(mouseWheelProxy);
			// make sure that IJ gets only unconsumed key events
			MouseWheelListener listener = null;
			for (MouseWheelListener listener2 : window.getMouseWheelListeners())
				if (listener2 == window ||
						listener2.getClass().getName().endsWith("MouseWheelProxyIfNotConsumed"))
					listener = listener2;
			if (listener == null)
				listener = new MouseWheelProxyIfNotConsumed(window);
			else {
				window.removeMouseWheelListener(listener);
				if (listener == window)
					listener = new MouseWheelProxyIfNotConsumed(window);
			}
			window.addMouseWheelListener(listener);
		}
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
		if (toolToggleListener != null && toolActive) {
			toolToggleListener.toolToggled(false);
			toolActive = false;
		}
		for (int id : WindowManager.getIDList())
			unregisterTool(WindowManager.getImage(id));
		ImagePlus.removeImageListener(this);
		for (SliceObserver observer : sliceObservers)
			observer.unregister();
		sliceObservers.clear();
		if (toolbarMouseListener != null)
			toolbar.removeMouseListener(toolbarMouseListener);
		IJ.getInstance().removeWindowFocusListener(this);
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
		if (image.getCanvas() != null)
			unregisterTool(image.getCanvas());
		if (image.getWindow() != null) {
			image.getWindow().removeWindowFocusListener(this);
			if (keyProxy != null)
				image.getWindow().removeKeyListener(keyProxy);
		}
	}

	protected void unregisterTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		if (mouseProxy != null)
			canvas.removeMouseListener(mouseProxy);
		if (mouseMotionProxy != null)
			// we leave the ijMouseMotionProxy in because it might be required by another active custom tool
			canvas.removeMouseMotionListener(mouseMotionProxy);
		if (mouseWheelProxy != null)
			canvas.removeMouseWheelListener(mouseWheelProxy);
		if (keyProxy != null)
			// we leave the ijKeyProxy in because it might be required by another active custom tool
			canvas.removeKeyListener(keyProxy);
	}

	/* convenience methods */

	public ImagePlus getImagePlus(ComponentEvent e) {
		ImageCanvas canvas = getImageCanvas(e);
		return canvas == null ? null : canvas.getImage();
	}

	public ImageCanvas getImageCanvas(ComponentEvent e) {
		Component component = e.getComponent();
		return component instanceof ImageCanvas ? (ImageCanvas)component :
			(component instanceof ImageWindow ? ((ImageWindow)component).getCanvas() : null);
	}

	public ImageWindow getImageWindow(ComponentEvent e) {
		for (Component component = e.getComponent(); component != null; component = component.getParent())
			if (component instanceof ImageWindow)
				return (ImageWindow)component;
		return null;
	}

	public int getOffscreenX(MouseEvent e) {
		ImageCanvas canvas = getImageCanvas(e);
		return canvas == null ? -1 : canvas.offScreenX(e.getX());
	}

	public int getOffscreenY(MouseEvent e) {
		ImageCanvas canvas = getImageCanvas(e);
		return canvas == null ? -1 : canvas.offScreenY(e.getY());
	}

	public double getOffscreenXDouble(MouseEvent e) {
		ImageCanvas canvas = getImageCanvas(e);
		return canvas == null ? -1 : canvas.offScreenXD(e.getX());
	}

	public double getOffscreenYDouble(MouseEvent e) {
		ImageCanvas canvas = getImageCanvas(e);
		return canvas == null ? -1 : canvas.offScreenXD(e.getY());
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