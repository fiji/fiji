/** Albet Cardona 2010-03-18 at EMBL
 *  Released under the General Public License in its latest version. */
package ij3d.segmentation;

import ij3d.Image3DUniverse;
import ij3d.Content;
import ij.gui.Toolbar;
import ij.ImagePlus;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import ij.plugin.PlugIn;
import ij.IJ;
import levelsets.ij.LevelSet;
import ij.measure.Calibration;
import javax.vecmath.Point3d;
import javax.vecmath.Color3f;
import customnode.CustomTriangleMesh;
import marchingcubes.MCTriangulator;
import ij3d.behaviors.Picker;
import java.util.List;
import ij.plugin.Duplicator;
import ij.gui.PointRoi;

/** A mouse listener for point-and-click segmentation and measurement
 *  of pixel clouds in an Image3DUniverse.
 */
public class SegmentationListener implements MouseListener {

	final private Image3DUniverse univ;

	public SegmentationListener(final Image3DUniverse univ) {
		this.univ = univ;
	}

	public void mousePressed(final MouseEvent me) {
		if (me.isConsumed() || Toolbar.getToolId() != Toolbar.WAND) return;
		new Thread() {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				segment(me.getX(), me.getY());
			}
		}.start();
	}

	public void mouseEntered(final MouseEvent me) {}
	public void mouseExited(final MouseEvent me) {}
	public void mouseReleased(final MouseEvent me) {}
	public void mouseClicked(final MouseEvent me) {}

	public boolean segment(final int mouse_x, final int mouse_y) {
		Picker picker = univ.getPicker();
		Content c = picker.getPickedContent(mouse_x, mouse_y);
		if (null == c) return false;
		ImagePlus imp = c.getImage();
		if (null == imp) {
			IJ.log("Cannot segment non-image object!");
			return false; // not a volume
		}
		Point3d point = picker.getPickPointGeometry(c, mouse_x, mouse_y);
		Calibration cal = imp.getCalibration();
		imp = new Duplicator().run(imp);
		imp.setSlice(1 + (int)(point.z / cal.pixelDepth));
		imp.setRoi(new PointRoi((int)(point.x / cal.pixelWidth),
					(int)(point.y / cal.pixelHeight)));
		ImagePlus seg = new LevelSet().execute(imp, false);
		if (null == seg) {
			IJ.log("3D Blob segmentation failed!");
			return false;
		}
		for (int i=seg.getNSlices(); i>0; i--) {
			seg.getStack().getProcessor(i).invert();
		}
		List triangles = new MCTriangulator().getTriangles(seg, 1, new boolean[]{true, true, true}, c.getResamplingFactor());
		String title = c.getName() + "--" + point;
		int num = 1;
		while (univ.contains(title)) {
			title = c.getName() + "--" + point + "-" + num;
			num++;
		}
		Content mesh = univ.createContent(new CustomTriangleMesh(triangles, new Color3f(0, 1, 0), 0), title);
		// TODO should set the transform to that of Content c!
		// TODO but I can't get it from c!
		univ.addContentLater(mesh);

		// TODO measure:
		//  * volume
		//  * surface
		//  * surface/volume ratio (as an indicator of blobiness, i.e. departure from spherical shape).
		return true;
	}
}
