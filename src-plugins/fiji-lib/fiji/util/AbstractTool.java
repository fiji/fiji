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

public abstract class AbstractTool implements ImageListener, MouseListener, MouseWheelListener, MouseMotionListener, PlugIn {
	protected Toolbar toolbar;
	protected int toolID = -1;

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

		toolID = toolbar.addTool(getToolName() + " - "	+ getToolIcon());
		if (toolID < 0) {
			toolID = toolbar.getToolId(getToolName());
			if (toolID < 0) {
				IJ.error("Could not register tool");
				return;
			}
		}
		toolbar.setTool(toolID);
		if (toolbar.getToolId() != toolID) {
			IJ.error("Could not set tool (id = " + toolID + ")");
			return;
		}
		registerTool();
	}

	@Override
	public final void mousePressed(MouseEvent e) {
		if (Toolbar.getInstance() != toolbar) {
			unregisterTool();
			IJ.showStatus("unregistered " + getToolName() + " Tool");
			return;
		}
		if (Toolbar.getToolId() != toolID)
			return;
		handleMousePress(e);
	}

	@Override
	public final void mouseReleased(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseRelease(e);
	}

	@Override
	public final void mouseDragged(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseDrag(e);
	}

	@Override
	public final void mouseMoved(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseMove(e);
	}

	@Override
	public final void mouseClicked(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseClick(e);
	}

	@Override
	public final void mouseWheelMoved(MouseWheelEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseWheelMove(e);
	}

	@Override
	public final void mouseEntered(MouseEvent e) {}
	@Override
	public final void mouseExited(MouseEvent e) {}

	@Override
	public void imageOpened(ImagePlus image) {
		registerTool(image);
	}

	@Override
	public void imageClosed(ImagePlus image) {
		unregisterTool(image);
	}

	@Override
	public void imageUpdated(ImagePlus image) { }

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
		registerTool(image.getCanvas());
	}

	protected void registerTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addMouseWheelListener(this);
	}

	protected void unregisterTool() {
		for (int id : WindowManager.getIDList())
			unregisterTool(WindowManager.getImage(id));
		ImagePlus.removeImageListener(this);
	}

	protected void unregisterTool(ImagePlus image) {
		if (image == null)
			return;
		unregisterTool(image.getCanvas());
	}

	protected void unregisterTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		canvas.getParent().removeMouseWheelListener(this);
	}

	protected void handleMouseClick(MouseEvent e) {}
	protected void handleMousePress(MouseEvent e) {}
	protected void handleMouseRelease(MouseEvent e) {}
	protected void handleMouseDrag(MouseEvent e) {}
	protected void handleMouseMove(MouseEvent e) {}
	protected void handleMouseWheelMove(MouseWheelEvent e) {}

	/*
	 * ABSTRACT METHODS
	 */

	/**
	 * Return the tool name.
	 */
	public abstract String getToolName();

	/**
	 * Return the string encoding of the tool icon as it will appear in the
	 * toolbar. See <a href="http://rsb.info.nih.gov/ij/developer/macro/macros.html#icons">syntax</a>
	 * for icon string.
	 */
	public abstract String getToolIcon();
}