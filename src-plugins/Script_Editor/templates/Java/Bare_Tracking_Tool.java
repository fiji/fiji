import fiji.tool.AbstractTrackingTool;
import fiji.tool.ToolToggleListener;
import fiji.tool.ToolWithOptions;

import fiji.util.gui.GenericDialogPlus;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.Roi;
import ij.gui.ShapeRoi;

import ij.process.ImageProcessor;

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
public class Bare_Tracking_Tool extends AbstractTrackingTool
	// remove the interfaces your tool should not handle
	implements ToolToggleListener, ToolWithOptions {
	{
		// for debugging, all custom tools can be removed to make space for this one if necessary
		clearToolsIfNecessary = true;
	}

	@Override
	public Roi optimizeRoi(Roi roi, ImageProcessor ip) {
		Roi result = new ShapeRoi(roi);
		Roi[] rois = ((ShapeRoi)result).getRois();
		if (rois.length == 1)
			result = rois[0];
		result.setImage(WindowManager.getCurrentImage());
		result.nudge(KeyEvent.VK_RIGHT);
		return result;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		super.mouseClicked(e);
		IJ.log("mouse clicked: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		IJ.log("mouse pressed: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		IJ.log("mouse released: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		IJ.log("mouse entered: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
		IJ.log("mouse exited: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		super.mouseMoved(e);
		IJ.log("mouse moved: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		IJ.log("mouse dragged: " + getOffscreenX(e) + ", " + getOffscreenY(e));
		e.consume(); // prevent ImageJ from handling this event
	}

	@Override
	public void sliceChanged(ImagePlus image) {
		super.sliceChanged(image);
		IJ.log("slice changed to " + image.getCurrentSlice() + " in " + image.getTitle());
	}

	@Override
	public void showOptionDialog() {
		GenericDialogPlus gd = new GenericDialogPlus(getToolName() + " Options");
		gd.addMessage("Here could be your option dialog!");
		addIOButtons(gd);
		gd.showDialog();
	}

	@Override
	public void toolToggled(boolean enabled) {
		IJ.log(getToolName() + " was switched " + (enabled ? "on" : "off"));
	}
}