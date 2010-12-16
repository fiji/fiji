package fiji.util;

import ij.gui.Toolbar;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public abstract class AbstractToolWithOptions extends AbstractTool {
	/**
	 * This class is used to monitor double-clicks on the toolbar icon of this concrete tool.
	 */
	protected class ToolbarMouseAdapter extends MouseAdapter {
		public void mouseReleased(MouseEvent e) {
			if (isThisTool() && e.getClickCount() > 1)
				showOptionDialog();
		}
	}

	protected ToolbarMouseAdapter toolbarMouseListener;

	/*
	 * PROTECTED METHODS
	 */

	@Override
	protected void registerTool() {
		super.registerTool();
		if (toolbarMouseListener == null)
			toolbarMouseListener = new ToolbarMouseAdapter();
		toolbar.addMouseListener(toolbarMouseListener);
	}

	@Override
	protected void unregisterTool() {
		if (toolbarMouseListener != null)
			toolbar.removeMouseListener(toolbarMouseListener);
	}

	/*
	 * ABSTRACT METHODS
	 */

	/**
	 * When called, this method displays the configuration panel for the concrete
	 * implementation of this tool. It is normally called when the user double-click
	 * the toolbar icon of this tool. If this tool does not have an option panel,
	 * this method does nothing.
	 */
	public abstract void showOptionDialog();
}