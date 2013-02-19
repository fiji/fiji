package fiji.tool;

public interface ToolWithOptions {
	/**
	 * When called, this method displays the configuration panel for the concrete
	 * implementation of this tool. It is normally called when the user double-click
	 * the toolbar icon of this tool.
	 */
	public void showOptionDialog();
}