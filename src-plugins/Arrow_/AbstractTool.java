import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.ImageCanvas;
import ij.gui.Toolbar;

import ij.plugin.PlugIn;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class AbstractTool implements ImageListener, MouseListener,
		 MouseMotionListener, PlugIn {
	
	Toolbar toolbar;
	int toolID = -1;

	
	/*
	 * INNER CLASS
	 */
	
	/**
	 * This class is used to monitor double-clicks on the toolbar icon of this concrete tool.
	 * It only registers has a MouseListener if the concrete implementation returns true on 
	 * {@link AbstractTool#hasOptionDialog()}.
	 */
	protected class ToolbarMouseAdapter extends MouseAdapter {
		
		private static final long DOUBLE_CLICK_TRESHOLD = 200; // ms
		long latest_time_release = -1;
		
		public void mouseReleased(MouseEvent e) {
			if (toolID != Toolbar.getToolId()) { return; }
			final long current_time = System.currentTimeMillis();
			final long delay = current_time-latest_time_release; 
			if (delay<DOUBLE_CLICK_TRESHOLD) { 	showOptionDialog(); }
			latest_time_release = current_time;
		}
	}
	
	
	
	/*
	 * PUBLIC METHODS
	 */
	
	
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
		if ( this.hasOptionDialog()) { toolbar.addMouseListener(new ToolbarMouseAdapter()); }
	}

	public void mousePressed(MouseEvent e) {
		if (Toolbar.getInstance() != toolbar) {
			unregisterTool();
			IJ.showStatus("unregistered " + getToolName() + " Tool");
			return;
		}
		if (Toolbar.getToolId() != toolID)
			return;
		handleMousePress(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseRelease(e);
	}

	public void mouseDragged(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseDrag(e);
	}

	public void mouseMoved(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseMove(e);
	}

	public void mouseClicked(MouseEvent e) {
		if (Toolbar.getToolId() == toolID)
			handleMouseClick(e);
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void imageOpened(ImagePlus image) {
		registerTool(image);
	}

	public void imageClosed(ImagePlus image) {
		unregisterTool(image);
	}

	public void imageUpdated(ImagePlus image) { }
	
	/*
	 * DEFAULT VISIBILITY METHODS
	 */

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


	/*
	 * PROTECTED METHODS
	 */

	protected void handleMouseClick(MouseEvent e) {}
	protected void handleMousePress(MouseEvent e) {}
	protected void handleMouseRelease(MouseEvent e) {}
	protected void handleMouseDrag(MouseEvent e) {}
	protected void handleMouseMove(MouseEvent e) {}

	
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
	
	/*
	 * DEAL WITH TOOLBAR
	 */
	
	/**
	 * This methods return true if the concrete implementation of this abstract tool 
	 * has an option dialog that pops up when the user double-click the toolbar icon.
	 * @see {@link #showOptionDialog()}
	 */
	public abstract boolean hasOptionDialog();
	
	/**
	 * When called, this method displays the configuration panel for the concrete 
	 * implementation of this tool. It is normally called when the user double-click
	 * the toolbar icon of this tool. If this tool does not have an option panel,
	 * this method does nothing.
	 */
	public abstract void showOptionDialog();
}
