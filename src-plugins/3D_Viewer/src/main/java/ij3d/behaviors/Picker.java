package ij3d.behaviors;

import com.sun.j3d.utils.pickfast.PickCanvas;
import com.sun.j3d.utils.pickfast.PickTool;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij3d.Volume;
import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import voltex.VoltexGroup;
import java.awt.event.MouseEvent;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PickInfo;
import javax.media.j3d.SceneGraphPath;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import vib.BenesNamedPoint;
import vib.PointList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * This class is a helper class which implements functions for picking.
 *
 * @author Benjamin Schmid
 */
public class Picker {
	private DefaultUniverse univ;
	private ImageCanvas3D canvas;

	/**
	 * Constructs a new Picker
	 * @param univ
	 */
	public Picker(DefaultUniverse univ) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D)univ.getCanvas();
	}

	/**
	 * Deletes a landmark point of the specified Content at the given mouse
	 * position
	 * @param c
	 * @param e
	 */
	public void deletePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		int ind = pl.indexOfPointAt(p3d.x, p3d.y, p3d.z, tol);
		if(ind != -1) {
			pl.remove(ind);
		}
	}

	private int movingIndex = -1;

	/**
	 * Moves the picked landmark point to the position specified by the
	 * MouseEvent.
	 * @param c
	 * @param e
	 */
	public synchronized void movePoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;

		PointList pl = c.getPointList();
		if(movingIndex == -1)
			movingIndex = pl.indexOfPointAt(
					p3d.x, p3d.y, p3d.z, c.getLandmarkPointSize());
		if(movingIndex != -1) {
			pl.placePoint(pl.get(movingIndex), p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Stop moving.
	 */
	public synchronized void stopMoving() {
		movingIndex = -1;
	}

	/**
	 * Adds a landmark point specfied by the canvas position
	 * @param c
	 * @param x position in the canvas
	 * @param y position in the canvas
	 */
	public void addPoint(Content c, int x, int y) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, x, y);
		if(p3d == null)
			return;
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if(bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Adds a landmark point specfied by the position of the MouseEvent.
	 * @param c
	 * @param e
	 */
	public void addPoint(Content c, MouseEvent e) {
		if(c == null) {
			IJ.error("Selection required");
			return;
		}
		Point3d p3d = getPickPointGeometry(c, e);
		if(p3d == null)
			return;
		PointList pl = c.getPointList();
		float tol = c.getLandmarkPointSize();
		BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if(bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Get the picked point using geometry picking. The pick line is specified
	 * by the given Point3d and Vector3d.
	 * @param c
	 * @param origin
	 * @param dir
	 * @return
	 */
	public Point3d getPickPointGeometry(Content c, Point3d origin, Vector3d dir) {
		PickTool pickTool = new PickTool(c);
		pickTool.setShapeRay(origin, dir);

		pickTool.setMode(PickInfo.PICK_GEOMETRY);
		pickTool.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		try {
			PickInfo[] result = pickTool.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			for(int i = 0; i < result.length; i++) {
				Point3d intersection = result[i].getClosestIntersectionPoint();
				if(c.getType() != Content.VOLUME)
					return intersection;

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}

	/**
	 * Get the picked point, using geometry picking, for the specified
	 * canvas position.
	 * @param c
	 * @param e
	 * @return
	 */
	public Point3d getPickPointGeometry(Content c, MouseEvent e) {
		return getPickPointGeometry(c, e.getX(), e.getY());
	}

	/**
	 * Get the picked point, using geometry picking, for the specified
	 * canvas position.
	 * @param c
	 * @param e
	 * @return
	 */
	public Point3d getPickPointGeometry(Content c, int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, c);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			for(int i = 0; i < result.length; i++) {
				Point3d intersection = result[i].getClosestIntersectionPoint();
				if(c.getType() != Content.VOLUME)
					return intersection;

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return intersection;
			}
			return null;
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public List<Map.Entry<Point3d,Float>> getPickPointColumn(final Content c, final int x, final int y) {
		if (c.getType() != Content.VOLUME) return null;
		PickCanvas pickCanvas = new PickCanvas(canvas, c);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null || result.length == 0)
				return null;

			final ArrayList<Map.Entry<Point3d,Float>> list = new ArrayList<Map.Entry<Point3d,Float>>();
			for(int i = 0; i < result.length; i++) {
				final Point3d intersection = result[i].getClosestIntersectionPoint();
				list.add(new Map.Entry<Point3d,Float>() {
					public Point3d getKey() { return intersection; }
					public Float getValue() { return getVolumePoint(c, intersection); }
					public Float setValue(final Float f) { throw new UnsupportedOperationException(); }
				});
			}
			return list;
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public int[] getPickedVertexIndices(BranchGroup bg, int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(univ.getCanvas(), bg);
		pickCanvas.setTolerance(3f);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_GEOM_INFO);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo result = pickCanvas.pickClosest();
			if(result == null)
				return null;

			PickInfo.IntersectionInfo info =
					result.getIntersectionInfos()[0];
			return info.getVertexIndices();
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the Content at the specified canvas position
	 * @param x
	 * @param y
	 * @return
	 */
	public Content getPickedContent(int x, int y) {
		PickCanvas pickCanvas = new PickCanvas(canvas, univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.SCENEGRAPHPATH | PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3);
		pickCanvas.setShapeLocation(x, y);
		try {
			PickInfo[] result = pickCanvas.pickAllSorted();
			if(result == null)
				return null;
			for(int i = 0; i < result.length; i++) {
				SceneGraphPath path = result[i].getSceneGraphPath();
				Content c = null;
				for(int j = path.nodeCount()-1; j >= 0; j--)
					if(path.getNode(j) instanceof Content)
						c = (Content)path.getNode(j);

				if(c == null)
					continue;

				if(c.getType() != Content.VOLUME
					&& c.getType() != Content.ORTHO)
					return c;

				Point3d intersection = result[i].getClosestIntersectionPoint();

				float v = getVolumePoint(c, intersection);
				if(v > 20)
					return c;
			}
			return null;
		} catch(Exception ex) {
			return null;
		}
	}


	private static float getVolumePoint(Content c, Point3d p) {

		Volume v = ((VoltexGroup)c.getContent()).getRenderer().
				getVolume();

		int ix = (int)Math.round(p.x / v.pw);
		int iy = (int)Math.round(p.y / v.ph);
		int iz = (int)Math.round(p.z / v.pd);
		if(ix < 0 || ix >= v.xDim ||
			iy < 0 || iy >= v.yDim ||
			iz < 0 || iz >= v.zDim)
			return 0;
		return (v.getAverage(ix, iy, iz) & 0xff);
	}
}
