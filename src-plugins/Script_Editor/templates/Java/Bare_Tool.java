import fiji.tool.AbstractTool;
import fiji.tool.SliceListener;
import fiji.tool.ToolToggleListener;
import fiji.tool.ToolWithOptions;

import ij.IJ;
import ij.ImagePlus;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * This is a template for a generic tool using Fiji's AbstractTool infrastructure.
 */
public class Bare_Tool extends AbstractTool
	// remove the interfaces your tool should not handle
	implements KeyListener,
		MouseListener, MouseMotionListener, MouseWheelListener,
		SliceListener, ToolToggleListener, ToolWithOptions {
	{
		// for debugging, all custom tools can be removed to make space for this one if necessary
		// clearToolsIfNecessary = true;
	}

	// The methods defined by the interfaces implemented by this tool
	@Override
	public void keyPressed(KeyEvent e) {
		IJ.log("key pressed: " + e);
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void keyReleased(KeyEvent e) {
		IJ.log("key released: " + e);
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void keyTyped(KeyEvent e) {
		IJ.log("key typed: " + e);
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		IJ.log("mouse clicked: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mousePressed(MouseEvent e) {
		IJ.log("mouse pressed: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		IJ.log("mouse released: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		IJ.log("mouse entered: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		IJ.log("mouse exited: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		IJ.log("mouse moved: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		IJ.log("mouse dragged: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public final void mouseWheelMoved(MouseWheelEvent e) {
		IJ.log("mouse wheel moved: " + e.getWheelRotation() + " at " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void sliceChanged(ImagePlus image) {
		IJ.log("slice changed to " + image.getCurrentSlice() + " in " + image.getTitle());
	}

	@Override
	public void showOptionDialog() {
		IJ.showMessage("Here could be your option dialog!");
	}

	@Override
	public void toolToggled(boolean enabled) {
		IJ.log(getToolName() + " was switched " + (enabled ? "on" : "off"));
	}
}