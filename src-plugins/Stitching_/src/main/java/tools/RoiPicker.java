
package tools;

import fiji.tool.AbstractTool;
import fiji.tool.ToolWithOptions;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JList;

/**
 * This tool is used to select the {@link Roi}s containing a specific x, y
 * position (obtained by a mouse click). Any Rois in the current
 * {@link RoiManager} containing that point will be added to this tool's list.
 * The Rois of the list can be itereated by repeated clicking.
 * <p>
 * NB: Known limitations to this tool:
 * <ul>
 * <li>When displaying a union of all matching Rois, only the first will be
 * selected in the RoiManager.</li>
 * <li>When a Roi is selected in the RoiManager, the list itself will not
 * automatically update to display that Roi.</li>
 * <li>Works best with "Show all" Rois off.
 * </ul>
 * </p>
 * 
 * @author Mark Hinier
 * @version 1.0 - 30 Aug 2013
 */
public class RoiPicker extends AbstractTool implements ActionListener,
	MouseListener, MouseMotionListener, MouseWheelListener, ToolWithOptions
{

	private ImagePlus imp;
	private ImageCanvas canvas;

	// Cached set of matched roi indices. Used for quick comparison
	private Set<Integer> roiSet;
	// Cached list of matched roi indices. Used for ordering iteration.
	private List<Integer> roiIndices;
	// index of the next roi to select
	int roiIndex;

	/*
	 * RUN METHOD
	 */

	public void run(String arg) {
		super.run(arg);
	}

	/*
	 * PUBLIC METHODS
	 */

	@Override
	public String getToolName() {
		return "Roi Picker";
	}

	@Override
	public String getToolIcon() {
		return "Cee0P31f1ff3f31PC000P01785abc9678P";
	}

	/**
	 * Functional method of the RoiPicker. Determines which Rois match the x,y
	 * coordinates of the received MouseEvent and the C,Z,T coordinates of the
	 * active image. The user can cycle through these Rois with repeated clicks.
	 */
	@Override
	public void mouseReleased(MouseEvent e) {
		ImageCanvas source = (ImageCanvas) e.getSource();
		if (source != canvas) {
			// We changed image window. Update fields accordingly
			ImageWindow window = (ImageWindow) source.getParent();
			imp = window.getImagePlus();
			canvas = source;
		}
		// Convert global coords to local
		double x = canvas.offScreenXD(e.getX());
		double y = canvas.offScreenYD(e.getY());
		// Get the RoiManager
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) return;

		Roi[] rois = rm.getRoisAsArray();
		// Get the active ImagePlus's current z,c,t coords
		int[] position = imp.convertIndexToPosition(imp.getCurrentSlice());

		Set<Integer> matchedSet = new HashSet<Integer>();
		List<Integer> matchedIndices = new ArrayList<Integer>();

		// generate list of all rois containing x,y and matching the ImagePlus's
		// position
		for (int i = 0; i < rois.length; i++) {
			Roi r = rois[i];
			// Check position
			if (containsPoint(r, position, x, y)) {
				// Matched. Add to the matched set and list
				Integer index = i;
				matchedSet.add(index);
				matchedIndices.add(index);
			}
		}

		// If we discovered the currently known roi set, display the next roi in
		// the series
		if (same(roiSet, matchedSet)) {
			incrementIndex();
		}
		else {
			// otherwise, update the cached indices and display the union of the rois
			roiSet = matchedSet;
			roiIndices = matchedIndices;
			roiIndex = roiIndices.size();
		}

		// Perform the roi selection
		if (matchedIndices.size() > 0) selectRois(rm);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
//		if (!roiIndices.isEmpty()) {
//			RoiManager rm = RoiManager.getInstance();
//			if (rm == null) return;
//			incrementIndex();
//			selectRois(rm);
//		}
	}

	@Override
	public void showOptionDialog() {}

	@Override
	public void mouseDragged(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void actionPerformed(ActionEvent e) {}

	protected void handleRecording() {}

	// for macros
	public static void select(String style, String x1, String y1, String x2,
		String y2, String width, String headLength)
	{}

	/*
	 * PRIVATE METHODS
	 */

	/**
	 * Returns true if the two provided sets are equivalent.
	 */
	private boolean same(Set<?> set1, Set<?> set2) {
		if (set1 == null || set2 == null) return set1 == set2;
		if (set1.size() != set2.size()) return false;

		for (Object r : set1) {
			if (!set2.contains(r)) return false;
		}

		return true;
	}

	/**
	 * Returns true iff the specified Roi contains the desired x and y positions,
	 * and is located at the given position (C, Z, T).
	 */
	private boolean containsPoint(Roi r, int[] position, double x, double y) {
		return r.getCPosition() == position[0] && r.getZPosition() == position[1] &&
			r.getTPosition() == position[2] &&
			r.contains((int) Math.floor(x), (int) Math.floor(y));
	}

	/**
	 * Selects the {@link #roiIndex} Roi from the list of matched
	 * {@link #roiIndices}, within the specified RoiManager. Cycles through each
	 * roi in the list, starting with a group select of all matching rois.
	 */
	private void selectRois(RoiManager rm) {
		if (roiIndices.size() == 1) {
			rm.select(roiIndices.get(0));
			IJ.showStatus("1 of 1 Rois selected.");
		}
		else if (roiIndex < roiIndices.size()) {
			rm.select(roiIndices.get(roiIndex));
			IJ.showStatus((roiIndex + 1) + " of " + roiIndices.size() +
				" Rois selected. Click to cycle...");
		}
		else {
			rm.select(roiIndices.get(0));
			for (int i = 1; i < roiIndices.size(); i++) {
				rm.select(roiIndices.get(i), true, false);
			}
			IJ.showStatus(roiIndices.size() + " overlapping Rois. Click to cycle...");
		}

		// Updates the displayed list of Rois to ensure the currently selected Roi
		// is visible.
		try {
			Field listField = RoiManager.class.getDeclaredField("list");
			listField.setAccessible(true);
			int scrollIndex = roiIndex == roiIndices.size() ? 0 : roiIndex;
			final JList list = (JList) listField.get(rm);
			list.ensureIndexIsVisible(roiIndices.get(scrollIndex));
		}
		catch (SecurityException e) {
			IJ.error("RoiPicker: SecurityException when updating RoiManager.");
		}
		catch (NoSuchFieldException e) {
			IJ.error("RoiPicker: NoSuchFieldException when updating RoiManager.");
		}
		catch (IllegalArgumentException e) {
			IJ.error("RoiPicker: IllegalArgumentException when updating RoiManager.");
		}
		catch (IllegalAccessException e) {
			IJ.error("RoiPicker: IllegalAccessException when updating RoiManager.");
		}

	}

	/**
	 * Bumps the {@link #roiIndex}, or resets to 0 if == {@link #roiIndices} size
	 * + 1. The extra index indicates multi-roi selection.
	 */
	private void incrementIndex() {
		roiIndex = (roiIndex + 1) % (roiIndices.size() + 1);
	}

}
