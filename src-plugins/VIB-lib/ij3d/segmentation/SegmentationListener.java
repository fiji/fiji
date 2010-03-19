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
import javax.vecmath.Vector3d;
import javax.vecmath.Point3f;
import customnode.CustomTriangleMesh;
import marchingcubes.MCTriangulator;
import ij3d.behaviors.Picker;
import java.util.List;
import ij.plugin.Duplicator;
import ij.gui.PointRoi;
import java.util.Map;
import ij.gui.Plot;
import java.awt.Color;
import java.util.Iterator;
import ij.measure.ResultsTable;
import ij.WindowManager;

/** A mouse listener for point-and-click segmentation and measurement
 *  of pixel clouds in an Image3DUniverse.
 */
public class SegmentationListener implements MouseListener {

	static public boolean debug = false;

	final private Image3DUniverse univ;

	private ResultsTable rt = null;
	private LevelSet levelsets = null;

	public SegmentationListener(final Image3DUniverse univ) {
		this.univ = univ;
		this.levelsets = new LevelSet();
	}

	public void mousePressed(final MouseEvent me) {
		if (me.isConsumed() || Toolbar.getToolId() != Toolbar.WAND) return;
		new Thread() {
			{ setPriority(Thread.NORM_PRIORITY); }
			public void run() {
				if (me.isShiftDown()) {
					// Setup static parameters
					levelsets.showDialog();
					return;
				}
				// Level sets on first intersecting blob
				Blob blob = segment(me.getX(), me.getY());
				if (null == blob) return;
				// Show segmentation as a mesh
				blob.show();
				// Measure
				double[] m = blob.measure();
				// ... and show a results table:
				String name = blob.c.getName();
				if (null == rt || null == WindowManager.getFrame(name)) {
					rt = new ResultsTable();
					rt.setHeading(0, "Volume");
					rt.setHeading(1, "Surface");
					rt.setHeading(2, "X-center");
					rt.setHeading(3, "Y-center");
					rt.setHeading(4, "Z-center");
				}
				rt.incrementCounter();
				rt.addLabel("units", blob.seg.getCalibration().getUnits());
				for (int i=0; i<m.length; i++)
					rt.addValue(i, m[i]);
				rt.show(name);
			}
		}.start();
	}

	public void mouseEntered(final MouseEvent me) {}
	public void mouseExited(final MouseEvent me) {}
	public void mouseReleased(final MouseEvent me) {}
	public void mouseClicked(final MouseEvent me) {}

	public final class Blob {
		Content c;
		Point3d point;
		ImagePlus seg;
		List<Point3f> triangles;

		Blob(Content c, Point3d point, ImagePlus seg) {
			this.c = c;
			this.point = point;
			this.seg = seg;
		}

		/** Returns the list of Point3f that define the triangles of the mesh.
		 *  Uses the same resampling factor as the source Contents object. */
		public List<Point3f> show() {
			// Create unique title
			String title = c.getName() + "--" + point;
			int num = 1;
			while (univ.contains(title)) {
				title = c.getName() + "--" + point + "-" + num;
				num++;
			}
			List<Point3f> triangles = new MCTriangulator().getTriangles(seg, 1, new boolean[]{true, true, true}, c.getResamplingFactor());
			Content mesh = univ.createContent(new CustomTriangleMesh(triangles, new Color3f(0, 1, 0), 0), title);
			// TODO should set the transform to that of Content c!
			// TODO but I can't get it from c!
			univ.addContentLater(mesh);
			this.triangles = triangles;
			return triangles;
		}

		public double[] measure() {
			if (ImagePlus.GRAY8 != seg.getType()) return null;
			// Measure:
			//  * volume
			//  * surface
			//  * surface/volume ratio (as an indicator of blobiness, i.e. departure from spherical shape).
			//  * center of gravity

			final byte[][] pix = new byte[seg.getNSlices()][];
			if (1 == seg.getNSlices()) {
				pix[0] = (byte[])seg.getProcessor().getPixels();
			} else {
				Object[] ob = seg.getStack().getImageArray();
				for (int i=0; i<pix.length; i++) {
					pix[i] = (byte[])ob[i];
				}
			}

			final int w = seg.getWidth();
			final int h = seg.getHeight();

			int X=0, Y=0, Z=0, N=0;

			for (int z=0; z<pix.length; z++) {
				final byte[] pixels = pix[z];
				for (int y=0; y<h; y++) {
					final int offset = y * w;
					for (int x=0; x<w; x++) {
						byte val = pixels[offset + x];
						if (0 == val) continue;
						N++;
						X += x;
						Y += y;
						Z += z;
					}
				}
			}

			final Calibration cal = seg.getCalibration();

			return new double[]{
				/* Volume */ N * cal.pixelWidth * cal.pixelHeight * cal.pixelDepth,
				/* Surface */ measureSurface(triangles),
				/* Center of gravity */
				X * cal.pixelWidth,
				Y * cal.pixelHeight,
				Z * cal.pixelDepth
			};
		}
	}

