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

public abstract class AbstractTool implements ImageListener, MouseListener,
		 MouseMotionListener, PlugIn {
	Toolbar toolbar;
	int toolID = -1;

	public void run(String arg) {
		toolbar = Toolbar.getInstance();
		if (toolbar == null) {
			IJ.error("No toolbar found");
			return;
		}

		toolID = toolbar.addTool(getToolName() + " - "
			+ getToolIcon());
		if (toolID < 0) {
			IJ.error("Could not register tool");
			return;
		}
		toolbar.setTool(toolID);
		registerTool();
	}

	public abstract String getToolName();

	public abstract String getToolIcon();

	void registerTool() {
		int[] ids = WindowManager.getIDList();
		if (ids != null)
			for (int id : ids)
				registerTool(WindowManager.getImage(id));
		ImagePlus.addImageListener(this);
	}

	void registerTool(ImagePlus image) {
		if (image == null)
			return;
		registerTool(image.getCanvas());
	}

	void registerTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	void unregisterTool() {
		for (int id : WindowManager.getIDList())
			unregisterTool(WindowManager.getImage(id));
		ImagePlus.removeImageListener(this);
	}

	void unregisterTool(ImagePlus image) {
		if (image == null)
			return;
		unregisterTool(image.getCanvas());
	}

	void unregisterTool(ImageCanvas canvas) {
		if (canvas == null)
			return;
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
	}

	public void imageOpened(ImagePlus image) {
		registerTool(image);
	}

	public void imageClosed(ImagePlus image) {
		unregisterTool(image);
	}

	public void imageUpdated(ImagePlus image) { }

	protected void handleMouseClick(MouseEvent e) {}
	protected void handleMousePress(MouseEvent e) {}
	protected void handleMouseRelease(MouseEvent e) {}
	protected void handleMouseDrag(MouseEvent e) {}
	protected void handleMouseMove(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		if (Toolbar.getInstance() != toolbar) {
			unregisterTool();
			IJ.showStatus("unregistered " + getToolName() + " Tool");
			return;
		}
		if (toolbar.getToolId() != toolID)
			return;
		handleMousePress(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (toolbar.getToolId() == toolID)
			handleMouseRelease(e);
	}

	public void mouseDragged(MouseEvent e) {
		if (toolbar.getToolId() == toolID)
			handleMouseDrag(e);
	}

	public void mouseMoved(MouseEvent e) {
		if (toolbar.getToolId() == toolID)
			handleMouseMove(e);
	}

	public void mouseClicked(MouseEvent e) {
		if (toolbar.getToolId() == toolID)
			handleMouseClick(e);
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
}