	/** Returns the segmented stack. */
	public Blob segment(final int mouse_x, final int mouse_y) {
		Picker picker = univ.getPicker();
		Content c = picker.getPickedContent(mouse_x, mouse_y);
		if (null == c) return null;

		ImagePlus imp = c.getImage();
		if (null == imp) {
			IJ.log("Cannot segment non-image object!");
			return null; // not a volume
		}
		//Point3d point = picker.getPickPointGeometry(c, mouse_x, mouse_y);

		float p = 1;
		Point3d point = null;
		final float transp = c.getTransparency(); // opposite of alpha
		float max_value = -Float.MAX_VALUE;

		final List<Map.Entry<Point3d,Float>> column = picker.getPickPointColumn(c, mouse_x, mouse_y);
		float[] vals = new float[column.size()];
		float[] w = new float[column.size()];
		float[] indices = new float[column.size()];
		int index = 0;
		int next = 0;

		// Find the last non-occluded pixel in the perpendicular ray
		// under the mouse click
		for (final Map.Entry<Point3d,Float> e : column) {
			//System.out.println(e.getValue() + " :: " + e.getKey());
			// Currently alpha and value are the same
			// (i.e. the pixel intensity is used as the inverse of the transparency value)
			final float val = (e.getValue() / 255.0f);
			float alpha = (1 - transp) * val;
			float beta = alpha * p;
			p *= 1 - alpha;
			final float weighted_value = beta * val;
			if (weighted_value > max_value) {
				max_value = weighted_value;
				point = e.getKey();
				index = next;
			}
			vals[next] = val;
			w[next] = weighted_value;
			indices[next] = next;
			next++;
		}

		// Search for the next local maximum
		float max = column.get(index).getValue();
		for (final Map.Entry<Point3d,Float> e : column.subList(index+1, column.size())) {
			if (e.getValue() > max) {
				max = e.getValue();
				index++;
			} else {
				point = column.get(index).getKey();
				break;
			}
		}

		if (debug) {
			//Plots:
			Plot plot = new Plot("ray", "depth", "amount", indices, vals);
			plot.setColor(Color.red);
			plot.addPoints(indices, w, Plot.DOT);
			plot.show();
		}

		Calibration cal = imp.getCalibration();
		imp = new Duplicator().run(imp);
		imp.setSlice(1 + (int)(point.z / cal.pixelDepth));
		imp.setRoi(new PointRoi((int)(point.x / cal.pixelWidth),
					(int)(point.y / cal.pixelHeight)));

		if (debug) imp.show();

		ImagePlus seg = levelsets.execute(imp, false);
		seg.setCalibration(imp.getCalibration());

		if (debug) seg.show();

		if (null == seg) {
			IJ.log("3D Blob segmentation failed!");
			return null;
		}
		for (int i=seg.getNSlices(); i>0; i--) {
			seg.getStack().getProcessor(i).invert();
		}
		return new Blob(c, point, seg);
	}


	static public final double measureSurface(final List<Point3f> triangles) {
		if (0 != triangles.size() % 3) {
			IJ.log("Could not measure surface: triangle list is not a multiple of 3.");
			return 0;
		}
		double s = 0;
		final Iterator<Point3f> it = triangles.iterator();
		while (it.hasNext())
			s += measureArea(it.next(), it.next(), it.next());
		return s;
	}

	/** Compute the area of the triangle defined by 3 points in 3D space, returning half of the length of the vector resulting from the cross product of vectors p1p2 and p1p3. */
	static public final double measureArea(final Point3f p1, final Point3f p2, final Point3f p3) {
		// Distance from p1 to line p2-p3, times length of line p2-p3, divided by 2:
		return 0.5 * distancePointToLine(p1.x, p1.y, p1.z,
						 p2.x, p2.y, p2.z,
						 p3.x, p3.y, p3.z)
			   * p2.distance(p3);
	}

	static public double distancePointToLine(final double px, final double py, final double pz, final double lx1, final double ly1, final double lz1, final double lx2, final double ly2, final double lz2 ) {
		final double segment_length = new Vector3d(lx2 - lx1, ly2 - ly1, lz2 - lz1).length();
		if (0 == segment_length) return 0;
		final Vector3d cross = new Vector3d();
		cross.cross(new Vector3d(px - lx1, py - ly1, pz - lz1),
			    new Vector3d(px - lx2, py - ly2, pz - lz2));
		return cross.length() / segment_length;
	}
}
